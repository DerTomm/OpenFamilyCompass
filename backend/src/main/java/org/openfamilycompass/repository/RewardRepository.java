package org.openfamilycompass.repository;

import java.util.List;

import org.openfamilycompass.model.Reward;
import org.openfamilycompass.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Long> {

    List<Reward> findByActiveTrue();

    List<Reward> findAllByOrderByPointsCostAsc();

    /**
     * Returns active rewards visible to a given child: those with no user
     * restriction or assigned to that child.
     */
    @Query("SELECT r FROM Reward r WHERE r.active = true AND (r.user IS NULL OR r.user = :user) ORDER BY r.pointsCost ASC")
    List<Reward> findActiveForUser(@Param("user") User user);
}
