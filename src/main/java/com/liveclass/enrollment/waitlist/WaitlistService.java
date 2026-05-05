package com.liveclass.enrollment.waitlist;

import com.liveclass.enrollment.common.exception.BusinessException;
import com.liveclass.enrollment.common.exception.ErrorCode;
import com.liveclass.enrollment.course.Course;
import com.liveclass.enrollment.course.CourseRepository;
import com.liveclass.enrollment.course.CourseStatus;
import com.liveclass.enrollment.enrollment.EnrollmentRepository;
import com.liveclass.enrollment.enrollment.EnrollmentStatus;
import com.liveclass.enrollment.waitlist.dto.WaitlistResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public WaitlistService(WaitlistRepository waitlistRepository,
                           CourseRepository courseRepository,
                           EnrollmentRepository enrollmentRepository) {
        this.waitlistRepository = waitlistRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional
    public WaitlistResponse join(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (course.getStatus() != CourseStatus.OPEN) {
            throw new BusinessException(ErrorCode.COURSE_NOT_OPEN);
        }

        List<EnrollmentStatus> active = List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED);
        if (enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(courseId, userId, active)) {
            throw new BusinessException(ErrorCode.ALREADY_ENROLLED);
        }
        if (waitlistRepository.existsByCourseIdAndUserId(courseId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_ON_WAITLIST);
        }

        int nextPosition = waitlistRepository.findMaxPositionByCourseId(courseId) + 1;
        Waitlist waitlist = waitlistRepository.save(new Waitlist(courseId, userId, nextPosition));
        return WaitlistResponse.from(waitlist);
    }

    @Transactional
    public void leave(Long courseId, Long userId) {
        Waitlist entry = waitlistRepository.findByCourseIdAndUserId(courseId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_ON_WAITLIST));
        waitlistRepository.delete(entry);
    }
}
