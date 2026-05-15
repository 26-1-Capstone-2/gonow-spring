package com.timemate.gonow.domain.appointment.entity;

import com.timemate.gonow.domain.appointment.constant.AppointmentStatus;
import com.timemate.gonow.domain.common.Location;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "invite_code", name = "uk_appointment_invite_code"))
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appointment_id", nullable = false)
    private Long id;


    @Column(nullable = false, unique = true)
    private String inviteCode; // 초대 코드 (NOT NULL, UNIQUE)

    private String title; // 약속 제목

    @Column(nullable = false)
    private LocalDate planDate; // 약속 예정 날짜 (NOT NULL)

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name",      column = @Column(name = "dest_name",    nullable = false)),
            @AttributeOverride(name = "address",   column = @Column(name = "dest_address", nullable = false)),
            @AttributeOverride(name = "point.lat", column = @Column(name = "dest_lat",     nullable = false, precision = 10, scale = 8)),
            @AttributeOverride(name = "point.lng", column = @Column(name = "dest_lng",     nullable = false, precision = 11, scale = 8))
    })
    private Location destination; // 목적지 이름/주소/위도/경도 (NOT NULL)

    @Column(nullable = false)
    private LocalDateTime targetTime; // 목표 시간 (NOT NULL)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @ColumnDefault("'WAITING'")
    private AppointmentStatus appointmentStatus; // 약속 상태 (NOT NULL, DEFAULT 'WAITING')

    @Builder
    private Appointment(String inviteCode, String title, Location destination, LocalDate planDate, LocalDateTime targetTime, AppointmentStatus appointmentStatus) {
        this.inviteCode = inviteCode;
        this.title = title;
        this.destination = destination;
        this.planDate = planDate;
        this.targetTime = targetTime;
        this.appointmentStatus = Objects.requireNonNullElse(appointmentStatus, AppointmentStatus.WAITING);
    }

    // 약속 정보 수정 (방장 전용)
    public void update(LocalDate planDate, LocalDateTime targetTime, Location destination) {
        this.planDate = planDate;
        this.targetTime = targetTime;
        this.destination = destination;
    }

    // 단방향이므로 연관관계 편의 메소드 X
}
