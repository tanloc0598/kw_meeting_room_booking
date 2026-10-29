package com.daoukiwoom.meetingroombooking.booking.dto;

import java.time.Instant;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBookingRequest(
    @NotNull 
    Long roomId,
    
    @NotBlank 
    @Size(max = 255)
    String title,
    
    @NotNull
    Instant startAt,
    
    @NotNull
    Instant endAt,
    
    @NotNull 
    @Min(1)
    Integer attendees
) {
    @AssertTrue(message = "endAt must be after startAt")
    public boolean isEndAtAfterStartAt() {
        return startAt == null || endAt == null || endAt.isAfter(startAt);
    }

}
