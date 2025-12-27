package org.openfamilycompass.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openfamilycompass.model.Child;
import org.openfamilycompass.model.Reward;
import org.openfamilycompass.model.RewardRedemption;
import org.openfamilycompass.model.RewardStatus;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.repository.RewardRedemptionRepository;

@ExtendWith(MockitoExtension.class)
class RewardRedemptionServiceTest {

    @Mock
    private RewardRedemptionRepository redemptionRepository;

    @Mock
    private PointService pointService;

    @InjectMocks
    private RewardRedemptionService redemptionService;

    private Child testChild;
    private Reward testReward;
    private User testUser;
    private RewardRedemption testRedemption;

    @BeforeEach
    void setUp() {
        testChild = new Child();
        testChild.setId(1L);
        testChild.setFirstName("TestChild");
        testChild.setTotalPoints(100);

        testReward = new Reward();
        testReward.setId(1L);
        testReward.setTitle("Video Game");
        testReward.setPointsCost(50);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("parent");
        testUser.setRole(UserRole.PARENT);

        testRedemption = new RewardRedemption();
        testRedemption.setId(1L);
        testRedemption.setChild(testChild);
        testRedemption.setReward(testReward);
        testRedemption.setPointsSpent(50);
        testRedemption.setStatus(RewardStatus.REQUESTED);
    }

    @Test
    void requestReward_ShouldCreateRedemption_WhenEnoughPoints() {
        // Given
        when(redemptionRepository.save(any(RewardRedemption.class))).thenReturn(testRedemption);

        // When
        RewardRedemption result = redemptionService.requestReward(testChild, testReward);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(RewardStatus.REQUESTED);
        verify(pointService).deductPoints(
                eq(testChild),
                eq(50),
                eq("REWARD"),
                anyString(),
                anyLong(),
                isNull());
    }

    @Test
    void requestReward_ShouldThrowException_WhenNotEnoughPoints() {
        // Given
        testChild.setTotalPoints(30);

        // When/Then
        assertThatThrownBy(() -> redemptionService.requestReward(testChild, testReward))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not enough points");
    }

    @Test
    void approveRedemption_ShouldApproveAndSetFields() {
        // Given
        when(redemptionRepository.findById(1L)).thenReturn(Optional.of(testRedemption));
        when(redemptionRepository.save(any(RewardRedemption.class))).thenReturn(testRedemption);

        // When
        RewardRedemption result = redemptionService.approveRedemption(1L, testUser, "Approved!");

        // Then
        assertThat(result.getStatus()).isEqualTo(RewardStatus.APPROVED);
        assertThat(result.getApprovedBy()).isEqualTo(testUser);
        assertThat(result.getApprovedAt()).isNotNull();
        assertThat(result.getNotes()).isEqualTo("Approved!");
    }

    @Test
    void approveRedemption_ShouldThrowException_WhenNotInRequestedState() {
        // Given
        testRedemption.setStatus(RewardStatus.APPROVED);
        when(redemptionRepository.findById(1L)).thenReturn(Optional.of(testRedemption));

        // When/Then
        assertThatThrownBy(() -> redemptionService.approveRedemption(1L, testUser, "Notes"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not in REQUESTED state");
    }

    @Test
    void markAsDelivered_ShouldUpdateStatus() {
        // Given
        testRedemption.setStatus(RewardStatus.APPROVED);
        when(redemptionRepository.findById(1L)).thenReturn(Optional.of(testRedemption));
        when(redemptionRepository.save(any(RewardRedemption.class))).thenReturn(testRedemption);

        // When
        RewardRedemption result = redemptionService.markAsDelivered(1L);

        // Then
        assertThat(result.getStatus()).isEqualTo(RewardStatus.DELIVERED);
        assertThat(result.getDeliveredAt()).isNotNull();
    }

    @Test
    void cancelRedemption_ShouldRefundPoints() {
        // Given
        when(redemptionRepository.findById(1L)).thenReturn(Optional.of(testRedemption));
        when(redemptionRepository.save(any(RewardRedemption.class))).thenReturn(testRedemption);

        // When
        RewardRedemption result = redemptionService.cancelRedemption(1L, testUser);

        // Then
        assertThat(result.getStatus()).isEqualTo(RewardStatus.CANCELLED);
        verify(pointService).addPoints(
                eq(testChild),
                eq(50),
                eq("REWARD"),
                anyString(),
                anyLong(),
                eq(testUser));
    }

    @Test
    void findByChild_ShouldReturnRedemptions() {
        // Given
        List<RewardRedemption> redemptions = Arrays.asList(testRedemption);
        when(redemptionRepository.findByChildOrderByRequestedAtDesc(testChild))
                .thenReturn(redemptions);

        // When
        List<RewardRedemption> result = redemptionService.findByChild(testChild);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChild()).isEqualTo(testChild);
    }

    @Test
    void findPendingApprovals_ShouldReturnRequestedRedemptions() {
        // Given
        List<RewardRedemption> pending = Arrays.asList(testRedemption);
        when(redemptionRepository.findByStatus(RewardStatus.REQUESTED))
                .thenReturn(pending);

        // When
        List<RewardRedemption> result = redemptionService.findPendingApprovals();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(RewardStatus.REQUESTED);
    }
}
