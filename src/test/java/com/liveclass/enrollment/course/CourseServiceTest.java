package com.liveclass.enrollment.course;

import com.liveclass.enrollment.common.exception.BusinessException;
import com.liveclass.enrollment.common.exception.ErrorCode;
import com.liveclass.enrollment.course.dto.CreateCourseRequest;
import com.liveclass.enrollment.course.dto.CourseResponse;
import com.liveclass.enrollment.course.dto.UpdateStatusRequest;
import com.liveclass.enrollment.enrollment.EnrollmentRepository;
import com.liveclass.enrollment.enrollment.EnrollmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock CourseRepository courseRepository;
    @Mock EnrollmentRepository enrollmentRepository;
    @InjectMocks CourseService courseService;

    private Course openCourse;

    @BeforeEach
    void setUp() {
        openCourse = new Course("Java 기초", "설명", 50000, 30,
                LocalDate.now(), LocalDate.now().plusMonths(3), 1L);
        openCourse.transitionTo(CourseStatus.OPEN);
    }

    @Test
    @DisplayName("강의를 등록하면 DRAFT 상태로 생성된다")
    void createCourse() {
        CreateCourseRequest req = new CreateCourseRequest(
                "Java 기초", "설명", 50000, 30,
                LocalDate.now(), LocalDate.now().plusMonths(3));
        Course saved = new Course("Java 기초", "설명", 50000, 30,
                LocalDate.now(), LocalDate.now().plusMonths(3), 1L);
        given(courseRepository.save(any())).willReturn(saved);

        CourseResponse res = courseService.create(req, 1L);

        assertThat(res.status()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    @DisplayName("DRAFT → OPEN 전이는 성공한다")
    void transitionDraftToOpen() {
        Course draft = new Course("Java 기초", "설명", 50000, 30,
                LocalDate.now(), LocalDate.now().plusMonths(3), 1L);
        given(courseRepository.findById(1L)).willReturn(Optional.of(draft));
        given(enrollmentRepository.countActiveEnrollments(any(), any())).willReturn(0L);

        CourseResponse res = courseService.updateStatus(1L, new UpdateStatusRequest(CourseStatus.OPEN), 1L);

        assertThat(res.status()).isEqualTo(CourseStatus.OPEN);
    }

    @Test
    @DisplayName("CLOSED → OPEN 전이는 실패한다")
    void invalidTransitionClosedToOpen() {
        Course closed = new Course("Java 기초", "설명", 50000, 30,
                LocalDate.now(), LocalDate.now().plusMonths(3), 1L);
        closed.transitionTo(CourseStatus.OPEN);
        closed.transitionTo(CourseStatus.CLOSED);
        given(courseRepository.findById(1L)).willReturn(Optional.of(closed));

        assertThatThrownBy(() -> courseService.updateStatus(1L, new UpdateStatusRequest(CourseStatus.OPEN), 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("강의 개설자가 아닌 사람은 상태 변경 불가")
    void onlyCreatorCanUpdateStatus() {
        Course draft = new Course("Java 기초", "설명", 50000, 30,
                LocalDate.now(), LocalDate.now().plusMonths(3), 1L);
        given(courseRepository.findById(1L)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> courseService.updateStatus(1L, new UpdateStatusRequest(CourseStatus.OPEN), 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_COURSE_CREATOR);
    }

    @Test
    @DisplayName("존재하지 않는 강의 조회 시 예외")
    void courseNotFound() {
        given(courseRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.get(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);
    }
}
