package org.openfamilycompass.service;

import java.util.List;
import java.util.Objects;

import org.openfamilycompass.model.Penalty;
import org.openfamilycompass.model.User;
import org.openfamilycompass.repository.PenaltyRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PenaltyService {

    private final PenaltyRepository penaltyRepository;
    private final PointService pointService;

    @Transactional
    public Penalty createPenalty(@NonNull User user, @NonNull String reason, int pointsDeducted,
            @NonNull User createdBy) {
        Penalty penalty = new Penalty();
        penalty.setUser(user);
        penalty.setReason(reason);
        penalty.setPointsDeducted(pointsDeducted);
        penalty.setCreatedBy(createdBy);

        Penalty saved = penaltyRepository.save(penalty);

        // Deduct points
        Long penaltyId = Objects.requireNonNull(saved.getId(), "Penalty ID must not be null");
        pointService.deductPoints(user, pointsDeducted, "PENALTY",
                "Penalty: " + reason, penaltyId, createdBy);

        return saved;
    }

    public List<Penalty> findByUser(@NonNull User user) {
        return penaltyRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void addBonusPoints(@NonNull User user, @NonNull String reason, int points, @NonNull User createdBy) {
        // Credit bonus points directly as ADJUSTMENT
        pointService.addPoints(user, points, "ADJUSTMENT",
                "Bonus points: " + reason, null, createdBy);
    }
}
