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
@Table(name = "task_instances")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "task_definition_id", nullable = false)
    private TaskDefinition taskDefinition;

    @ManyToOne
    @JoinColumn(name = "assigned_user_id", nullable = false)
    private User assignedUser; // Das spezifische Kind

    @Column(name = "deadline")
    private LocalDateTime deadline; // Deadline of this instance (date+time, null = no deadline)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    @Column(name = "completed_at")
    private LocalDateTime completedAt; // Marked as completed by child

    @Column(name = "approved_at")
    private LocalDateTime approvedAt; // Approved by parents

    @Column(name = "awarded_points")
    private Integer awardedPoints; // Actually awarded points (can differ)

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy; // Which parent approved

    @Column(name = "parent_notes", columnDefinition = "TEXT")
    private String parentNotes; // Notes from parents

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}