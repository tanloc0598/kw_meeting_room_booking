package com.daoukiwoom.meetingroombooking.room.dto;

public record RoomResponse(
    Long id,
    String name,
    String code,
    String location,
    boolean active,
    int capacity
) {
}