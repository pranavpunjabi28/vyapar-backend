alter table product_image
    drop constraint if exists product_image_object_key_key;

create index idx_product_image_object_key_archived
    on product_image (object_key, archived);
