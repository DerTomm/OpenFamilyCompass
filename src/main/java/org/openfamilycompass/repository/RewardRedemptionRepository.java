package org.openfamilycompass.repository;

import org.openfamilycompass.model.Child;
import org.openfamilycompass.model.RewardRedemption;
import org.openfamilycompass.model.RewardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardRedemptionRepository extends JpaRepository<RewardRedemption, Long> {
    
    List<RewardRedemption> findByChild(Child child);
    
    List<RewardRedemption> findByChildAndStatus(Child child, RewardStatus status);
    
    List<RewardRedemption> findByStatus(RewardStatus status);
    
    List<RewardRedemption> findByChildOrderByRequestedAtDesc(Child child);
}
