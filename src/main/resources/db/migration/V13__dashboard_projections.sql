alter table sales_order
    add column dashboard_revision bigint not null default 0;

create table dashboard_event
(
    id                varchar(40) primary key,
    version           bigint       not null,
    created_at        bigint       not null,
    updated_at        bigint       not null,
    archived          boolean      not null default false,
    outlet_id         varchar(40)  not null references outlet (id),
    order_id          varchar(40)  not null references sales_order (id),
    event_type        varchar(40)  not null,
    order_revision    bigint       not null,
    idempotency_key   varchar(100) not null,
    processed_at      bigint,
    attempt_count     integer      not null default 0,
    next_attempt_at   bigint       not null,
    last_failure_code varchar(100),
    constraint uk_dashboard_event_idempotency unique (idempotency_key)
);

create index idx_dashboard_event_pending
    on dashboard_event (next_attempt_at, created_at, id)
    where processed_at is null;
create index idx_dashboard_event_outlet_pending
    on dashboard_event (outlet_id, processed_at, created_at);
create index idx_dashboard_event_order_revision
    on dashboard_event (order_id, order_revision);

create table outlet_order_summary_contribution
(
    id                    varchar(40) primary key,
    version               bigint         not null,
    created_at            bigint         not null,
    updated_at            bigint         not null,
    archived              boolean        not null default false,
    order_id              varchar(40)    not null references sales_order (id),
    outlet_id             varchar(40)    not null references outlet (id),
    business_day_start_at bigint         not null,
    applied_revision      bigint         not null,
    received_orders       bigint         not null,
    completed_orders      bigint         not null,
    cancelled_orders      bigint         not null,
    gross_sales           numeric(19, 4) not null,
    discount_amount       numeric(19, 4) not null,
    refund_amount         numeric(19, 4) not null,
    net_sales             numeric(19, 4) not null,
    unpaid_amount         numeric(19, 4) not null,
    constraint uk_order_summary_contribution_order unique (order_id)
);

create index idx_order_contribution_outlet_day
    on outlet_order_summary_contribution (outlet_id, business_day_start_at);

create table outlet_daily_summary
(
    id                    varchar(40) primary key,
    version               bigint         not null,
    created_at            bigint         not null,
    updated_at            bigint         not null,
    archived              boolean        not null default false,
    outlet_id             varchar(40)    not null references outlet (id),
    business_day_start_at bigint         not null,
    received_orders       bigint         not null,
    completed_orders      bigint         not null,
    cancelled_orders      bigint         not null,
    gross_sales           numeric(19, 4) not null,
    discount_amount       numeric(19, 4) not null,
    refund_amount         numeric(19, 4) not null,
    net_sales             numeric(19, 4) not null,
    unpaid_amount         numeric(19, 4) not null,
    constraint uk_outlet_daily_summary_day unique (outlet_id, business_day_start_at)
);

create index idx_outlet_daily_summary_range
    on outlet_daily_summary (outlet_id, business_day_start_at);

-- Seed projections from existing orders so upgrading databases immediately retain their dashboard history.
with refund_totals as
         (select r.order_id, coalesce(sum(r.amount), 0) as refund_amount
          from refund r
          where r.archived = false
          group by r.order_id),
     source as
         (select o.id                                                                       as order_id,
                 o.outlet_id,
                 o.dashboard_revision,
                 (extract(epoch from ((date_trunc('day', to_timestamp(o.created_at / 1000.0)
                     at time zone ot.timezone)) at time zone ot.timezone)) * 1000)::bigint     as business_day_start_at,
                 case when o.status <> 'DRAFT' then 1 else 0 end                             as received_orders,
                 case when o.status = 'CLOSED' then 1 else 0 end                             as completed_orders,
                 case when o.status = 'CANCELLED' then 1 else 0 end                          as cancelled_orders,
                 case when o.status = 'CLOSED' then o.total else 0 end                       as gross_sales,
                 case when o.status = 'CLOSED' then o.discount_amount else 0 end             as discount_amount,
                 case when o.status = 'CLOSED' then coalesce(r.refund_amount, 0) else 0 end   as refund_amount,
                 case when o.status = 'CLOSED' then o.total - coalesce(r.refund_amount, 0)
                      else 0 end                                                             as net_sales,
                 case when o.status not in ('DRAFT', 'CANCELLED') then o.due_amount else 0 end as unpaid_amount
          from sales_order o
                   join outlet ot on ot.id = o.outlet_id
                   left join refund_totals r on r.order_id = o.id
          where o.archived = false)
insert
into outlet_order_summary_contribution
    (id, version, created_at, updated_at, archived, order_id, outlet_id, business_day_start_at,
     applied_revision, received_orders, completed_orders, cancelled_orders, gross_sales, discount_amount,
     refund_amount, net_sales, unpaid_amount)
select 'ordercontrib_' || substring(md5(order_id) from 1 for 20),
       0,
       (extract(epoch from clock_timestamp()) * 1000)::bigint,
       (extract(epoch from clock_timestamp()) * 1000)::bigint,
       false,
       order_id,
       outlet_id,
       business_day_start_at,
       dashboard_revision,
       received_orders,
       completed_orders,
       cancelled_orders,
       gross_sales,
       discount_amount,
       refund_amount,
       net_sales,
       unpaid_amount
from source;

insert into outlet_daily_summary
    (id, version, created_at, updated_at, archived, outlet_id, business_day_start_at, received_orders,
     completed_orders, cancelled_orders, gross_sales, discount_amount, refund_amount, net_sales, unpaid_amount)
select 'daysummary_' || substring(md5(outlet_id || ':' || business_day_start_at::text) from 1 for 20),
       0,
       (extract(epoch from clock_timestamp()) * 1000)::bigint,
       (extract(epoch from clock_timestamp()) * 1000)::bigint,
       false,
       outlet_id,
       business_day_start_at,
       sum(received_orders),
       sum(completed_orders),
       sum(cancelled_orders),
       sum(gross_sales),
       sum(discount_amount),
       sum(refund_amount),
       sum(net_sales),
       sum(unpaid_amount)
from outlet_order_summary_contribution
where archived = false
group by outlet_id, business_day_start_at;
