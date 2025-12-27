package org.openfamilycompass.repository;

import org.openfamilycompass.model.Child;
import org.openfamilycompass.model.Penalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PenaltyRepository extends JpaRepository<Penalty, Long> {
    
    List<Penalty> findByChild(Child child);
    
    List<Penalty> findByChildOrderByCreatedAtDesc(Child child);
}
