package com.liveclass.enrollment.waitlist.dto;

import com.liveclass.enrollment.waitlist.Waitlist;

import java.time.LocalDateTime;

public record WaitlistResponse(
        Long id,
        Long courseId,
        Long userId,
        int position,
        LocalDateTime createdAt
) {
    public static WaitlistResponse from(Waitlist w) {
        return new WaitlistResponse(w.getId(), w.getCourseId(), w.getUserId(), w.getPosition(), w.getCreatedAt());
    }
}
