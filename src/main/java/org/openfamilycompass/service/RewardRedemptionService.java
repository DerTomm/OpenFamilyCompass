package org.openfamilycompass.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.openfamilycompass.model.Reward;
import org.openfamilycompass.model.RewardRedemption;
import org.openfamilycompass.model.RewardStatus;
import org.openfamilycompass.model.User;
import org.openfamilycompass.repository.RewardRedemptionRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RewardRedemptionService {

    private final RewardRedemptionRepository redemptionRepository;
    private final PointService pointService;

    @Transactional
    public RewardRedemption requestReward(@NonNull User user, @NonNull Reward reward) {
        // Check if enough points are available
        if (user.getTotalPoints() < reward.getPointsCost()) {
            throw new IllegalStateException("Not enough points");
        }

        RewardRedemption redemption = new RewardRedemption();
        redemption.setUser(user);
        redemption.setReward(reward);
        redemption.setPointsSpent(reward.getPointsCost());
        redemption.setStatus(RewardStatus.REQUESTED);

        RewardRedemption saved = redemptionRepository.save(redemption);

        // Note: Points are NOT deducted here - they will be deducted only after
        // approval
        // This allows children to request rewards without losing points immediately

        return saved;
    }

    @Transactional
    public RewardRedemption approveRedemption(@NonNull Long redemptionId, @NonNull User approver, String notes) {
        RewardRedemption redemption = redemptionRepository.findById(redemptionId)
                .orElseThrow(() -> new IllegalArgumentException("Redemption not found"));

        if (redemption.getStatus() != RewardStatus.REQUESTED) {
            throw new IllegalStateException("Redemption not in REQUESTED state");
        }

        // Deduct points when approving the redemption
        pointService.deductPoints(redemption.getUser(), redemption.getPointsSpent(), "REWARD",
                "Reward approved: " + redemption.getReward().getTitle(),
                redemptionId, approver);

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

        // Return points
        Long redemptionId2 = Objects.requireNonNull(redemption.getId(), "Redemption ID must not be null");
        pointService.addPoints(redemption.getUser(), redemption.getPointsSpent(),
                "REWARD", "Reward cancelled: " + redemption.getReward().getTitle(),
                redemptionId2, cancelledBy);

        return saved;
    }

    public List<RewardRedemption> findByUser(@NonNull User user) {
        return redemptionRepository.findByUserOrderByRequestedAtDesc(user);
    }

    public List<RewardRedemption> findPendingApprovals() {
        return redemptionRepository.findByStatus(RewardStatus.REQUESTED);
    }

    public List<RewardRedemption> findApproved() {
        return redemptionRepository.findByStatus(RewardStatus.APPROVED);
    }
}
