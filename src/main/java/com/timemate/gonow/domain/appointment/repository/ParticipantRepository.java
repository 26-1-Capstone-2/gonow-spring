package com.timemate.gonow.domain.appointment.repository;

import com.timemate.gonow.domain.appointment.constant.ParticipantStatus;
import com.timemate.gonow.domain.appointment.entity.Participant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.timemate.gonow.domain.appointment.dto.TokenAppointmentIdsProjection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    // 당일 SCHEDULED → READY 벌크 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Participant p SET p.participantStatus = :ready WHERE p.appointment.planDate = :today AND p.participantStatus = :scheduled")
    int bulkUpdateToReadyInternal(@Param("ready") ParticipantStatus ready, @Param("today") LocalDate today, @Param("scheduled") ParticipantStatus scheduled);

    // 외부 호출용 래퍼 — Enum 파라미터 조립을 캡슐화
    default int bulkUpdateToReady(LocalDate today) {
        return bulkUpdateToReadyInternal(ParticipantStatus.READY, today, ParticipantStatus.SCHEDULED);
    }

    // 전원 도착 여부 확인 (ARRIVED가 아닌 참가자 수 조회)
    @Query("SELECT COUNT(p) FROM Participant p WHERE p.appointment.id = :appointmentId AND p.participantStatus != :status")
    long countNotArrivedByAppointmentIdInternal(@Param("appointmentId") Long appointmentId, @Param("status") ParticipantStatus status);

    // 외부 호출용 래퍼 — ARRIVED 고정값 캡슐화
    default long countNotArrivedByAppointmentId(Long appointmentId) {
        return countNotArrivedByAppointmentIdInternal(appointmentId, ParticipantStatus.ARRIVED);
    }

    // 나를 제외한 다른 참가자들의 FCM 토큰 조회 (null 토큰 제외)
    @Query("SELECT p.member.fcmToken FROM Participant p WHERE p.appointment.id = :appointmentId AND p.member.id != :excludeMemberId AND p.member.fcmToken IS NOT NULL")
    List<String> findFcmTokensByAppointmentIdExcluding(@Param("appointmentId") Long appointmentId, @Param("excludeMemberId") Long excludeMemberId);

    // NEARDEST 상태 + targetTime 초과 참가자가 속한 약속 ID 목록 조회 (중복 제거)
    @Query("SELECT DISTINCT p.appointment.id FROM Participant p WHERE p.participantStatus = :status AND p.appointment.planDate = :today AND p.appointment.targetTime < :now")
    List<Long> findAppointmentIdsWithOverdueParticipantsInternal(@Param("status") ParticipantStatus status, @Param("today") LocalDate today, @Param("now") LocalDateTime now);

    // 외부 호출용 래퍼 — Enum 파라미터 조립을 캡슐화
    default List<Long> findAppointmentIdsWithOverdueParticipants(LocalDate today, LocalDateTime now) {
        return findAppointmentIdsWithOverdueParticipantsInternal(ParticipantStatus.NEARDEST, today, now);
    }

    // 약속 ID 목록 기준으로 NEARDEST + targetTime 초과 참가자 → ARRIVED 벌크 전환
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Participant p SET p.participantStatus = :arrived WHERE p.appointment.id IN :appIds AND p.participantStatus = :neardest AND p.appointment.targetTime < :now")
    int bulkUpdateToArrivedByAppointmentIdsInternal(@Param("arrived") ParticipantStatus arrived, @Param("appIds") List<Long> appIds, @Param("neardest") ParticipantStatus neardest, @Param("now") LocalDateTime now);

    // 외부 호출용 래퍼 — Enum 파라미터 조립을 캡슐화
    default int bulkUpdateToArrivedByAppointmentIds(List<Long> appIds, LocalDateTime now) {
        return bulkUpdateToArrivedByAppointmentIdsInternal(ParticipantStatus.ARRIVED, appIds, ParticipantStatus.NEARDEST, now);
    }

    // 스케줄러: 당일 READY 전환 대상 참가자의 (fcmToken, appointmentIds 콤마 문자열) 조회 (null 토큰 제외)
    // GROUP_CONCAT으로 DB에서 토큰별 그룹화
    @Query(value = "SELECT m.fcm_token AS fcmToken, GROUP_CONCAT(p.appointment_id) AS appointmentIds FROM participant p JOIN member m ON p.member_id = m.member_id JOIN appointment a ON p.appointment_id = a.appointment_id WHERE a.plan_date = :today AND p.status = :status AND m.fcm_token IS NOT NULL GROUP BY m.fcm_token", nativeQuery = true)
    List<TokenAppointmentIdsProjection> findTokenAppointmentIdPairsForReadyTransitionInternal(@Param("today") LocalDate today, @Param("status") String status);

    // 외부 호출용 래퍼 — 반환: Map<fcmToken, appointmentId 콤마 문자열>
    default Map<String, String> findTokenToAppointmentIdsForReadyTransition(LocalDate today) {
        return findTokenAppointmentIdPairsForReadyTransitionInternal(today, ParticipantStatus.SCHEDULED.name())
                .stream()
                .collect(Collectors.toMap(
                        TokenAppointmentIdsProjection::getFcmToken,
                        TokenAppointmentIdsProjection::getAppointmentIds
                ));
    }
}
