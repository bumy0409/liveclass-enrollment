package com.liveclass.enrollment.enrollment;

import com.liveclass.enrollment.common.exception.BusinessException;
import com.liveclass.enrollment.common.exception.ErrorCode;
import com.liveclass.enrollment.course.Course;
import com.liveclass.enrollment.course.CourseRepository;
import com.liveclass.enrollment.course.CourseStatus;
import com.liveclass.enrollment.enrollment.dto.EnrollmentResponse;
import com.liveclass.enrollment.waitlist.WaitlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock EnrollmentRepository enrollmentRepository;
    @Mock CourseRepository courseRepository;
    @Mock WaitlistRepository waitlistRepository;
    @InjectMocks EnrollmentService enrollmentService;

    private Course openCourse;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(enrollmentService, "cancellationDeadlineDays", 7L);

        openCourse = new Course("Java 기초", "설명", 50000, 30,
                LocalDate.now(), LocalDate.now().plusMonths(3), 1L);
        openCourse.transitionTo(CourseStatus.OPEN);
    }

    @Test
    @DisplayName("정원이 남아있으면 수강 신청 성공")
    void enrollSuccess() {
        given(courseRepository.findByIdWithLock(1L)).willReturn(Optional.of(openCourse));
        given(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(any(), any(), any())).willReturn(false);
        given(enrollmentRepository.countActiveEnrollments(any(), any())).willReturn(5L);
        Enrollment saved = new Enrollment(1L, 2L);
        given(enrollmentRepository.save(any())).willReturn(saved);

        EnrollmentResponse res = enrollmentService.enroll(1L, 2L);

        assertThat(res.status()).isEqualTo(EnrollmentStatus.PENDING);
    }

    @Test
    @DisplayName("정원 초과 시 수강 신청 거부")
    void enrollFailWhenFull() {
        openCourse = new Course("Java 기초", "설명", 50000, 1,
                LocalDate.now(), LocalDate.now().plusMonths(3), 1L);
        openCourse.transitionTo(CourseStatus.OPEN);
        given(courseRepository.findByIdWithLock(1L)).willReturn(Optional.of(openCourse));
        given(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(any(), any(), any())).willReturn(false);
        given(enrollmentRepository.countActiveEnrollments(any(), any())).willReturn(1L);

        assertThatThrownBy(() -> enrollmentService.enroll(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_FULL);
    }

    @Test
    @DisplayName("DRAFT 강의에는 신청 불가")
    void enrollFailWhenDraft() {
        Course draftCourse = new Course("Java 기초", "설명", 50000, 30,
                LocalDate.now(), LocalDate.now().plusMonths(3), 1L);
        given(courseRepository.findByIdWithLock(1L)).willReturn(Optional.of(draftCourse));

        assertThatThrownBy(() -> enrollmentService.enroll(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_NOT_OPEN);
    }

    @Test
    @DisplayName("이미 신청한 강의에 재신청 불가")
    void enrollFailWhenAlreadyEnrolled() {
        given(courseRepository.findByIdWithLock(1L)).willReturn(Optional.of(openCourse));
        given(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(any(), any(), any())).willReturn(true);

        assertThatThrownBy(() -> enrollmentService.enroll(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_ENROLLED);
    }

    @Test
    @DisplayName("결제 확정 시 CONFIRMED 상태로 변경")
    void confirmSuccess() {
        Enrollment enrollment = new Enrollment(1L, 2L);
        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));

        EnrollmentResponse res = enrollmentService.confirm(1L, 2L);

        assertThat(res.status()).isEqualTo(EnrollmentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("PENDING 취소 시 즉시 취소됨")
    void cancelPendingSuccess() {
        Enrollment enrollment = new Enrollment(1L, 2L);
        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));
        given(waitlistRepository.findFirstByCourseIdOrderByPositionAsc(any())).willReturn(Optional.empty());

        EnrollmentResponse res = enrollmentService.cancel(1L, 2L);

        assertThat(res.status()).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("결제 후 7일 이내 취소 가능")
    void cancelConfirmedWithinDeadline() {
        Enrollment enrollment = new Enrollment(1L, 2L);
        enrollment.confirm();
        // confirmedAt을 3일 전으로 조작
        ReflectionTestUtils.setField(enrollment, "confirmedAt", LocalDateTime.now().minusDays(3));
        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));
        given(waitlistRepository.findFirstByCourseIdOrderByPositionAsc(any())).willReturn(Optional.empty());

        EnrollmentResponse res = enrollmentService.cancel(1L, 2L);

        assertThat(res.status()).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("결제 후 7일 초과 시 취소 불가")
    void cancelConfirmedAfterDeadline() {
        Enrollment enrollment = new Enrollment(1L, 2L);
        enrollment.confirm();
        ReflectionTestUtils.setField(enrollment, "confirmedAt", LocalDateTime.now().minusDays(8));
        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> enrollmentService.cancel(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANCELLATION_PERIOD_EXPIRED);
    }

    @Test
    @DisplayName("본인 신청이 아닌 경우 취소 불가")
    void cancelNotOwned() {
        Enrollment enrollment = new Enrollment(1L, 2L);
        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> enrollmentService.cancel(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ENROLLMENT_NOT_OWNED);
    }
}
