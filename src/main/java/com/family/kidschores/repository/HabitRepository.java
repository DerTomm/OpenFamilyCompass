package com.family.kidschores.repository;

import com.family.kidschores.model.Child;
import com.family.kidschores.model.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HabitRepository extends JpaRepository<Habit, Long> {
    
    List<Habit> findByActiveTrue();
    
    List<Habit> findByChild(Child child);
    
    List<Habit> findByChildIsNullAndActiveTrue(); // Gewohnheiten für alle Kinder
}
