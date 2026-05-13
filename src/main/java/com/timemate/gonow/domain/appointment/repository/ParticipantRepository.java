package com.timemate.gonow.domain.appointment.repository;

import com.timemate.gonow.domain.appointment.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    // 방장 여부 확인 + Appointment fetch join (N+1 방지)
    @Query("""
        SELECT p FROM Participant p
        JOIN FETCH p.appointment                                                                                                            \s
        WHERE p.appointment.id = :appointmentId
          AND p.member.id = :memberId
          AND p.isHost = true
        """)
    Optional<Participant> findHostWithAppointment(@Param("appointmentId") Long appointmentId, @Param("memberId") Long memberId);

    // 본인 Participant 조회
    Optional<Participant> findByAppointmentIdAndMemberId(Long appointmentId, Long memberId);

    // 탈퇴/추방 시 요청자 + 대상자 한 번에 조회
    List<Participant> findAllByAppointmentIdAndMemberIdIn(Long appointmentId, List<Long> memberIds);

    // 약속 전체 삭제 시 모든 참여자 벌크 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Participant p WHERE p.appointment.id = :appointmentId")
    void bulkDeleteByAppointmentId(@Param("appointmentId") Long appointmentId);

    // 약속별 참가자 수 조회 (단건)
    int countByAppointmentId(Long appointmentId);

    // 내가 참여한 약속 전체 조회 (appointment fetch join, departure_alarm_time)
    @Query("SELECT p FROM Participant p JOIN FETCH p.appointment WHERE p.member.id = :memberId ORDER BY p.departureAlarmTime")
    List<Participant> findAllByMemberId(@Param("memberId") Long memberId);

    // 내가 참여한 약속 날짜별 조회 (appointment fetch join, departure_alarm_time)
    @Query("SELECT p FROM Participant p JOIN FETCH p.appointment a WHERE p.member.id = :memberId AND a.planDate = :planDate ORDER BY p.departureAlarmTime")
    List<Participant> findAllByMemberIdAndPlanDate(@Param("memberId") Long memberId, @Param("planDate") LocalDate planDate);
}
