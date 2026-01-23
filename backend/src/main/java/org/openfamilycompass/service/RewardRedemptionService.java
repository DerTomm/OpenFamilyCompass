package org.openfamilycompass.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.openfamilycompass.model.NotificationType;
import org.openfamilycompass.model.PointTransactionType;
import org.openfamilycompass.model.Reward;
import org.openfamilycompass.model.RewardRedemption;
import org.openfamilycompass.model.RewardStatus;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.repository.RewardRedemptionRepository;
import org.openfamilycompass.repository.UserRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RewardRedemptionService {

    private final RewardRedemptionRepository redemptionRepository;
    private final PointService pointService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

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

        // Deduct points immediately when requesting the reward
        pointService.deductPoints(user, reward.getPointsCost(), PointTransactionType.REWARD,
                "Reward requested: " + reward.getTitle(), saved.getId(), user);

        // Notify all parents about reward request
        List<User> parents = userRepository.findByRole(UserRole.PARENT);
        for (User parent : parents) {
            notificationService.createNotification(
                    parent,
                    NotificationType.REWARD_REQUESTED,
                    "Belohnungsanfrage",
                    String.format("%s möchte die Belohnung '%s' (%d Punkte) einlösen.",
                            user.getFirstName(), reward.getTitle(), reward.getPointsCost()),
                    saved.getId());
        }

        return saved;
    }

    @Transactional
    public RewardRedemption approveRedemption(@NonNull Long redemptionId, @NonNull User approver, String notes) {
        RewardRedemption redemption = redemptionRepository.findById(redemptionId)
                .orElseThrow(() -> new IllegalArgumentException("Redemption not found"));

        if (redemption.getStatus() != RewardStatus.REQUESTED) {
            throw new IllegalStateException("Redemption not in REQUESTED state");
        }

        // Points are already deducted when requesting, so no need to deduct again

        redemption.setStatus(RewardStatus.APPROVED);
        redemption.setApprovedAt(LocalDateTime.now());
        redemption.setApprovedBy(approver);
        redemption.setNotes(notes);

        RewardRedemption saved = redemptionRepository.save(redemption);

        // Notify child about approved reward
        notificationService.createNotification(
                redemption.getUser(),
                NotificationType.REWARD_APPROVED,
                "Belohnung genehmigt",
                String.format("Deine Belohnung '%s' wurde genehmigt! %s",
                        redemption.getReward().getTitle(),
                        notes != null ? "Notiz: " + notes : ""),
                redemption.getId());

        return saved;
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

        // Return points since they were deducted when the reward was requested
        Long redemptionId2 = Objects.requireNonNull(redemption.getId(), "Redemption ID must not be null");
        pointService.addPoints(redemption.getUser(), redemption.getPointsSpent(),
                PointTransactionType.REWARD, "Reward cancelled: " + redemption.getReward().getTitle(),
                redemptionId2, cancelledBy);

        redemption.setStatus(RewardStatus.CANCELLED);

        RewardRedemption saved = redemptionRepository.save(redemption);

        // Notify child about rejected reward
        notificationService.createNotification(
                redemption.getUser(),
                NotificationType.REWARD_REJECTED,
                "Belohnung abgelehnt",
                String.format("Deine Belohnung '%s' wurde abgelehnt. Die Punkte wurden zurückerstattet.",
                        redemption.getReward().getTitle()),
                redemption.getId());

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

    public java.util.Optional<RewardRedemption> findById(@NonNull Long id) {
        return redemptionRepository.findById(id);
    }

    public List<RewardRedemption> findAll() {
        return redemptionRepository.findAll();
    }

    public List<RewardRedemption> findByStatus(@NonNull RewardStatus status) {
        return redemptionRepository.findByStatus(status);
    }
}
