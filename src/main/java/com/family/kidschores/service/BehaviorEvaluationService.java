package com.family.kidschores.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.family.kidschores.model.Behavior;
import com.family.kidschores.model.BehaviorEvaluation;
import com.family.kidschores.model.Child;
import com.family.kidschores.model.User;
import com.family.kidschores.repository.BehaviorEvaluationRepository;
import com.family.kidschores.repository.BehaviorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BehaviorEvaluationService {

    private final BehaviorEvaluationRepository evaluationRepository;
    private final BehaviorRepository behaviorRepository;
    private final PointService pointService;

    /**
     * Ermittelt den Start der aktuellen Woche (Montag 00:00)
     */
    private LocalDateTime getCurrentWeekStart() {
        return LocalDateTime.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    /**
     * Aktualisiert oder erstellt eine Bewertung für ein Verhalten
     */
    @Transactional
    public BehaviorEvaluation updateEvaluation(@NonNull Long behaviorId,
            @NonNull Child child,
            int currentPoints,
            String remarks,
            @NonNull User updatedBy) {
        Behavior behavior = behaviorRepository.findById(behaviorId)
                .orElseThrow(() -> new IllegalArgumentException("Behavior not found"));

        if (!behavior.isActive()) {
            throw new IllegalStateException("Behavior is not active");
        }

        // Punkte dürfen nicht negativ sein oder das Maximum überschreiten
        if (currentPoints < 0 || currentPoints > behavior.getPoints()) {
            throw new IllegalArgumentException(
                    "Points must be between 0 and " + behavior.getPoints());
        }

        LocalDateTime weekStart = getCurrentWeekStart();

        // Vorhandene Bewertung suchen oder neue erstellen
        BehaviorEvaluation evaluation = evaluationRepository
                .findByChildAndBehaviorAndWeekStartDateAndCommittedFalse(child, behavior, weekStart)
                .orElseGet(() -> {
                    BehaviorEvaluation newEval = new BehaviorEvaluation();
                    newEval.setBehavior(behavior);
                    newEval.setChild(child);
                    newEval.setWeekStartDate(weekStart);
                    newEval.setCreatedBy(updatedBy);
                    return newEval;
                });

        evaluation.setCurrentPoints(currentPoints);
        evaluation.setRemarks(remarks);

        return evaluationRepository.save(evaluation);
    }

    /**
     * Lädt alle nicht eingecheckten Bewertungen für ein Kind der aktuellen Woche
     */
    @Transactional(readOnly = true)
    public List<BehaviorEvaluation> getCurrentWeekEvaluations(@NonNull Child child) {
        LocalDateTime weekStart = getCurrentWeekStart();
        return evaluationRepository.findByChildAndWeekStartDateAndCommittedFalse(child, weekStart);
    }

    /**
     * Lädt eine spezifische Bewertung
     */
    @Transactional(readOnly = true)
    public Optional<BehaviorEvaluation> getEvaluation(Long evaluationId) {
        return evaluationRepository.findById(evaluationId);
    }

    /**
     * Bucht alle Bewertungen eines Kindes der aktuellen Woche ein
     */
    @Transactional
    public void commitWeeklyEvaluations(@NonNull Child child, @NonNull User committedBy) {
        LocalDateTime weekStart = getCurrentWeekStart();
        List<BehaviorEvaluation> evaluations = evaluationRepository
                .findByChildAndWeekStartDateAndCommittedFalse(child, weekStart);

        if (evaluations.isEmpty()) {
            throw new IllegalStateException("No evaluations to commit for this week");
        }

        // Für jede Bewertung eine separate Transaktion erstellen
        for (BehaviorEvaluation evaluation : evaluations) {
            if (evaluation.getCurrentPoints() > 0) {
                // Punkte gutschreiben
                String description = "Wochenverhalten: " + evaluation.getBehavior().getTitle();

                pointService.addPointsWithRemarks(
                        child,
                        evaluation.getCurrentPoints(),
                        "BEHAVIOR",
                        description,
                        evaluation.getBehavior().getId(),
                        evaluation.getRemarks(),
                        committedBy);
            }

            // Bewertung als eingebucht markieren
            evaluation.setCommitted(true);
            evaluationRepository.save(evaluation);
        }
    }

    /**
     * Lädt alle aktiven Verhaltensregeln für ein Kind (inkl. globale Regeln)
     */
    @Transactional(readOnly = true)
    public List<Behavior> getActiveBehaviorsForChild(@NonNull Child child) {
        List<Behavior> childSpecific = behaviorRepository.findByChildAndActiveTrue(child);
        List<Behavior> global = behaviorRepository.findByChildIsNullAndActiveTrue();

        childSpecific.addAll(global);
        return childSpecific;
    }

    /**
     * Berechnet die Gesamtpunktzahl der aktuellen Woche für ein Kind
     */
    @Transactional(readOnly = true)
    public int calculateWeeklyTotal(@NonNull Child child) {
        return getCurrentWeekEvaluations(child).stream()
                .mapToInt(BehaviorEvaluation::getCurrentPoints)
                .sum();
    }
}
