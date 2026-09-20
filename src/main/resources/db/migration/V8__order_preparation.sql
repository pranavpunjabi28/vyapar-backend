alter table sales_order
    add column prepared_at bigint;

create index idx_sales_order_outlet_status_prepared
    on sales_order (outlet_id, status, prepared_at, order_number);
