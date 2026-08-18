package org.openfamilycompass.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

        int minPoints = -behavior.getMinusPoints();
        int maxPoints = behavior.getPoints();

        // Points must be within the configured range
        if (currentPoints < minPoints || currentPoints > maxPoints) {
            throw new IllegalArgumentException(
                    "Points must be between " + minPoints + " and " + maxPoints);
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
            int pointsToBook = evaluation.getCurrentPoints();

            if (pointsToBook != 0) {
                String description = "Behavior: " + evaluation.getBehavior().getTitle();
                pointService.addPointsWithRemarks(
                        user,
                        pointsToBook,
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
            nextEvaluation.setCurrentPoints(0); // Start next period at 0
            nextEvaluation.setCommitted(false);
            nextEvaluation.setCreatedBy(committedBy);
            nextEvaluation.setRemarks(null);

            evaluationRepository.save(nextEvaluation);
        }
    }

    @Transactional(readOnly = true)
    public List<Behavior> getActiveBehaviorsForUser(@NonNull User user) {
        List<Behavior> userSpecific = behaviorRepository.findByUserAndActiveTrueOrderByRankAsc(user);
        List<Behavior> global = behaviorRepository.findByUserIsNullAndActiveTrueOrderByRankAsc();

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

    /**
     * Get all current behavior evaluations for a child as DTOs.
     * Returns active behavior evaluations that are not yet committed.
     * 
     * @param user The child user
     * @return List of DTOs with current evaluation data
     */
    @Transactional(readOnly = true)
    public List<BehaviorEvaluationDTO> getCurrentBehaviorEvaluationsForChild(@NonNull User user) {
        List<BehaviorEvaluation> evaluations = evaluationRepository.findByUserAndCommittedFalse(user);

        return evaluations.stream()
                .filter(eval -> eval.getBehavior().isActive())
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(dto -> {
                    // Find the original evaluation to get the behavior rank
                    return evaluations.stream()
                            .filter(eval -> eval.getBehavior().getId().equals(dto.getBehaviorId()))
                            .findFirst()
                            .map(eval -> eval.getBehavior().getRank())
                            .orElse(0);
                }))
                .collect(Collectors.toList());
    }

    /**
     * Convert a BehaviorEvaluation entity to a DTO for display.
     */
    private BehaviorEvaluationDTO convertToDTO(BehaviorEvaluation evaluation) {
        BehaviorEvaluationDTO dto = new BehaviorEvaluationDTO();
        dto.setBehaviorId(evaluation.getBehavior().getId());
        dto.setBehaviorTitle(evaluation.getBehavior().getTitle());
        dto.setGuideline(evaluation.getBehavior().getGuideline());
        dto.setMaxPoints(evaluation.getBehavior().getPoints());
        dto.setCurrentPoints(evaluation.getCurrentPoints());
        dto.setRemarks(evaluation.getRemarks());
        dto.setHasRemarks(evaluation.getRemarks() != null && !evaluation.getRemarks().trim().isEmpty());

        return dto;
    }

    /**
     * Get total current points from all active behavior evaluations.
     */
    @Transactional(readOnly = true)
    public int getTotalCurrentPointsForChild(@NonNull User user) {
        return getCurrentBehaviorEvaluationsForChild(user)
                .stream()
                .mapToInt(BehaviorEvaluationDTO::getCurrentPoints)
                .sum();
    }

    @Transactional(readOnly = true)
    public List<BehaviorEvaluation> findByUser(@NonNull User user) {
        return evaluationRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public List<BehaviorEvaluation> findAll() {
        return evaluationRepository.findAll();
    }

    @Transactional
    public BehaviorEvaluation save(@NonNull BehaviorEvaluation evaluation) {
        return evaluationRepository.save(evaluation);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<BehaviorEvaluation> findPendingEvaluation(@NonNull Behavior behavior, @NonNull User user) {
        return evaluationRepository.findByUserAndBehaviorAndCommittedFalse(user, behavior);
    }
}
