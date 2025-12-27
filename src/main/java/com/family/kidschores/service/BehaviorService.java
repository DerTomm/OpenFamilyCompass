package com.family.kidschores.service;

import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.family.kidschores.model.Behavior;
import com.family.kidschores.model.Child;
import com.family.kidschores.model.User;
import com.family.kidschores.repository.BehaviorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BehaviorService {

    private final BehaviorRepository behaviorRepository;
    private final PointService pointService;

    @Transactional
    public Behavior createBehavior(@NonNull String title, @NonNull String guideline, int points, Child child) {
        Behavior behavior = new Behavior();
        behavior.setTitle(title);
        behavior.setGuideline(guideline);
        behavior.setPoints(points);
        behavior.setChild(child);
        behavior.setActive(true);

        return behaviorRepository.save(behavior);
    }

    @Transactional
    public void recordBehavior(@NonNull Long behaviorId, @NonNull Child child, @NonNull User recordedBy) {
        Behavior behavior = behaviorRepository.findById(behaviorId)
                .orElseThrow(() -> new IllegalArgumentException("Behavior not found"));

        if (!behavior.isActive()) {
            throw new IllegalStateException("Behavior is not active");
        }

        // Credit points - ID must not be null after loading from DB
        Long id = Objects.requireNonNull(behavior.getId(), "Behavior ID must not be null");
        pointService.addPoints(child, behavior.getPoints(), "BEHAVIOR",
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

    public List<Behavior> findByChild(@NonNull Child child) {
        return behaviorRepository.findByChild(child);
    }

    public List<Behavior> findGlobalBehaviors() {
        return behaviorRepository.findByChildIsNullAndActiveTrue();
    }
}
