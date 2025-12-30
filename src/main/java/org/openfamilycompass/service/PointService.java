package org.openfamilycompass.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openfamilycompass.model.PointTransaction;
import org.openfamilycompass.model.PointTransactionType;
import org.openfamilycompass.model.User;
import org.openfamilycompass.repository.PointTransactionRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointTransactionRepository pointTransactionRepository;
    private final UserService userService;

    @Transactional
    public PointTransaction addPoints(@NonNull User user, int points, @NonNull PointTransactionType type,
            @NonNull String description, Long referenceId, User createdBy) {
        PointTransaction transaction = new PointTransaction();
        transaction.setUser(user);
        transaction.setPoints(points);
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setReferenceId(referenceId);
        transaction.setCreatedBy(createdBy);

        PointTransaction saved = pointTransactionRepository.save(transaction);

        // Update user's points
        updateUserPoints(user);

        return saved;
    }

    @Transactional
    public PointTransaction deductPoints(@NonNull User user, int points, @NonNull PointTransactionType type,
            @NonNull String description, Long referenceId, User createdBy) {
        return addPoints(user, -points, type, description, referenceId, createdBy);
    }

    @Transactional
    public PointTransaction addPointsWithRemarks(@NonNull User user, int points, @NonNull PointTransactionType type,
            @NonNull String description, Long referenceId,
            String remarks, User createdBy) {
        PointTransaction transaction = new PointTransaction();
        transaction.setUser(user);
        transaction.setPoints(points);
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setReferenceId(referenceId);
        transaction.setRemarks(remarks);
        transaction.setCreatedBy(createdBy);

        PointTransaction saved = pointTransactionRepository.save(transaction);

        // Update user's points
        updateUserPoints(user);

        return saved;
    }

    @Transactional
    public void updateUserPoints(@NonNull User user) {
        Integer totalPoints = pointTransactionRepository.sumPointsByUser(user);
        if (totalPoints == null) {
            totalPoints = 0;
        }
        user.setTotalPoints(totalPoints);
        userService.save(user);
    }

    public List<PointTransaction> getTransactionHistory(@NonNull User user) {
        return pointTransactionRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<PointTransactionWithBalance> getTransactionHistoryWithBalance(@NonNull User user) {
        List<PointTransaction> transactions = pointTransactionRepository.findByUserOrderByCreatedAtAsc(user);
        List<PointTransactionWithBalance> result = new ArrayList<>();
        int runningBalance = 0;

        for (PointTransaction transaction : transactions) {
            runningBalance += transaction.getPoints();
            result.add(new PointTransactionWithBalance(transaction, runningBalance));
        }

        // Reverse to show newest first
        Collections.reverse(result);
        return result;
    }

    public int calculateTotalPoints(@NonNull User user) {
        Integer total = pointTransactionRepository.sumPointsByUser(user);
        return total != null ? total : 0;
    }
}
