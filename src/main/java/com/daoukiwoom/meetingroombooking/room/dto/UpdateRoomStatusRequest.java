package com.daoukiwoom.meetingroombooking.room.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateRoomStatusRequest(
    @NotNull
    Boolean active
) {

}
