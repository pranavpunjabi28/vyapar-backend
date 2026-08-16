package com.bbu.vyaparbackend.shared;

public final class ApiEndpoints {
    public static final String API_V1 = "/api/v1";

    private ApiEndpoints() {
    }

    public static final class Auth {
        public static final String ROOT = API_V1 + "/auth";
        public static final String REGISTER = "/register";
        public static final String LOGIN = "/login";
        public static final String REFRESH = "/refresh";
        public static final String LOGOUT = "/logout";
        public static final String ME = "/me";
        public static final String PASSWORD = "/password";
        public static final String INVITATIONS_PUBLIC = API_V1 + "/auth/invitations/**";

        private Auth() {
        }
    }

    public static final class Customer {
        public static final String ROOT = API_V1 + "/businesses/{businessId}/customers";
        public static final String BY_ID = "/{id}";

        private Customer() {
        }
    }

    public static final class Business {
        public static final String ROOT = API_V1;
        public static final String BUSINESSES = "/businesses";
        public static final String BUSINESS = "/businesses/{id}";
        public static final String OUTLETS = "/businesses/{id}/outlets";
        public static final String OUTLET = "/outlets/{id}";
        public static final String INVITATIONS = "/businesses/{id}/invitations";
        public static final String STAFF = "/businesses/{id}/staff";
        public static final String STAFF_MEMBER = "/businesses/{id}/staff/{membershipId}";
        public static final String ACCEPT_INVITATION = "/auth/invitations/{token}/accept";

        private Business() {
        }
    }

    public static final class Outlet {
        public static final String ROOT = API_V1 + "/outlets/{outletId}";

        private Outlet() {
        }
    }

    public static final class Catalog {
        public static final String ROOT = Outlet.ROOT;
        public static final String CATEGORIES = "/categories";
        public static final String CATEGORY = "/categories/{id}";
        public static final String PRODUCTS = "/products";
        public static final String PRODUCT = "/products/{id}";
        public static final String INGREDIENTS = "/ingredients";
        public static final String INGREDIENT = "/ingredients/{id}";
        public static final String RECIPE = "/products/{id}/recipe";

        private Catalog() {
        }
    }

    public static final class Inventory {
        public static final String ROOT = Outlet.ROOT;
        public static final String SUPPLIERS = "/suppliers";
        public static final String SUPPLIER = "/suppliers/{id}";
        public static final String PURCHASES = "/purchases";
        public static final String PURCHASE = "/purchases/{id}";
        public static final String POST_PURCHASE = "/purchases/{id}/post";
        public static final String CANCEL_PURCHASE = "/purchases/{id}/cancel";
        public static final String ADJUSTMENTS = "/inventory/adjustments";
        public static final String BALANCE = "/inventory/{ingredientId}/balance";
        public static final String MOVEMENTS = "/inventory/{ingredientId}/movements";

        private Inventory() {
        }
    }

    public static final class Order {
        public static final String ROOT = Outlet.ROOT + "/orders";
        public static final String BY_ID = "/{id}";
        public static final String HOLD = "/{id}/hold";
        public static final String CHECKOUT = "/{id}/checkout";
        public static final String PAYMENTS = "/{id}/payments";
        public static final String CANCEL = "/{id}/cancel";
        public static final String REFUNDS = "/{id}/refunds";
        public static final String RECEIPT_ROOT = Outlet.ROOT + "/orders/{orderId}";
        public static final String RECEIPT = "/receipt";
        public static final String RECEIPT_PDF = "/receipt.pdf";

        private Order() {
        }
    }

    public static final class Report {
        public static final String ROOT = Outlet.ROOT + "/reports";
        public static final String ORDERS = "/orders";
        public static final String DASHBOARD = "/dashboard";
        public static final String ITEMS = "/items";
        public static final String STOCK = "/stock";
        public static final String ORDERS_CSV = "/orders.csv";
        public static final String ITEMS_CSV = "/items.csv";
        public static final String STOCK_CSV = "/stock.csv";

        private Report() {
        }
    }

    public static final class Media {
        public static final String ROOT = API_V1;
        public static final String BUSINESS_LOGO = "/businesses/{businessId}/logo";
        public static final String PRODUCT_IMAGE = "/outlets/{outletId}/products/{productId}/image";

        private Media() {
        }
    }

    public static final class Documentation {
        public static final String SWAGGER = "/swagger-ui/**";
        public static final String SWAGGER_HTML = "/swagger-ui.html";
        public static final String OPEN_API = "/v3/api-docs/**";
        public static final String HEALTH = "/actuator/health";

        private Documentation() {
        }
    }
}
