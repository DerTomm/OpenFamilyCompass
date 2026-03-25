package org.openfamilycompass.api.v1.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openfamilycompass.model.RecurrenceType;
import org.openfamilycompass.model.TaskDefinition;
import org.openfamilycompass.model.TaskInstance;
import org.openfamilycompass.model.TaskStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class TaskDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefinitionResponse {
        private Long id;
        private String title;
        private String description;
        private int basePoints;
        private RecurrenceType recurrenceType;
        private List<UserDto.ChildResponse> assignedUsers;
        private UserDto.Response createdBy;
        private LocalDate startDate;
        private LocalDate seriesEndDate;
        private LocalDateTime deadline;
        private List<String> weeklyDays;
        private String monthlyMode;
        private Integer monthlyWeekNumber;
        private Integer monthlyDayOfMonth;
        private Boolean monthlyAdjustToLastDay;
        private LocalDateTime createdAt;

        public static DefinitionResponse fromEntity(TaskDefinition def) {
            List<String> days = null;
            String monthlyMode = null;
            Integer monthlyWeekNumber = null;
            Integer monthlyDayOfMonth = null;
            Boolean monthlyAdjustToLastDay = null;
            if (def.getWeeklyDays() != null && !def.getWeeklyDays().isBlank()) {
                String weeklyDaysRaw = def.getWeeklyDays();
                if (def.getRecurrenceType() == RecurrenceType.MONTHLY && weeklyDaysRaw.startsWith("MW:")) {
                    monthlyMode = "WEEKDAY_PATTERN";
                    String[] parts = weeklyDaysRaw.split(":", 3);
                    if (parts.length >= 2) {
                        try {
                            monthlyWeekNumber = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException ignored) {
                            monthlyWeekNumber = null;
                        }
                    }
                    if (parts.length == 3 && !parts[2].isBlank()) {
                        days = Arrays.asList(parts[2].split(","));
                    }
                } else if (def.getRecurrenceType() == RecurrenceType.MONTHLY && weeklyDaysRaw.startsWith("MD:")) {
                    monthlyMode = "DAY_OF_MONTH";
                    String[] parts = weeklyDaysRaw.split(":", 3);
                    if (parts.length >= 2) {
                        try {
                            monthlyDayOfMonth = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException ignored) {
                            monthlyDayOfMonth = null;
                        }
                    }
                    monthlyAdjustToLastDay = parts.length >= 3 && "LAST".equalsIgnoreCase(parts[2]);
                } else if (def.getRecurrenceType() == RecurrenceType.MONTHLY && weeklyDaysRaw.startsWith("W")
                        && weeklyDaysRaw.contains(":")) {
                    // Backward compatibility with legacy format W2:WEDNESDAY
                    monthlyMode = "WEEKDAY_PATTERN";
                    String[] parts = weeklyDaysRaw.split(":", 2);
                    try {
                        monthlyWeekNumber = Integer.parseInt(parts[0].substring(1));
                    } catch (NumberFormatException | IndexOutOfBoundsException ignored) {
                        monthlyWeekNumber = null;
                    }
                    if (parts.length > 1 && !parts[1].isBlank()) {
                        days = Arrays.asList(parts[1].split(","));
                    }
                } else {
                    days = Arrays.asList(weeklyDaysRaw.split(","));
                }
            }

            return DefinitionResponse.builder()
                    .id(def.getId())
                    .title(def.getTitle())
                    .description(def.getDescription())
                    .basePoints(def.getBasePoints())
                    .recurrenceType(def.getRecurrenceType())
                    .assignedUsers(def.getAssignedUsers() != null
                            ? def.getAssignedUsers().stream()
                                    .map(UserDto.ChildResponse::fromEntity)
                                    .collect(Collectors.toList())
                            : Collections.emptyList())
                    .createdBy(def.getCreatedBy() != null ? UserDto.Response.fromEntity(def.getCreatedBy()) : null)
                    .startDate(def.getStartDate())
                    .seriesEndDate(def.getSeriesEndDate())
                    .deadline(def.getDeadline())
                    .weeklyDays(days)
                    .monthlyMode(monthlyMode)
                    .monthlyWeekNumber(monthlyWeekNumber)
                    .monthlyDayOfMonth(monthlyDayOfMonth)
                    .monthlyAdjustToLastDay(monthlyAdjustToLastDay)
                    .createdAt(def.getCreatedAt())
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateDefinitionRequest {
        @NotBlank
        private String title;

        private String description;

        @Min(1)
        private int basePoints;

        @NotNull
        private RecurrenceType recurrenceType;

        @NotEmpty
        private Set<Long> assignedUserIds;

        private LocalDate startDate;
        private LocalDate seriesEndDate;
        private LocalDateTime deadline;
        private List<String> weeklyDays;
        private String monthlyMode;
        private Integer monthlyWeekNumber;
        private Integer monthlyDayOfMonth;
        private Boolean monthlyAdjustToLastDay;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateDefinitionRequest {
        private String title;
        private String description;

        @Min(1)
        private Integer basePoints;

        private RecurrenceType recurrenceType;
        private Set<Long> assignedUserIds;
        private LocalDate startDate;
        private LocalDate seriesEndDate;
        private LocalDateTime deadline;
        private List<String> weeklyDays;
        private String monthlyMode;
        private Integer monthlyWeekNumber;
        private Integer monthlyDayOfMonth;
        private Boolean monthlyAdjustToLastDay;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstanceResponse {
        private Long id;
        private DefinitionResponse taskDefinition;
        private UserDto.ChildResponse assignedUser;
        private LocalDateTime deadline;
        private TaskStatus status;
        private LocalDateTime completedAt;
        private LocalDateTime approvedAt;
        private Integer awardedPoints;
        private UserDto.Response approvedBy;
        private String parentNotes;
        private LocalDateTime createdAt;

        public static InstanceResponse fromEntity(TaskInstance instance) {
            return InstanceResponse.builder()
                    .id(instance.getId())
                    .taskDefinition(DefinitionResponse.fromEntity(instance.getTaskDefinition()))
                    .assignedUser(UserDto.ChildResponse.fromEntity(instance.getAssignedUser()))
                    .deadline(instance.getDeadline())
                    .status(instance.getStatus())
                    .completedAt(instance.getCompletedAt())
                    .approvedAt(instance.getApprovedAt())
                    .awardedPoints(instance.getAwardedPoints())
                    .approvedBy(instance.getApprovedBy() != null
                            ? UserDto.Response.fromEntity(instance.getApprovedBy())
                            : null)
                    .parentNotes(instance.getParentNotes())
                    .createdAt(instance.getCreatedAt())
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApproveRequest {
        private Integer awardedPoints;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectRequest {
        private String notes;
    }
}
