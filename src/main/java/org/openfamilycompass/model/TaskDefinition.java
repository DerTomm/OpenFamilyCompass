package org.openfamilycompass.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "task_definitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int basePoints; // Base reward points

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrenceType recurrenceType = RecurrenceType.ONCE;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "task_definition_assigned_users", joinColumns = @JoinColumn(name = "task_definition_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> assignedUsers; // Kinder, denen die Aufgabe gilt

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy; // Elternteil, der die Definition erstellt hat

    @Column(name = "start_date")
    private LocalDate startDate; // Optional: Wann die Aufgabe startet

    @Column(name = "end_date")
    private LocalDate endDate; // Optional: Wann die Aufgabe endet

    @Column(name = "weekly_days", columnDefinition = "TEXT")
    private String weeklyDays; // JSON oder kommasepariert: MONDAY,TUESDAY für WEEKLY

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}