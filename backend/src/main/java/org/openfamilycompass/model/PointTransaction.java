package org.openfamilycompass.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "point_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int points; // Positive for credit, negative for deduction

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointTransactionType type; // "TASK", "REWARD", "BEHAVIOR", "PENALTY", "BONUS"

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "reference_id")
    private Long referenceId; // ID of the associated Task, Reward, etc.

    @Column(columnDefinition = "TEXT")
    private String remarks; // Additional remarks (e.g. for behavior rules)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointTransactionStatus status = PointTransactionStatus.COMPLETED;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Whether this transaction counts towards the user's visible balance.
     * Mirrors the filter in
     * {@code PointTransactionRepository#sumPointsByUser(User)}.
     *
     * <p>COMPLETED transactions always count. PENDING transactions only count
     * when they reduce the balance (reservation for a requested reward).
     * PENDING credits (e.g. a task awaiting parent approval) and CANCELLED
     * transactions do not affect the balance.
     */
    public boolean affectsBalance() {
        if (status == PointTransactionStatus.COMPLETED) {
            return true;
        }
        if (status == PointTransactionStatus.PENDING && points < 0) {
            return true;
        }
        return false;
    }
}
