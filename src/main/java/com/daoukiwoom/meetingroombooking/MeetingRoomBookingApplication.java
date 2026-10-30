package com.daoukiwoom.meetingroombooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MeetingRoomBookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeetingRoomBookingApplication.class, args);
    }
}

