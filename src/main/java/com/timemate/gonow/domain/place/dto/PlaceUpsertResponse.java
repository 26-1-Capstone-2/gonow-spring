package com.timemate.gonow.domain.place.dto;

public record PlaceUpsertResponse(
        Long placeId
) {
    public static PlaceUpsertResponse from(Long placeId) {
        return new PlaceUpsertResponse(placeId);
    }
}
