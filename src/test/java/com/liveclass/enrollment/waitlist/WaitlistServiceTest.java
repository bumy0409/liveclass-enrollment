package com.liveclass.enrollment.waitlist;

import com.liveclass.enrollment.common.exception.BusinessException;
import com.liveclass.enrollment.common.exception.ErrorCode;
import com.liveclass.enrollment.course.Course;
import com.liveclass.enrollment.course.CourseRepository;
import com.liveclass.enrollment.course.CourseStatus;
import com.liveclass.enrollment.enrollment.EnrollmentRepository;
import com.liveclass.enrollment.waitlist.dto.WaitlistResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WaitlistServiceTest {

    @Mock WaitlistRepository waitlistRepository;
    @Mock CourseRepository courseRepository;
    @Mock EnrollmentRepository enrollmentRepository;
    @InjectMocks WaitlistService waitlistService;

    private Course openCourse;

    @BeforeEach
    void setUp() {
        openCourse = new Course("Java 기초", "설명", 50000, 1,
                LocalDate.now(), LocalDate.now().plusMonths(3), 1L);
        openCourse.transitionTo(CourseStatus.OPEN);
    }

    @Test
    @DisplayName("대기열 등록 성공")
    void joinWaitlist() {
        given(courseRepository.findById(1L)).willReturn(Optional.of(openCourse));
        given(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(any(), any(), any())).willReturn(false);
        given(waitlistRepository.existsByCourseIdAndUserId(any(), any())).willReturn(false);
        given(waitlistRepository.findMaxPositionByCourseId(1L)).willReturn(2);
        Waitlist saved = new Waitlist(1L, 2L, 3);
        given(waitlistRepository.save(any())).willReturn(saved);

        WaitlistResponse res = waitlistService.join(1L, 2L);

        assertThat(res.position()).isEqualTo(3);
    }

    @Test
    @DisplayName("이미 수강 중인 사용자는 대기열 등록 불가")
    void cannotJoinIfAlreadyEnrolled() {
        given(courseRepository.findById(1L)).willReturn(Optional.of(openCourse));
        given(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(any(), any(), any())).willReturn(true);

        assertThatThrownBy(() -> waitlistService.join(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_ENROLLED);
    }

    @Test
    @DisplayName("이미 대기열에 있는 사용자는 중복 등록 불가")
    void cannotJoinTwice() {
        given(courseRepository.findById(1L)).willReturn(Optional.of(openCourse));
        given(enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(any(), any(), any())).willReturn(false);
        given(waitlistRepository.existsByCourseIdAndUserId(1L, 2L)).willReturn(true);

        assertThatThrownBy(() -> waitlistService.join(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_ON_WAITLIST);
    }

    @Test
    @DisplayName("대기열에 없는 사용자는 대기열 취소 불가")
    void leaveWhenNotOnWaitlist() {
        given(waitlistRepository.findByCourseIdAndUserId(1L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> waitlistService.leave(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_ON_WAITLIST);
    }
}
