package com.daoukiwoom.meetingroombooking.booking;

import java.time.Instant;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bookings")
public class Booking {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long roomId;

  @Column(nullable = false, updatable = false)
  private Long userId;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private Instant startAt;

  @Column(nullable = false)
  private Instant endAt;

  @Column(nullable = false)
  private Integer attendees;

  @Enumerated(EnumType.STRING)
  private BookingStatus status = BookingStatus.CONFIRMED;

  @Column(length = 500)
  private String cancelReason;

  @Column(insertable = false, updatable = false)
  private Instant createdAt;

  @Column(insertable = false)
  @UpdateTimestamp
  private Instant updatedAt;

  protected Booking() {

  }

  public Booking(String title, Long roomId, Long userId, Instant startAt, Instant endAt, Integer attendees) {
    this.title = title;
    this.roomId = roomId;
    this.userId = userId;
    this.startAt = startAt;
    this.endAt = endAt;
    this.attendees = attendees;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public Long getRoomId() {
    return roomId;
  }

  public Long getUserId() {
    return userId;
  }

  public Instant getStartAt() {
    return startAt;
  } 

  public Instant getEndAt() {
    return endAt;
  } 

  public Integer getAttendees() {
    return attendees;
  } 

  public BookingStatus getStatus() {
    return status;
  }

  public String getCancelReason() {
    return cancelReason;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
  
  public void update(String title, Long roomId, Instant startAt, Instant endAt, Integer attendees) {
    this.title = title;
    this.roomId = roomId;
    this.startAt = startAt;
    this.endAt = endAt;
    this.attendees = attendees;
  }

  public void cancel(String reason) {
    this.status = BookingStatus.CANCELLED;
    this.cancelReason = reason;
  }
}
