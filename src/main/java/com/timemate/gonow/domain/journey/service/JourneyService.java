package com.timemate.gonow.domain.journey.service;

import com.timemate.gonow.domain.common.Location;
import com.timemate.gonow.domain.common.Point;
import com.timemate.gonow.domain.common.constant.TransportType;
import com.timemate.gonow.domain.journey.constant.JourneyStatus;
import com.timemate.gonow.domain.journey.constant.JourneyType;
import com.timemate.gonow.domain.common.constant.GeoConstants;
import com.timemate.gonow.domain.common.dto.LocationUpdateRequest;
import com.timemate.gonow.domain.journey.dto.LocationUpdateResponse;
import com.timemate.gonow.global.client.FlaskClient;
import com.timemate.gonow.global.client.dto.FlaskJourneyRequest;
import com.timemate.gonow.global.client.dto.FlaskResponse;
import com.timemate.gonow.global.util.GeoUtils;

import com.timemate.gonow.domain.journey.dto.HomeJourneyCreateRequest;
import com.timemate.gonow.domain.journey.dto.HomeJourneyUpdateRequest;
import com.timemate.gonow.domain.journey.dto.JourneyActiveUpdateRequest;
import com.timemate.gonow.domain.journey.dto.JourneyResponse;
import com.timemate.gonow.domain.journey.dto.JourneySaveResponse;
import com.timemate.gonow.domain.journey.dto.PersonalJourneyCreateRequest;
import com.timemate.gonow.domain.journey.dto.PersonalJourneyUpdateRequest;
import com.timemate.gonow.domain.journey.entity.Journey;
import com.timemate.gonow.domain.journey.repository.JourneyRepository;
import com.timemate.gonow.domain.member.entity.Member;
import com.timemate.gonow.domain.member.entity.MemberSetting;
import com.timemate.gonow.domain.member.repository.MemberRepository;
import com.timemate.gonow.domain.member.repository.MemberSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class JourneyService {
    private final JourneyRepository journeyRepository;
    private final MemberRepository memberRepository;
    private final MemberSettingRepository memberSettingRepository;

    private final FlaskClient flaskClient;

    // 개인 여정 생성
    @Transactional
    public JourneySaveResponse createPersonalJourney(Long memberId, PersonalJourneyCreateRequest request) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Location destination = new Location(
                request.destName(),
                request.destAddress(),
                new Point(request.destLat(), request.destLng())
        );

        Journey journey = Journey.builder()
                .member(member)
                .title(request.title())
                .journeyType(JourneyType.PERSONAL)
                .planDate(request.planDate())
                .destination(destination)
                .transportType(request.transportType())
                .isLastMode(false)
                .targetTime(request.targetTime())
                .repeatDays(request.repeatDays())
                .journeyStatus(resolveInitialStatus(request.planDate()))
                .build();

        return JourneySaveResponse.from(journeyRepository.save(journey));
    }

    // 개인 여정 수정
    @Transactional
    public JourneySaveResponse updatePersonalJourney(Long memberId, Long journeyId, PersonalJourneyUpdateRequest request) {
        Journey journey = journeyRepository.findByIdAndMemberId(journeyId, memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 여정이거나 수정 권한이 없습니다."));

        Location destination = new Location(
                request.destName(),
                request.destAddress(),
                new Point(request.destLat(), request.destLng())
        );

        journey.updatePersonal(request.title(), request.planDate(), request.targetTime(), destination, request.transportType(), request.repeatDays(), resolveInitialStatus(request.planDate()));
        return JourneySaveResponse.from(journey);
    }

    // 귀가 여정 생성 (막차/데드라인 공통)
    @Transactional
    public JourneySaveResponse createHomeJourney(Long memberId, HomeJourneyCreateRequest request) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        TransportType transportType = resolveTransportType(request.isLastMode(), request.transportType());

        Location destination = new Location(
                request.destName(),
                request.destAddress(),
                new Point(request.destLat(), request.destLng())
        );

        Journey journey = Journey.builder()
                .member(member)
                .title(request.title())
                .journeyType(JourneyType.HOME)
                .planDate(request.planDate())
                .destination(destination)
                .transportType(transportType)
                .isLastMode(request.isLastMode())
                .targetTime(request.targetTime())
                .repeatDays(request.repeatDays())
                .journeyStatus(resolveInitialStatus(request.planDate()))
                .build();

        return JourneySaveResponse.from(journeyRepository.save(journey));
    }

    // 귀가 여정 수정 (막차/데드라인 공통)
    @Transactional
    public JourneySaveResponse updateHomeJourney(Long memberId, Long journeyId, HomeJourneyUpdateRequest request) {
        Journey journey = journeyRepository.findByIdAndMemberId(journeyId, memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 여정이거나 수정 권한이 없습니다."));

        TransportType transportType = resolveTransportType(request.isLastMode(), request.transportType());

        Location destination = new Location(
                request.destName(),
                request.destAddress(),
                new Point(request.destLat(), request.destLng())
        );

        journey.updateHome(request.title(), request.isLastMode(), request.planDate(), request.targetTime(), destination, transportType, request.repeatDays(), resolveInitialStatus(request.planDate()));
        return JourneySaveResponse.from(journey);
    }

    // 막차 모드면 TRANSIT 강제, 데드라인 모드면 클라이언트 값 사용 (null이면 예외)
    private TransportType resolveTransportType(boolean isLastMode, TransportType transportType) {
        return isLastMode ? TransportType.TRANSIT :
                Optional.ofNullable(transportType)
                .orElseThrow(() -> new IllegalArgumentException("데드라인 모드에서는 이동 수단 필수"));
    }


    // 당일 생성이면 READY, 그 외엔 SCHEDULED
    private JourneyStatus resolveInitialStatus(LocalDate planDate) {
        return planDate.isEqual(LocalDate.now()) ? JourneyStatus.READY : JourneyStatus.SCHEDULED;
    }

    // 여정 조회 (개인/귀가 공통)
    public JourneyResponse getJourney(Long memberId, Long journeyId) {
        Journey journey = journeyRepository.findByIdAndMemberId(journeyId, memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 여정이거나 조회 권한이 없습니다."));

        return JourneyResponse.from(journey);
    }



    // 알람 스위치 ON/OFF (개인/귀가 공통)
    @Transactional
    public void updateActive(Long memberId, Long journeyId, JourneyActiveUpdateRequest request) {
        Journey journey = journeyRepository.findByIdAndMemberId(journeyId, memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 여정이거나 수정 권한이 없습니다."));

        journey.updateActive(request.isActive());
    }

    // 여정 삭제 (개인/귀가 공통)
    @Transactional
    public void deleteJourney(Long memberId, Long journeyId) {
        Journey journey = journeyRepository.findByIdAndMemberId(journeyId, memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 여정이거나 삭제 권한이 없습니다."));

        journeyRepository.delete(journey);
    }

    // GPS 좌표 수신 + 상태 전이
    @Transactional
    public LocationUpdateResponse updateLocation(Long memberId, Long journeyId, LocationUpdateRequest request) {
        Journey journey = journeyRepository.findByIdAndMemberId(journeyId, memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 여정이거나 권한이 없습니다."));

        BigDecimal newLat = request.lat();
        BigDecimal newLng = request.lng();
        Point newPoint = new Point(newLat, newLng);

        // 목적지까지의 거리
        double distToDest = GeoUtils.calculateDistance(
                newLat.doubleValue(), newLng.doubleValue(),
                journey.getDestination().point().lat().doubleValue(),
                journey.getDestination().point().lng().doubleValue()
        );
        // 앵커로부터의 거리
        double distFromAnchor = journey.getCurrentPoint() == null ? -1 :
                GeoUtils.calculateDistance(
                        newLat.doubleValue(), newLng.doubleValue(),
                        journey.getCurrentPoint().lat().doubleValue(),
                        journey.getCurrentPoint().lng().doubleValue()
                );

        boolean isNearDest = (distToDest < GeoConstants.ARRIVAL_THRESHOLD_METERS); // 목적지 100m 이내?

        switch (journey.getJourneyStatus()) {
            case READY -> {
                boolean isFirstReceive = (distFromAnchor == -1); // 첫 좌표 수신?
                boolean isOutOfAnchor = (distFromAnchor >= GeoConstants.RECOMPUTE_THRESHOLD_METERS); // 앵커 500m 이탈?

                // 앵커 갱신 및 Q 계산이 필요한 조건 통합 (isPastAlarmTime 계산 전에 반드시 실행)
                if (isNearDest || isFirstReceive || isOutOfAnchor) {
                    journey.updateCurrentPoint(newPoint);
                    callFlaskAndUpdate(journey, newPoint, memberId);
                }

                boolean isPastAlarmTime = !LocalDateTime.now().isBefore(journey.getDepartureAlarmTime()); // P >= Q?

                if (isNearDest) {
                    // 100m 이내 -> NEARDEST
                    journey.updateStatus(JourneyStatus.NEARDEST);
                } else if (isPastAlarmTime) {
                    // P >= Q → DEPARTING
                    journey.updateStatus(JourneyStatus.DEPARTING);
                }
                // P < Q → READY 유지
            }
            case NEARDEST -> {
                boolean isPastAlarmTime = !LocalDateTime.now().isBefore(journey.getDepartureAlarmTime()); // P >= Q?

                if (!isNearDest && !isPastAlarmTime) {
                    // 100m 벗어남 + P < Q → READY 복귀 (스쳐지나간 케이스)
                    journey.updateCurrentPoint(newPoint);
                    journey.updateStatus(JourneyStatus.READY);
                    callFlaskAndUpdate(journey, newPoint, memberId); // Q 재계산
                }
                // P >= Q면 100m 벗어나도 NEARDEST 고정 (알람 울리는 중)
                // - 사용자 확인 → /arrive API → ARRIVED
                // - targetTime 초과 → ArrivedTransitionScheduler가 자동 ARRIVED
            }
            case DEPARTING -> {
                boolean isDepartedFromAnchor = (distFromAnchor >= GeoConstants.DEPARTURE_THRESHOLD_METERS);

                if (isNearDest) {
                    // 100m 진입 → ARRIVED
                    journey.updateCurrentPoint(newPoint);
                    journey.updateStatus(JourneyStatus.ARRIVED);
                } else if (isDepartedFromAnchor) {
                    // 300m 이탈 → MOVING
                    journey.updateCurrentPoint(newPoint);
                    journey.updateStatus(JourneyStatus.MOVING);
                }
                // 300m 미만이면 DEPARTING 유지, 앵커 보존
            }
            case MOVING -> {
                journey.updateCurrentPoint(newPoint);
                if (isNearDest) {
                    // 100m 진입 → ARRIVED
                    journey.updateStatus(JourneyStatus.ARRIVED);
                }
            }
            default -> {
                // SCHEDULED, ARRIVED 상태에서는 무시
            }
        }

        return LocationUpdateResponse.from(journey);
    }

    // 도착 확인 (사용자가 확인 버튼 눌렀을 때 ARRIVED로 전환)
    @Transactional
    public void arrive(Long memberId, Long journeyId) {
        Journey journey = journeyRepository.findByIdAndMemberId(journeyId, memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 여정이거나 권한이 없습니다."));

        if (journey.getJourneyStatus() != JourneyStatus.NEARDEST) {
            throw new IllegalStateException("NEARDEST 상태에서만 도착 확인이 가능합니다.");
        }
        journey.updateStatus(JourneyStatus.ARRIVED);
    }

    // 플라스크 호출 → departureAlarmTime + estimatedArrival 저장
    private void callFlaskAndUpdate(Journey journey, Point currentPoint, Long memberId) {
        MemberSetting setting = memberSettingRepository.findByMemberId(memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 회원 설정입니다."));

        FlaskJourneyRequest request = new FlaskJourneyRequest(
                currentPoint.lat(),
                currentPoint.lng(),
                journey.getDestination().point().lat(),
                journey.getDestination().point().lng(),
                journey.getTransportType(),
                journey.getTargetTime(),
                journey.isLastMode(),
                setting.getPreparationTime()
        );

        FlaskResponse response = flaskClient.calculateJourneyAlarm(request);
        journey.updateAlarmInfo(response.departureAlarmTime(), response.estimatedArrival());
    }
}
