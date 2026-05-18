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

    // 날짜별 조회
    @Query("SELECT j FROM Journey j WHERE j.member.id = :memberId AND j.planDate = :planDate ORDER BY j.departureAlarmTime")
    List<Journey> findAllByPlanDate(@Param("memberId") Long memberId, @Param("planDate") LocalDate planDate);

    // 스케줄러: 당일 SCHEDULED → READY 벌크 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Journey j SET j.journeyStatus = 'READY' WHERE j.planDate = :today AND j.journeyStatus = 'SCHEDULED'")
    int bulkUpdateToReady(@Param("today") LocalDate today);

    // 스케줄러: ID 목록 → ARRIVED 벌크 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Journey j SET j.journeyStatus = 'ARRIVED' WHERE j.id IN :ids")
    int bulkUpdateToArrived(@Param("ids") List<Long> ids);

    // 스케줄러: NEARDEST + 해당 날짜 + targetTime 초과 → ID 목록 반환
    // NEARDEST 진입 시점에 이미 100m 이내 보장됨 → Haversine 재계산 불필요
    // planDate: 자정~새벽 4시 사이는 어제 날짜로 보정 (막차 모드 자정 넘김 커버)
    @Query("SELECT j.id FROM Journey j WHERE j.journeyStatus = 'NEARDEST' AND j.planDate = :planDate AND j.targetTime < :now")
    List<Long> findIdsNeardestOverdue(@Param("planDate") LocalDate planDate, @Param("now") LocalDateTime now);
}
