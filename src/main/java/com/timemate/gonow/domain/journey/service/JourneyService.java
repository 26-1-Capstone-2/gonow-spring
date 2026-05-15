package com.timemate.gonow.domain.journey.service;

import com.timemate.gonow.domain.common.Location;
import com.timemate.gonow.domain.common.Point;
import com.timemate.gonow.domain.common.constant.TransportType;
import com.timemate.gonow.domain.journey.constant.JourneyStatus;
import com.timemate.gonow.domain.journey.constant.JourneyType;

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
}
