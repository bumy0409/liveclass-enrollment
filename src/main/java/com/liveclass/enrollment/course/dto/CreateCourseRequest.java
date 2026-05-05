package com.liveclass.enrollment.course.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateCourseRequest(
        @NotBlank String title,
        String description,
        @Min(0) int price,
        @Min(1) int maxCapacity,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {}
