package org.openfamilycompass.repository;

import java.time.LocalDateTime;
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

    List<BehaviorEvaluation> findByUserAndWeekStartDateAndCommittedFalse(User user, LocalDateTime weekStartDate);

    Optional<BehaviorEvaluation> findByUserAndBehaviorAndWeekStartDateAndCommittedFalse(
            User user, Behavior behavior, LocalDateTime weekStartDate);

    List<BehaviorEvaluation> findByWeekStartDateAndCommittedFalse(LocalDateTime weekStartDate);
}
