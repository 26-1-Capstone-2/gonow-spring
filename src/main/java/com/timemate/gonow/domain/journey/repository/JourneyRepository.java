package com.timemate.gonow.domain.journey.repository;

import com.timemate.gonow.domain.journey.constant.JourneyType;
import com.timemate.gonow.domain.journey.entity.Journey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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
}
