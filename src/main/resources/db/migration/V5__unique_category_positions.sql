with ranked_categories as (
    select id,
           row_number() over (
               partition by outlet_id
               order by display_order, created_at, id
           ) - 1 as normalized_display_order
    from category category
    where archived = false
)
update category
set display_order = ranked_categories.normalized_display_order
from ranked_categories
where category.id = ranked_categories.id;

create unique index uq_category_outlet_active_display_order
    on category (outlet_id, display_order)
    where archived = false;
