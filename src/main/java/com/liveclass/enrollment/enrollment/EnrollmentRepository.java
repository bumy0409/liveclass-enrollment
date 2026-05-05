package com.liveclass.enrollment.enrollment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.courseId = :courseId AND e.status IN :statuses")
    long countActiveEnrollments(@Param("courseId") Long courseId, @Param("statuses") List<EnrollmentStatus> statuses);

    boolean existsByCourseIdAndUserIdAndStatusIn(Long courseId, Long userId, List<EnrollmentStatus> statuses);

    Page<Enrollment> findByUserId(Long userId, Pageable pageable);

    Page<Enrollment> findByCourseIdAndStatusIn(Long courseId, List<EnrollmentStatus> statuses, Pageable pageable);
}
