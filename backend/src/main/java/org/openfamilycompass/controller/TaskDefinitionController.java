package org.openfamilycompass.controller;

import java.time.LocalDate;
import java.util.Set;

import org.openfamilycompass.model.RecurrenceType;

import lombok.Data;

@Data
public class TaskDefinitionController {
    private String title;
    private String description;
    private int basePoints;
    private Set<Long> userIds; // Multiple children
    private RecurrenceType recurrenceType;
    private LocalDate startDate;
    private LocalDate seriesEndDate;
    private String weeklyDays;
}