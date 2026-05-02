package com.timemate.gonow.domain.appointment.entity;

import com.timemate.gonow.domain.appointment.constant.AppointmentStatus;
import com.timemate.gonow.domain.common.Location;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appointment_id", nullable = false)
    private Long id;

    private String title; // 약속 제목

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "dest_address", nullable = false)),
            @AttributeOverride(name = "point.lat", column = @Column(name = "dest_lat", nullable = false, precision = 10, scale = 8)),
            @AttributeOverride(name = "point.lng", column = @Column(name = "dest_lng", nullable = false, precision = 11, scale = 8))
    })
    private Location destination; // 목적지 주소/위치/경도 (NOT NULL)

    @Column(nullable = false)
    private LocalDateTime targetTime; // 목표 시간 (NOT NULL)

    @Column(nullable = false)
    @ColumnDefault("TRUE")
    private boolean isActive = true; // 약속 알람 스위치 (NOT NULL, DEFAULT TRUE)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @ColumnDefault("'READY'")
    private AppointmentStatus appointmentStatus = AppointmentStatus.READY; // 약속 상태 (NOT NULL, DEFAULT 'READY')

    @Builder
    private Appointment(String title, Location destination, LocalDateTime targetTime, boolean isActive, AppointmentStatus appointmentStatus) {
        this.title = title;
        this.destination = destination;
        this.targetTime = targetTime;
        this.isActive = isActive;
        this.appointmentStatus = appointmentStatus;
    }

    // 단방향이므로 연관관계 편의 메소드 X
}
