package com.family.kidschores.service;

import com.family.kidschores.model.*;
import com.family.kidschores.repository.RewardRedemptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RewardRedemptionService {

    private final RewardRedemptionRepository redemptionRepository;
    private final PointService pointService;

    @Transactional
    public RewardRedemption requestReward(@NonNull Child child, @NonNull Reward reward) {
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
        Long redemptionId = Objects.requireNonNull(saved.getId(), "Redemption ID must not be null");
        pointService.deductPoints(child, reward.getPointsCost(), "REWARD", 
                                 "Belohnung angefordert: " + reward.getTitle(), 
                                 redemptionId, null);

        return saved;
    }

    @Transactional
    public RewardRedemption approveRedemption(@NonNull Long redemptionId, @NonNull User approver, String notes) {
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
    public RewardRedemption markAsDelivered(@NonNull Long redemptionId) {
        RewardRedemption redemption = redemptionRepository.findById(redemptionId)
                .orElseThrow(() -> new IllegalArgumentException("Redemption not found"));

        redemption.setStatus(RewardStatus.DELIVERED);
        redemption.setDeliveredAt(LocalDateTime.now());

        return redemptionRepository.save(redemption);
    }

    @Transactional
    public RewardRedemption cancelRedemption(@NonNull Long redemptionId, @NonNull User cancelledBy) {
        RewardRedemption redemption = redemptionRepository.findById(redemptionId)
                .orElseThrow(() -> new IllegalArgumentException("Redemption not found"));

        redemption.setStatus(RewardStatus.CANCELLED);

        RewardRedemption saved = redemptionRepository.save(redemption);

        // Punkte zurückgeben
        Long redemptionId2 = Objects.requireNonNull(redemption.getId(), "Redemption ID must not be null");
        pointService.addPoints(redemption.getChild(), redemption.getPointsSpent(), 
                              "REWARD", "Belohnung storniert: " + redemption.getReward().getTitle(), 
                              redemptionId2, cancelledBy);

        return saved;
    }

    public List<RewardRedemption> findByChild(@NonNull Child child) {
        return redemptionRepository.findByChildOrderByRequestedAtDesc(child);
    }

    public List<RewardRedemption> findPendingApprovals() {
        return redemptionRepository.findByStatus(RewardStatus.REQUESTED);
    }

    public List<RewardRedemption> findApproved() {
        return redemptionRepository.findByStatus(RewardStatus.APPROVED);
    }
}
