package org.openfamilycompass.repository;

import java.util.List;

import org.openfamilycompass.model.PointTransaction;
import org.openfamilycompass.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    List<PointTransaction> findByUser(User user);

    List<PointTransaction> findByUserOrderByCreatedAtDesc(User user);

    List<PointTransaction> findByUserOrderByCreatedAtAsc(User user);

    /**
     * Sum of point transactions that affect a user's visible balance.
     *
     * <p>Reservation semantics:
     * <ul>
     *   <li>COMPLETED transactions always count (positive credits and negative
     *   deductions alike).</li>
     *   <li>PENDING transactions with negative points (e.g. a requested reward
     *   redemption awaiting parent approval) count as a reservation against the
     *   balance, so the child cannot queue multiple redemptions that together
     *   exceed their funds.</li>
     *   <li>PENDING transactions with non-negative points (e.g. a task submitted
     *   by the child, awaiting parent approval) are excluded - the points are
     *   only credited once the parent approves.</li>
     *   <li>CANCELLED transactions are excluded.</li>
     * </ul>
     */
    @Query("SELECT SUM(pt.points) FROM PointTransaction pt "
            + "WHERE pt.user = :user "
            + "AND (pt.status = org.openfamilycompass.model.PointTransactionStatus.COMPLETED "
            + "     OR (pt.status = org.openfamilycompass.model.PointTransactionStatus.PENDING AND pt.points < 0))")
    Integer sumPointsByUser(User user);

    List<PointTransaction> findByReferenceIdAndType(Long referenceId,
            org.openfamilycompass.model.PointTransactionType type);
}
