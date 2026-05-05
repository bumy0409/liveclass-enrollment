package com.liveclass.enrollment.waitlist.dto;

import jakarta.validation.constraints.NotNull;

public record WaitlistRequest(@NotNull Long courseId) {}
