package com.liveclass.enrollment.course.dto;

import com.liveclass.enrollment.course.Course;
import com.liveclass.enrollment.course.CourseStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CourseResponse(
        Long id,
        String title,
        String description,
        int price,
        int maxCapacity,
        long currentEnrollmentCount,
        LocalDate startDate,
        LocalDate endDate,
        CourseStatus status,
        Long creatorId,
        LocalDateTime createdAt
) {
    public static CourseResponse of(Course course, long currentEnrollmentCount) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getPrice(),
                course.getMaxCapacity(),
                currentEnrollmentCount,
                course.getStartDate(),
                course.getEndDate(),
                course.getStatus(),
                course.getCreatorId(),
                course.getCreatedAt()
        );
    }
}
