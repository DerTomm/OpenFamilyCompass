package com.family.kidschores.repository;

import com.family.kidschores.model.Child;
import com.family.kidschores.model.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    
    List<PointTransaction> findByChild(Child child);
    
    List<PointTransaction> findByChildOrderByCreatedAtDesc(Child child);
    
    @Query("SELECT SUM(pt.points) FROM PointTransaction pt WHERE pt.child = :child")
    Integer sumPointsByChild(Child child);
}
