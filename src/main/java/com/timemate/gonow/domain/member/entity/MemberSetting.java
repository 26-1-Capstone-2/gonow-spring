package com.timemate.gonow.domain.member.entity;


import com.timemate.gonow.domain.member.constant.PriorityType;
import com.timemate.gonow.domain.member.constant.TransitType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

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
    @ColumnDefault("'ALL'") // DEFAULT 'ALL'
    private TransitType transitType = TransitType.ALL;        // 선호 교통수단

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ColumnDefault("'FASTEST'") // DEFAULT 'FASTEST'
    private PriorityType priorityType = PriorityType.FASTEST; // 경로 우선순위

    @Builder
    public MemberSetting(Member member, TransitType transitType, PriorityType priorityType) {
        this.member = member;
        this.transitType = transitType;
        this.priorityType = priorityType;
    }

    // 단방향이므로 연관관계 편의 메소드 X
}
