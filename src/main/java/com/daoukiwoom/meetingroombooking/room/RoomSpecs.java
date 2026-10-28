package com.daoukiwoom.meetingroombooking.room;

import org.springframework.data.jpa.domain.Specification;

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

  private RoomSpecs() {
    
  }
  
}
