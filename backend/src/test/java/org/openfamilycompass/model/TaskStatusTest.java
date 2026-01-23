package org.openfamilycompass.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TaskStatusTest {

    @Test
    void enumValues_ShouldContainAllExpectedStatuses() {
        // When
        TaskStatus[] statuses = TaskStatus.values();

        // Then
        assertThat(statuses).hasSize(6);
        assertThat(statuses).containsExactlyInAnyOrder(
                TaskStatus.PENDING,
                TaskStatus.IN_PROGRESS,
                TaskStatus.CHILD_COMPLETED,
                TaskStatus.APPROVED,
                TaskStatus.REJECTED,
                TaskStatus.EXPIRED
        );
    }

    @Test
    void enumValueOf_ShouldReturnCorrectEnum() {
        // When & Then
        assertThat(TaskStatus.valueOf("PENDING")).isEqualTo(TaskStatus.PENDING);
        assertThat(TaskStatus.valueOf("IN_PROGRESS")).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(TaskStatus.valueOf("CHILD_COMPLETED")).isEqualTo(TaskStatus.CHILD_COMPLETED);
        assertThat(TaskStatus.valueOf("APPROVED")).isEqualTo(TaskStatus.APPROVED);
        assertThat(TaskStatus.valueOf("REJECTED")).isEqualTo(TaskStatus.REJECTED);
        assertThat(TaskStatus.valueOf("EXPIRED")).isEqualTo(TaskStatus.EXPIRED);
    }

    @Test
    void enumOrdinal_ShouldReturnCorrectOrder() {
        // Then
        assertThat(TaskStatus.PENDING.ordinal()).isEqualTo(0);
        assertThat(TaskStatus.IN_PROGRESS.ordinal()).isEqualTo(1);
        assertThat(TaskStatus.CHILD_COMPLETED.ordinal()).isEqualTo(2);
        assertThat(TaskStatus.APPROVED.ordinal()).isEqualTo(3);
        assertThat(TaskStatus.REJECTED.ordinal()).isEqualTo(4);
        assertThat(TaskStatus.EXPIRED.ordinal()).isEqualTo(5);
    }

    @Test
    void enumName_ShouldReturnCorrectName() {
        // Then
        assertThat(TaskStatus.PENDING.name()).isEqualTo("PENDING");
        assertThat(TaskStatus.IN_PROGRESS.name()).isEqualTo("IN_PROGRESS");
        assertThat(TaskStatus.CHILD_COMPLETED.name()).isEqualTo("CHILD_COMPLETED");
        assertThat(TaskStatus.APPROVED.name()).isEqualTo("APPROVED");
        assertThat(TaskStatus.REJECTED.name()).isEqualTo("REJECTED");
        assertThat(TaskStatus.EXPIRED.name()).isEqualTo("EXPIRED");
    }

    @Test
    void taskStatusWorkflow_ShouldRepresentValidTransitions() {
        // Test typical workflow transitions
        // PENDING -> IN_PROGRESS (child starts task)
        assertThat(TaskStatus.PENDING).isNotEqualTo(TaskStatus.IN_PROGRESS);

        // IN_PROGRESS -> CHILD_COMPLETED (child completes task)
        assertThat(TaskStatus.IN_PROGRESS).isNotEqualTo(TaskStatus.CHILD_COMPLETED);

        // CHILD_COMPLETED -> APPROVED (parent approves)
        assertThat(TaskStatus.CHILD_COMPLETED).isNotEqualTo(TaskStatus.APPROVED);

        // CHILD_COMPLETED -> REJECTED (parent rejects)
        assertThat(TaskStatus.CHILD_COMPLETED).isNotEqualTo(TaskStatus.REJECTED);

        // PENDING -> EXPIRED (task expires)
        assertThat(TaskStatus.PENDING).isNotEqualTo(TaskStatus.EXPIRED);
    }

    @Test
    void statusEquality_ShouldWorkCorrectly() {
        // Given
        TaskStatus status1 = TaskStatus.PENDING;
        TaskStatus status2 = TaskStatus.PENDING;
        TaskStatus status3 = TaskStatus.APPROVED;

        // Then
        assertThat(status1).isEqualTo(status2);
        assertThat(status1).isNotEqualTo(status3);
        assertThat(status1 == status2).isTrue();
        assertThat(status1 == status3).isFalse();
    }

    @Test
    void toString_ShouldReturnEnumName() {
        // Then
        assertThat(TaskStatus.PENDING.toString()).isEqualTo("PENDING");
        assertThat(TaskStatus.IN_PROGRESS.toString()).isEqualTo("IN_PROGRESS");
        assertThat(TaskStatus.CHILD_COMPLETED.toString()).isEqualTo("CHILD_COMPLETED");
        assertThat(TaskStatus.APPROVED.toString()).isEqualTo("APPROVED");
        assertThat(TaskStatus.REJECTED.toString()).isEqualTo("REJECTED");
        assertThat(TaskStatus.EXPIRED.toString()).isEqualTo("EXPIRED");
    }
}