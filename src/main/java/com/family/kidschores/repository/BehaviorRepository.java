package com.family.kidschores.repository;

import com.family.kidschores.model.Child;
import com.family.kidschores.model.Behavior;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BehaviorRepository extends JpaRepository<Behavior, Long> {
    
    List<Behavior> findByActiveTrue();
    
    List<Behavior> findByChild(Child child);
    
    List<Behavior> findByChildIsNullAndActiveTrue(); // Verhaltensregeln für alle Kinder
}
