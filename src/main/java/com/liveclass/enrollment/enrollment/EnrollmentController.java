package com.liveclass.enrollment.enrollment;

import com.liveclass.enrollment.enrollment.dto.EnrollRequest;
import com.liveclass.enrollment.enrollment.dto.EnrollmentResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    // 수강 신청
    @PostMapping("/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(
            @Valid @RequestBody EnrollRequest req,
            @RequestHeader("X-User-Id") Long userId) {
        return enrollmentService.enroll(req.courseId(), userId);
    }

    // 결제 확정 (PENDING → CONFIRMED)
    @PatchMapping("/enrollments/{enrollmentId}/confirm")
    public EnrollmentResponse confirm(
            @PathVariable Long enrollmentId,
            @RequestHeader("X-User-Id") Long userId) {
        return enrollmentService.confirm(enrollmentId, userId);
    }

    //수강 취소
    @PatchMapping("/enrollments/{enrollmentId}/cancel")
    public EnrollmentResponse cancel(
            @PathVariable Long enrollmentId,
            @RequestHeader("X-User-Id") Long userId) {
        return enrollmentService.cancel(enrollmentId, userId);
    }

    // 내 수강 신청 목록
    @GetMapping("/enrollments/me")
    public Page<EnrollmentResponse> getMyEnrollments(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return enrollmentService.getMyEnrollments(userId, pageable);
    }

    // 강의별 수강생 목록 (크리에이터 전용)
    @GetMapping("/courses/{courseId}/enrollments")
    public Page<EnrollmentResponse> getCourseEnrollments(
            @PathVariable Long courseId,
            @RequestHeader("X-User-Id") Long creatorId,
            @PageableDefault(size = 20) Pageable pageable) {
        return enrollmentService.getCourseEnrollments(courseId, creatorId, pageable);
    }
}
