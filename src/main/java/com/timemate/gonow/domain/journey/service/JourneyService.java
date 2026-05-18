package com.timemate.gonow.domain.journey.service;

import com.timemate.gonow.domain.common.Location;
import com.timemate.gonow.domain.common.Point;
import com.timemate.gonow.domain.common.constant.TransportType;
import com.timemate.gonow.domain.journey.constant.JourneyStatus;
import com.timemate.gonow.domain.journey.constant.JourneyType;
import com.timemate.gonow.domain.common.constant.GeoConstants;
import com.timemate.gonow.domain.common.dto.LocationUpdateRequest;
import com.timemate.gonow.domain.journey.dto.LocationUpdateResponse;
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
import com.timemate.gonow.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class JourneyService {
    private final JourneyRepository journeyRepository;
    private final MemberRepository memberRepository;

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
        // TODO: 플라스크랑 연동하면, DEPARTING도 고려하기
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

        switch (journey.getJourneyStatus()) {
            case READY -> {
                double distToDest = GeoUtils.calculateDistance(
                        newLat.doubleValue(), newLng.doubleValue(),
                        journey.getDestination().point().lat().doubleValue(),
                        journey.getDestination().point().lng().doubleValue()
                );

                if (distToDest < GeoConstants.ARRIVAL_THRESHOLD_METERS) {
                    // 1. NEARDEST 우선: 목적지 100m 이내 → 도착 확인 대기
                    journey.updateCurrentPoint(newPoint);
                    journey.updateStatus(JourneyStatus.NEARDEST);
                //} else if (P >= Q) {
                //    // 2. TODO: DEPARTING 전환 + 앵커 확정 (플라스크 연동 후 departureAlarmTime 채워지면 활성화)
                } else if (journey.getCurrentPoint() == null) {
                    // 3. 최초 좌표 수신 → 앵커 저장
                    journey.updateCurrentPoint(newPoint);
                } else {
                    // 4. 앵커 존재 → 500m 이탈 시 앵커 갱신
                    double distFromAnchor = GeoUtils.calculateDistance(
                            newLat.doubleValue(), newLng.doubleValue(),
                            journey.getCurrentPoint().lat().doubleValue(),
                            journey.getCurrentPoint().lng().doubleValue()
                    );
                    if (distFromAnchor >= GeoConstants.RECOMPUTE_THRESHOLD_METERS) {
                        journey.updateCurrentPoint(newPoint);
                        // TODO: 플라스크 호출 → ETA 계산 → departureAlarmTime 재계산
                    }
                    // 500m 미만이면 좌표 갱신 안 함 (앵커 보존)
                }
            }
            case DEPARTING -> {
                double distToDest = GeoUtils.calculateDistance(
                        newLat.doubleValue(), newLng.doubleValue(),
                        journey.getDestination().point().lat().doubleValue(),
                        journey.getDestination().point().lng().doubleValue()
                );

                if (distToDest < GeoConstants.ARRIVAL_THRESHOLD_METERS) {
                    // 출발 알람 후 이동 중 100m 진입 → 바로 ARRIVED (도착 확인 불필요)
                    journey.updateCurrentPoint(newPoint);
                    journey.updateStatus(JourneyStatus.ARRIVED);
                }
                else {
                    double distFromAnchor = GeoUtils.calculateDistance(
                            newLat.doubleValue(), newLng.doubleValue(),
                            journey.getCurrentPoint().lat().doubleValue(),
                            journey.getCurrentPoint().lng().doubleValue()
                    );
                    if (distFromAnchor >= GeoConstants.DEPARTURE_THRESHOLD_METERS) {
                        // 앵커로부터 300m 이탈 → MOVING
                        journey.updateCurrentPoint(newPoint);
                        journey.updateStatus(JourneyStatus.MOVING);
                    }
                    // 300m 미만이면 DEPARTING 유지, 좌표 갱신 안 함 (앵커 보존)
                }
            }
            case MOVING -> {
                double distToDest = GeoUtils.calculateDistance(
                        newLat.doubleValue(), newLng.doubleValue(),
                        journey.getDestination().point().lat().doubleValue(),
                        journey.getDestination().point().lng().doubleValue()
                );

                journey.updateCurrentPoint(newPoint);
                // TODO: 플라스크 호출 → ETA 재계산 → estimatedArrival 갱신 (대시보드용)

                if (distToDest < GeoConstants.ARRIVAL_THRESHOLD_METERS) {
                    // 이동 중 목적지 100m 이내 진입 → 바로 ARRIVED (도착 확인 불필요)
                    journey.updateStatus(JourneyStatus.ARRIVED);
                }
            }
            case NEARDEST -> {
                double distToDest = GeoUtils.calculateDistance(
                        newLat.doubleValue(), newLng.doubleValue(),
                        journey.getDestination().point().lat().doubleValue(),
                        journey.getDestination().point().lng().doubleValue()
                );

                if (distToDest >= GeoConstants.ARRIVAL_THRESHOLD_METERS) {
                    // 100m 벗어남 (스쳐지나간 케이스)
                    journey.updateCurrentPoint(newPoint);
                    //if (P >= Q) {
                    // TODO: DEPARTING 전환 (플라스크 연동 후 departureAlarmTime 채워지면 활성화)
                    //} else {
                    journey.updateStatus(JourneyStatus.READY);
                    //}
                }
                // 100m 이내면 NEARDEST 유지, 좌표 갱신 안 함
                // - 사용자 확인 → /arrive API → ARRIVED
                // - 미확인 + P >= Q → 앱이 단계별 알람 처리 (서버 상태 변경 없음)
                // - 미확인 + targetTime 초과 → ArrivedTransitionScheduler가 자동 ARRIVED
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

}
