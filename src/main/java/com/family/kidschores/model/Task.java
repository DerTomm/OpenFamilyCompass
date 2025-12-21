package com.family.kidschores.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private int basePoints;  // Basis-Belohnungspunkte

    @ManyToOne
    @JoinColumn(name = "assigned_child_id")
    private Child assignedChild;  // Kann null sein, wenn für alle Kinder

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrenceType recurrenceType = RecurrenceType.ONCE;

    @Column(name = "due_date")
    private LocalDate dueDate;  // Fälligkeitsdatum

    @Column(name = "completed_at")
    private LocalDateTime completedAt;  // Von Kind als erledigt markiert

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;  // Von Eltern genehmigt

    @Column(name = "awarded_points")
    private Integer awardedPoints;  // Tatsächlich vergebene Punkte (kann abweichen)

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy;  // Welcher Elternteil hat genehmigt

    @Column(name = "parent_notes", columnDefinition = "TEXT")
    private String parentNotes;  // Notizen der Eltern

    @Column(name = "is_template")
    private boolean isTemplate = false;  // Für wiederkehrende Aufgaben

    @ManyToOne
    @JoinColumn(name = "template_id")
    private Task templateTask;  // Referenz zur ursprünglichen Vorlage

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
