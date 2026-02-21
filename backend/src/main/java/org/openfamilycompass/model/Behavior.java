package org.openfamilycompass.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "behaviors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Behavior {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String guideline; // Guideline for the behavior

    @Column(nullable = false)
    private int points; // Max. plus points per week

    @Column(name = "minus_points", nullable = false)
    private int minusPoints = 0; // Max. minus points per week (as positive number)

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Can be null for all users

    @Column(nullable = false)
    private int rank = 0; // For ordering behaviors

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
