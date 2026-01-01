package org.openfamilycompass.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openfamilycompass.model.Behavior;
import org.openfamilycompass.model.BehaviorEvaluation;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.repository.BehaviorEvaluationRepository;
import org.openfamilycompass.repository.BehaviorRepository;

@ExtendWith(MockitoExtension.class)
class BehaviorServiceTest {

    @Mock
    private BehaviorRepository behaviorRepository;

    @Mock
    private BehaviorEvaluationRepository behaviorEvaluationRepository;

    @Mock
    private PointService pointService;

    @InjectMocks
    private BehaviorService behaviorService;

    private Behavior testBehavior;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("TestParent");
        testUser.setRole(UserRole.PARENT);

        testBehavior = new Behavior();
        testBehavior.setId(1L);
        testBehavior.setTitle("Respect");
        testBehavior.setGuideline("Be respectful to others");
        testBehavior.setPoints(10);
        testBehavior.setActive(true);
    }

    @Test
    void createBehavior_ShouldCreateNewBehavior() {
        // Given
        when(behaviorRepository.save(any(Behavior.class))).thenReturn(testBehavior);

        // When
        Behavior result = behaviorService.createBehavior("Respect", "Be respectful to others", 10, testUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Respect");
        assertThat(result.getPoints()).isEqualTo(10);
        verify(behaviorRepository).save(any(Behavior.class));
    }

    @Test
    void editBehavior_ShouldUpdateAllProperties_WhenPointsIncreased() {
        // Given
        when(behaviorRepository.findById(1L)).thenReturn(Optional.of(testBehavior));
        when(behaviorRepository.save(any(Behavior.class))).thenReturn(testBehavior);

        // When
        Behavior result = behaviorService.editBehavior(1L, "Updated Respect", "Updated guideline", 15, testUser);

        // Then
        assertThat(result.getTitle()).isEqualTo("Updated Respect");
        assertThat(result.getGuideline()).isEqualTo("Updated guideline");
        assertThat(result.getPoints()).isEqualTo(15);
        verify(behaviorRepository).save(any(Behavior.class));
    }

    @Test
    void editBehavior_ShouldCapPointsWhenMaxPointsReduced() {
        // Given
        BehaviorEvaluation eval1 = new BehaviorEvaluation();
        eval1.setId(1L);
        eval1.setCurrentPoints(12); // Over new maximum

        BehaviorEvaluation eval2 = new BehaviorEvaluation();
        eval2.setId(2L);
        eval2.setCurrentPoints(5); // Below new maximum

        when(behaviorRepository.findById(1L)).thenReturn(Optional.of(testBehavior));
        when(behaviorEvaluationRepository.findByBehaviorAndCommittedFalse(testBehavior))
                .thenReturn(Arrays.asList(eval1, eval2));
        when(behaviorRepository.save(any(Behavior.class))).thenReturn(testBehavior);

        // When - reduce points from 10 to 8
        behaviorService.editBehavior(1L, "Respect", "Be respectful", 8, testUser);

        // Then - verify that points were capped
        verify(behaviorEvaluationRepository).save(eval1);
        assertThat(eval1.getCurrentPoints()).isEqualTo(8);
        // eval2 should not be modified
        assertThat(eval2.getCurrentPoints()).isEqualTo(5);
    }

    @Test
    void editBehavior_ShouldThrowException_WhenBehaviorNotFound() {
        // Given
        when(behaviorRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> behaviorService.editBehavior(1L, "Title", "Guideline", 10, testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void deleteBehavior_ShouldDeleteBehaviorAndEvaluations() {
        // Given
        when(behaviorRepository.findById(1L)).thenReturn(Optional.of(testBehavior));

        // When
        behaviorService.deleteBehavior(1L);

        // Then
        verify(behaviorEvaluationRepository).deleteByBehavior(testBehavior);
        verify(behaviorRepository).delete(testBehavior);
    }

    @Test
    void findById_ShouldReturnBehavior() {
        // Given
        when(behaviorRepository.findById(1L)).thenReturn(Optional.of(testBehavior));

        // When
        Optional<Behavior> result = behaviorService.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Respect");
    }
}
