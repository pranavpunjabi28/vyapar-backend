package com.bbu.vyaparbackend.business;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

public final class BusinessApi {
    private BusinessApi() {
    }

    public record BusinessRequest(@NotBlank @Size(max = 160) String name, @Size(max = 160) String legalName,
                                  @Size(max = 30) String phone, @Size(max = 20) String gstin,
                                  @Size(max = 30) String fssai) {
        BusinessCommands.BusinessData toCommand() {
            return new BusinessCommands.BusinessData(name, legalName, phone, gstin, fssai);
        }
    }

    public record OutletRequest(@NotBlank @Size(max = 160) String name, @Size(max = 30) String phone,
                                @Size(max = 1000) String address, @Pattern(regexp = "[A-Z]{3}") String currency,
                                String timezone, @Size(max = 150) String upiId,
                                @Size(max = 500) String receiptFooter) {
        BusinessCommands.OutletData toCommand() {
            return new BusinessCommands.OutletData(name, phone, address, currency, timezone, upiId, receiptFooter);
        }
    }

    public record InvitationRequest(@NotBlank @Email String email, @NotNull Role role,
                                    @NotNull List<String> outletIds) {
        BusinessCommands.Invitation toCommand() {
            return new BusinessCommands.Invitation(email, role, outletIds);
        }
    }

    public record AcceptInvitation(@Size(max = 120) String displayName,
                                   @Size(min = 8, max = 100) String password) {
        BusinessCommands.AcceptInvitation toCommand() {
            return new BusinessCommands.AcceptInvitation(displayName, password);
        }
    }

    public record StaffUpdate(@NotNull Role role, @NotNull List<String> outletIds) {
        BusinessCommands.StaffUpdate toCommand() {
            return new BusinessCommands.StaffUpdate(role, outletIds);
        }
    }

    public record BusinessView(String id, String name, String legalName, String phone, String gstin, String fssai,
                               String logoKey) {
    }

    public record MembershipView(String businessId, String businessName, Role role) {
    }

    public record OutletView(String id, String businessId, String name, String phone, String address, String currency,
                             String timezone, String upiId, String receiptFooter) {
    }

    public record InvitationView(String id, String email, Role role, Instant expiresAt, String invitationCode) {
    }

    public record StaffView(String membershipId, String userId, String displayName, String email, Role role,
                            List<String> outletIds) {
    }
}
