alter table sales_order
    add column ready_at bigint;

create index idx_sales_order_outlet_status_ready
    on sales_order (outlet_id, status, ready_at, order_number);
