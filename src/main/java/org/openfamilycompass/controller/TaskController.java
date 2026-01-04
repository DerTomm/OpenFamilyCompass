package org.openfamilycompass.controller;

import java.time.LocalDate;

import org.openfamilycompass.model.RecurrenceType;

import lombok.Data;

@Data
public class TaskController {
    private String title;
    private String description;
    private int basePoints;
    private Long userId;
    private RecurrenceType recurrenceType;
    private LocalDate dueDate;
}
