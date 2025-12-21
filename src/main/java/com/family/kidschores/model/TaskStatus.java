package com.family.kidschores.model;

public enum TaskStatus {
    PENDING,           // Aufgabe wartet auf Bearbeitung
    IN_PROGRESS,       // Kind hat begonnen
    CHILD_COMPLETED,   // Kind hat als erledigt markiert
    APPROVED,          // Eltern haben genehmigt
    REJECTED           // Eltern haben abgelehnt
}
