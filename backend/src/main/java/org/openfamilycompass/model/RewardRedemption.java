package org.openfamilycompass.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reward_redemptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "reward_id", nullable = false)
    private Reward reward;

    @Column(nullable = false)
    private int pointsSpent; // Punkte, die ausgegeben wurden

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RewardStatus status = RewardStatus.REQUESTED;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onRequest() {
        requestedAt = LocalDateTime.now();
    }
}
