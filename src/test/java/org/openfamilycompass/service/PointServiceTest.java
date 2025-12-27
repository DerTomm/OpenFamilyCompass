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
import org.openfamilycompass.model.Child;
import org.openfamilycompass.model.PointTransaction;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.repository.PointTransactionRepository;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @Mock
    private ChildService childService;

    @InjectMocks
    private PointService pointService;

    private Child testChild;
    private User testUser;

    @BeforeEach
    void setUp() {
        testChild = new Child();
        testChild.setId(1L);
        testChild.setFirstName("TestChild");
        testChild.setTotalPoints(100);

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
        when(pointTransactionRepository.sumPointsByChild(testChild)).thenReturn(150);

        // When
        PointTransaction result = pointService.addPoints(
                testChild, 50, "TASK", "Completed homework", 1L, testUser);

        // Then
        assertThat(result).isNotNull();
        verify(pointTransactionRepository).save(argThat(t -> t.getPoints() == 50 &&
                t.getType().equals("TASK") &&
                t.getDescription().equals("Completed homework")));
        verify(childService).updatePoints(1L, 150);
    }

    @Test
    void deductPoints_ShouldCreateNegativeTransaction() {
        // Given
        PointTransaction transaction = new PointTransaction();
        transaction.setPoints(-30);
        when(pointTransactionRepository.save(any(PointTransaction.class))).thenReturn(transaction);
        when(pointTransactionRepository.sumPointsByChild(testChild)).thenReturn(70);

        // When
        PointTransaction result = pointService.deductPoints(
                testChild, 30, "PENALTY", "Misbehavior", 1L, testUser);

        // Then
        assertThat(result).isNotNull();
        verify(pointTransactionRepository).save(argThat(t -> t.getPoints() == -30 &&
                t.getType().equals("PENALTY")));
    }

    @Test
    void addPointsWithRemarks_ShouldIncludeRemarks() {
        // Given
        when(pointTransactionRepository.save(any(PointTransaction.class))).thenReturn(new PointTransaction());
        when(pointTransactionRepository.sumPointsByChild(testChild)).thenReturn(110);

        // When
        pointService.addPointsWithRemarks(
                testChild, 10, "BEHAVIOR", "Good behavior", 1L, "Excellent work!", testUser);

        // Then
        verify(pointTransactionRepository).save(argThat(t -> t.getRemarks() != null &&
                t.getRemarks().equals("Excellent work!")));
    }

    @Test
    void updateChildPoints_ShouldCalculateTotalAndUpdate() {
        // Given
        when(pointTransactionRepository.sumPointsByChild(testChild)).thenReturn(250);

        // When
        pointService.updateChildPoints(testChild);

        // Then
        verify(childService).updatePoints(1L, 250);
    }

    @Test
    void updateChildPoints_ShouldHandleNullSum() {
        // Given
        when(pointTransactionRepository.sumPointsByChild(testChild)).thenReturn(null);

        // When
        pointService.updateChildPoints(testChild);

        // Then
        verify(childService).updatePoints(1L, 0);
    }

    @Test
    void getTransactionHistory_ShouldReturnTransactions() {
        // Given
        PointTransaction t1 = new PointTransaction();
        t1.setPoints(50);
        PointTransaction t2 = new PointTransaction();
        t2.setPoints(-20);
        List<PointTransaction> transactions = Arrays.asList(t1, t2);
        when(pointTransactionRepository.findByChildOrderByCreatedAtDesc(testChild))
                .thenReturn(transactions);

        // When
        List<PointTransaction> result = pointService.getTransactionHistory(testChild);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPoints()).isEqualTo(50);
        assertThat(result.get(1).getPoints()).isEqualTo(-20);
    }
}
