package com.daoukiwoom.meetingroombooking.common.exception;

public class KwException extends RuntimeException {

    private final ErrorCode code;

    public KwException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
