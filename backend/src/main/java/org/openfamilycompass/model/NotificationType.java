package org.openfamilycompass.model;

public enum NotificationType {
    TASK_ASSIGNED("clipboard-list", "warning"), // Kind: Neue Aufgabe zugewiesen
    TASK_COMPLETED("check-circle", "success"), // Eltern: Aufgabe wurde von Kind abgeschlossen
    TASK_APPROVED("check-circle", "success"), // Kind: Aufgabe wurde genehmigt
    TASK_REJECTED("times-circle", "danger"), // Kind: Aufgabe wurde abgelehnt
    TASK_EXPIRED("times-circle", "danger"), // Kind: Aufgabe ist abgelaufen
    REWARD_REQUESTED("gift", "warning"), // Eltern: Belohnung wurde beantragt
    REWARD_APPROVED("gift", "success"), // Kind: Belohnung wurde genehmigt
    REWARD_REJECTED("times-circle", "danger"), // Kind: Belohnung wurde abgelehnt
    POINTS_EARNED("star", "warning"); // Kind: Neue Punkte erhalten

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