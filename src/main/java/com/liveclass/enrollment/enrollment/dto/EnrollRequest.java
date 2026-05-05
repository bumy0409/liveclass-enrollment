package com.liveclass.enrollment.enrollment.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollRequest(@NotNull Long courseId) {}
