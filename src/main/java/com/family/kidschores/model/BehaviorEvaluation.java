package com.family.kidschores.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the weekly evaluation of a behavior for a child.
 * Contains the current point value and remarks for history.
 */
@Entity
@Table(name = "behavior_evaluations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BehaviorEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "behavior_id", nullable = false)
    private Behavior behavior;

    @ManyToOne
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Column(nullable = false)
    private int currentPoints; // Currently assigned points (0 to max. points of the behavior)

    @Column(columnDefinition = "TEXT")
    private String remarks; // Remarks for the history of the current week

    @Column(name = "week_start_date", nullable = false)
    private LocalDateTime weekStartDate; // Start of the evaluation week

    @Column(nullable = false)
    private boolean committed = false; // Has the evaluation already been committed?

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
