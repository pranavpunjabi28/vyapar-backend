package com.bbu.vyaparbackend.business;

import com.bbu.vyaparbackend.auth.AuthService;
import com.bbu.vyaparbackend.auth.User;
import com.bbu.vyaparbackend.auth.UserRepository;
import com.bbu.vyaparbackend.shared.ApiException;
import com.bbu.vyaparbackend.shared.PrefixedIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class StaffService {
    private final MembershipRepository memberships;
    private final OutletRepository outlets;
    private final OutletAssignmentRepository assignments;
    private final StaffInvitationRepository invitations;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final long invitationHours;

    StaffService(MembershipRepository memberships, OutletRepository outlets,
                 OutletAssignmentRepository assignments, StaffInvitationRepository invitations,
                 UserRepository users, PasswordEncoder passwordEncoder,
                 @Value("${app.invitation.hours}") long invitationHours) {
        this.memberships = memberships;
        this.outlets = outlets;
        this.assignments = assignments;
        this.invitations = invitations;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.invitationHours = invitationHours;
    }

    @Transactional(readOnly = true)
    public List<Membership> list(Membership actor) {
        return memberships.findAllByBusinessIdAndArchivedFalse(actor.getBusiness().getId());
    }

    @Transactional(readOnly = true)
    public List<OutletAssignment> assignments(Membership membership) {
        return assignments.findAllByMembershipIdAndArchivedFalse(membership.getId());
    }

    @Transactional
    public Membership update(Membership actor, String membershipId, BusinessCommands.StaffUpdate command) {
        Membership staff = memberships.findById(membershipId)
                .filter(value -> !value.isArchived()
                        && value.getBusiness().getId().equals(actor.getBusiness().getId()))
                .orElseThrow(() -> ApiException.notFound("Staff membership"));
        if (staff.getRole() == Role.OWNER || command.role() == Role.OWNER) {
            throw ApiException.invalid("Owner membership cannot be changed here");
        }
        staff.setRole(command.role());
        Set<String> desired = new HashSet<>(command.outletIds());
        for (String outletId : desired) requireOutlet(actor, outletId);
        for (OutletAssignment assignment : assignments.findAllByMembershipId(staff.getId())) {
            assignment.setArchived(!desired.remove(assignment.getOutlet().getId()));
        }
        for (String outletId : desired) {
            OutletAssignment assignment = new OutletAssignment();
            assignment.setMembership(staff);
            assignment.setOutlet(outlets.getReferenceById(outletId));
            assignments.save(assignment);
        }
        return staff;
    }

    @Transactional
    public BusinessApi.InvitationView invite(Membership actor, BusinessCommands.Invitation command) {
        if (command.role() == Role.OWNER) throw ApiException.invalid("Owner role cannot be invited");
        String raw = PrefixedIdGenerator.randomPart(40) + "." + PrefixedIdGenerator.randomPart(40);
        StaffInvitation invitation = new StaffInvitation();
        invitation.setBusiness(actor.getBusiness());
        invitation.setEmail(command.email().trim().toLowerCase());
        invitation.setRole(command.role());
        invitation.setTokenHash(AuthService.hash(raw));
        invitation.setExpiresAt(Instant.now().plus(invitationHours, ChronoUnit.HOURS));
        for (String outletId : command.outletIds()) {
            requireOutlet(actor, outletId);
            invitation.getOutletIds().add(outletId);
        }
        invitations.save(invitation);
        return new BusinessApi.InvitationView(invitation.getId(), invitation.getEmail(), invitation.getRole(),
                invitation.getExpiresAt(), raw);
    }

    @Transactional
    public void accept(String rawToken, BusinessCommands.AcceptInvitation command) {
        StaffInvitation invitation = invitations.findByTokenHash(AuthService.hash(rawToken))
                .orElseThrow(() -> ApiException.invalid("Invalid invitation"));
        if (invitation.getAcceptedAt() != null || invitation.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.invalid("Invitation expired or already used");
        }
        User user = users.findByEmailIgnoreCase(invitation.getEmail()).orElseGet(() -> createUser(invitation, command));
        if (memberships.existsByBusinessIdAndUserIdAndArchivedFalse(invitation.getBusiness().getId(), user.getId())) {
            throw ApiException.conflict("User already belongs to this business");
        }
        Membership membership = new Membership();
        membership.setBusiness(invitation.getBusiness());
        membership.setUser(user);
        membership.setRole(invitation.getRole());
        memberships.save(membership);
        for (String outletId : invitation.getOutletIds()) {
            Outlet outlet = outlets.findById(outletId)
                    .filter(value -> value.getBusiness().getId().equals(invitation.getBusiness().getId()))
                    .orElseThrow(() -> ApiException.notFound("Outlet"));
            OutletAssignment assignment = new OutletAssignment();
            assignment.setMembership(membership);
            assignment.setOutlet(outlet);
            assignments.save(assignment);
        }
        invitation.setAcceptedAt(Instant.now());
    }

    private User createUser(StaffInvitation invitation, BusinessCommands.AcceptInvitation command) {
        if (command.password() == null || command.displayName() == null) {
            throw ApiException.invalid("Name and password are required for a new account");
        }
        User created = new User();
        created.setEmail(invitation.getEmail());
        created.setDisplayName(command.displayName().trim());
        created.setPasswordHash(passwordEncoder.encode(command.password()));
        return users.save(created);
    }

    private void requireOutlet(Membership actor, String outletId) {
        outlets.findById(outletId)
                .filter(outlet -> !outlet.isArchived()
                        && outlet.getBusiness().getId().equals(actor.getBusiness().getId()))
                .orElseThrow(() -> ApiException.notFound("Outlet"));
    }
}
