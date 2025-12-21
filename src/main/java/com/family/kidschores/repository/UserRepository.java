package com.family.kidschores.repository;

import com.family.kidschores.model.User;
import com.family.kidschores.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    List<User> findByRole(UserRole role);
    
    boolean existsByUsername(String username);
    
    List<User> findByActiveTrue();
}
