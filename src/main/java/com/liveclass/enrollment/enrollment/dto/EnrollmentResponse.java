package com.liveclass.enrollment.enrollment.dto;

import com.liveclass.enrollment.enrollment.Enrollment;
import com.liveclass.enrollment.enrollment.EnrollmentStatus;

import java.time.LocalDateTime;

public record EnrollmentResponse(
        Long id,
        Long courseId,
        Long userId,
        EnrollmentStatus status,
        LocalDateTime confirmedAt,
        LocalDateTime createdAt
) {
    public static EnrollmentResponse from(Enrollment e) {
        return new EnrollmentResponse(
                e.getId(), e.getCourseId(), e.getUserId(),
                e.getStatus(), e.getConfirmedAt(), e.getCreatedAt()
        );
    }
}
