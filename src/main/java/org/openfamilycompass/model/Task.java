package org.openfamilycompass.model;

import java.time.LocalDate;
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
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int basePoints; // Base reward points

    @ManyToOne
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser; // Can be null if for all users

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrenceType recurrenceType = RecurrenceType.ONCE;

    @Column(name = "due_date")
    private LocalDate dueDate; // Due date

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

    @Column(name = "is_template")
    private boolean isTemplate = false; // For recurring tasks

    @ManyToOne
    @JoinColumn(name = "template_id")
    private Task templateTask; // Reference to original template

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
