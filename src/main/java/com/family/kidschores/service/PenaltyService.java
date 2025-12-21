package com.family.kidschores.service;

import com.family.kidschores.model.Child;
import com.family.kidschores.model.Penalty;
import com.family.kidschores.model.User;
import com.family.kidschores.repository.PenaltyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PenaltyService {

    private final PenaltyRepository penaltyRepository;
    private final PointService pointService;

    @Transactional
    public Penalty createPenalty(Child child, String reason, int pointsDeducted, User createdBy) {
        Penalty penalty = new Penalty();
        penalty.setChild(child);
        penalty.setReason(reason);
        penalty.setPointsDeducted(pointsDeducted);
        penalty.setCreatedBy(createdBy);

        Penalty saved = penaltyRepository.save(penalty);

        // Punkte abziehen
        pointService.deductPoints(child, pointsDeducted, "PENALTY", 
                                 "Strafe: " + reason, saved.getId(), createdBy);

        return saved;
    }

    public List<Penalty> findByChild(Child child) {
        return penaltyRepository.findByChildOrderByCreatedAtDesc(child);
    }
}
