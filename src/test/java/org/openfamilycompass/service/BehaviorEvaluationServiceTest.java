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
import org.openfamilycompass.model.Child;
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
        private Child testChild;
        private User testUser;
        private BehaviorEvaluation testEvaluation;

        @BeforeEach
        void setUp() {
                testChild = new Child();
                testChild.setId(1L);
                testChild.setFirstName("TestChild");

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
                testEvaluation.setChild(testChild);
                testEvaluation.setCurrentPoints(8);
                testEvaluation.setCommitted(false);
        }

        @Test
        void updateEvaluation_ShouldCreateNewEvaluation_WhenNotExists() {
                // Given
                when(behaviorRepository.findById(1L)).thenReturn(Optional.of(testBehavior));
                when(evaluationRepository.findByChildAndBehaviorAndWeekStartDateAndCommittedFalse(
                                any(), any(), any())).thenReturn(Optional.empty());
                when(evaluationRepository.save(any(BehaviorEvaluation.class))).thenReturn(testEvaluation);

                // When
                BehaviorEvaluation result = evaluationService.updateEvaluation(
                                1L, testChild, 8, "Good progress", testUser);

                // Then
                assertThat(result).isNotNull();
                verify(evaluationRepository).save(any(BehaviorEvaluation.class));
        }

        @Test
        void updateEvaluation_ShouldUpdateExisting_WhenExists() {
                // Given
                when(behaviorRepository.findById(1L)).thenReturn(Optional.of(testBehavior));
                when(evaluationRepository.findByChildAndBehaviorAndWeekStartDateAndCommittedFalse(
                                any(), any(), any())).thenReturn(Optional.of(testEvaluation));
                when(evaluationRepository.save(any(BehaviorEvaluation.class))).thenReturn(testEvaluation);

                // When
                BehaviorEvaluation result = evaluationService.updateEvaluation(
                                1L, testChild, 9, "Excellent!", testUser);

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
                                1L, testChild, 5, "Notes", testUser))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("not active");
        }

        @Test
        void updateEvaluation_ShouldThrowException_WhenPointsNegative() {
                // Given
                when(behaviorRepository.findById(1L)).thenReturn(Optional.of(testBehavior));

                // When/Then
                assertThatThrownBy(() -> evaluationService.updateEvaluation(
                                1L, testChild, -1, "Notes", testUser))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("between 0 and");
        }

        @Test
        void updateEvaluation_ShouldThrowException_WhenPointsExceedMaximum() {
                // Given
                when(behaviorRepository.findById(1L)).thenReturn(Optional.of(testBehavior));

                // When/Then
                assertThatThrownBy(() -> evaluationService.updateEvaluation(
                                1L, testChild, 15, "Notes", testUser))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("between 0 and");
        }

        @Test
        void getCurrentWeekEvaluations_ShouldReturnEvaluations() {
                // Given
                List<BehaviorEvaluation> evaluations = Arrays.asList(testEvaluation);
                when(evaluationRepository.findByChildAndWeekStartDateAndCommittedFalse(
                                any(), any())).thenReturn(evaluations);

                // When
                List<BehaviorEvaluation> result = evaluationService.getCurrentWeekEvaluations(testChild);

                // Then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getChild()).isEqualTo(testChild);
        }

        @Test
        void commitWeeklyEvaluations_ShouldAddPointsAndMarkCommitted() {
                // Given
                List<BehaviorEvaluation> evaluations = Arrays.asList(testEvaluation);
                when(evaluationRepository.findByChildAndWeekStartDateAndCommittedFalse(
                                any(), any())).thenReturn(evaluations);
                when(evaluationRepository.save(any(BehaviorEvaluation.class))).thenReturn(testEvaluation);

                // When
                evaluationService.commitWeeklyEvaluations(testChild, testUser);

                // Then
                verify(pointService).addPointsWithRemarks(
                                eq(testChild),
                                eq(8),
                                eq("BEHAVIOR"),
                                anyString(),
                                anyLong(),
                                any(),
                                eq(testUser));
                verify(evaluationRepository).save(argThat(eval -> eval.isCommitted()));
        }

        @Test
        void commitWeeklyEvaluations_ShouldThrowException_WhenNoEvaluations() {
                // Given
                when(evaluationRepository.findByChildAndWeekStartDateAndCommittedFalse(
                                any(), any())).thenReturn(Arrays.asList());

                // When/Then
                assertThatThrownBy(() -> evaluationService.commitWeeklyEvaluations(testChild, testUser))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("No evaluations to commit");
        }

        @Test
        void getActiveBehaviorsForChild_ShouldReturnChildAndGlobalBehaviors() {
                // Given
                Behavior childBehavior = new Behavior();
                childBehavior.setChild(testChild);
                Behavior globalBehavior = new Behavior();
                globalBehavior.setChild(null);

                when(behaviorRepository.findByChildAndActiveTrue(testChild))
                                .thenReturn(new java.util.ArrayList<>(Arrays.asList(childBehavior)));
                when(behaviorRepository.findByChildIsNullAndActiveTrue())
                                .thenReturn(new java.util.ArrayList<>(Arrays.asList(globalBehavior)));

                // When
                List<Behavior> result = evaluationService.getActiveBehaviorsForChild(testChild);

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

                when(evaluationRepository.findByChildAndWeekStartDateAndCommittedFalse(
                                any(), any())).thenReturn(Arrays.asList(eval1, eval2));

                // When
                int total = evaluationService.calculateWeeklyTotal(testChild);

                // Then
                assertThat(total).isEqualTo(12);
        }
}
