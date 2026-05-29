package com.timemate.gonow.domain.appointment.repository;

import com.timemate.gonow.domain.appointment.constant.AppointmentStatus;
import com.timemate.gonow.domain.appointment.constant.ParticipantStatus;
import com.timemate.gonow.domain.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    boolean existsByInviteCode(String inviteCode);
    Optional<Appointment> findByInviteCode(String inviteCode);

    // 스케줄러: WAITING인 약속 → ACTIVE 벌크 전환
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Appointment a SET a.appointmentStatus = :active WHERE a.id IN :ids AND a.appointmentStatus = :waiting")
    void bulkUpdateToActiveInternal(@Param("active") AppointmentStatus active, @Param("ids") List<Long> ids, @Param("waiting") AppointmentStatus waiting);

    // 외부 호출용 래퍼 — Enum 파라미터 조립을 캡슐화
    default void bulkUpdateToActive(List<Long> ids) {
        bulkUpdateToActiveInternal(AppointmentStatus.ACTIVE, ids, AppointmentStatus.WAITING);
    }

    // 스케줄러: 전원 ARRIVED인 약속 → FINISHED 벌크 전환
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Appointment a SET a.appointmentStatus = :finished WHERE a.id IN :ids AND a.appointmentStatus != :finished AND NOT EXISTS (SELECT p FROM Participant p WHERE p.appointment.id = a.id AND p.participantStatus != :arrived)")
    void bulkUpdateToFinishedInternal(@Param("finished") AppointmentStatus finished, @Param("ids") List<Long> ids, @Param("arrived") ParticipantStatus arrived);

    // 외부 호출용 래퍼 — Enum 파라미터 조립을 캡슐화
    default void bulkUpdateToFinished(List<Long> ids) {
        bulkUpdateToFinishedInternal(AppointmentStatus.FINISHED, ids, ParticipantStatus.ARRIVED);
    }
}
