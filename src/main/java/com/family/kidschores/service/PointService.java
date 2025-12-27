package com.family.kidschores.service;

import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.family.kidschores.model.Child;
import com.family.kidschores.model.PointTransaction;
import com.family.kidschores.model.User;
import com.family.kidschores.repository.PointTransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointTransactionRepository pointTransactionRepository;
    private final ChildService childService;

    @Transactional
    public PointTransaction addPoints(@NonNull Child child, int points, @NonNull String type,
            @NonNull String description, Long referenceId, User createdBy) {
        PointTransaction transaction = new PointTransaction();
        transaction.setChild(child);
        transaction.setPoints(points);
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setReferenceId(referenceId);
        transaction.setCreatedBy(createdBy);

        PointTransaction saved = pointTransactionRepository.save(transaction);

        // Update child's points
        updateChildPoints(child);

        return saved;
    }

    @Transactional
    public PointTransaction deductPoints(@NonNull Child child, int points, @NonNull String type,
            @NonNull String description, Long referenceId, User createdBy) {
        return addPoints(child, -points, type, description, referenceId, createdBy);
    }

    @Transactional
    public PointTransaction addPointsWithRemarks(@NonNull Child child, int points, @NonNull String type,
            @NonNull String description, Long referenceId,
            String remarks, User createdBy) {
        PointTransaction transaction = new PointTransaction();
        transaction.setChild(child);
        transaction.setPoints(points);
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setReferenceId(referenceId);
        transaction.setRemarks(remarks);
        transaction.setCreatedBy(createdBy);

        PointTransaction saved = pointTransactionRepository.save(transaction);

        // Update child's points
        updateChildPoints(child);

        return saved;
    }

    @Transactional
    public void updateChildPoints(@NonNull Child child) {
        Long childId = Objects.requireNonNull(child.getId(), "Child ID must not be null");
        Integer totalPoints = pointTransactionRepository.sumPointsByChild(child);
        if (totalPoints == null) {
            totalPoints = 0;
        }
        childService.updatePoints(childId, totalPoints);
    }

    public List<PointTransaction> getTransactionHistory(@NonNull Child child) {
        return pointTransactionRepository.findByChildOrderByCreatedAtDesc(child);
    }

    public int calculateTotalPoints(@NonNull Child child) {
        Integer total = pointTransactionRepository.sumPointsByChild(child);
        return total != null ? total : 0;
    }
}
