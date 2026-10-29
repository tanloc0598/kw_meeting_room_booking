package com.daoukiwoom.meetingroombooking.booking;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.daoukiwoom.meetingroombooking.booking.dto.BookingResponse;
import com.daoukiwoom.meetingroombooking.booking.dto.CreateBookingRequest;
import com.daoukiwoom.meetingroombooking.common.exception.ErrorCode;
import com.daoukiwoom.meetingroombooking.common.exception.KwException;
import com.daoukiwoom.meetingroombooking.room.Room;
import com.daoukiwoom.meetingroombooking.room.RoomRepository;

@Service
@Transactional(readOnly = true)
public class BookingService {
  private final BookingRepository bookingRepository;
  private final RoomRepository roomRepository;
  private final BookingProperties bookingProperties;

  public BookingService(BookingRepository bookingRepository, RoomRepository roomRepository,
      BookingProperties bookingProperties) {
    this.bookingRepository = bookingRepository;
    this.roomRepository = roomRepository;
    this.bookingProperties = bookingProperties;
  }

  @Transactional
  public BookingResponse create(CreateBookingRequest request, Long userId) {
    Room room = findBookableRoom(request.roomId(), request.attendees());
    Instant now = Instant.now();

    if (request.startAt().isBefore(now)) {
      throw new KwException(ErrorCode.BOOKING_IN_PAST, "Start time must be in the future");
    }
    validateDuration(request.startAt(), request.endAt());

    Booking booking = new Booking(request.title(), room.getId(), userId, request.startAt(), request.endAt(),
        request.attendees());

    Booking bookingSaved;

    try {
      bookingSaved = bookingRepository.saveAndFlush(booking);
    } catch (DataIntegrityViolationException e) {
      if (e.getMostSpecificCause().getMessage().contains("bookings_no_overlap")) {
        throw new KwException(ErrorCode.BOOKING_OVERLAP, "Room is already booked in this time range");
      }
      throw e;
    }
    return BookingResponse.from(bookingSaved);
  }

  @Transactional
  public BookingResponse cancel(Long bookingId, Long userId, boolean isAdmin, String cancelReason) {
    Booking booking = findBooking(bookingId);
    checkCanModify(booking, userId, isAdmin);
    booking.cancel(cancelReason);
    return BookingResponse.from(booking);
  }

  private Booking findBooking(Long id) {
    return bookingRepository.findById(id)
        .orElseThrow(() -> new KwException(ErrorCode.BOOKING_NOT_FOUND, "Booking not found with id: " + id));
  }

  private void checkCanModify(Booking booking, Long userId, boolean isAdmin) {
    if (!isAdmin && !booking.getUserId().equals(userId)) {
      throw new AccessDeniedException("User is not the owner of the booking");
    }
    if (booking.getStatus() == BookingStatus.CANCELLED) {
      throw new KwException(ErrorCode.BOOKING_ALREADY_CANCELLED, "Booking is already cancelled");
    }
    if (!booking.getEndAt().isAfter(Instant.now())) {
      throw new KwException(ErrorCode.BOOKING_ALREADY_ENDED, "Booking has already ended");
    }
  }

  private Room findBookableRoom(Long roomId, int attendees) {
    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new KwException(ErrorCode.ROOM_NOT_FOUND, "Room not found with id: " + roomId));
    if (!room.isActive()) {
      throw new KwException(ErrorCode.ROOM_INACTIVE, "Room is inactive: " + roomId);
    }
    if (attendees > room.getCapacity()) {
      throw new KwException(ErrorCode.BOOKING_EXCEEDS_CAPACITY, "Attendees exceed room capacity");
    }
    return room;
  }

  private void validateDuration(Instant startAt, Instant endAt) {
    Duration duration = Duration.between(startAt, endAt);
    if (duration.compareTo(bookingProperties.minDuration()) < 0) {
      throw new KwException(ErrorCode.BOOKING_INVALID_DURATION, "Booking duration must be at least " + bookingProperties.minDuration().toMinutes() + " minutes");
    }
    if (duration.toMinutes() % bookingProperties.durationStep().toMinutes() != 0) {
      throw new KwException(ErrorCode.BOOKING_INVALID_DURATION, "Duration must be a multiple of " + bookingProperties.durationStep().toMinutes() + " minutes");
    }
    ZoneId zone = bookingProperties.zone();
    LocalDate day = startAt.atZone(zone).toLocalDate();
    Instant endOfDay = day.plusDays(1).atStartOfDay(zone).toInstant();
    if (endAt.isAfter(endOfDay)) {
      throw new KwException(ErrorCode.BOOKING_INVALID_DURATION, "Booking must not cross midnight");
    }
  }

}
