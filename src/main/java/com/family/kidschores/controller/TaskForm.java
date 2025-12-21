package com.family.kidschores.controller;

import com.family.kidschores.model.RecurrenceType;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskForm {
    private String title;
    private String description;
    private int basePoints;
    private Long childId;
    private RecurrenceType recurrenceType;
    private LocalDate dueDate;
    private boolean template;
}
