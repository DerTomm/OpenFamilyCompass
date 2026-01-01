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
    public Behavior createBehavior(@NonNull String title, @NonNull String guideline, int points, User user) {
        Behavior behavior = new Behavior();
        behavior.setTitle(title);
        behavior.setGuideline(guideline);
        behavior.setPoints(points);
        behavior.setUser(user);
        behavior.setActive(true);

        return behaviorRepository.save(behavior);
    }

    @Transactional
    public Behavior editBehavior(@NonNull Long behaviorId, @NonNull String title, @NonNull String guideline,
            int newPoints, User user) {
        Behavior behavior = behaviorRepository.findById(behaviorId)
                .orElseThrow(() -> new IllegalArgumentException("Behavior not found"));

        int oldPoints = behavior.getPoints();

        // Update all properties
        behavior.setTitle(title);
        behavior.setGuideline(guideline);
        behavior.setPoints(newPoints);
        behavior.setUser(user);

        // If points were reduced, cap existing evaluations in the current week
        if (newPoints < oldPoints) {
            capPointsInCurrentWeek(behavior, newPoints);
        }

        return behaviorRepository.save(behavior);
    }

    /**
     * Caps all uncommitted evaluations of a behavior to the new maximum points
     */
    private void capPointsInCurrentWeek(@NonNull Behavior behavior, int newMaxPoints) {
        List<BehaviorEvaluation> evaluations = behaviorEvaluationRepository
                .findByBehaviorAndCommittedFalse(behavior);

        for (BehaviorEvaluation evaluation : evaluations) {
            if (evaluation.getCurrentPoints() > newMaxPoints) {
                evaluation.setCurrentPoints(newMaxPoints);
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
        return behaviorRepository.findByActiveTrue();
    }

    public java.util.Optional<Behavior> findById(@NonNull Long id) {
        return behaviorRepository.findById(id);
    }

    public List<Behavior> findByUser(@NonNull User user) {
        return behaviorRepository.findByUser(user);
    }

    public List<Behavior> findGlobalBehaviors() {
        return behaviorRepository.findByUserIsNullAndActiveTrue();
    }
}
