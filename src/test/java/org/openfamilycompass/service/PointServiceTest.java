package org.openfamilycompass.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openfamilycompass.model.PointTransaction;
import org.openfamilycompass.model.PointTransactionType;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.repository.PointTransactionRepository;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private PointService pointService;

    private User testUserChild;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUserChild = new User();
        testUserChild.setId(1L);
        testUserChild.setFirstName("TestChild");
        testUserChild.setRole(UserRole.CHILD);
        testUserChild.setTotalPoints(100);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("parent");
        testUser.setRole(UserRole.PARENT);
    }

    @Test
    void addPoints_ShouldCreateTransactionAndUpdateChildPoints() {
        // Given
        PointTransaction transaction = new PointTransaction();
        transaction.setId(1L);
        transaction.setPoints(50);
        when(pointTransactionRepository.save(any(PointTransaction.class))).thenReturn(transaction);
        when(pointTransactionRepository.sumPointsByUser(testUserChild)).thenReturn(150);

        // When
        PointTransaction result = pointService.addPoints(
                testUserChild, 50, PointTransactionType.TASK, "Completed homework", 1L, testUser);

        // Then
        assertThat(result).isNotNull();
        verify(pointTransactionRepository).save(argThat(t -> t.getPoints() == 50 &&
                t.getType().equals(PointTransactionType.TASK) &&
                t.getDescription().equals("Completed homework")));
        verify(userService).save(testUserChild);
    }

    @Test
    void deductPoints_ShouldCreateNegativeTransaction() {
        // Given
        PointTransaction transaction = new PointTransaction();
        transaction.setPoints(-30);
        when(pointTransactionRepository.save(any(PointTransaction.class))).thenReturn(transaction);
        when(pointTransactionRepository.sumPointsByUser(testUserChild)).thenReturn(70);

        // When
        PointTransaction result = pointService.deductPoints(
                testUserChild, 30, PointTransactionType.PENALTY, "Misbehavior", 1L, testUser);

        // Then
        assertThat(result).isNotNull();
        verify(pointTransactionRepository).save(argThat(t -> t.getPoints() == -30 &&
                t.getType().equals(PointTransactionType.PENALTY)));
        verify(userService).save(testUserChild);
    }

    @Test
    void addPointsWithRemarks_ShouldIncludeRemarks() {
        // Given
        when(pointTransactionRepository.save(any(PointTransaction.class))).thenReturn(new PointTransaction());
        when(pointTransactionRepository.sumPointsByUser(testUserChild)).thenReturn(110);

        // When
        pointService.addPointsWithRemarks(
                testUserChild, 10, PointTransactionType.BEHAVIOR, "Good behavior", 1L, "Excellent work!", testUser);

        // Then
        verify(pointTransactionRepository).save(argThat(t -> t.getRemarks() != null &&
                t.getRemarks().equals("Excellent work!")));
        verify(userService).save(testUserChild);
    }

    @Test
    void updateUserPoints_ShouldCalculateTotalAndUpdate() {
        // Given
        when(pointTransactionRepository.sumPointsByUser(testUserChild)).thenReturn(250);

        // When
        pointService.updateUserPoints(testUserChild);

        // Then
        assertThat(testUserChild.getTotalPoints()).isEqualTo(250);
        verify(userService).save(testUserChild);
    }

    @Test
    void updateUserPoints_ShouldHandleNullSum() {
        // Given
        when(pointTransactionRepository.sumPointsByUser(testUserChild)).thenReturn(null);

        // When
        pointService.updateUserPoints(testUserChild);

        // Then
        assertThat(testUserChild.getTotalPoints()).isEqualTo(0);
        verify(userService).save(testUserChild);
    }

    @Test
    void getTransactionHistory_ShouldReturnTransactions() {
        // Given
        PointTransaction t1 = new PointTransaction();
        t1.setPoints(50);
        PointTransaction t2 = new PointTransaction();
        t2.setPoints(-20);
        List<PointTransaction> transactions = Arrays.asList(t1, t2);
        when(pointTransactionRepository.findByUserOrderByCreatedAtDesc(testUserChild))
                .thenReturn(transactions);

        // When
        List<PointTransaction> result = pointService.getTransactionHistory(testUserChild);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPoints()).isEqualTo(50);
        assertThat(result.get(1).getPoints()).isEqualTo(-20);
    }
}
