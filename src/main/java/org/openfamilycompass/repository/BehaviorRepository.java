package org.openfamilycompass.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.openfamilycompass.model.Behavior;
import org.openfamilycompass.model.Child;

@Repository
public interface BehaviorRepository extends JpaRepository<Behavior, Long> {

    List<Behavior> findByActiveTrue();

    List<Behavior> findByChild(Child child);

    List<Behavior> findByChildAndActiveTrue(Child child);

    List<Behavior> findByChildIsNullAndActiveTrue(); // Behavior rules for all children
}
