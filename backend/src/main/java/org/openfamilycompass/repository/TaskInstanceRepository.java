package org.openfamilycompass.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.openfamilycompass.model.TaskInstance;
import org.openfamilycompass.model.TaskStatus;
import org.openfamilycompass.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskInstanceRepository extends JpaRepository<TaskInstance, Long> {

    List<TaskInstance> findByAssignedUser(User user);

    List<TaskInstance> findByAssignedUserAndStatus(User user, TaskStatus status);

    List<TaskInstance> findByStatus(TaskStatus status);

    @Query("SELECT t FROM TaskInstance t WHERE t.assignedUser = :user AND t.status IN :statuses ORDER BY t.dueDate ASC")
    List<TaskInstance> findByAssignedUserAndStatusIn(User user, List<TaskStatus> statuses);

    @Query("SELECT t FROM TaskInstance t WHERE t.assignedUser = :user AND t.status = 'CHILD_COMPLETED'")
    List<TaskInstance> findPendingApprovalForUser(User user);

    List<TaskInstance> findByDueDateBeforeAndStatusNotIn(LocalDate date, List<TaskStatus> statuses);

    List<TaskInstance> findByStatusAndDueDateBefore(TaskStatus status, LocalDate date);

    List<TaskInstance> findByStatusAndDueAtBefore(TaskStatus status, LocalDateTime dateTime);

    void deleteByTaskDefinition_Id(Long taskDefinitionId);
}