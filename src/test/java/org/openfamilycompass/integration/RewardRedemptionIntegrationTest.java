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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
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

    private User testParent;
    private User testChild;
    private Reward testReward;
    private RewardRedemption testRedemption;

    @BeforeEach
    void setUp() {
        // Create test parent
        testParent = new User();
        testParent.setUsername("testparent");
        testParent.setPassword("password");
        testParent.setRole(UserRole.PARENT);
        testParent.setFirstName("Test Parent");
        testParent.setActive(true);
        testParent.setTotalPoints(0);
        testParent = userRepository.save(testParent);

        // Create test child
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

        // Create test reward
        testReward = new Reward();
        testReward.setTitle("Test Reward");
        testReward.setDescription("A test reward");
        testReward.setPointsCost(50);
        testReward.setActive(true);
        testReward = rewardRepository.save(testReward);

        // Create test redemption
        testRedemption = new RewardRedemption();
        testRedemption.setUser(testChild);
        testRedemption.setReward(testReward);
        testRedemption.setPointsSpent(50);
        testRedemption.setStatus(RewardStatus.REQUESTED);
        testRedemption = redemptionRepository.save(testRedemption);
    }

    @Test
    @WithMockUser(username = "testparent", roles = "PARENT")
    void rejectRedemption_ShouldCancelRedemptionAndRefundPoints() throws Exception {
        // Given
        int initialPoints = testChild.getTotalPoints();

        // When
        mockMvc.perform(post("/parent/redemptions/{id}/reject", testRedemption.getId())
                .param("notes", "Not appropriate at this time")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/parent/redemptions/pending"));

        // Then
        RewardRedemption updatedRedemption = redemptionRepository.findById(testRedemption.getId()).orElseThrow();
        assertThat(updatedRedemption.getStatus()).isEqualTo(RewardStatus.CANCELLED);

        // Points should be refunded
        User updatedChild = userRepository.findById(testChild.getId()).orElseThrow();
        assertThat(updatedChild.getTotalPoints()).isEqualTo(initialPoints); // Points refunded
    }

    @Test
    @WithMockUser(username = "testparent", roles = "PARENT")
    void approveRedemption_ShouldApproveAndDeductPoints() throws Exception {
        // Given
        int initialPoints = testChild.getTotalPoints();

        // When
        mockMvc.perform(post("/parent/redemptions/{id}/approve", testRedemption.getId())
                .param("notes", "Well done!")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/parent/redemptions/pending"));

        // Then
        RewardRedemption updatedRedemption = redemptionRepository.findById(testRedemption.getId()).orElseThrow();
        assertThat(updatedRedemption.getStatus()).isEqualTo(RewardStatus.APPROVED);
        assertThat(updatedRedemption.getApprovedBy()).isEqualTo(testParent);

        // Points should be deducted
        User updatedChild = userRepository.findById(testChild.getId()).orElseThrow();
        assertThat(updatedChild.getTotalPoints()).isEqualTo(initialPoints - 50);
    }
}