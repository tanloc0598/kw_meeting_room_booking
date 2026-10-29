package com.daoukiwoom.meetingroombooking.booking;

import java.time.Duration;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.booking")
public record BookingProperties(Duration minDuration, Duration durationStep, ZoneId zone) { 
  
}