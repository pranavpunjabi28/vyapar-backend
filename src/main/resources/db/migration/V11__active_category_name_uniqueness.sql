alter table category
    drop constraint if exists category_outlet_id_name_key;

create unique index uq_category_outlet_active_name
    on category (outlet_id, lower(name))
    where archived = false;
