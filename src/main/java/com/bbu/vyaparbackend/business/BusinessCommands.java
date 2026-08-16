package com.bbu.vyaparbackend.business;

import java.util.List;

public final class BusinessCommands {
    private BusinessCommands() {
    }

    public record BusinessData(String name, String legalName, String phone, String gstin, String fssai) {
    }

    public record OutletData(String name, String phone, String address, String currency, String timezone, String upiId,
                             String receiptFooter) {
    }

    public record Invitation(String email, Role role, List<String> outletIds) {
    }

    public record AcceptInvitation(String displayName, String password) {
    }

    public record StaffUpdate(Role role, List<String> outletIds) {
    }
}
