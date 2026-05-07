package com.timemate.gonow.domain.place.controller;

import com.timemate.gonow.domain.place.constant.PlaceType;
import com.timemate.gonow.domain.place.dto.PlaceResponse;
import com.timemate.gonow.domain.place.dto.PlaceUpsertRequest;
import com.timemate.gonow.domain.place.service.PlaceService;
import com.timemate.gonow.global.response.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
public class PlaceController {
    private final PlaceService placeService;

    // 장소 목록 조회 (type 필터링)
    @GetMapping
    public ApiResult<List<PlaceResponse>> getPlaces(@AuthenticationPrincipal UserDetails userDetails,
                                                    @RequestParam(name = "place_type", required = false) PlaceType placeType) {
        Long memberId = Long.parseLong(userDetails.getUsername());

        List<PlaceResponse> responses = placeService.getPlaces(memberId, placeType);

        return ApiResult.success("장소 목록 조회 완료", responses);
    }

    // 장소 저장 (동일 주소이면 updatedAt만 갱신)
    @PostMapping
    public ApiResult<PlaceResponse> upsertPlace(@AuthenticationPrincipal UserDetails userDetails,
                                                @Valid @RequestBody PlaceUpsertRequest request) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        PlaceResponse response = placeService.upsertPlace(memberId, request);

        return ApiResult.success("장소 저장 완료", response);
    }

    // 장소 삭제
    @DeleteMapping("/{placeId}")
    public ApiResult<Void> deletePlace(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable Long placeId) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        placeService.deletePlace(placeId, memberId);

        return ApiResult.success("장소 삭제 완료");
    }
}
