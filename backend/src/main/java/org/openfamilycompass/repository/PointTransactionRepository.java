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

    @Query("SELECT SUM(pt.points) FROM PointTransaction pt WHERE pt.user = :user")
    Integer sumPointsByUser(User user);

    List<PointTransaction> findByReferenceIdAndType(Long referenceId,
            org.openfamilycompass.model.PointTransactionType type);
}
