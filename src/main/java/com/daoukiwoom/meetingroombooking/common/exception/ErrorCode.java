package com.daoukiwoom.meetingroombooking.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND),
    ROOM_CODE_DUPLICATED(HttpStatus.CONFLICT),
    ROOM_HAS_FUTURE_BOOKINGS(HttpStatus.CONFLICT),
    ROOM_INACTIVE(HttpStatus.UNPROCESSABLE_ENTITY),
    BOOKING_IN_PAST(HttpStatus.UNPROCESSABLE_ENTITY),
    BOOKING_TOO_FAR_AHEAD(HttpStatus.UNPROCESSABLE_ENTITY),
    BOOKING_INVALID_DURATION(HttpStatus.UNPROCESSABLE_ENTITY),
    BOOKING_EXCEEDS_CAPACITY(HttpStatus.UNPROCESSABLE_ENTITY),
    BOOKING_OVERLAP(HttpStatus.CONFLICT),
    BOOKING_NOT_FOUND(HttpStatus.NOT_FOUND),
    BOOKING_ALREADY_ENDED(HttpStatus.UNPROCESSABLE_ENTITY),
    BOOKING_ALREADY_CANCELLED(HttpStatus.UNPROCESSABLE_ENTITY),
    BOOKING_VERSION_CONFLICT(HttpStatus.CONFLICT),
    BOOKING_IN_PROGRESS(HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_SEARCH_PARAMS(HttpStatus.BAD_REQUEST);
    
    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
