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
        String homeName,
        String homeAddress,
        BigDecimal homeLat,
        BigDecimal homeLng,

        // MemberSetting 정보
        TransitType transitType,
        PriorityType priorityType,
        int preparationTime
) {
    // Member + MemberSetting -> MyProfileResponse
    public static MyProfileResponse from(Member member, MemberSetting setting) {
        Location location = member.getLocation();
        return new MyProfileResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                location.name(),
                location.address(),
                location.point().lat(),
                location.point().lng(),
                setting.getTransitType(),
                setting.getPriorityType(),
                setting.getPreparationTime()
        );
    }
}
