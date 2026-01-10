package org.openfamilycompass.repository;

import java.util.List;

import org.openfamilycompass.model.TaskDefinition;
import org.openfamilycompass.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskDefinitionRepository extends JpaRepository<TaskDefinition, Long> {

    List<TaskDefinition> findByCreatedBy(User createdBy);

    List<TaskDefinition> findByAssignedUsersContaining(User user);
}