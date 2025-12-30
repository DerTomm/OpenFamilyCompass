package org.openfamilycompass.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openfamilycompass.model.PointTransactionType;
import org.openfamilycompass.model.Reward;
import org.openfamilycompass.model.RewardRedemption;
import org.openfamilycompass.model.RewardStatus;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.repository.RewardRedemptionRepository;
import org.openfamilycompass.repository.RewardRepository;
import org.openfamilycompass.repository.UserRepository;
import org.openfamilycompass.service.PointService;
import org.openfamilycompass.service.RewardRedemptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RewardRedemptionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private RewardRedemptionRepository redemptionRepository;

    @Autowired
    private PointService pointService;

    @Autowired
    private RewardRedemptionService redemptionService;

    private User testParent;
    private User testChild;
    private Reward testReward;

    @BeforeEach
    void setUp() {
        // Find or create test parent
        testParent = userRepository.findByUsername("testparent").orElse(null);
        if (testParent == null) {
            testParent = new User();
            testParent.setUsername("testparent");
            testParent.setPassword("password");
            testParent.setRole(UserRole.PARENT);
            testParent.setFirstName("Test Parent");
            testParent.setActive(true);
            testParent.setTotalPoints(0);
            testParent = userRepository.save(testParent);
        }

        // Find or create test child
        testChild = userRepository.findByUsername("testchild").orElse(null);
        if (testChild == null) {
            testChild = new User();
            testChild.setUsername("testchild");
            testChild.setPassword("password");
            testChild.setRole(UserRole.CHILD);
            testChild.setFirstName("Test Child");
            testChild.setActive(true);
            testChild.setTotalPoints(0); // Will be updated by point transaction
            testChild = userRepository.save(testChild);

            // Add initial points via bonus transaction
            pointService.addPoints(testChild, 100, PointTransactionType.BONUS, "Initial test points", null, testParent);
        }

        // Find or create test reward
        testReward = rewardRepository.findAll().stream()
                .filter(r -> "Test Reward".equals(r.getTitle()))
                .findFirst()
                .orElse(null);
        if (testReward == null) {
            testReward = new Reward();
            testReward.setTitle("Test Reward");
            testReward.setDescription("A test reward");
            testReward.setPointsCost(50);
            testReward.setActive(true);
            testReward = rewardRepository.save(testReward);
        }

        // Request reward (this will deduct points)
        redemptionService.requestReward(testChild, testReward);
    }

    @Test
    @WithMockUser(username = "testparent", roles = "PARENT")
    @Commit
    void rejectRedemption_ShouldCancelRedemptionAndRefundPoints() throws Exception {
        // Given - Create fresh data for this test
        User testChildLocal = new User();
        testChildLocal.setUsername("testchild_reject");
        testChildLocal.setPassword("password");
        testChildLocal.setRole(UserRole.CHILD);
        testChildLocal.setFirstName("Test Child Reject");
        testChildLocal.setActive(true);
        testChildLocal.setTotalPoints(0);
        testChildLocal = userRepository.save(testChildLocal);

        // Add initial points
        pointService.addPoints(testChildLocal, 100, PointTransactionType.BONUS, "Initial test points", null,
                testParent);

        Reward testRewardLocal = new Reward();
        testRewardLocal.setTitle("Test Reward Reject");
        testRewardLocal.setDescription("A test reward for reject");
        testRewardLocal.setPointsCost(50);
        testRewardLocal.setActive(true);
        testRewardLocal = rewardRepository.save(testRewardLocal);

        // Request reward (this will deduct points)
        RewardRedemption testRedemptionLocal = redemptionService.requestReward(testChildLocal, testRewardLocal);

        // When
        mockMvc.perform(post("/parent/redemptions/{id}/reject", testRedemptionLocal.getId())
                .param("notes", "Not appropriate at this time")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/parent/redemptions/pending"));

        // Then
        RewardRedemption updatedRedemption = redemptionRepository.findById(testRedemptionLocal.getId()).orElseThrow();
        assertThat(updatedRedemption.getStatus()).isEqualTo(RewardStatus.CANCELLED);

        // Points should be refunded
        User updatedChild = userRepository.findById(testChildLocal.getId()).orElseThrow();
        assertThat(updatedChild.getTotalPoints()).isEqualTo(100); // Points refunded back to original 100
    }

    @Test
    @WithMockUser(username = "testparent", roles = "PARENT")
    @Commit
    void approveRedemption_ShouldApproveAndDeductPoints() throws Exception {
        // Given - Create fresh data for this test
        User testChildLocal = new User();
        testChildLocal.setUsername("testchild_approve");
        testChildLocal.setPassword("password");
        testChildLocal.setRole(UserRole.CHILD);
        testChildLocal.setFirstName("Test Child Approve");
        testChildLocal.setActive(true);
        testChildLocal.setTotalPoints(0);
        testChildLocal = userRepository.save(testChildLocal);

        // Add initial points
        pointService.addPoints(testChildLocal, 100, PointTransactionType.BONUS, "Initial test points", null,
                testParent);

        Reward testRewardLocal = new Reward();
        testRewardLocal.setTitle("Test Reward Approve");
        testRewardLocal.setDescription("A test reward for approve");
        testRewardLocal.setPointsCost(50);
        testRewardLocal.setActive(true);
        testRewardLocal = rewardRepository.save(testRewardLocal);

        // Request reward (this will deduct points)
        RewardRedemption testRedemptionLocal = redemptionService.requestReward(testChildLocal, testRewardLocal);

        // When
        mockMvc.perform(post("/parent/redemptions/{id}/approve", testRedemptionLocal.getId())
                .param("notes", "Well done!")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/parent/redemptions/pending"));

        // Then
        RewardRedemption updatedRedemption = redemptionRepository.findById(testRedemptionLocal.getId()).orElseThrow();
        assertThat(updatedRedemption.getApprovedBy().getId()).isEqualTo(testParent.getId());

        // Points should remain the same (already deducted on request)
        User updatedChild = userRepository.findById(testChildLocal.getId()).orElseThrow();
        assertThat(updatedChild.getTotalPoints()).isEqualTo(50); // No additional deduction
    }
}