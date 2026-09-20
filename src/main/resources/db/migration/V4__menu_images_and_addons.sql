create table product_image
(
    id            varchar(40) primary key,
    version       bigint       not null,
    created_at    bigint       not null,
    updated_at    bigint       not null,
    archived      boolean      not null default false,
    product_id    varchar(40)  not null references product (id),
    object_key    varchar(255) not null unique,
    display_order integer      not null
);

insert into product_image (id, version, created_at, updated_at, archived, product_id, object_key, display_order)
select 'productimage_' || substr(replace(gen_random_uuid()::text, '-', ''), 1, 20),
       0,
       created_at,
       updated_at,
       false,
       id,
       image_key,
       0
from product
where image_key is not null
  and image_key <> '';

alter table product drop column image_key;

create table addon_group
(
    id                 varchar(40) primary key,
    version            bigint       not null,
    created_at         bigint       not null,
    updated_at         bigint       not null,
    archived           boolean      not null default false,
    outlet_id          varchar(40)  not null references outlet (id),
    name               varchar(255) not null,
    display_order      integer      not null,
    maximum_selections integer      not null,
    unique (outlet_id, name)
);

create table addon_option
(
    id             varchar(40) primary key,
    version        bigint         not null,
    created_at     bigint         not null,
    updated_at     bigint         not null,
    archived       boolean        not null default false,
    addon_group_id varchar(40)    not null references addon_group (id),
    name           varchar(255)   not null,
    price          numeric(19, 4) not null,
    display_order  integer        not null,
    active         boolean        not null
);

create table product_addon_group
(
    id             varchar(40) primary key,
    version        bigint      not null,
    created_at     bigint      not null,
    updated_at     bigint      not null,
    archived       boolean     not null default false,
    product_id     varchar(40) not null references product (id),
    addon_group_id varchar(40) not null references addon_group (id),
    unique (product_id, addon_group_id)
);

create index idx_product_outlet_category_archived_name
    on product (outlet_id, category_id, archived, name, id);
create index idx_product_image_product_archived_order
    on product_image (product_id, archived, display_order, id);
create index idx_addon_group_outlet_archived_order
    on addon_group (outlet_id, archived, display_order, name);
create index idx_addon_option_group_archived_order
    on addon_option (addon_group_id, archived, display_order, name);
create index idx_product_addon_product_archived
    on product_addon_group (product_id, archived, addon_group_id);
