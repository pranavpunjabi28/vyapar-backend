create table app_user
(
    id                       varchar(40) primary key,
    version                  bigint       not null,
    created_at               bigint  not null,
    updated_at               bigint  not null,
    archived                 boolean      not null default false,
    email                    varchar(320) not null unique,
    password_hash            varchar(255) not null,
    display_name             varchar(120) not null,
    password_change_required boolean      not null default false
);
create table refresh_token
(
    id         varchar(40) primary key,
    version    bigint      not null,
    created_at bigint not null,
    updated_at bigint not null,
    archived   boolean     not null default false,
    user_id    varchar(40)        not null references app_user (id),
    token_hash varchar(64) not null unique,
    expires_at bigint not null,
    revoked_at bigint
);
create table business
(
    id         varchar(40) primary key,
    version    bigint       not null,
    created_at bigint  not null,
    updated_at bigint  not null,
    archived   boolean      not null default false,
    name       varchar(255) not null,
    legal_name varchar(255),
    phone      varchar(255),
    gstin      varchar(255),
    fssai      varchar(255),
    logo_key   varchar(255)
);
create table outlet
(
    id                  varchar(40) primary key,
    version             bigint       not null,
    created_at          bigint  not null,
    updated_at          bigint  not null,
    archived            boolean      not null default false,
    business_id         varchar(40)         not null references business (id),
    name                varchar(255) not null,
    phone               varchar(255),
    address             varchar(1000),
    currency            varchar(3)   not null,
    timezone            varchar(255) not null,
    upi_id              varchar(255),
    receipt_footer      varchar(500),
    next_invoice_number bigint       not null,
    unique (business_id, name)
);
create table membership
(
    id          varchar(40) primary key,
    version     bigint      not null,
    created_at  bigint not null,
    updated_at  bigint not null,
    archived    boolean     not null default false,
    business_id varchar(40)        not null references business (id),
    user_id     varchar(40)        not null references app_user (id),
    role        varchar(30) not null,
    unique (business_id, user_id)
);
create table outlet_assignment
(
    id            varchar(40) primary key,
    version       bigint      not null,
    created_at    bigint not null,
    updated_at    bigint not null,
    archived      boolean     not null default false,
    membership_id varchar(40)        not null references membership (id),
    outlet_id     varchar(40)        not null references outlet (id),
    unique (membership_id, outlet_id)
);
create table staff_invitation
(
    id          varchar(40) primary key,
    version     bigint       not null,
    created_at  bigint  not null,
    updated_at  bigint  not null,
    archived    boolean      not null default false,
    business_id varchar(40)         not null references business (id),
    email       varchar(320) not null,
    role        varchar(30)  not null,
    token_hash  varchar(64)  not null unique,
    expires_at  bigint  not null,
    accepted_at bigint
);
create table staff_invitation_outlet
(
    invitation_id varchar(40) not null references staff_invitation (id),
    outlet_id     varchar(40) not null references outlet (id),
    primary key (invitation_id, outlet_id)
);
create table category
(
    id            varchar(40) primary key,
    version       bigint       not null,
    created_at    bigint  not null,
    updated_at    bigint  not null,
    archived      boolean      not null default false,
    outlet_id     varchar(40)         not null references outlet (id),
    name          varchar(255) not null,
    display_order integer      not null,
    unique (outlet_id, name)
);
create table product
(
    id          varchar(40) primary key,
    version     bigint         not null,
    created_at  bigint    not null,
    updated_at  bigint    not null,
    archived    boolean        not null default false,
    outlet_id   varchar(40)           not null references outlet (id),
    category_id varchar(40) references category (id),
    name        varchar(255)   not null,
    sku         varchar(255),
    description varchar(1000),
    price       numeric(19, 4) not null,
    image_key   varchar(255),
    active      boolean        not null
);
create table ingredient
(
    id                  varchar(40) primary key,
    version             bigint         not null,
    created_at          bigint    not null,
    updated_at          bigint    not null,
    archived            boolean        not null default false,
    outlet_id           varchar(40)           not null references outlet (id),
    name                varchar(255)   not null,
    unit                varchar(20)    not null,
    low_stock_threshold numeric(19, 4) not null,
    unique (outlet_id, name)
);
create table recipe_component
(
    id            varchar(40) primary key,
    version       bigint         not null,
    created_at    bigint    not null,
    updated_at    bigint    not null,
    archived      boolean        not null default false,
    product_id    varchar(40)           not null references product (id),
    ingredient_id varchar(40)           not null references ingredient (id),
    quantity      numeric(19, 4) not null,
    unique (product_id, ingredient_id)
);
create table customer
(
    id          varchar(40) primary key,
    version     bigint       not null,
    created_at  bigint  not null,
    updated_at  bigint  not null,
    archived    boolean      not null default false,
    business_id varchar(40)         not null references business (id),
    name        varchar(255) not null,
    phone       varchar(255),
    email       varchar(255),
    address     varchar(1000)
);
create index idx_customer_business_phone on customer (business_id, phone);
create table supplier
(
    id         varchar(40) primary key,
    version    bigint       not null,
    created_at bigint  not null,
    updated_at bigint  not null,
    archived   boolean      not null default false,
    outlet_id  varchar(40)         not null references outlet (id),
    name       varchar(255) not null,
    phone      varchar(255),
    email      varchar(255),
    gstin      varchar(255),
    address    varchar(1000)
);
create table purchase
(
    id               varchar(40) primary key,
    version          bigint      not null,
    created_at       bigint not null,
    updated_at       bigint not null,
    archived         boolean     not null default false,
    outlet_id        varchar(40)        not null references outlet (id),
    supplier_id      varchar(40) references supplier (id),
    status           varchar(30) not null,
    purchased_at     bigint not null,
    reference_number varchar(255),
    note             varchar(1000)
);
create table purchase_item
(
    id            varchar(40) primary key,
    version       bigint         not null,
    created_at    bigint    not null,
    updated_at    bigint    not null,
    archived      boolean        not null default false,
    purchase_id   varchar(40)           not null references purchase (id),
    ingredient_id varchar(40)           not null references ingredient (id),
    quantity      numeric(19, 4) not null,
    unit_cost     numeric(19, 4) not null
);
create table inventory_movement
(
    id             varchar(40) primary key,
    version        bigint         not null,
    created_at     bigint    not null,
    updated_at     bigint    not null,
    archived       boolean        not null default false,
    outlet_id      varchar(40)           not null references outlet (id),
    ingredient_id  varchar(40)           not null references ingredient (id),
    type           varchar(40)    not null,
    quantity       numeric(19, 4) not null,
    reference_type varchar(40)    not null,
    reference_id   varchar(40)           not null,
    note           varchar(500)
);
create index idx_movement_stock on inventory_movement (outlet_id, ingredient_id, created_at);
create table sales_order
(
    id              varchar(40) primary key,
    version         bigint         not null,
    created_at      bigint    not null,
    updated_at      bigint    not null,
    archived        boolean        not null default false,
    outlet_id       varchar(40)           not null references outlet (id),
    customer_id     varchar(40) references customer (id),
    status          varchar(30)    not null,
    payment_status  varchar(40)    not null,
    table_reference varchar(255),
    invoice_number  varchar(255),
    discount_type   varchar(30)    not null,
    discount_value  numeric(19, 4) not null,
    subtotal        numeric(19, 4) not null,
    discount_amount numeric(19, 4) not null,
    total           numeric(19, 4) not null,
    paid_amount     numeric(19, 4) not null,
    due_amount      numeric(19, 4) not null,
    closed_at       bigint,
    cancelled_at    bigint,
    unique (outlet_id, invoice_number)
);
create index idx_order_outlet_closed on sales_order (outlet_id, closed_at);
create table order_item
(
    id           varchar(40) primary key,
    version      bigint         not null,
    created_at   bigint    not null,
    updated_at   bigint    not null,
    archived     boolean        not null default false,
    order_id     varchar(40)           not null references sales_order (id),
    product_id   varchar(40)           not null references product (id),
    product_name varchar(255)   not null,
    unit_price   numeric(19, 4) not null,
    quantity     numeric(19, 4) not null,
    line_total   numeric(19, 4) not null,
    note         varchar(500)
);
create table payment
(
    id            varchar(40) primary key,
    version       bigint         not null,
    created_at    bigint    not null,
    updated_at    bigint    not null,
    archived      boolean        not null default false,
    order_id      varchar(40)           not null references sales_order (id),
    method        varchar(30)    not null,
    amount        numeric(19, 4) not null,
    custom_method varchar(255),
    reference     varchar(255)
);
create table refund
(
    id            varchar(40) primary key,
    version       bigint         not null,
    created_at    bigint    not null,
    updated_at    bigint    not null,
    archived      boolean        not null default false,
    order_id      varchar(40)           not null references sales_order (id),
    created_by_id varchar(40)           not null references app_user (id),
    amount        numeric(19, 4) not null,
    restore_stock boolean        not null,
    reason        varchar(500)   not null
);
create table refund_item
(
    id            varchar(40) primary key,
    version       bigint         not null,
    created_at    bigint    not null,
    updated_at    bigint    not null,
    archived      boolean        not null default false,
    refund_id     varchar(40)           not null references refund (id),
    order_item_id varchar(40)           not null references order_item (id),
    quantity      numeric(19, 4) not null
);
create index idx_order_status on sales_order (outlet_id, status, created_at);
create index idx_product_search on product (outlet_id, name);
create index idx_ingredient_search on ingredient (outlet_id, name);

create index idx_order_outlet_archived_created
    on sales_order (outlet_id, archived, created_at desc, id);
create index idx_order_outlet_status_closed
    on sales_order (outlet_id, status, closed_at, id);
create index idx_order_outlet_customer_created
    on sales_order (outlet_id, customer_id, created_at desc, id);
create index idx_payment_order_archived
    on payment (order_id, archived);
create index idx_payment_method_order
    on payment (method, order_id) where archived = false;
create index idx_order_item_order_archived
    on order_item (order_id, archived);
create index idx_order_item_product_archived
    on order_item (product_id, archived);
create index idx_inventory_outlet_ingredient_type
    on inventory_movement (outlet_id, ingredient_id, archived, type);
create index idx_ingredient_outlet_archived_name
    on ingredient (outlet_id, archived, name, id);
create index idx_product_outlet_archived_name
    on product (outlet_id, archived, name, id);
create index idx_supplier_outlet_archived_name
    on supplier (outlet_id, archived, name, id);
create index idx_purchase_outlet_archived_date
    on purchase (outlet_id, archived, purchased_at desc, id);
