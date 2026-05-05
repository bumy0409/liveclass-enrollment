package com.liveclass.enrollment.course.dto;

import com.liveclass.enrollment.course.CourseStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull CourseStatus status) {}
