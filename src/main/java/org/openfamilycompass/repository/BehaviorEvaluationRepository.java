package org.openfamilycompass.repository;

import java.util.List;
import java.util.Optional;

import org.openfamilycompass.model.Behavior;
import org.openfamilycompass.model.BehaviorEvaluation;
import org.openfamilycompass.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BehaviorEvaluationRepository extends JpaRepository<BehaviorEvaluation, Long> {

    List<BehaviorEvaluation> findByUserAndCommittedFalse(User user);

    Optional<BehaviorEvaluation> findByUserAndBehaviorAndCommittedFalse(User user, Behavior behavior);

    List<BehaviorEvaluation> findByCommittedFalse();

    List<BehaviorEvaluation> findByBehaviorAndCommittedFalse(Behavior behavior);
}
