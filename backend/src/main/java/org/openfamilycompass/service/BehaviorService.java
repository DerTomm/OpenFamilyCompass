package org.openfamilycompass.service;

import java.util.List;
import java.util.Objects;

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
public class BehaviorService {

    private final BehaviorRepository behaviorRepository;
    private final BehaviorEvaluationRepository behaviorEvaluationRepository;
    private final PointService pointService;

    @Transactional
    public Behavior createBehavior(@NonNull String title, @NonNull String guideline, int plusPoints, int minusPoints, User user) {
        Behavior behavior = new Behavior();
        behavior.setTitle(title);
        behavior.setGuideline(guideline);
        behavior.setPoints(plusPoints);
        behavior.setMinusPoints(minusPoints);
        behavior.setUser(user);
        behavior.setActive(true);

        return behaviorRepository.save(behavior);
    }

    @Transactional
    public Behavior editBehavior(@NonNull Long behaviorId, @NonNull String title, @NonNull String guideline,
            int newPlusPoints, int newMinusPoints, User user) {
        Behavior behavior = behaviorRepository.findById(behaviorId)
                .orElseThrow(() -> new IllegalArgumentException("Behavior not found"));

        int oldPlusPoints = behavior.getPoints();
        int oldMinusPoints = behavior.getMinusPoints();

        // Update all properties
        behavior.setTitle(title);
        behavior.setGuideline(guideline);
        behavior.setPoints(newPlusPoints);
        behavior.setMinusPoints(newMinusPoints);
        behavior.setUser(user);

        // If range was reduced, cap existing evaluations in the current week
        if (newPlusPoints < oldPlusPoints || newMinusPoints < oldMinusPoints) {
            capPointsInCurrentWeek(behavior, newPlusPoints, newMinusPoints);
        }

        return behaviorRepository.save(behavior);
    }

    /**
     * Caps all uncommitted evaluations of a behavior to the new maximum points
     */
    public void capPointsInCurrentWeek(@NonNull Behavior behavior, int newPlusPoints, int newMinusPoints) {
        List<BehaviorEvaluation> evaluations = behaviorEvaluationRepository
                .findByBehaviorAndCommittedFalse(behavior);

        for (BehaviorEvaluation evaluation : evaluations) {
            int current = evaluation.getCurrentPoints();
            if (current > newPlusPoints) {
                evaluation.setCurrentPoints(newPlusPoints);
                behaviorEvaluationRepository.save(evaluation);
                continue;
            }
            if (current < -newMinusPoints) {
                evaluation.setCurrentPoints(-newMinusPoints);
                behaviorEvaluationRepository.save(evaluation);
            }
        }
    }

    @Transactional
    public void recordBehavior(@NonNull Long behaviorId, @NonNull User user, @NonNull User recordedBy) {
        Behavior behavior = behaviorRepository.findById(behaviorId)
                .orElseThrow(() -> new IllegalArgumentException("Behavior not found"));

        if (!behavior.isActive()) {
            throw new IllegalStateException("Behavior is not active");
        }

        // Credit points - ID must not be null after loading from DB
        Long id = Objects.requireNonNull(behavior.getId(), "Behavior ID must not be null");
        pointService.addPoints(user, behavior.getPoints(), PointTransactionType.BEHAVIOR,
                "Positive behavior: " + behavior.getTitle(),
                id, recordedBy);
    }

    @Transactional
    public void deleteBehavior(@NonNull Long behaviorId) {
        Behavior behavior = behaviorRepository.findById(behaviorId)
                .orElseThrow(() -> new IllegalArgumentException("Behavior not found"));

        // Delete all associated behavior evaluations first
        behaviorEvaluationRepository.deleteByBehavior(behavior);

        // Then delete the behavior itself
        behaviorRepository.delete(behavior);
    }

    public List<Behavior> findAllActive() {
        return behaviorRepository.findByActiveTrueOrderByRankAsc();
    }

    public java.util.Optional<Behavior> findById(@NonNull Long id) {
        return behaviorRepository.findById(id);
    }

    public List<Behavior> findByUser(@NonNull User user) {
        return behaviorRepository.findByUserAndActiveTrueOrderByRankAsc(user);
    }

    public List<Behavior> findGlobalBehaviors() {
        return behaviorRepository.findByUserIsNullAndActiveTrueOrderByRankAsc();
    }

    public List<Behavior> findAll() {
        return behaviorRepository.findAll();
    }

    public List<Behavior> findByUserOrGlobal(@NonNull User user) {
        List<Behavior> userBehaviors = behaviorRepository.findByUserAndActiveTrueOrderByRankAsc(user);
        List<Behavior> globalBehaviors = behaviorRepository.findByUserIsNullAndActiveTrueOrderByRankAsc();
        userBehaviors.addAll(globalBehaviors);
        return userBehaviors;
    }

    @Transactional
    public Behavior save(@NonNull Behavior behavior) {
        return behaviorRepository.save(behavior);
    }

    @Transactional
    public void updateBehaviorRanks(List<Long> behaviorIdsInOrder) {
        for (int i = 0; i < behaviorIdsInOrder.size(); i++) {
            Long id = behaviorIdsInOrder.get(i);
            Behavior behavior = behaviorRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Behavior not found: " + id));
            behavior.setRank(i);
            behaviorRepository.save(behavior);
        }
    }
}
