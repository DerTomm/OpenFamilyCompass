package org.openfamilycompass.service;

import java.util.List;
import java.util.Objects;

import org.openfamilycompass.model.Behavior;
import org.openfamilycompass.model.PointTransactionType;
import org.openfamilycompass.model.User;
import org.openfamilycompass.repository.BehaviorRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BehaviorService {

    private final BehaviorRepository behaviorRepository;
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
    public void deactivateBehavior(@NonNull Long behaviorId) {
        Behavior behavior = behaviorRepository.findById(behaviorId)
                .orElseThrow(() -> new IllegalArgumentException("Behavior not found"));

        behavior.setActive(false);
        behaviorRepository.save(behavior);
    }

    public List<Behavior> findAllActive() {
        return behaviorRepository.findByActiveTrue();
    }

    public List<Behavior> findByUser(@NonNull User user) {
        return behaviorRepository.findByUser(user);
    }

    public List<Behavior> findGlobalBehaviors() {
        return behaviorRepository.findByUserIsNullAndActiveTrue();
    }
}
