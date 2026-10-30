package com.daoukiwoom.meetingroombooking.booking;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    boolean existsByRoomIdAndStatusAndEndAtAfter(Long roomId, BookingStatus status, Instant time);
}
