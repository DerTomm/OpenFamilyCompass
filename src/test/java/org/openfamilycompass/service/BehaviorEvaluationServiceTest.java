package org.openfamilycompass.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
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
import org.openfamilycompass.model.Behavior;
import org.openfamilycompass.model.BehaviorEvaluation;
import org.openfamilycompass.model.PointTransactionType;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.repository.BehaviorEvaluationRepository;
import org.openfamilycompass.repository.BehaviorRepository;

@ExtendWith(MockitoExtension.class)
class BehaviorEvaluationServiceTest {

        @Mock
        private BehaviorEvaluationRepository evaluationRepository;

        @Mock
        private BehaviorRepository behaviorRepository;

        @Mock
        private PointService pointService;

        @InjectMocks
        private BehaviorEvaluationService evaluationService;

        private Behavior testBehavior;
        private User testUserChild;
        private User testUser;
        private BehaviorEvaluation testEvaluation;

        @BeforeEach
        void setUp() {
                testUserChild = new User();
                testUserChild.setId(1L);
                testUserChild.setFirstName("TestChild");
                testUserChild.setRole(UserRole.CHILD);

                testUser = new User();
                testUser.setId(1L);
                testUser.setUsername("parent");
                testUser.setRole(UserRole.PARENT);

                testBehavior = new Behavior();
                testBehavior.setId(1L);
                testBehavior.setTitle("Respect");
                testBehavior.setGuideline("Be respectful to others");
                testBehavior.setPoints(10);
                testBehavior.setActive(true);

                testEvaluation = new BehaviorEvaluation();
                testEvaluation.setId(1L);
                testEvaluation.setBehavior(testBehavior);
                testEvaluation.setUser(testUserChild);
                testEvaluation.setCurrentPoints(8);
                testEvaluation.setCommitted(false);
        }

        @Test
        void updateEvaluation_ShouldCreateNewEvaluation_WhenNotExists() {
                // Given
                when(behaviorRepository.findById(1L)).thenReturn(Optional.of(testBehavior));
                when(evaluationRepository.findByUserAndBehaviorAndCommittedFalse(
                                any(), any())).thenReturn(Optional.empty());
                when(evaluationRepository.save(any(BehaviorEvaluation.class))).thenReturn(testEvaluation);

                // When
                BehaviorEvaluation result = evaluationService.updateEvaluation(
                                1L, testUserChild, 8, "Good progress", testUser);

                // Then
                assertThat(result).isNotNull();
                verify(evaluationRepository).save(any(BehaviorEvaluation.class));
        }

        @Test
        void updateEvaluation_ShouldUpdateExisting_WhenExists() {
                // Given
                when(behaviorRepository.findById(1L)).thenReturn(Optional.of(testBehavior));
                when(evaluationRepository.findByUserAndBehaviorAndCommittedFalse(
                                any(), any())).thenReturn(Optional.of(testEvaluation));
                when(evaluationRepository.save(any(BehaviorEvaluation.class))).thenReturn(testEvaluation);

                // When
                BehaviorEvaluation result = evaluationService.updateEvaluation(
                                1L, testUserChild, 9, "Excellent!", testUser);

                // Then
                assertThat(result.getCurrentPoints()).isEqualTo(9);
                assertThat(result.getRemarks()).isEqualTo("Excellent!");
        }

        @Test
        void updateEvaluation_ShouldThrowException_WhenBehaviorNotActive() {
                // Given
                testBehavior.setActive(false);
                when(behaviorRepository.findById(1L)).thenReturn(Optional.of(testBehavior));

                // When/Then
                assertThatThrownBy(() -> evaluationService.updateEvaluation(
                                1L, testUserChild, 5, "Notes", testUser))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("not active");
        }

        @Test
        void updateEvaluation_ShouldThrowException_WhenPointsNegative() {
                // Given
                when(behaviorRepository.findById(1L)).thenReturn(Optional.of(testBehavior));

                // When/Then
                assertThatThrownBy(() -> evaluationService.updateEvaluation(
                                1L, testUserChild, -1, "Notes", testUser))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("between 0 and");
        }

        @Test
        void updateEvaluation_ShouldThrowException_WhenPointsExceedMaximum() {
                // Given
                when(behaviorRepository.findById(1L)).thenReturn(Optional.of(testBehavior));

                // When/Then
                assertThatThrownBy(() -> evaluationService.updateEvaluation(
                                1L, testUserChild, 15, "Notes", testUser))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("between 0 and");
        }

        @Test
        void getCurrentWeekEvaluations_ShouldReturnEvaluations() {
                // Given
                List<BehaviorEvaluation> evaluations = Arrays.asList(testEvaluation);
                when(evaluationRepository.findByUserAndCommittedFalse(testUserChild))
                                .thenReturn(evaluations);

                // When
                List<BehaviorEvaluation> result = evaluationService.getCurrentWeekEvaluations(testUserChild);

                // Then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getUser()).isEqualTo(testUserChild);
        }

        @Test
        void commitWeeklyEvaluations_ShouldAddPointsAndMarkCommitted() {
                // Given
                List<BehaviorEvaluation> evaluations = Arrays.asList(testEvaluation);
                when(evaluationRepository.findByUserAndCommittedFalse(testUserChild))
                                .thenReturn(evaluations);
                when(evaluationRepository.save(any(BehaviorEvaluation.class))).thenReturn(testEvaluation);

                // When
                evaluationService.commitWeeklyEvaluations(testUserChild, testUser);

                // Then - verify points were credited
                verify(pointService).addPointsWithRemarks(
                                eq(testUserChild),
                                eq(8),
                                eq(PointTransactionType.BEHAVIOR),
                                anyString(),
                                anyLong(),
                                any(),
                                eq(testUser));

                // Verify that current evaluation was marked as committed
                verify(evaluationRepository).save(argThat(eval -> eval.isCommitted()));

                // Verify that a new uncommitted evaluation was created with maximum points
                verify(evaluationRepository).save(argThat(eval -> !eval.isCommitted() &&
                                eval.getCurrentPoints() == testBehavior.getPoints() &&
                                eval.getUser().equals(testUserChild) &&
                                eval.getBehavior().equals(testBehavior)));
        }

        @Test
        void commitWeeklyEvaluations_ShouldThrowException_WhenNoEvaluations() {
                // Given
                when(evaluationRepository.findByUserAndCommittedFalse(testUserChild))
                                .thenReturn(Arrays.asList());

                // When/Then
                assertThatThrownBy(() -> evaluationService.commitWeeklyEvaluations(testUserChild, testUser))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("No evaluations to commit");
        }

        @Test
        void getActiveBehaviorsForUser_ShouldReturnUserAndGlobalBehaviors() {
                // Given
                Behavior userBehavior = new Behavior();
                userBehavior.setUser(testUserChild);
                Behavior globalBehavior = new Behavior();
                globalBehavior.setUser(null);

                when(behaviorRepository.findByUserAndActiveTrueOrderByRankAsc(testUserChild))
                                .thenReturn(new java.util.ArrayList<>(Arrays.asList(userBehavior)));
                when(behaviorRepository.findByUserIsNullAndActiveTrueOrderByRankAsc())
                                .thenReturn(new java.util.ArrayList<>(Arrays.asList(globalBehavior)));

                // When
                List<Behavior> result = evaluationService.getActiveBehaviorsForUser(testUserChild);

                // Then
                assertThat(result).hasSize(2);
        }

        @Test
        void calculateWeeklyTotal_ShouldSumCurrentPoints() {
                // Given
                BehaviorEvaluation eval1 = new BehaviorEvaluation();
                eval1.setCurrentPoints(5);
                BehaviorEvaluation eval2 = new BehaviorEvaluation();
                eval2.setCurrentPoints(7);

                when(evaluationRepository.findByUserAndCommittedFalse(testUserChild))
                                .thenReturn(Arrays.asList(eval1, eval2));

                // When
                int total = evaluationService.calculateWeeklyTotal(testUserChild);

                // Then
                assertThat(total).isEqualTo(12);
        }
}
