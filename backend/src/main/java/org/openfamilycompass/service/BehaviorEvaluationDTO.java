package org.openfamilycompass.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for displaying behavior evaluations in the child dashboard.
 * Shows current points, max points, guideline, and remarks for each behavior.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BehaviorEvaluationDTO {

    private Long behaviorId;

    private String behaviorTitle;

    private String guideline;

    private int maxPoints; // Points from the behavior definition

    private int currentPoints; // Currently awarded points in the evaluation

    private String remarks; // Optional remarks from the evaluation

    private boolean hasRemarks; // Helper flag to determine if remarks button should be shown

    /**
     * Calculates the percentage of achieved points.
     */
    public int getProgressPercentage() {
        if (maxPoints == 0) {
            return 0;
        }
        return (int) ((currentPoints * 100.0) / maxPoints);
    }
}
