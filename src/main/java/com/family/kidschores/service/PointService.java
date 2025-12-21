package com.family.kidschores.service;

import com.family.kidschores.model.Child;
import com.family.kidschores.model.PointTransaction;
import com.family.kidschores.model.User;
import com.family.kidschores.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointTransactionRepository pointTransactionRepository;
    private final ChildService childService;

    @Transactional
    public PointTransaction addPoints(Child child, int points, String type, 
                                     String description, Long referenceId, User createdBy) {
        PointTransaction transaction = new PointTransaction();
        transaction.setChild(child);
        transaction.setPoints(points);
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setReferenceId(referenceId);
        transaction.setCreatedBy(createdBy);

        PointTransaction saved = pointTransactionRepository.save(transaction);

        // Punkte des Kindes aktualisieren
        updateChildPoints(child);

        return saved;
    }

    @Transactional
    public PointTransaction deductPoints(Child child, int points, String type, 
                                        String description, Long referenceId, User createdBy) {
        return addPoints(child, -points, type, description, referenceId, createdBy);
    }

    @Transactional
    public void updateChildPoints(Child child) {
        Integer totalPoints = pointTransactionRepository.sumPointsByChild(child);
        if (totalPoints == null) {
            totalPoints = 0;
        }
        childService.updatePoints(child.getId(), totalPoints);
    }

    public List<PointTransaction> getTransactionHistory(Child child) {
        return pointTransactionRepository.findByChildOrderByCreatedAtDesc(child);
    }

    public int calculateTotalPoints(Child child) {
        Integer total = pointTransactionRepository.sumPointsByChild(child);
        return total != null ? total : 0;
    }
}
