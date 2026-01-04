package org.openfamilycompass.repository;

import java.util.List;

import org.openfamilycompass.model.Task;
import org.openfamilycompass.model.TaskStatus;
import org.openfamilycompass.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByAssignedUser(User user);

    List<Task> findByAssignedUserAndStatus(User user, TaskStatus status);

    List<Task> findByStatus(TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.assignedUser = :user AND t.status IN :statuses ORDER BY t.dueDate ASC")
    List<Task> findByAssignedUserAndStatusIn(User user, List<TaskStatus> statuses);

    @Query("SELECT t FROM Task t WHERE t.assignedUser = :user AND t.status = 'CHILD_COMPLETED'")
    List<Task> findPendingApprovalForUser(User user);
}
