package com.timemate.gonow.domain.member.dto;

import com.timemate.gonow.domain.common.Location;
import com.timemate.gonow.domain.member.constant.PriorityType;
import com.timemate.gonow.domain.member.constant.TransitType;
import com.timemate.gonow.domain.member.entity.Member;
import com.timemate.gonow.domain.member.entity.MemberSetting;

import java.math.BigDecimal;

public record MyProfileResponse(
        // Member 정보
        Long memberId,
        String email,
        String nickname,
        String homeAddress,
        BigDecimal homeLat,
        BigDecimal homeLng,

        // MemberSetting 정보
        TransitType transitType,
        PriorityType priorityType
) {
    // Member + MemberSetting -> MyProfileResponse
    public static MyProfileResponse from(Member member, MemberSetting setting) {
        Location location = member.getLocation();

        return new MyProfileResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                location != null ? location.address() : null,
                location != null ? location.point().lat() : null,
                location != null ? location.point().lng() : null,

                setting.getTransitType(),
                setting.getPriorityType()
        );
    }
}
