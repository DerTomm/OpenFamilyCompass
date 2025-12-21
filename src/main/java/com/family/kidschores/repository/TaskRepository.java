package com.family.kidschores.repository;

import com.family.kidschores.model.Child;
import com.family.kidschores.model.Task;
import com.family.kidschores.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    List<Task> findByAssignedChild(Child child);
    
    List<Task> findByAssignedChildAndStatus(Child child, TaskStatus status);
    
    List<Task> findByStatus(TaskStatus status);
    
    @Query("SELECT t FROM Task t WHERE t.assignedChild = :child AND t.status IN :statuses ORDER BY t.dueDate ASC")
    List<Task> findByAssignedChildAndStatusIn(Child child, List<TaskStatus> statuses);
    
    List<Task> findByIsTemplateTrue();
    
    @Query("SELECT t FROM Task t WHERE t.dueDate = :date AND t.isTemplate = false")
    List<Task> findByDueDate(LocalDate date);
    
    @Query("SELECT t FROM Task t WHERE t.assignedChild = :child AND t.status = 'CHILD_COMPLETED'")
    List<Task> findPendingApprovalForChild(Child child);
}
