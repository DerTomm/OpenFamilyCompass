package org.openfamilycompass.repository;

import java.util.List;

import org.openfamilycompass.model.Behavior;
import org.openfamilycompass.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BehaviorRepository extends JpaRepository<Behavior, Long> {

    List<Behavior> findByActiveTrue();

    List<Behavior> findByUser(User user);

    List<Behavior> findByUserAndActiveTrue(User user);

    List<Behavior> findByUserIsNullAndActiveTrue(); // Behavior rules for all users
}
