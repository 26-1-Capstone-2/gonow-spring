package com.timemate.gonow.domain.appointment.entity;

import com.timemate.gonow.domain.appointment.constant.ParticipantStatus;
import com.timemate.gonow.domain.common.Point;
import com.timemate.gonow.domain.member.entity.Member;
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

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "origin_lat", precision = 10, scale = 8)),
            @AttributeOverride(name = "lng", column = @Column(name = "origin_lng", precision = 11, scale = 8))
    })
    private Point originPos; // 출발지 위도/경도

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "current_lat", precision = 10, scale = 8)),
            @AttributeOverride(name = "lng", column = @Column(name = "current_lng", precision = 11, scale = 8))
    })
    private Point currentPos; // 현재 위치 위도/경도

    private LocalDateTime estimatedArrival; // 도착 예정 시간

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @ColumnDefault("'READY'")
    private ParticipantStatus participantStatus = ParticipantStatus.READY; // 참여자 상태 (NOT NULL, DEFAULT 'READY')

//    @Column(nullable = false) // 임시 컬럼이므로 NULL로 허용하자
    @ColumnDefault("TRUE")
    private Boolean isAlarmOn = true; // 확정 컬럼이 되면 NOT NULL, boolean으로 바꾸자

    @Builder
    private Participant(Member member, Appointment appointment, boolean isHost, Point originPos, Point currentPos, LocalDateTime estimatedArrival, ParticipantStatus participantStatus, boolean isAlarmOn) {
        this.member = member;
        this.appointment = appointment;
        this.isHost = isHost;
        this.originPos = originPos;
        this.currentPos = currentPos;
        this.estimatedArrival = estimatedArrival;
        this.participantStatus = participantStatus;
        this.isAlarmOn = isAlarmOn;
    }

    // 단방향이므로 연관관계 편의 메소드 X
}
