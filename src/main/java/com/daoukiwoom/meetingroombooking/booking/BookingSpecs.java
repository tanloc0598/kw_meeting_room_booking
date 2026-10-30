package com.daoukiwoom.meetingroombooking.booking;

import java.time.Instant;

import org.springframework.data.jpa.domain.Specification;

final class BookingSpecs {

  /** Bắt buộc, không bao giờ trả null — thiếu là lộ booking của người khác. */
  static Specification<Booking> ownedBy(Long userId) {
    return (root, query, cb) -> cb.equal(root.get("userId"), userId);
  }

  static Specification<Booking> hasStatus(BookingStatus status) {
    return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
  }

  static Specification<Booking> inRoom(Long roomId) {
    return (root, query, cb) -> roomId == null ? null : cb.equal(root.get("roomId"), roomId);
  }

  /** Chưa kết thúc tại mốc from (nửa mở: endAt > from). */
  static Specification<Booking> endsAfter(Instant from) {
    return (root, query, cb) -> from == null ? null : cb.greaterThan(root.get("endAt"), from);
  }

  /** Bắt đầu trước mốc to (nửa mở: startAt < to). */
  static Specification<Booking> startsBefore(Instant to) {
    return (root, query, cb) -> to == null ? null : cb.lessThan(root.get("startAt"), to);
  }

  private BookingSpecs() {
  }
}
