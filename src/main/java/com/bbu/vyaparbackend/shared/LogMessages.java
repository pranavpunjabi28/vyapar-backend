package com.bbu.vyaparbackend.shared;

public final class LogMessages {
    public static final String HTTP_COMPLETED = "HTTP request completed method={} path={} status={} durationMs={} queryParameters={} requestBody={} responseBody={}";
    public static final String HTTP_FAILED = "HTTP request failed method={} path={} status={} durationMs={}";
    public static final String AUTH_REGISTERED = "User registered userId={}";
    public static final String AUTH_LOGIN_SUCCEEDED = "User login succeeded userId={}";
    public static final String AUTH_LOGIN_FAILED = "User login rejected reason=invalid_credentials";
    public static final String AUTH_REFRESHED = "User session refreshed userId={}";
    public static final String AUTH_LOGGED_OUT = "User session logged out userId={}";
    public static final String AUTH_PASSWORD_CHANGED = "User password changed userId={}";
    public static final String BUSINESS_CREATED = "Business created businessId={} ownerId={}";
    public static final String BUSINESS_UPDATED = "Business updated businessId={}";
    public static final String BUSINESS_ARCHIVED = "Business archived businessId={}";
    public static final String OUTLET_CREATED = "Outlet created outletId={} businessId={} catalogCopied={}";
    public static final String OUTLET_UPDATED = "Outlet updated outletId={} businessId={}";
    public static final String OUTLET_ARCHIVED = "Outlet archived outletId={} businessId={}";
    public static final String PRODUCT_SAVED = "Product saved productId={} outletId={} operation={} active={} addonGroupCount={}";
    public static final String ADDON_GROUP_SAVED = "Add-on group saved addonGroupId={} outletId={} operation={} optionCount={}";
    public static final String ORDER_SAVED = "Order saved orderId={} outletId={} orderNumber={} operation={} itemCount={} addonSelectionCount={}";
    public static final String ORDER_HELD = "Order held orderId={} outletId={} orderNumber={}";
    public static final String ORDER_PREPARING = "Order preparation started orderId={} outletId={} orderNumber={} paid={}";
    public static final String ORDER_COMPLETED = "Order completed orderId={} outletId={} orderNumber={} paymentStatus={}";
    public static final String ORDER_CANCELLED = "Order cancelled orderId={} outletId={} orderNumber={} previousStatus={}";
    public static final String PAYMENT_RECORDED = "Payment recorded paymentId={} orderId={} method={} amount={}";
    public static final String REFUND_RECORDED = "Refund recorded refundId={} orderId={} amount={} restoreStock={}";

    private LogMessages() {
    }
}
