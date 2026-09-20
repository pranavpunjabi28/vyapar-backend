alter table outlet
    add column preparing_order_cancellation_policy varchar(30) not null default 'ALWAYS',
    add column preparing_order_cancellation_minutes integer not null default 10;

alter table outlet
    add constraint ck_outlet_preparing_cancellation_minutes
        check (preparing_order_cancellation_minutes between 1 and 1440);
