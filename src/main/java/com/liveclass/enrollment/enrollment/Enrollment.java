package com.liveclass.enrollment.enrollment;

import com.liveclass.enrollment.common.exception.BusinessException;
import com.liveclass.enrollment.common.exception.ErrorCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "enrollment")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.PENDING;

    @Column
    private LocalDateTime confirmedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected Enrollment() {}

    public Enrollment(Long courseId, Long userId) {
        this.courseId = courseId;
        this.userId = userId;
    }

    public void confirm() {
        if (status != EnrollmentStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "PENDING 상태의 신청만 결제 확정할 수 있습니다");
        }
        this.status = EnrollmentStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel(long deadlineDays) {
        switch (status) {
            case PENDING -> {
                this.status = EnrollmentStatus.CANCELLED;
                this.updatedAt = LocalDateTime.now();
            }
            case CONFIRMED -> {
                if (LocalDateTime.now().isAfter(confirmedAt.plusDays(deadlineDays))) {
                    throw new BusinessException(ErrorCode.CANCELLATION_PERIOD_EXPIRED,
                            "결제 후 " + deadlineDays + "일 이내에만 취소할 수 있습니다");
                }
                this.status = EnrollmentStatus.CANCELLED;
                this.updatedAt = LocalDateTime.now();
            }
            case CANCELLED -> throw new BusinessException(ErrorCode.ALREADY_CANCELLED);
        }
    }

    public void requireOwner(Long userId) {
        if (!this.userId.equals(userId)) {
            throw new BusinessException(ErrorCode.ENROLLMENT_NOT_OWNED);
        }
    }

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public Long getUserId() { return userId; }
    public EnrollmentStatus getStatus() { return status; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
