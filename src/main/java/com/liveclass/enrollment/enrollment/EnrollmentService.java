package com.liveclass.enrollment.enrollment;

import com.liveclass.enrollment.common.exception.BusinessException;
import com.liveclass.enrollment.common.exception.ErrorCode;
import com.liveclass.enrollment.course.Course;
import com.liveclass.enrollment.course.CourseRepository;
import com.liveclass.enrollment.course.CourseStatus;
import com.liveclass.enrollment.enrollment.dto.EnrollmentResponse;
import com.liveclass.enrollment.waitlist.WaitlistRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EnrollmentService {

    static final List<EnrollmentStatus> ACTIVE = List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED);

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final WaitlistRepository waitlistRepository;

    @Value("${enrollment.cancellation-deadline-days:7}")
    private long cancellationDeadlineDays;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             CourseRepository courseRepository,
                             WaitlistRepository waitlistRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.waitlistRepository = waitlistRepository;
    }

    @Transactional
    public EnrollmentResponse enroll(Long courseId, Long userId) {
        Course course = courseRepository.findByIdWithLock(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (course.getStatus() != CourseStatus.OPEN) {
            throw new BusinessException(ErrorCode.COURSE_NOT_OPEN);
        }
        if (enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(courseId, userId, ACTIVE)) {
            throw new BusinessException(ErrorCode.ALREADY_ENROLLED);
        }

        long count = enrollmentRepository.countActiveEnrollments(courseId, ACTIVE);
        if (count >= course.getMaxCapacity()) {
            throw new BusinessException(ErrorCode.COURSE_FULL);
        }

        Enrollment enrollment = enrollmentRepository.save(new Enrollment(courseId, userId));
        return EnrollmentResponse.from(enrollment);
    }

    @Transactional
    public EnrollmentResponse confirm(Long enrollmentId, Long userId) {
        Enrollment enrollment = findOrThrow(enrollmentId);
        enrollment.requireOwner(userId);
        enrollment.confirm();
        return EnrollmentResponse.from(enrollment);
    }

    @Transactional
    public EnrollmentResponse cancel(Long enrollmentId, Long userId) {
        Enrollment enrollment = findOrThrow(enrollmentId);
        enrollment.requireOwner(userId);
        enrollment.cancel(cancellationDeadlineDays);

        // 취소 시 대기열 1순위를 자동 승격
        promoteFirstWaitlisted(enrollment.getCourseId());

        return EnrollmentResponse.from(enrollment);
    }

    public Page<EnrollmentResponse> getMyEnrollments(Long userId, Pageable pageable) {
        return enrollmentRepository.findByUserId(userId, pageable)
                .map(EnrollmentResponse::from);
    }

    public Page<EnrollmentResponse> getCourseEnrollments(Long courseId, Long creatorId, Pageable pageable) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        course.requireCreator(creatorId);

        return enrollmentRepository.findByCourseIdAndStatusIn(courseId, ACTIVE, pageable)
                .map(EnrollmentResponse::from);
    }

    private void promoteFirstWaitlisted(Long courseId) {
        waitlistRepository.findFirstByCourseIdOrderByPositionAsc(courseId).ifPresent(first -> {
            waitlistRepository.delete(first);
            enrollmentRepository.save(new Enrollment(courseId, first.getUserId()));
        });
    }

    private Enrollment findOrThrow(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENROLLMENT_NOT_FOUND));
    }
}
