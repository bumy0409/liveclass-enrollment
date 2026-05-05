package com.liveclass.enrollment.common.exception;

public enum ErrorCode {
    COURSE_NOT_FOUND("강의를 찾을 수 없습니다", 404),
    ENROLLMENT_NOT_FOUND("수강 신청 내역을 찾을 수 없습니다", 404),
    WAITLIST_NOT_FOUND("대기열 항목을 찾을 수 없습니다", 404),

    COURSE_NOT_OPEN("수강 신청이 가능한 강의가 아닙니다", 400),
    COURSE_FULL("정원이 초과되었습니다", 409),
    INVALID_STATUS_TRANSITION("유효하지 않은 상태 전이입니다", 400),
    NOT_COURSE_CREATOR("강의 개설자만 접근할 수 있습니다", 403),

    ALREADY_ENROLLED("이미 수강 신청한 강의입니다", 409),
    ALREADY_ON_WAITLIST("이미 대기열에 등록되어 있습니다", 409),
    ALREADY_CANCELLED("이미 취소된 수강 신청입니다", 400),
    CANCELLATION_PERIOD_EXPIRED("취소 가능 기간이 지났습니다", 400),
    ENROLLMENT_NOT_OWNED("본인의 수강 신청만 처리할 수 있습니다", 403),
    NOT_ON_WAITLIST("대기열에 등록되어 있지 않습니다", 404);

    private final String message;
    private final int httpStatus;

    ErrorCode(String message, int httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getMessage() { return message; }
    public int getHttpStatus() { return httpStatus; }
}
