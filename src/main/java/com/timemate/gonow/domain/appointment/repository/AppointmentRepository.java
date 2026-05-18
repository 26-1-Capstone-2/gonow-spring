package com.timemate.gonow.domain.appointment.repository;

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
    @Query("UPDATE Appointment a SET a.appointmentStatus = 'ACTIVE' WHERE a.id IN :ids AND a.appointmentStatus = 'WAITING'")
    void bulkUpdateToActive(@Param("ids") List<Long> ids);

    // 스케줄러: 전원 ARRIVED인 약속 → FINISHED 벌크 전환
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Appointment a SET a.appointmentStatus = 'FINISHED' WHERE a.id IN :ids AND a.appointmentStatus != 'FINISHED' AND NOT EXISTS (SELECT p FROM Participant p WHERE p.appointment.id = a.id AND p.participantStatus != 'ARRIVED')")
    void bulkUpdateToFinished(@Param("ids") List<Long> ids);
}
