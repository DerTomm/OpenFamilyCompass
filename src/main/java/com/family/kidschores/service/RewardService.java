package com.family.kidschores.service;

import com.family.kidschores.model.Reward;
import com.family.kidschores.repository.RewardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RewardService {

    private final RewardRepository rewardRepository;

    @Transactional
    public Reward createReward(String title, String description, int pointsCost, String imagePath) {
        Reward reward = new Reward();
        reward.setTitle(title);
        reward.setDescription(description);
        reward.setPointsCost(pointsCost);
        reward.setImagePath(imagePath);
        reward.setActive(true);

        return rewardRepository.save(reward);
    }

    @Transactional
    public Reward updateReward(Long id, String title, String description, 
                              int pointsCost, String imagePath) {
        Reward reward = rewardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reward not found"));

        reward.setTitle(title);
        reward.setDescription(description);
        reward.setPointsCost(pointsCost);
        if (imagePath != null) {
            reward.setImagePath(imagePath);
        }

        return rewardRepository.save(reward);
    }

    @Transactional
    public void deactivateReward(Long id) {
        Reward reward = rewardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reward not found"));
        
        reward.setActive(false);
        rewardRepository.save(reward);
    }

    public Optional<Reward> findById(Long id) {
        return rewardRepository.findById(id);
    }

    public List<Reward> findAllActive() {
        return rewardRepository.findByActiveTrue();
    }

    public List<Reward> findAll() {
        return rewardRepository.findAllByOrderByPointsCostAsc();
    }
}
