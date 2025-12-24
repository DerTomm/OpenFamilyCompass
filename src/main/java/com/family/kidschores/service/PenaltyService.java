package com.family.kidschores.service;

import com.family.kidschores.model.Child;
import com.family.kidschores.model.Penalty;
import com.family.kidschores.model.User;
import com.family.kidschores.repository.PenaltyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PenaltyService {

    private final PenaltyRepository penaltyRepository;
    private final PointService pointService;

    @Transactional
    public Penalty createPenalty(@NonNull Child child, @NonNull String reason, int pointsDeducted, @NonNull User createdBy) {
        Penalty penalty = new Penalty();
        penalty.setChild(child);
        penalty.setReason(reason);
        penalty.setPointsDeducted(pointsDeducted);
        penalty.setCreatedBy(createdBy);

        Penalty saved = penaltyRepository.save(penalty);

        // Punkte abziehen
        Long penaltyId = Objects.requireNonNull(saved.getId(), "Penalty ID must not be null");
        pointService.deductPoints(child, pointsDeducted, "PENALTY", 
                                 "Strafe: " + reason, penaltyId, createdBy);

        return saved;
    }

    public List<Penalty> findByChild(@NonNull Child child) {
        return penaltyRepository.findByChildOrderByCreatedAtDesc(child);
    }
}
