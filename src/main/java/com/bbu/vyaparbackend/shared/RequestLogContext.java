package com.bbu.vyaparbackend.shared;

import org.slf4j.MDC;

public final class RequestLogContext {
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID = "requestId";
    public static final String USER_ID = "userId";
    public static final String MERCHANT_ID = "merchantId";
    public static final String OUTLET_ID = "outletId";

    private RequestLogContext() {
    }

    public static void user(String userId) {
        put(USER_ID, userId);
    }

    public static void merchant(String businessId) {
        put(MERCHANT_ID, businessId);
    }

    public static void outlet(String outletId) {
        put(OUTLET_ID, outletId);
    }

    public static String requestId() {
        return MDC.get(REQUEST_ID);
    }

    private static void put(String key, String value) {
        if (value != null && !value.isBlank()) MDC.put(key, value);
    }
}
