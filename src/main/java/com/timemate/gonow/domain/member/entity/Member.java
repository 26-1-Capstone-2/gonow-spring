package com.timemate.gonow.domain.member.entity;

import com.timemate.gonow.domain.common.Location;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// role은 안 쓸거므로 과감하게 지움
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = "email", name = "uk_member_email"),
        @UniqueConstraint(columnNames = "nickname", name = "uk_member_nickname")
})
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false)
    private Long id;

    @Column(nullable = false)
    private String email; // 이메일(NOT NULL, UNIQUE)

    @Column(nullable = false)
    private String password; // 비밀번호(NOT NULL)

    @Column(nullable = false)
    private String nickname; // 닉네임(NOT NULL, UNIQUE)

    // 귀가지 주소: NULL 허용
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "home_address")),
            @AttributeOverride(name = "point.lat", column = @Column(name = "home_lat", precision = 10, scale = 8)),
            @AttributeOverride(name = "point.lng", column = @Column(name = "home_lng", precision = 11, scale = 8))
    })
    private Location location; // 집 주소, 집 위도, 집 경도

    @Builder
    private Member(String nickname, String email, String password, Location location) {
        this.nickname = nickname;
        this.email = email;
        this.password = password;
        this.location = location;
    }


    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    // 단방향이므로 연관관계 편의 메소드 X
}
