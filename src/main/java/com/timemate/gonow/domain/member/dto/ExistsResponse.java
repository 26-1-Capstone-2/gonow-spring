package com.timemate.gonow.domain.member.dto;

public record ExistsResponse(
        boolean exists
) {
    public static ExistsResponse from(boolean exists) {
        return new ExistsResponse(exists);
    }
}
