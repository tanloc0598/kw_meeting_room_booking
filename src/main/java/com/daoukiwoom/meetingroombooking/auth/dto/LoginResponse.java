package com.daoukiwoom.meetingroombooking.auth.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
