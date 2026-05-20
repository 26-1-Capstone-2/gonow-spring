package com.timemate.gonow.domain.journey.repository;

import com.timemate.gonow.domain.journey.constant.JourneyType;
import com.timemate.gonow.domain.journey.entity.Journey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JourneyRepository extends JpaRepository<Journey, Long> {
    Optional<Journey> findByIdAndMemberId(Long journeyId, Long memberId);

    // 타입별 전체 조회
    @Query("SELECT j FROM Journey j WHERE j.member.id = :memberId AND j.journeyType = :type ORDER BY j.departureAlarmTime")
    List<Journey> findAllByJourneyType(@Param("memberId") Long memberId, @Param("type") JourneyType type);

    // 날짜별 조회 (일반 여정 + 반복 여정 포함)
    @Query(value = "SELECT * FROM journey WHERE member_id = :memberId AND (plan_date = :planDate OR (repeat_days & :dateBit) > 0) ORDER BY departure_alarm_time", nativeQuery = true)
    List<Journey> findAllByPlanDate(@Param("memberId") Long memberId, @Param("planDate") LocalDate planDate, @Param("dateBit") int dateBit);

    // 스케줄러: 당일 SCHEDULED/ARRIVED → READY 벌크 업데이트 (반복 여정은 ARRIVED도 포함)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE journey SET status = 'READY' WHERE (plan_date = :today OR (repeat_days & :todayBit) > 0) AND plan_date <= :today AND status IN ('SCHEDULED', 'ARRIVED')", nativeQuery = true)
    int bulkUpdateToReady(@Param("today") LocalDate today, @Param("todayBit") int todayBit);

    // 스케줄러: ID 목록 → ARRIVED 벌크 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Journey j SET j.journeyStatus = 'ARRIVED' WHERE j.id IN :ids")
    int bulkUpdateToArrived(@Param("ids") List<Long> ids);

    // 스케줄러: NEARDEST + 해당 날짜 + targetTime 초과 → ID 목록 반환
    // NEARDEST 진입 시점에 이미 100m 이내 보장됨 → Haversine 재계산 불필요
    // planDate: 자정~새벽 4시 사이는 어제 날짜로 보정 (막차 모드 자정 넘김 커버)
    @Query(value = "SELECT journey_id FROM journey WHERE status = 'NEARDEST' AND (plan_date = :planDate OR (repeat_days & :planDateBit) > 0) AND plan_date <= :planDate AND target_time < :now", nativeQuery = true)
    List<Long> findIdsNeardestOverdue(@Param("planDate") LocalDate planDate, @Param("now") LocalDateTime now, @Param("planDateBit") int planDateBit);
}
