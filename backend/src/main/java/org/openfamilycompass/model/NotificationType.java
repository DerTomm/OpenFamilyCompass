package org.openfamilycompass.model;

public enum NotificationType {
    TASK_ASSIGNED("clipboard-list", "warning"), // Child: New task assigned
    TASK_COMPLETED("check-circle", "success"), // Parents: Task was completed by child
    TASK_APPROVED("check-circle", "success"), // Child: Task was approved
    TASK_REJECTED("times-circle", "danger"), // Child: Task was rejected
    TASK_EXPIRED("times-circle", "danger"), // Child: Task has expired
    REWARD_REQUESTED("gift", "warning"), // Parents: Reward was requested
    REWARD_APPROVED("gift", "success"), // Child: Reward was approved
    REWARD_REJECTED("times-circle", "danger"), // Child: Reward was rejected
    POINTS_EARNED("star", "warning"); // Child: New points received

    private final String iconClass;
    private final String colorClass;

    NotificationType(String iconClass, String colorClass) {
        this.iconClass = iconClass;
        this.colorClass = colorClass;
    }

    public String getIconClass() {
        return iconClass;
    }

    public String getColorClass() {
        return colorClass;
    }
}