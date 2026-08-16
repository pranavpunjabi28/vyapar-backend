package com.bbu.vyaparbackend.shared;

public final class DbFields {
    public static final String BUSINESS_ID = "business_id";
    public static final String OUTLET_ID = "outlet_id";
    public static final String INGREDIENT_ID = "ingredient_id";
    public static final String PRODUCT_ID = "product_id";
    public static final String USER_ID = "user_id";
    public static final String MEMBERSHIP_ID = "membership_id";
    public static final String INVITATION_ID = "invitation_id";
    public static final String CREATED_AT = "created_at";
    public static final String CLOSED_AT = "closed_at";
    public static final String NAME = "name";
    public static final String PHONE = "phone";
    public static final String INVOICE_NUMBER = "invoice_number";

    public static final class Tables {
        public static final String APP_USER = "app_user";
        public static final String BUSINESS = "business";
        public static final String SALES_ORDER = "sales_order";
        public static final String STAFF_INVITATION_OUTLET = "staff_invitation_outlet";

        private Tables() {
        }
    }

    public static final class Indexes {
        public static final String CUSTOMER_BUSINESS_PHONE = "idx_customer_business_phone";
        public static final String MOVEMENT_STOCK = "idx_movement_stock";
        public static final String ORDER_OUTLET_CLOSED = "idx_order_outlet_closed";
        public static final String ORDER_INVOICE = "idx_order_invoice";

        private Indexes() {
        }
    }

    private DbFields() {
    }
}
