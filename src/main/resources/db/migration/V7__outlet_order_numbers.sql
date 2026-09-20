alter table outlet
    add column next_order_number bigint not null default 1;

alter table sales_order
    add column order_number bigint;

with numbered_orders as (
    select id,
           row_number() over (
               partition by outlet_id
               order by created_at, id
           ) as assigned_order_number
    from sales_order
)
update sales_order
set order_number = numbered_orders.assigned_order_number
from numbered_orders
where sales_order.id = numbered_orders.id;

update outlet
set next_order_number = coalesce(
        (select max(sales_order.order_number) + 1
         from sales_order
         where sales_order.outlet_id = outlet.id),
        1
    );

alter table sales_order
    alter column order_number set not null;

create unique index uq_sales_order_outlet_order_number
    on sales_order (outlet_id, order_number);
