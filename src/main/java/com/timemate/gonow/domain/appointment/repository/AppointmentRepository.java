package com.timemate.gonow.domain.appointment.repository;

import com.timemate.gonow.domain.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    boolean existsByInviteCode(String inviteCode);
    Optional<Appointment> findByInviteCode(String inviteCode);
}
