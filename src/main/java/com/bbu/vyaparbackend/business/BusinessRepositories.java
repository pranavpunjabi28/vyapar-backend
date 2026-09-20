package com.bbu.vyaparbackend.business;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface BusinessRepository extends JpaRepository<Business, String> {
}

interface OutletRepository extends JpaRepository<Outlet, String> {
    @EntityGraph(attributePaths = "business")
    List<Outlet> findAllByBusinessIdAndArchivedFalseOrderByNameAscIdAsc(String businessId);

    Optional<Outlet> findFirstByBusinessIdAndArchivedFalseOrderByCreatedAtAscIdAsc(String businessId);

    @Override
    @EntityGraph(attributePaths = "business")
    Optional<Outlet> findById(String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Outlet o join fetch o.business where o.id = :id")
    Optional<Outlet> findByIdForUpdate(@Param("id") String id);
}

interface MembershipRepository extends JpaRepository<Membership, String> {
    @EntityGraph(attributePaths = {"business", "user"})
    Optional<Membership> findByBusinessIdAndUserIdAndArchivedFalseAndBusinessArchivedFalse(String businessId, String userId);

    @EntityGraph(attributePaths = {"business", "user"})
    List<Membership> findAllByUserIdAndArchivedFalseAndBusinessArchivedFalse(String userId);

    boolean existsByBusinessIdAndUserIdAndArchivedFalse(String businessId, String userId);

    @EntityGraph(attributePaths = {"business", "user"})
    List<Membership> findAllByBusinessIdAndArchivedFalse(String businessId);
}

interface OutletAssignmentRepository extends JpaRepository<OutletAssignment, String> {
    boolean existsByMembershipIdAndOutletIdAndArchivedFalse(String membershipId, String outletId);

    @EntityGraph(attributePaths = "outlet")
    List<OutletAssignment> findAllByMembershipIdAndArchivedFalse(String membershipId);

    @EntityGraph(attributePaths = "outlet")
    List<OutletAssignment> findAllByMembershipId(String membershipId);
}

interface StaffInvitationRepository extends JpaRepository<StaffInvitation, String> {
    Optional<StaffInvitation> findByTokenHash(String tokenHash);
}
