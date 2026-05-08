package com.timemate.gonow.domain.member.entity;


import com.timemate.gonow.domain.member.constant.PriorityType;
import com.timemate.gonow.domain.member.constant.TransitType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
// member_id(FK) -> UK
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "member_id", name = "uk_member_setting_member_id"))
public class MemberSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id", nullable = false)
    private Long id;

    // 현재는 일대일 단방향
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_member_setting_member"))
    private Member member; // NOT NULL, UNIQUE(1:1)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ColumnDefault("'ALL'")             // DEFAULT 'ALL'
    private TransitType transitType;    // 선호 교통수단

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ColumnDefault("'MIN_TIME'")         // DEFAULT 'MIN_TIME'
    private PriorityType priorityType;   // 경로 우선순위

    @Column(nullable = false)
    private int preparationTime;         // 여유시간(준비 시간, 단위: 분)

    // 생성자 ----------------------------------------------------------------------------------
    @Builder
    public MemberSetting(Member member, TransitType transitType, PriorityType priorityType, int preparationTime) {
        this.member = member;
        this.transitType = Objects.requireNonNullElse(transitType, TransitType.ALL);
        this.priorityType = Objects.requireNonNullElse(priorityType, PriorityType.MIN_TIME);
        this.preparationTime = preparationTime;
    }

    // 변경 메소드 ----------------------------------------------------------------------------------
    // 설정 변경
    public void updateSetting(TransitType transitType, PriorityType priorityType, int preparationTime) {
        this.transitType = transitType;
        this.priorityType = priorityType;
        this.preparationTime = preparationTime;
    }

    // 단방향이므로 연관관계 편의 메소드 X
}
