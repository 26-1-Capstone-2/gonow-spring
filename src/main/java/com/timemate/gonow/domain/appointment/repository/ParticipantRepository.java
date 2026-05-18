package com.timemate.gonow.domain.appointment.repository;

import com.timemate.gonow.domain.appointment.entity.Participant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    // 방장 권한 확인 + host.getAppointment() 사용 → appointment fetch join
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

    // appointment.targetTime, appointment.destination 사용 → appointment fetch join
    @EntityGraph(attributePaths = {"appointment"})
    Optional<Participant> findWithAppointmentByAppointmentIdAndMemberId(Long appointmentId, Long memberId);

    // 중복 참여 확인
    boolean existsByAppointmentIdAndMemberId(Long appointmentId, Long memberId);

    // 탈퇴/추방 시 요청자 + 대상자 한 번에 조회
    List<Participant> findAllByAppointmentIdAndMemberIdIn(Long appointmentId, List<Long> memberIds);

    // 약속 전체 삭제 시 모든 참여자 벌크 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Participant p WHERE p.appointment.id = :appointmentId")
    void bulkDeleteByAppointmentId(@Param("appointmentId") Long appointmentId);

    // 약속별 참가자 수 조회 (단건)
    int countByAppointmentId(Long appointmentId);

    // 약속 조회용 참가자 전체 조회 (member + appointment fetch join)
    @EntityGraph(attributePaths = {"member", "appointment"})
    List<Participant> findAllByAppointmentId(Long appointmentId);

    // 내가 참여한 약속 전체 조회 (appointment.destination, appointment.targetTime 사용 → appointment fetch join)
    @Query("SELECT p FROM Participant p JOIN FETCH p.appointment WHERE p.member.id = :memberId ORDER BY p.departureAlarmTime")
    List<Participant> findAllByMemberId(@Param("memberId") Long memberId);

    // 내가 참여한 약속 날짜별 조회 (appointment.destination, appointment.targetTime 사용 → appointment fetch join)
    @Query("SELECT p FROM Participant p JOIN FETCH p.appointment a WHERE p.member.id = :memberId AND a.planDate = :planDate ORDER BY p.departureAlarmTime")
    List<Participant> findAllByMemberIdAndPlanDate(@Param("memberId") Long memberId, @Param("planDate") LocalDate planDate);

    // 스케줄러: 당일 SCHEDULED → READY 벌크 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Participant p SET p.participantStatus = 'READY' WHERE p.appointment.planDate = :today AND p.participantStatus = 'SCHEDULED'")
    int bulkUpdateToReady(@Param("today") LocalDate today);

    // 전원 도착 여부 확인 (ARRIVED가 아닌 참가자 수 조회)
    @Query("SELECT COUNT(p) FROM Participant p WHERE p.appointment.id = :appointmentId AND p.participantStatus != 'ARRIVED'")
    long countNotArrivedByAppointmentId(@Param("appointmentId") Long appointmentId);

    // 스케줄러: NEARDEST 초과 참가자가 속한 약속 ID 목록 (중복 제거)
    @Query("SELECT DISTINCT p.appointment.id FROM Participant p WHERE p.participantStatus = 'NEARDEST' AND p.appointment.planDate = :today AND p.appointment.targetTime < :now")
    List<Long> findAppointmentIdsWithOverdueParticipants(@Param("today") LocalDate today, @Param("now") LocalDateTime now);

    // 스케줄러: 약속 ID 목록 기준으로 NEARDEST + targetTime 초과 참가자 → ARRIVED 벌크 전환
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Participant p SET p.participantStatus = 'ARRIVED' WHERE p.appointment.id IN :appIds AND p.participantStatus = 'NEARDEST' AND p.appointment.targetTime < :now")
    int bulkUpdateToArrivedByAppointmentIds(@Param("appIds") List<Long> appIds, @Param("now") LocalDateTime now);
}
