package com.daoukiwoom.meetingroombooking.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND),
    ROOM_CODE_DUPLICATED(HttpStatus.CONFLICT),
    ROOM_HAS_FUTURE_BOOKINGS(HttpStatus.CONFLICT);
    // Booking sẽ thêm sau: BOOKING_CONFLICT, CAPACITY_EXCEEDED, ...

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
