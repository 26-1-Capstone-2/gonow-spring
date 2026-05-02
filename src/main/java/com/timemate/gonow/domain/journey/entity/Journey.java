package com.timemate.gonow.domain.journey.entity;

import com.timemate.gonow.domain.common.Location;
import com.timemate.gonow.domain.common.Point;
import com.timemate.gonow.domain.journey.constant.JourneyStatus;
import com.timemate.gonow.domain.journey.constant.JourneyType;
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

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "current_lat", precision = 10, scale = 8)),
            @AttributeOverride(name = "lng", column = @Column(name = "current_lng", precision = 11, scale = 8))
    })
    private Point currentPoint; // 현재 위치 위도/경도 (nullable)

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "dest_address", nullable = false)),
            @AttributeOverride(name = "point.lat", column = @Column(name = "dest_lat", nullable = false, precision = 10, scale = 8)),
            @AttributeOverride(name = "point.lng", column = @Column(name = "dest_lng", nullable = false, precision = 11, scale = 8))
    })
    private Location destination; // 목적지 주소/위치/경도 (NOT NULL)


    @Column(nullable = false)
    @ColumnDefault("FALSE")
    private boolean isLastMode = false; // 막차 여부 (NOT NULL, DEFAULT FALSE)

    @Column(nullable = false)
    private LocalDateTime targetTime; // 목표 시간 (NOT NULL)

    private LocalDateTime estimatedArrival; // 도착 예정 시간

    @Column(nullable = false)
    @ColumnDefault("0")
    private int repeatDays = 0; // 반복 요일 (NOT NULL, DEFAULT 0)

    @Column(nullable = false)
    @ColumnDefault("TRUE")
    private boolean isActive = true; // 여정 알람 스위치 (NOT NULL, DEFAULT TRUE)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @ColumnDefault("'READY'")
    private JourneyStatus journeyStatus = JourneyStatus.READY; // 이동 상태 (NOT NULL, DEFAULT 'READY')

    @Builder
    private Journey(Member member, String title, JourneyType journeyType, Point currentPoint, Location destination, boolean isLastMode, LocalDateTime targetTime, LocalDateTime estimatedArrival, int repeatDays, boolean isActive, JourneyStatus journeyStatus) {
        this.member = member;
        this.title = title;
        this.journeyType = journeyType;
        this.currentPoint = currentPoint;
        this.destination = destination;
        this.isLastMode = isLastMode;
        this.targetTime = targetTime;
        this.estimatedArrival = estimatedArrival;
        this.repeatDays = repeatDays;
        this.isActive = isActive;
        this.journeyStatus = journeyStatus;
    }

    // 단방향이므로 연관관계 편의 메소드 X
}
