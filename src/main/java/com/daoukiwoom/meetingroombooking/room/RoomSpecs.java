package com.daoukiwoom.meetingroombooking.room;

import java.time.Instant;

import org.springframework.data.jpa.domain.Specification;

import com.daoukiwoom.meetingroombooking.booking.Booking;
import com.daoukiwoom.meetingroombooking.booking.BookingStatus;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

final class RoomSpecs {
  
  static Specification<Room> hasActive(Boolean active) {
    return (root, query, cb) -> active == null ? null : cb.equal(root.get("active"), active);
  }

  static Specification<Room> capacityAtLeast(Integer minCapacity) {
    return (root, query, cb) -> minCapacity == null ? null : cb.greaterThanOrEqualTo(root.get("capacity"), minCapacity);
  }

  static Specification<Room> locationContains(String location) {
    return (root, query, cb) -> location == null || location.isBlank() ? null : cb.like(cb.lower(root.get("location")), "%" + location.trim().toLowerCase() + "%");
  }
  
  static Specification<Room> availableBetween(Instant startAt, Instant endAt) {
    if (startAt == null || endAt == null) {
        return null;
    }
    return (root, query, cb) -> {
        Subquery<Long> overlap = query.subquery(Long.class);
        Root<Booking> b = overlap.from(Booking.class);
        overlap.select(b.get("id")).where(
                cb.equal(b.get("roomId"), root.get("id")),
                cb.equal(b.get("status"), BookingStatus.CONFIRMED),
                cb.lessThan(b.get("startAt"), endAt),
                cb.greaterThan(b.get("endAt"), startAt));
        return cb.not(cb.exists(overlap));
    };
  } 
  
  private RoomSpecs() {
    
  }
  
}
