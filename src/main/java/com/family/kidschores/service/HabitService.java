package com.family.kidschores.service;

import com.family.kidschores.model.Child;
import com.family.kidschores.model.Habit;
import com.family.kidschores.model.User;
import com.family.kidschores.repository.HabitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HabitService {

    private final HabitRepository habitRepository;
    private final PointService pointService;

    @Transactional
    public Habit createHabit(String title, String description, int points, Child child) {
        Habit habit = new Habit();
        habit.setTitle(title);
        habit.setDescription(description);
        habit.setPoints(points);
        habit.setChild(child);
        habit.setActive(true);

        return habitRepository.save(habit);
    }

    @Transactional
    public void recordHabit(Long habitId, Child child, User recordedBy) {
        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new IllegalArgumentException("Habit not found"));

        if (!habit.isActive()) {
            throw new IllegalStateException("Habit is not active");
        }

        // Punkte gutschreiben
        pointService.addPoints(child, habit.getPoints(), "HABIT", 
                              "Positive Gewohnheit: " + habit.getTitle(), 
                              habit.getId(), recordedBy);
    }

    @Transactional
    public void deactivateHabit(Long habitId) {
        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new IllegalArgumentException("Habit not found"));
        
        habit.setActive(false);
        habitRepository.save(habit);
    }

    public List<Habit> findAllActive() {
        return habitRepository.findByActiveTrue();
    }

    public List<Habit> findByChild(Child child) {
        return habitRepository.findByChild(child);
    }

    public List<Habit> findGlobalHabits() {
        return habitRepository.findByChildIsNullAndActiveTrue();
    }
}
