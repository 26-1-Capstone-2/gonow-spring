package com.timemate.gonow.domain.place.dto;

import com.timemate.gonow.domain.place.constant.PlaceType;
import com.timemate.gonow.domain.place.entity.Place;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PlaceResponse(
        Long placeId,
        String name,
        PlaceType placeType,
        String address,
        BigDecimal lat,
        BigDecimal lng
) {
    public static PlaceResponse from(Place place) {
        return new PlaceResponse(
                place.getId(),
                place.getName(),
                place.getPlaceType(),
                place.getLocation().address(),
                place.getLocation().point().lat(),
                place.getLocation().point().lng()
        );
    }
}
