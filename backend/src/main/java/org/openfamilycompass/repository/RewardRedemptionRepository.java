package org.openfamilycompass.repository;

import java.util.List;

import org.openfamilycompass.model.RewardRedemption;
import org.openfamilycompass.model.RewardStatus;
import org.openfamilycompass.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RewardRedemptionRepository extends JpaRepository<RewardRedemption, Long> {

    List<RewardRedemption> findByUser(User user);

    List<RewardRedemption> findByUserAndStatus(User user, RewardStatus status);

    List<RewardRedemption> findByStatus(RewardStatus status);

    List<RewardRedemption> findByUserOrderByRequestedAtDesc(User user);
}
