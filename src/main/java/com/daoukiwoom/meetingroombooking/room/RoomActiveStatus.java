package com.daoukiwoom.meetingroombooking.room;

public enum RoomActiveStatus {
    ALL(null),
    ACTIVE(true),
    INACTIVE(false);

    private final Boolean value;

    RoomActiveStatus(Boolean value) {
        this.value = value;
    }

    public Boolean getValue() {
        return value;
    }
}
