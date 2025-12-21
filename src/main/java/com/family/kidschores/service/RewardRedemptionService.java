package com.family.kidschores.service;

import com.family.kidschores.model.*;
import com.family.kidschores.repository.RewardRedemptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RewardRedemptionService {

    private final RewardRedemptionRepository redemptionRepository;
    private final PointService pointService;

    @Transactional
    public RewardRedemption requestReward(Child child, Reward reward) {
        // Prüfen ob genug Punkte vorhanden
        if (child.getTotalPoints() < reward.getPointsCost()) {
            throw new IllegalStateException("Not enough points");
        }

        RewardRedemption redemption = new RewardRedemption();
        redemption.setChild(child);
        redemption.setReward(reward);
        redemption.setPointsSpent(reward.getPointsCost());
        redemption.setStatus(RewardStatus.REQUESTED);

        RewardRedemption saved = redemptionRepository.save(redemption);

        // Punkte vorläufig abziehen
        pointService.deductPoints(child, reward.getPointsCost(), "REWARD", 
                                 "Belohnung angefordert: " + reward.getTitle(), 
                                 saved.getId(), null);

        return saved;
    }

    @Transactional
    public RewardRedemption approveRedemption(Long redemptionId, User approver, String notes) {
        RewardRedemption redemption = redemptionRepository.findById(redemptionId)
                .orElseThrow(() -> new IllegalArgumentException("Redemption not found"));

        if (redemption.getStatus() != RewardStatus.REQUESTED) {
            throw new IllegalStateException("Redemption not in REQUESTED state");
        }

        redemption.setStatus(RewardStatus.APPROVED);
        redemption.setApprovedAt(LocalDateTime.now());
        redemption.setApprovedBy(approver);
        redemption.setNotes(notes);

        return redemptionRepository.save(redemption);
    }

    @Transactional
    public RewardRedemption markAsDelivered(Long redemptionId) {
        RewardRedemption redemption = redemptionRepository.findById(redemptionId)
                .orElseThrow(() -> new IllegalArgumentException("Redemption not found"));

        redemption.setStatus(RewardStatus.DELIVERED);
        redemption.setDeliveredAt(LocalDateTime.now());

        return redemptionRepository.save(redemption);
    }

    @Transactional
    public RewardRedemption cancelRedemption(Long redemptionId, User cancelledBy) {
        RewardRedemption redemption = redemptionRepository.findById(redemptionId)
                .orElseThrow(() -> new IllegalArgumentException("Redemption not found"));

        redemption.setStatus(RewardStatus.CANCELLED);

        RewardRedemption saved = redemptionRepository.save(redemption);

        // Punkte zurückgeben
        pointService.addPoints(redemption.getChild(), redemption.getPointsSpent(), 
                              "REWARD", "Belohnung storniert: " + redemption.getReward().getTitle(), 
                              redemption.getId(), cancelledBy);

        return saved;
    }

    public List<RewardRedemption> findByChild(Child child) {
        return redemptionRepository.findByChildOrderByRequestedAtDesc(child);
    }

    public List<RewardRedemption> findPendingApprovals() {
        return redemptionRepository.findByStatus(RewardStatus.REQUESTED);
    }

    public List<RewardRedemption> findApproved() {
        return redemptionRepository.findByStatus(RewardStatus.APPROVED);
    }
}
