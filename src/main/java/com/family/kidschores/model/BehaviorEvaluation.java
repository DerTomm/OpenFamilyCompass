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
 * Repräsentiert die wöchentliche Bewertung eines Verhaltens für ein Kind.
 * Enthält den aktuellen Punktwert und Bemerkungen zur Historie.
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
    private int currentPoints; // Aktuell zugewiesene Punkte (0 bis max. Punkte des Verhaltens)

    @Column(columnDefinition = "TEXT")
    private String remarks; // Bemerkungen zur Historie der aktuellen Woche

    @Column(name = "week_start_date", nullable = false)
    private LocalDateTime weekStartDate; // Start der Bewertungswoche

    @Column(nullable = false)
    private boolean committed = false; // Wurde die Bewertung bereits eingebucht?

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
