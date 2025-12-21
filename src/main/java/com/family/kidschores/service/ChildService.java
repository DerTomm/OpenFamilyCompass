package com.family.kidschores.service;

import com.family.kidschores.model.Child;
import com.family.kidschores.model.User;
import com.family.kidschores.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChildService {

    private final ChildRepository childRepository;

    @Transactional
    public Child createChild(User user, String firstName, String avatarPath) {
        Child child = new Child();
        child.setUser(user);
        child.setFirstName(firstName);
        child.setAvatarPath(avatarPath);
        child.setTotalPoints(0);

        return childRepository.save(child);
    }

    @Transactional
    public Child updateAvatar(Long childId, String avatarPath) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));
        
        child.setAvatarPath(avatarPath);
        return childRepository.save(child);
    }

    @Transactional
    public Child updatePoints(Long childId, int points) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));
        
        child.setTotalPoints(points);
        return childRepository.save(child);
    }

    public Optional<Child> findById(Long id) {
        return childRepository.findById(id);
    }

    public Optional<Child> findByUser(User user) {
        return childRepository.findByUser(user);
    }

    public Optional<Child> findByUserId(Long userId) {
        return childRepository.findByUserId(userId);
    }

    public List<Child> findAll() {
        return childRepository.findAllByOrderByFirstNameAsc();
    }
}
