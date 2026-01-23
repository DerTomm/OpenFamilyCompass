package org.openfamilycompass.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openfamilycompass.model.RecurrenceType;

class TaskDefinitionControllerTest {

    private TaskDefinitionController dto;

    @BeforeEach
    void setUp() {
        dto = new TaskDefinitionController();
        dto.setTitle("Test Task");
        dto.setDescription("Test Description");
        dto.setBasePoints(10);
        dto.setUserIds(Set.of(1L, 2L, 3L));
        dto.setRecurrenceType(RecurrenceType.ONCE);
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusDays(7));
        dto.setWeeklyDays("MONDAY,TUESDAY");
    }

    @Test
    void constructor_ShouldCreateTaskDefinitionController() {
        // Given
        TaskDefinitionController newDto = new TaskDefinitionController();

        // Then
        assertThat(newDto).isNotNull();
    }

    @Test
    void noArgsConstructor_ShouldCreateEmptyDto() {
        // When
        TaskDefinitionController newDto = new TaskDefinitionController();

        // Then
        assertThat(newDto.getTitle()).isNull();
        assertThat(newDto.getDescription()).isNull();
        assertThat(newDto.getBasePoints()).isEqualTo(0);
        assertThat(newDto.getUserIds()).isNull();
        assertThat(newDto.getRecurrenceType()).isNull();
        assertThat(newDto.getStartDate()).isNull();
        assertThat(newDto.getEndDate()).isNull();
        assertThat(newDto.getWeeklyDays()).isNull();
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        // Given
        String title = "Updated Task";
        String description = "Updated Description";
        int basePoints = 20;
        Set<Long> userIds = Set.of(4L, 5L);
        RecurrenceType recurrenceType = RecurrenceType.WEEKLY;
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        String weeklyDays = "WEDNESDAY,FRIDAY";

        // When
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setBasePoints(basePoints);
        dto.setUserIds(userIds);
        dto.setRecurrenceType(recurrenceType);
        dto.setStartDate(startDate);
        dto.setEndDate(endDate);
        dto.setWeeklyDays(weeklyDays);

        // Then
        assertThat(dto.getTitle()).isEqualTo(title);
        assertThat(dto.getDescription()).isEqualTo(description);
        assertThat(dto.getBasePoints()).isEqualTo(basePoints);
        assertThat(dto.getUserIds()).isEqualTo(userIds);
        assertThat(dto.getRecurrenceType()).isEqualTo(recurrenceType);
        assertThat(dto.getStartDate()).isEqualTo(startDate);
        assertThat(dto.getEndDate()).isEqualTo(endDate);
        assertThat(dto.getWeeklyDays()).isEqualTo(weeklyDays);
    }

    @Test
    void userIds_ShouldHandleMultipleUsers() {
        // Given
        Set<Long> userIds = Set.of(1L, 2L, 3L, 4L, 5L);

        // When
        dto.setUserIds(userIds);

        // Then
        assertThat(dto.getUserIds()).hasSize(5);
        assertThat(dto.getUserIds()).containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    void userIds_ShouldHandleEmptySet() {
        // Given
        Set<Long> userIds = Set.of();

        // When
        dto.setUserIds(userIds);

        // Then
        assertThat(dto.getUserIds()).isEmpty();
    }

    @Test
    void recurrenceType_ShouldHandleDifferentTypes() {
        // Test ONCE
        dto.setRecurrenceType(RecurrenceType.ONCE);
        assertThat(dto.getRecurrenceType()).isEqualTo(RecurrenceType.ONCE);

        // Test WEEKLY
        dto.setRecurrenceType(RecurrenceType.WEEKLY);
        assertThat(dto.getRecurrenceType()).isEqualTo(RecurrenceType.WEEKLY);

        // Test WEEKLY
        dto.setRecurrenceType(RecurrenceType.WEEKLY);
        assertThat(dto.getRecurrenceType()).isEqualTo(RecurrenceType.WEEKLY);
    }

    @Test
    void dates_ShouldHandleNullValues() {
        // When
        dto.setStartDate(null);
        dto.setEndDate(null);

        // Then
        assertThat(dto.getStartDate()).isNull();
        assertThat(dto.getEndDate()).isNull();
    }

    @Test
    void weeklyDays_ShouldHandleNullAndEmptyValues() {
        // Test null
        dto.setWeeklyDays(null);
        assertThat(dto.getWeeklyDays()).isNull();

        // Test empty string
        dto.setWeeklyDays("");
        assertThat(dto.getWeeklyDays()).isEqualTo("");
    }

    @Test
    void weeklyDays_ShouldHandleCommaSeparatedValues() {
        // Given
        String weeklyDays = "MONDAY,WEDNESDAY,FRIDAY";

        // When
        dto.setWeeklyDays(weeklyDays);

        // Then
        assertThat(dto.getWeeklyDays()).isEqualTo(weeklyDays);
    }

    @Test
    void description_ShouldHandleNullValue() {
        // When
        dto.setDescription(null);

        // Then
        assertThat(dto.getDescription()).isNull();
    }

    @Test
    void basePoints_ShouldHandleZeroAndNegativeValues() {
        // Test zero
        dto.setBasePoints(0);
        assertThat(dto.getBasePoints()).isEqualTo(0);

        // Test negative (though this might not make business sense)
        dto.setBasePoints(-5);
        assertThat(dto.getBasePoints()).isEqualTo(-5);
    }

    @Test
    void equalsAndHashCode_ShouldWorkBasedOnFields() {
        // Given
        TaskDefinitionController dto1 = new TaskDefinitionController();
        dto1.setTitle("Task");
        dto1.setDescription("Desc");
        dto1.setBasePoints(10);

        TaskDefinitionController dto2 = new TaskDefinitionController();
        dto2.setTitle("Task");
        dto2.setDescription("Desc");
        dto2.setBasePoints(10);

        TaskDefinitionController dto3 = new TaskDefinitionController();
        dto3.setTitle("Different Task");
        dto3.setDescription("Desc");
        dto3.setBasePoints(10);

        // Then
        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1).isNotEqualTo(dto3);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
        assertThat(dto1.hashCode()).isNotEqualTo(dto3.hashCode());
    }

    @Test
    void toString_ShouldContainKeyFields() {
        // When
        String toString = dto.toString();

        // Then
        assertThat(toString).contains("TaskDefinitionController");
        assertThat(toString).contains("title=Test Task");
        assertThat(toString).contains("description=Test Description");
        assertThat(toString).contains("basePoints=10");
    }
}