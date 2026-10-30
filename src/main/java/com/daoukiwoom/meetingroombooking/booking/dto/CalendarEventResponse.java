package com.daoukiwoom.meetingroombooking.booking.dto;

import java.time.Instant;

import com.daoukiwoom.meetingroombooking.booking.Booking;
import com.daoukiwoom.meetingroombooking.room.Room;

public record CalendarEventResponse(
    Long id,
    String title,
    Long userId,
    boolean mine,
    Long roomId,
    String roomCode,
    String roomName,
    Instant startAt,
    Instant endAt,
    Integer attendees
) {
  public static CalendarEventResponse from(Booking booking, Room room, Long viewerId) {
    return new CalendarEventResponse(
        booking.getId(), booking.getTitle(), booking.getUserId(), booking.getUserId().equals(viewerId),
        booking.getRoomId(), room.getCode(), room.getName(),
        booking.getStartAt(), booking.getEndAt(), booking.getAttendees());
  }
}
