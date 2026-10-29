package com.daoukiwoom.meetingroombooking.booking;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.daoukiwoom.meetingroombooking.booking.dto.BookingResponse;
import com.daoukiwoom.meetingroombooking.booking.dto.CancelBookingRequest;
import com.daoukiwoom.meetingroombooking.booking.dto.CreateBookingRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {
  private final BookingService bookingService;

  public BookingController(BookingService bookingService) {
      this.bookingService = bookingService;
  }

  @PostMapping
  public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request, Authentication authentication) {
      Long userId = Long.parseLong(authentication.getName());
      BookingResponse booking = bookingService.create(request, userId);
      return ResponseEntity.created(URI.create("/api/v1/bookings/" + booking.id())).body(booking);
  }

  @PostMapping("/{id}/cancel")
  public ResponseEntity<BookingResponse> cancel(@PathVariable Long id,@Valid @RequestBody(required = false) CancelBookingRequest request,
                                              Authentication authentication) {
      Long userId = Long.parseLong(authentication.getName());
       boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
      String reason = request == null ? null : request.cancelReason ();
      BookingResponse booking = bookingService.cancel(id, userId, isAdmin, reason);
      return ResponseEntity.ok(booking);
  }
}
