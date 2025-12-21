package com.family.kidschores.repository;

import com.family.kidschores.model.Child;
import com.family.kidschores.model.Penalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PenaltyRepository extends JpaRepository<Penalty, Long> {
    
    List<Penalty> findByChild(Child child);
    
    List<Penalty> findByChildOrderByCreatedAtDesc(Child child);
}
