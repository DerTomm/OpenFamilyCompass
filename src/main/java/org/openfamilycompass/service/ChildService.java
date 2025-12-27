package org.openfamilycompass.service;

import org.openfamilycompass.model.Child;
import org.openfamilycompass.model.User;
import org.openfamilycompass.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChildService {

    private final ChildRepository childRepository;

    @Transactional
    public Child createChild(@NonNull User user, @NonNull String firstName, String avatarPath) {
        Child child = new Child();
        child.setUser(user);
        child.setFirstName(firstName);
        child.setAvatarPath(avatarPath);
        child.setTotalPoints(0);

        return childRepository.save(child);
    }

    @Transactional
    public Child updateAvatar(@NonNull Long childId, String avatarPath) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));
        
        child.setAvatarPath(avatarPath);
        return childRepository.save(child);
    }

    @Transactional
    public Child updatePoints(@NonNull Long childId, int points) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));
        
        child.setTotalPoints(points);
        return childRepository.save(child);
    }

    public Optional<Child> findById(@NonNull Long id) {
        return childRepository.findById(id);
    }

    public Optional<Child> findByUser(@NonNull User user) {
        return childRepository.findByUser(user);
    }

    public Optional<Child> findByUserId(@NonNull Long userId) {
        return childRepository.findByUserId(userId);
    }

    public List<Child> findAll() {
        return childRepository.findAllByOrderByFirstNameAsc();
    }
}
