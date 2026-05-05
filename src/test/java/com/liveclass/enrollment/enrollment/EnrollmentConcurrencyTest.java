package com.liveclass.enrollment.enrollment;

import com.liveclass.enrollment.common.exception.BusinessException;
import com.liveclass.enrollment.course.Course;
import com.liveclass.enrollment.course.CourseRepository;
import com.liveclass.enrollment.course.CourseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class EnrollmentConcurrencyTest {

    @Autowired EnrollmentService enrollmentService;
    @Autowired CourseRepository courseRepository;
    @Autowired EnrollmentRepository enrollmentRepository;

    @Test
    @DisplayName("정원 1명 강의에 10명이 동시 신청하면 1명만 성공한다")
    void concurrentEnrollShouldRespectCapacity() throws InterruptedException {
        Course course = courseRepository.save(
                new Course("한정판 강의", "설명", 10000, 1,
                        LocalDate.now(), LocalDate.now().plusMonths(1), 1L));
        course.transitionTo(CourseStatus.OPEN);
        courseRepository.save(course);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            long userId = 100L + i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await(); // 모든 스레드가 준비될 때까지 대기
                    enrollmentService.enroll(course.getId(), userId);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await(); // 모든 스레드 준비 완료
        start.countDown(); // 동시 출발

        executor.shutdown();
        executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        // 성공은 정확히 1건이어야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(9);

        long activeCount = enrollmentRepository.countActiveEnrollments(
                course.getId(), java.util.List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED));
        assertThat(activeCount).isEqualTo(1);
    }

    @Test
    @DisplayName("정원 5명 강의에 20명이 동시 신청하면 5명만 성공한다")
    void concurrentEnrollWithHigherCapacity() throws InterruptedException {
        Course course = courseRepository.save(
                new Course("인기 강의", "설명", 10000, 5,
                        LocalDate.now(), LocalDate.now().plusMonths(1), 1L));
        course.transitionTo(CourseStatus.OPEN);
        courseRepository.save(course);

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            long userId = 200L + i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    enrollmentService.enroll(course.getId(), userId);
                    successCount.incrementAndGet();
                } catch (BusinessException | InterruptedException ignored) {}
            });
        }

        ready.await();
        start.countDown();

        executor.shutdown();
        executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        assertThat(successCount.get()).isEqualTo(5);

        long activeCount = enrollmentRepository.countActiveEnrollments(
                course.getId(), java.util.List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED));
        assertThat(activeCount).isEqualTo(5);
    }
}
