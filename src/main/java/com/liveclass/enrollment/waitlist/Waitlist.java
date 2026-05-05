package com.liveclass.enrollment.waitlist;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "waitlist",
        uniqueConstraints = @UniqueConstraint(columnNames = {"courseId", "userId"}))
public class Waitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Long userId;

    // 대기 순서 (1부터 시작, 낮을수록 우선)
    @Column(nullable = false)
    private int position;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Waitlist() {}

    public Waitlist(Long courseId, Long userId, int position) {
        this.courseId = courseId;
        this.userId = userId;
        this.position = position;
    }

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public Long getUserId() { return userId; }
    public int getPosition() { return position; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
