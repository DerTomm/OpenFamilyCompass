package org.openfamilycompass.repository;

import org.openfamilycompass.model.Reward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Long> {
    
    List<Reward> findByActiveTrue();
    
    List<Reward> findAllByOrderByPointsCostAsc();
}
