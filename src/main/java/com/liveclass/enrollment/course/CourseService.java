package com.liveclass.enrollment.course;

import com.liveclass.enrollment.common.exception.BusinessException;
import com.liveclass.enrollment.common.exception.ErrorCode;
import com.liveclass.enrollment.course.dto.CourseResponse;
import com.liveclass.enrollment.course.dto.CreateCourseRequest;
import com.liveclass.enrollment.course.dto.UpdateStatusRequest;
import com.liveclass.enrollment.enrollment.EnrollmentRepository;
import com.liveclass.enrollment.enrollment.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public CourseService(CourseRepository courseRepository, EnrollmentRepository enrollmentRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional
    public CourseResponse create(CreateCourseRequest req, Long creatorId) {
        Course course = new Course(
                req.title(), req.description(), req.price(),
                req.maxCapacity(), req.startDate(), req.endDate(), creatorId
        );
        courseRepository.save(course);
        return CourseResponse.of(course, 0);
    }

    public Page<CourseResponse> list(CourseStatus status, Pageable pageable) {
        Page<Course> courses = (status != null)
                ? courseRepository.findByStatus(status, pageable)
                : courseRepository.findAll(pageable);
        return courses.map(c -> CourseResponse.of(c, countActiveEnrollments(c.getId())));
    }

    public CourseResponse get(Long courseId) {
        Course course = findOrThrow(courseId);
        return CourseResponse.of(course, countActiveEnrollments(courseId));
    }

    @Transactional
    public CourseResponse updateStatus(Long courseId, UpdateStatusRequest req, Long creatorId) {
        Course course = findOrThrow(courseId);
        course.requireCreator(creatorId);
        course.transitionTo(req.status());
        return CourseResponse.of(course, countActiveEnrollments(courseId));
    }

    Course findOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
    }

    private long countActiveEnrollments(Long courseId) {
        return enrollmentRepository.countActiveEnrollments(
                courseId, List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED));
    }
}
