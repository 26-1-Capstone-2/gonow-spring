package com.timemate.gonow.domain.place.dto;

import com.timemate.gonow.domain.place.constant.PlaceType;
import com.timemate.gonow.domain.place.entity.Place;

import java.math.BigDecimal;

public record PlaceResponse(
        Long placeId,
        PlaceType placeType,
        String name,
        String address,
        BigDecimal lat,
        BigDecimal lng
) {
    public static PlaceResponse from(Place place) {
        return new PlaceResponse(
                place.getId(),
                place.getPlaceType(),
                place.getLocation().name(),
                place.getLocation().address(),
                place.getLocation().point().lat(),
                place.getLocation().point().lng()
        );
    }
}
