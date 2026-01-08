package org.openfamilycompass.repository;

import java.util.List;

import org.openfamilycompass.model.Behavior;
import org.openfamilycompass.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BehaviorRepository extends JpaRepository<Behavior, Long> {

    @Query("SELECT b FROM Behavior b WHERE b.active = true ORDER BY b.rank ASC")
    List<Behavior> findByActiveTrueOrderByRankAsc();

    List<Behavior> findByUser(User user);

    @Query("SELECT b FROM Behavior b WHERE b.user = :user AND b.active = true ORDER BY b.rank ASC")
    List<Behavior> findByUserAndActiveTrueOrderByRankAsc(User user);

    @Query("SELECT b FROM Behavior b WHERE b.user IS NULL AND b.active = true ORDER BY b.rank ASC")
    List<Behavior> findByUserIsNullAndActiveTrueOrderByRankAsc(); // Behavior rules for all users
}
