package com.timemate.gonow.domain.place.service;

import com.timemate.gonow.domain.common.Location;
import com.timemate.gonow.domain.common.Point;
import com.timemate.gonow.domain.member.entity.Member;
import com.timemate.gonow.domain.member.repository.MemberRepository;
import com.timemate.gonow.domain.place.constant.PlaceType;
import com.timemate.gonow.domain.place.dto.PlaceResponse;
import com.timemate.gonow.domain.place.dto.PlaceUpsertRequest;
import com.timemate.gonow.domain.place.entity.Place;
import com.timemate.gonow.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlaceService {
    private final PlaceRepository placeRepository;
    private final MemberRepository memberRepository;

    // 목록 조회 (type 필터링)
    public List<PlaceResponse> getPlaces(Long memberId, PlaceType placeType) {
        List<Place> places = (placeType == null)
                ? placeRepository.findAllByMember(memberId)
                : placeRepository.findAllByMemberAndType(memberId, placeType);

        return places.stream()
                .map(PlaceResponse::from)
                .toList();
    }

    // Upsert용 조회 메서드 (동일 주소 중복 확인용)
    @Transactional
    public PlaceResponse upsertPlace(Long memberId, PlaceUpsertRequest request) {
        // 1. 동일한 유저가 동일한 주소를 이미 저장했는지 확인 (Optional 활용)
        return placeRepository.findByMemberAndAddress(memberId, request.address())
                .map(existingPlace -> {
                    // [Case 1] 이미 존재한다면: 시간만 갱신 (touch)
                    existingPlace.touch();
                    // JPA의 변경 감지(Dirty Checking)에 의해 자동으로 업데이트 쿼리가 나감
                    return PlaceResponse.from(existingPlace);
                })
                .orElseGet(() -> {
                    // [Case 2] 존재하지 않는다면: 새로 생성 및 저장
                    Member member = memberRepository.findById(memberId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

                    Place newPlace = Place.builder()
                            .member(member)
                            .name(request.name())
                            .placeType(request.placeType())
                            .location(new Location(request.address(), new Point(request.lat(), request.lng())))
                            .build();

                    return PlaceResponse.from(placeRepository.save(newPlace));
                });
    }

    // 검색 장소 삭제
    @Transactional
    public void deletePlace(Long placeId, Long memberId) {
        Place place = placeRepository.findByIdAndMemberId(placeId, memberId).orElseThrow(
                () -> new IllegalArgumentException("장소를 찾을 수 없거나 삭제 권한이 없습니다."));

        placeRepository.delete(place);
    }
}
