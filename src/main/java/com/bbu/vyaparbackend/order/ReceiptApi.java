package com.bbu.vyaparbackend.order;

public final class ReceiptApi {
    private ReceiptApi() {
    }

    public record ReceiptView(String businessName, String outletName, String address, String gstin, String fssai,
                              String currency, String upiId, String footer, OrderApi.OrderView order) {
    }
}
