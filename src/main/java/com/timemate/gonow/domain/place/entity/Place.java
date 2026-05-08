package com.timemate.gonow.domain.place.entity;

import com.timemate.gonow.domain.common.Location;
import com.timemate.gonow.domain.member.entity.Member;
import com.timemate.gonow.domain.place.constant.PlaceType;
import com.timemate.gonow.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id", nullable = false)
    private Long id;

    // 다대일 단방향
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_place_member"))

    private Member member; // NOT NULL, NOT UNIQUE(1:N)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlaceType placeType; // 귀가지, 목적지

    @Embedded
    private Location location; // 이름, 주소, 위도, 경도

    @Builder
    private Place(Member member, PlaceType placeType, Location location) {
        this.member = member;
        this.placeType = placeType;
        this.location = location;
    }

    // 단방향이므로 연관관계 편의 메소드 X
}

