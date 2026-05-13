package com.timemate.gonow.domain.appointment.entity;

import com.timemate.gonow.domain.appointment.constant.ParticipantStatus;
import com.timemate.gonow.domain.common.Point;
import com.timemate.gonow.domain.common.constant.TransportType;
import com.timemate.gonow.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.util.Objects;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "appointment_id"}, name = "uk_participant_member_id_appointment_id"))
public class Participant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participant_id", nullable = false)
    private Long id;

    // 현재는 다대일 단방향
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_participant_member"))
    private Member member; // member_id (NOT NULL, 복합 UK)

    // 현재는 다대일 단방향
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_participant_appointment"))
    private Appointment appointment; // appointment_id (NOT NULL, 복합 UK)

    @Column(nullable = false)
    @ColumnDefault("FALSE")
    private boolean isHost = false; // 방장 여부 (NOT NULL, DEFAULT FALSE)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransportType transportType; // 이동 수단 (NOT NULL)

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "current_lat", precision = 10, scale = 8)),
            @AttributeOverride(name = "lng", column = @Column(name = "current_lng", precision = 11, scale = 8))
    })
    private Point currentPos; // 현재 위치 위도/경도

    // TODO: 플라스크 연동 후 NOT NULL로 변경 예정
    private LocalDateTime departureAlarmTime; // 출발 알람 시각

    private LocalDateTime estimatedArrival; // 도착 예정 시간

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @ColumnDefault("'SCHEDULED'")
    private ParticipantStatus participantStatus; // 참여자 상태 (NOT NULL, DEFAULT 'SCHEDULED')

    @Column(nullable = false)
    @ColumnDefault("TRUE")
    private boolean isActive; // 개인 알람 ON/OFF (NOT NULL, DEFAULT TRUE)

    @Builder
    private Participant(Member member, Appointment appointment, Boolean isHost, Point currentPos, LocalDateTime estimatedArrival, LocalDateTime departureAlarmTime, TransportType transportType, ParticipantStatus participantStatus, Boolean isActive) {
        this.member = member;
        this.appointment = appointment;
        this.isHost = Objects.requireNonNullElse(isHost, false);
        this.currentPos = currentPos;
        this.estimatedArrival = estimatedArrival;
        this.departureAlarmTime = departureAlarmTime;
        this.transportType = transportType;
        this.participantStatus = Objects.requireNonNullElse(participantStatus, ParticipantStatus.SCHEDULED);
        this.isActive = Objects.requireNonNullElse(isActive, true);
    }

    // 개인 알람 ON/OFF
    public void updateActive(boolean isActive) {
        this.isActive = isActive;
    }

    // 단방향이므로 연관관계 편의 메소드 X
}
