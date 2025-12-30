package org.openfamilycompass.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.openfamilycompass.model.PointTransactionType;
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

    private User testChildUser;
    private Reward testReward;
    private User testUser;
    private RewardRedemption testRedemption;

    @BeforeEach
    void setUp() {
        testChildUser = new User();
        testChildUser.setId(1L);
        testChildUser.setUsername("testchild");
        testChildUser.setRole(UserRole.CHILD);
        testChildUser.setFirstName("TestChild");
        testChildUser.setTotalPoints(100);

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
        testRedemption.setUser(testChildUser);
        testRedemption.setReward(testReward);
        testRedemption.setPointsSpent(50);
        testRedemption.setStatus(RewardStatus.REQUESTED);
    }

    @Test
    void requestReward_ShouldCreateRedemption_WhenEnoughPoints() {
        // Given
        when(redemptionRepository.save(any(RewardRedemption.class))).thenReturn(testRedemption);

        // When
        RewardRedemption result = redemptionService.requestReward(testChildUser, testReward);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(RewardStatus.REQUESTED);
        verify(pointService).deductPoints(
                eq(testChildUser),
                eq(50),
                eq(PointTransactionType.REWARD),
                anyString(),
                anyLong(),
                eq(testChildUser));
    }

    @Test
    void requestReward_ShouldThrowException_WhenNotEnoughPoints() {
        // Given
        testChildUser.setTotalPoints(30);

        // When/Then
        assertThatThrownBy(() -> redemptionService.requestReward(testChildUser, testReward))
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
                eq(testChildUser),
                eq(50),
                eq(PointTransactionType.REWARD),
                anyString(),
                anyLong(),
                eq(testUser));
    }

    @Test
    void findByUser_ShouldReturnRedemptions() {
        // Given
        List<RewardRedemption> redemptions = Arrays.asList(testRedemption);
        when(redemptionRepository.findByUserOrderByRequestedAtDesc(testChildUser))
                .thenReturn(redemptions);

        // When
        List<RewardRedemption> result = redemptionService.findByUser(testChildUser);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser()).isEqualTo(testChildUser);
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
