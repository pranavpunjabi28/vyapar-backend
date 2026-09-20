create table order_item_addon
(
    id              varchar(40) primary key,
    version         bigint         not null,
    created_at      bigint         not null,
    updated_at      bigint         not null,
    archived        boolean        not null default false,
    order_item_id   varchar(40)    not null references order_item (id),
    addon_group_id  varchar(40)    not null references addon_group (id),
    addon_option_id varchar(40)    not null references addon_option (id),
    group_name      varchar(255)   not null,
    option_name     varchar(255)   not null,
    unit_price      numeric(19, 4) not null,
    unique (order_item_id, addon_option_id)
);

create index idx_order_item_addon_item_archived
    on order_item_addon (order_item_id, archived, created_at, id);
