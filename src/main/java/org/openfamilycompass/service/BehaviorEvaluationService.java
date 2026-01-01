package org.openfamilycompass.service;

import java.util.List;
import java.util.Optional;

import org.openfamilycompass.model.Behavior;
import org.openfamilycompass.model.BehaviorEvaluation;
import org.openfamilycompass.model.PointTransactionType;
import org.openfamilycompass.model.User;
import org.openfamilycompass.repository.BehaviorEvaluationRepository;
import org.openfamilycompass.repository.BehaviorRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BehaviorEvaluationService {

    private final BehaviorEvaluationRepository evaluationRepository;
    private final BehaviorRepository behaviorRepository;
    private final PointService pointService;

    /**
     * Updates or creates an evaluation for a behavior
     */
    @Transactional
    public BehaviorEvaluation updateEvaluation(@NonNull Long behaviorId,
            @NonNull User user,
            int currentPoints,
            String remarks,
            @NonNull User updatedBy) {
        Behavior behavior = behaviorRepository.findById(behaviorId)
                .orElseThrow(() -> new IllegalArgumentException("Behavior not found"));

        if (!behavior.isActive()) {
            throw new IllegalStateException("Behavior is not active");
        }

        // Points must not be negative or exceed the maximum
        if (currentPoints < 0 || currentPoints > behavior.getPoints()) {
            throw new IllegalArgumentException(
                    "Points must be between 0 and " + behavior.getPoints());
        }

        // Find existing uncommitted evaluation or create new one
        BehaviorEvaluation evaluation = evaluationRepository
                .findByUserAndBehaviorAndCommittedFalse(user, behavior)
                .orElseGet(() -> {
                    BehaviorEvaluation newEval = new BehaviorEvaluation();
                    newEval.setBehavior(behavior);
                    newEval.setUser(user);
                    newEval.setCreatedBy(updatedBy);
                    return newEval;
                });

        evaluation.setCurrentPoints(currentPoints);
        evaluation.setRemarks(remarks);

        return evaluationRepository.save(evaluation);
    }

    /**
     * Loads all uncommitted evaluations for a child
     */
    @Transactional(readOnly = true)
    public List<BehaviorEvaluation> getCurrentWeekEvaluations(@NonNull User user) {
        return evaluationRepository.findByUserAndCommittedFalse(user);
    }

    /**
     * Loads a specific evaluation
     */
    @Transactional(readOnly = true)
    public Optional<BehaviorEvaluation> getEvaluation(Long evaluationId) {
        return evaluationRepository.findById(evaluationId);
    }

    /**
     * Commits all uncommitted evaluations of a child and resets them with maximum
     * points
     */
    @Transactional
    public void commitWeeklyEvaluations(@NonNull User user, @NonNull User committedBy) {
        List<BehaviorEvaluation> evaluations = evaluationRepository.findByUserAndCommittedFalse(user);

        if (evaluations.isEmpty()) {
            throw new IllegalStateException("No evaluations to commit");
        }

        // Process each evaluation
        for (BehaviorEvaluation evaluation : evaluations) {
            if (evaluation.getCurrentPoints() > 0) {
                // Credit points
                String description = "Behavior: " + evaluation.getBehavior().getTitle();

                pointService.addPointsWithRemarks(
                        user,
                        evaluation.getCurrentPoints(),
                        PointTransactionType.BEHAVIOR,
                        description,
                        evaluation.getBehavior().getId(),
                        evaluation.getRemarks(),
                        committedBy);
            }

            // Mark evaluation as committed
            evaluation.setCommitted(true);
            evaluationRepository.save(evaluation);

            // Create new evaluation with maximum points for next period
            BehaviorEvaluation nextEvaluation = new BehaviorEvaluation();
            nextEvaluation.setUser(user);
            nextEvaluation.setBehavior(evaluation.getBehavior());
            nextEvaluation.setCurrentPoints(evaluation.getBehavior().getPoints()); // Initialize with maximum points
            nextEvaluation.setCommitted(false);
            nextEvaluation.setCreatedBy(committedBy);
            nextEvaluation.setRemarks(null);

            evaluationRepository.save(nextEvaluation);
        }
    }

    @Transactional(readOnly = true)
    public List<Behavior> getActiveBehaviorsForUser(@NonNull User user) {
        List<Behavior> userSpecific = behaviorRepository.findByUserAndActiveTrue(user);
        List<Behavior> global = behaviorRepository.findByUserIsNullAndActiveTrue();

        userSpecific.addAll(global);
        return userSpecific;
    }

    /**
     * Calculates the total points of the current week for a user
     */
    @Transactional(readOnly = true)
    public int calculateWeeklyTotal(@NonNull User user) {
        return getCurrentWeekEvaluations(user).stream()
                .mapToInt(BehaviorEvaluation::getCurrentPoints)
                .sum();
    }
}
