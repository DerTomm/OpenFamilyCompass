package com.family.kidschores.model;

public enum TaskStatus {
    PENDING, // Task waiting for processing
    IN_PROGRESS, // Child has started
    CHILD_COMPLETED, // Child marked as completed
    APPROVED, // Parents approved
    REJECTED // Parents rejected
}
