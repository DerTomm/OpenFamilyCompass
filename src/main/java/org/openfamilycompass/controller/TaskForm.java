package org.openfamilycompass.controller;

import java.time.LocalDate;

import org.openfamilycompass.model.RecurrenceType;

import lombok.Data;

@Data
public class TaskForm {
    private String title;
    private String description;
    private int basePoints;
    private Long userId;
    private RecurrenceType recurrenceType;
    private LocalDate dueDate;
    private boolean template;
}
