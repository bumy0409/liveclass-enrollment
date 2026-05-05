package com.liveclass.enrollment.course;

import com.liveclass.enrollment.course.dto.CourseResponse;
import com.liveclass.enrollment.course.dto.CreateCourseRequest;
import com.liveclass.enrollment.course.dto.UpdateStatusRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(
            @Valid @RequestBody CreateCourseRequest req,
            @RequestHeader("X-User-Id") Long creatorId) {
        return courseService.create(req, creatorId);
    }

    @GetMapping
    public Page<CourseResponse> list(
            @RequestParam(required = false) CourseStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return courseService.list(status, pageable);
    }

    @GetMapping("/{courseId}")
    public CourseResponse get(@PathVariable Long courseId) {
        return courseService.get(courseId);
    }

    @PatchMapping("/{courseId}/status")
    public CourseResponse updateStatus(
            @PathVariable Long courseId,
            @Valid @RequestBody UpdateStatusRequest req,
            @RequestHeader("X-User-Id") Long creatorId) {
        return courseService.updateStatus(courseId, req, creatorId);
    }
}
