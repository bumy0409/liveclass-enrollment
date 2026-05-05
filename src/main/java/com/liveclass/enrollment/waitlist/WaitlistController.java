package com.liveclass.enrollment.waitlist;

import com.liveclass.enrollment.waitlist.dto.WaitlistRequest;
import com.liveclass.enrollment.waitlist.dto.WaitlistResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {

    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    // 대기열 등록
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WaitlistResponse join(
            @Valid @RequestBody WaitlistRequest req,
            @RequestHeader("X-User-Id") Long userId) {
        return waitlistService.join(req.courseId(), userId);
    }

    // 대기열 취소
    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(
            @PathVariable Long courseId,
            @RequestHeader("X-User-Id") Long userId) {
        waitlistService.leave(courseId, userId);
    }
}
