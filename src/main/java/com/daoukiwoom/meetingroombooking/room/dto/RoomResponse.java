package com.daoukiwoom.meetingroombooking.room.dto;

import com.daoukiwoom.meetingroombooking.room.Room;

public record RoomResponse(
    Long id,
    String name,
    String code,
    String location,
    boolean active,
    int capacity
) {
  public static RoomResponse from(Room room) {
    return new RoomResponse( room.getId(), room.getName(), room.getCode(), room.getLocation(), room.isActive(), room.getCapacity());
}

}