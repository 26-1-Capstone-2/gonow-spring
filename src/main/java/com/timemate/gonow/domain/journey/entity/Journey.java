package com.timemate.gonow.domain.journey.entity;

import com.timemate.gonow.domain.common.Location;
import com.timemate.gonow.domain.common.Point;
import com.timemate.gonow.domain.common.constant.TransportType;
import com.timemate.gonow.domain.journey.constant.JourneyStatus;
import com.timemate.gonow.domain.journey.constant.JourneyType;
import com.timemate.gonow.domain.member.entity.Member;
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
public class Journey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "journey_id", nullable = false)
    private Long id;

    // 현재는 다대일 단방향
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_journey_member"))
    private Member member; // NOT NULL, NOT UNIQUE(1:N)

    private String title; // 여정 제목 (후순위)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JourneyType journeyType; // 여정 타입 (NOT NULL)

    @Column(nullable = false)
    private boolean isLastMode; // 막차 여부 (NOT NULL)

    @Column(nullable = false)
    private LocalDate planDate; // 여정 예정 날짜 (NOT NULL)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransportType transportType; // 이동 수단 (NOT NULL)

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "current_lat", precision = 10, scale = 8)),
            @AttributeOverride(name = "lng", column = @Column(name = "current_lng", precision = 11, scale = 8))
    })
    private Point currentPoint; // 현재 위치 위도/경도

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

    private LocalDateTime departureAlarmTime; // 출발 알람 시각 (SCHEDULED 상태에서는 null, READY 최초 GPS 수신 시 플라스크 호출로 확정)

    @Column(nullable = false)
    private int repeatDays; // 반복 요일 (NOT NULL)

    @Column(nullable = false)
    @ColumnDefault("TRUE")
    private boolean isActive; // 여정 알람 스위치 (NOT NULL, DEFAULT TRUE)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @ColumnDefault("'SCHEDULED'")
    private JourneyStatus journeyStatus; // 이동 상태 (NOT NULL, DEFAULT 'SCHEDULED')

    @Builder
    private Journey(Member member, String title, JourneyType journeyType, Point currentPoint, Location destination, TransportType transportType, LocalDate planDate, boolean isLastMode, LocalDateTime targetTime, LocalDateTime departureAlarmTime, int repeatDays, Boolean isActive, JourneyStatus journeyStatus) {
        this.member = member;
        this.title = title;
        this.journeyType = journeyType;
        this.currentPoint = currentPoint;
        this.destination = destination;
        this.transportType = transportType;
        this.planDate = planDate;
        this.isLastMode = isLastMode;
        this.targetTime = targetTime;
        this.departureAlarmTime = departureAlarmTime;
        this.repeatDays = repeatDays;
        this.isActive = Objects.requireNonNullElse(isActive, true);
        this.journeyStatus = Objects.requireNonNullElse(journeyStatus, JourneyStatus.SCHEDULED);
    }

    // 변경 메소드 ------------------------------------------------------------------------------
    // 개인 여정 수정
    public void updatePersonal(String title, LocalDate planDate, LocalDateTime targetTime, Location destination, TransportType transportType, int repeatDays, JourneyStatus journeyStatus) {
        this.title = title;
        this.planDate = planDate;
        this.targetTime = targetTime;
        this.destination = destination;
        this.transportType = transportType;
        this.repeatDays = repeatDays;
        this.journeyStatus = journeyStatus;
    }

    // 알람 스위치 ON/OFF
    public void updateActive(boolean isActive) {
        this.isActive = isActive;
    }

    // 귀가 여정 수정
    public void updateHome(String title, boolean isLastMode, LocalDate planDate, LocalDateTime targetTime, Location destination, TransportType transportType, int repeatDays, JourneyStatus journeyStatus) {
        this.title = title;
        this.isLastMode = isLastMode;
        this.planDate = planDate;
        this.targetTime = targetTime;
        this.destination = destination;
        this.transportType = transportType;
        this.repeatDays = repeatDays;
        this.journeyStatus = journeyStatus;
    }

    // 현재 위치 갱신
    public void updateCurrentPoint(Point point) {
        this.currentPoint = point;
    }

    // 상태 전이
    public void updateStatus(JourneyStatus status) {
        this.journeyStatus = status;
    }

    public void updateDepartureAlarmTime(LocalDateTime departureAlarmTime) {
        this.departureAlarmTime = departureAlarmTime;
    }

    // 단방향이므로 연관관계 편의 메소드 X
}
