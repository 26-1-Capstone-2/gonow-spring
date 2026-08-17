package com.timemate.gonow.domain.journey.repository;

import com.timemate.gonow.domain.journey.constant.JourneyStatus;
import com.timemate.gonow.domain.journey.constant.JourneyType;
import com.timemate.gonow.domain.journey.entity.Journey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.timemate.gonow.domain.journey.dto.TokenJourneyIdsProjection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface JourneyRepository extends JpaRepository<Journey, Long> {
    Optional<Journey> findByIdAndMemberId(Long journeyId, Long memberId);

    // 타입별 전체 조회
    @Query("SELECT j FROM Journey j WHERE j.member.id = :memberId AND j.journeyType = :type ORDER BY j.departureAlarmTime")
    List<Journey> findAllByJourneyType(@Param("memberId") Long memberId, @Param("type") JourneyType type);

    // 날짜별 조회 (일반 여정 + 반복 여정 포함)
    @Query(value = "SELECT * FROM journey WHERE member_id = :memberId AND (plan_date = :planDate OR (repeat_days & :dateBit) > 0) ORDER BY departure_alarm_time", nativeQuery = true)
    List<Journey> findAllByPlanDate(@Param("memberId") Long memberId, @Param("planDate") LocalDate planDate, @Param("dateBit") int dateBit);

    // 스케줄러: 당일 SCHEDULED/ARRIVED → READY 벌크 업데이트 (반복 여정은 ARRIVED도 포함)
    // 비트마스크 연산으로 네이티브 쿼리 필수 — bulkUpdateToReady(LocalDate, int) 래퍼를 통해 호출할 것
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE journey SET status = :readyStatus WHERE (plan_date = :today OR (repeat_days & :todayBit) > 0) AND plan_date <= :today AND status IN (:scheduledStatus, :arrivedStatus)", nativeQuery = true)
    int bulkUpdateToReadyInternal(@Param("readyStatus") String readyStatus, @Param("today") LocalDate today, @Param("todayBit") int todayBit, @Param("scheduledStatus") String scheduledStatus, @Param("arrivedStatus") String arrivedStatus);

    // 외부 호출용 래퍼 — Enum.name() 조립을 캡슐화
    default int bulkUpdateToReady(LocalDate today, int todayBit) {
        return bulkUpdateToReadyInternal(
                JourneyStatus.READY.name(), today, todayBit,
                JourneyStatus.SCHEDULED.name(), JourneyStatus.ARRIVED.name());
    }

    // 반복 여정의 어제 앵커/출발시각이 남아있지 않도록 리셋 (bug44, 2026-08-17)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE journey SET current_lat = NULL, current_lng = NULL, departure_alarm_time = NULL WHERE (plan_date = :today OR (repeat_days & :todayBit) > 0) AND plan_date <= :today AND status IN (:scheduledStatus, :arrivedStatus)", nativeQuery = true)
    int bulkResetAnchorAndAlarmInternal(@Param("today") LocalDate today, @Param("todayBit") int todayBit, @Param("scheduledStatus") String scheduledStatus, @Param("arrivedStatus") String arrivedStatus);

    // bulkUpdateToReady 직전에 호출 — 상태가 READY로 바뀌기 전에 리셋해야 WHERE 조건이 유효함
    default int bulkResetAnchorAndAlarmForReadyTransition(LocalDate today, int todayBit) {
        return bulkResetAnchorAndAlarmInternal(today, todayBit,
                JourneyStatus.SCHEDULED.name(), JourneyStatus.ARRIVED.name());
    }

    // 스케줄러: ID 목록 → ARRIVED 벌크 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Journey j SET j.journeyStatus = :arrived WHERE j.id IN :ids")
    int bulkUpdateToArrivedInternal(@Param("arrived") JourneyStatus arrived, @Param("ids") List<Long> ids);

    // 외부 호출용 래퍼 — Enum 파라미터 조립을 캡슐화
    default int bulkUpdateToArrived(List<Long> ids) {
        return bulkUpdateToArrivedInternal(JourneyStatus.ARRIVED, ids);
    }

    // 스케줄러: READY + departureAlarmTime 도달 → DEPARTING 벌크 전환.
    // P>=Q는 위치와 무관해 지오펜스로 못 잡으므로 서버가 시간으로 감시(좌표는 READY 앵커 재사용).
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Journey j SET j.journeyStatus = :departing WHERE j.journeyStatus = :ready AND j.departureAlarmTime IS NOT NULL AND j.departureAlarmTime <= :now")
    int bulkUpdateToDepartingInternal(@Param("departing") JourneyStatus departing, @Param("ready") JourneyStatus ready, @Param("now") LocalDateTime now);

    default int bulkUpdateToDeparting(LocalDateTime now) {
        return bulkUpdateToDepartingInternal(JourneyStatus.DEPARTING, JourneyStatus.READY, now);
    }

    // 스케줄러: READY→DEPARTING 전환 대상의 (fcmToken, journeyIds) 조회 — 벌크 업데이트 전에 호출
    @Query(value = "SELECT m.fcm_token AS fcmToken, GROUP_CONCAT(j.journey_id) AS journeyIds FROM journey j JOIN member m ON j.member_id = m.member_id WHERE j.status = :readyStatus AND j.departure_alarm_time IS NOT NULL AND j.departure_alarm_time <= :now AND m.fcm_token IS NOT NULL GROUP BY m.fcm_token", nativeQuery = true)
    List<TokenJourneyIdsProjection> findTokenJourneyIdPairsForDepartingTransitionInternal(@Param("readyStatus") String readyStatus, @Param("now") LocalDateTime now);

    default Map<String, String> findTokenToJourneyIdsForDepartingTransition(LocalDateTime now) {
        return findTokenJourneyIdPairsForDepartingTransitionInternal(JourneyStatus.READY.name(), now)
                .stream()
                .collect(Collectors.toMap(
                        TokenJourneyIdsProjection::getFcmToken,
                        TokenJourneyIdsProjection::getJourneyIds
                ));
    }

    // 스케줄러: NEARDEST + 해당 날짜 + targetTime 초과 → ID 목록 반환 (즉시 ARRIVED)
    // NEARDEST 진입 시점에 이미 100m 이내 보장됨 → Haversine 재계산 불필요
    // planDate: 자정~새벽 4시 사이는 어제 날짜로 보정 (막차 모드 자정 넘김 커버)
    // 비트마스크 연산으로 네이티브 쿼리 필수 — findIdsNeardestOverdue(LocalDate, LocalDateTime, int) 래퍼를 통해 호출할 것
    @Query(value = "SELECT journey_id FROM journey WHERE status = :neardestStatus AND (plan_date = :planDate OR (repeat_days & :planDateBit) > 0) AND plan_date <= :planDate AND target_time < :now", nativeQuery = true)
    List<Long> findIdsNeardestOverdueInternal(@Param("neardestStatus") String neardestStatus, @Param("planDate") LocalDate planDate, @Param("now") LocalDateTime now, @Param("planDateBit") int planDateBit);

    // 외부 호출용 래퍼 — Enum.name() 조립을 캡슐화
    default List<Long> findIdsNeardestOverdue(LocalDate planDate, LocalDateTime now, int planDateBit) {
        return findIdsNeardestOverdueInternal(JourneyStatus.NEARDEST.name(), planDate, now, planDateBit);
    }

    // 스케줄러: READY/DEPARTING/MOVING + 해당 날짜 + targetTime+1시간 초과 → ID 목록 반환 (지각 1시간 후 자동 정리)
    // planDate: 자정~새벽 4시 사이는 어제 날짜로 보정 (막차 모드 자정 넘김 커버)
    // 비트마스크 연산으로 네이티브 쿼리 필수
    @Query(value = "SELECT journey_id FROM journey WHERE status IN (:readyStatus, :departingStatus, :movingStatus) AND (plan_date = :planDate OR (repeat_days & :planDateBit) > 0) AND plan_date <= :planDate AND target_time < :oneHourAgo", nativeQuery = true)
    List<Long> findIdsActiveOverdueInternal(@Param("readyStatus") String readyStatus, @Param("departingStatus") String departingStatus, @Param("movingStatus") String movingStatus, @Param("planDate") LocalDate planDate, @Param("oneHourAgo") LocalDateTime oneHourAgo, @Param("planDateBit") int planDateBit);

    // 외부 호출용 래퍼
    default List<Long> findIdsActiveOverdue(LocalDate planDate, LocalDateTime oneHourAgo, int planDateBit) {
        return findIdsActiveOverdueInternal(
                JourneyStatus.READY.name(), JourneyStatus.DEPARTING.name(), JourneyStatus.MOVING.name(),
                planDate, oneHourAgo, planDateBit);
    }

    // 스케줄러: 당일 READY 전환 대상 여정의 (fcmToken, journeyIds 콤마 문자열) 조회 (null 토큰 제외)
    // GROUP_CONCAT으로 DB에서 토큰별 그룹화 — 비트마스크 연산으로 네이티브 쿼리 필수
    @Query(value = "SELECT m.fcm_token AS fcmToken, GROUP_CONCAT(j.journey_id) AS journeyIds FROM journey j JOIN member m ON j.member_id = m.member_id WHERE (j.plan_date = :today OR (j.repeat_days & :todayBit) > 0) AND j.plan_date <= :today AND j.status IN (:scheduledStatus, :arrivedStatus) AND m.fcm_token IS NOT NULL GROUP BY m.fcm_token", nativeQuery = true)
    List<TokenJourneyIdsProjection> findTokenJourneyIdPairsForReadyTransitionInternal(@Param("today") LocalDate today, @Param("todayBit") int todayBit, @Param("scheduledStatus") String scheduledStatus, @Param("arrivedStatus") String arrivedStatus);

    // 외부 호출용 래퍼 — 반환: Map<fcmToken, journeyId 콤마 문자열>
    default Map<String, String> findTokenToJourneyIdsForReadyTransition(LocalDate today, int todayBit) {
        return findTokenJourneyIdPairsForReadyTransitionInternal(today, todayBit,
                JourneyStatus.SCHEDULED.name(), JourneyStatus.ARRIVED.name())
                .stream()
                .collect(Collectors.toMap(
                        TokenJourneyIdsProjection::getFcmToken,
                        TokenJourneyIdsProjection::getJourneyIds
                ));
    }

    // 스케줄러: NEARDEST + targetTime 초과 자동 ARRIVED 전환 대상의 (fcmToken, journeyIds 콤마 문자열) 조회
    // (NEARDEST가 지오펜싱 기반이 된 뒤로는 클라이언트가 이 자동 전환을 스스로 알 방법이 없어짐
    // → FCM으로 알려줘서 대신 트리거
    // findIdsNeardestOverdue와 동일 WHERE 조건, 벌크 업데이트 전에 조회해야 NEARDEST 상태 기준으로 잡힘
    // 비트마스크 연산으로 네이티브 쿼리 필수 — findTokenToJourneyIdsForNeardestOverdue(...) 래퍼를 통해 호출할 것
    @Query(value = "SELECT m.fcm_token AS fcmToken, GROUP_CONCAT(j.journey_id) AS journeyIds FROM journey j JOIN member m ON j.member_id = m.member_id WHERE j.status = :neardestStatus AND (j.plan_date = :planDate OR (j.repeat_days & :planDateBit) > 0) AND j.plan_date <= :planDate AND j.target_time < :now AND m.fcm_token IS NOT NULL GROUP BY m.fcm_token", nativeQuery = true)
    List<TokenJourneyIdsProjection> findTokenJourneyIdPairsForNeardestOverdueInternal(@Param("neardestStatus") String neardestStatus, @Param("planDate") LocalDate planDate, @Param("now") LocalDateTime now, @Param("planDateBit") int planDateBit);

    // 외부 호출용 래퍼 — 반환: Map<fcmToken, journeyId 콤마 문자열>
    default Map<String, String> findTokenToJourneyIdsForNeardestOverdue(LocalDate planDate, LocalDateTime now, int planDateBit) {
        return findTokenJourneyIdPairsForNeardestOverdueInternal(JourneyStatus.NEARDEST.name(), planDate, now, planDateBit)
                .stream()
                .collect(Collectors.toMap(
                        TokenJourneyIdsProjection::getFcmToken,
                        TokenJourneyIdsProjection::getJourneyIds
                ));
    }
}
