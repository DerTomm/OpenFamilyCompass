package com.family.kidschores.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.family.kidschores.model.Behavior;
import com.family.kidschores.model.BehaviorEvaluation;
import com.family.kidschores.model.Child;

@Repository
public interface BehaviorEvaluationRepository extends JpaRepository<BehaviorEvaluation, Long> {

    List<BehaviorEvaluation> findByChildAndCommittedFalse(Child child);

    List<BehaviorEvaluation> findByChildAndWeekStartDateAndCommittedFalse(Child child, LocalDateTime weekStartDate);

    Optional<BehaviorEvaluation> findByChildAndBehaviorAndWeekStartDateAndCommittedFalse(
            Child child, Behavior behavior, LocalDateTime weekStartDate);

    List<BehaviorEvaluation> findByWeekStartDateAndCommittedFalse(LocalDateTime weekStartDate);
}
