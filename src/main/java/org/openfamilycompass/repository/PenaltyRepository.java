package org.openfamilycompass.repository;

import java.util.List;

import org.openfamilycompass.model.Penalty;
import org.openfamilycompass.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PenaltyRepository extends JpaRepository<Penalty, Long> {

    List<Penalty> findByUser(User user);

    List<Penalty> findByUserOrderByCreatedAtDesc(User user);
}
