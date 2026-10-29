package com.daoukiwoom.meetingroombooking.booking.dto;

import java.time.Instant;

import com.daoukiwoom.meetingroombooking.booking.Booking;
import com.daoukiwoom.meetingroombooking.booking.BookingStatus;

public record BookingResponse(
    Long id,
    String title,
    Long roomId,
    Long userId,
    Instant startAt,
    Instant endAt,
    Integer attendees,
    BookingStatus status,
    String cancelReason,
    Instant createdAt,
    Instant updatedAt
) {
  public static BookingResponse from(Booking booking) {
    return new BookingResponse(booking.getId(), booking.getTitle(), booking.getRoomId(), booking.getUserId(), booking.getStartAt(), booking.getEndAt(), booking.getAttendees(), booking.getStatus(), booking.getCancelReason(), booking.getCreatedAt(), booking.getUpdatedAt());
  }
}
