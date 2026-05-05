package com.liveclass.enrollment.waitlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {

    Optional<Waitlist> findFirstByCourseIdOrderByPositionAsc(Long courseId);

    boolean existsByCourseIdAndUserId(Long courseId, Long userId);

    Optional<Waitlist> findByCourseIdAndUserId(Long courseId, Long userId);

    @Query("SELECT COALESCE(MAX(w.position), 0) FROM Waitlist w WHERE w.courseId = :courseId")
    int findMaxPositionByCourseId(@Param("courseId") Long courseId);
}
