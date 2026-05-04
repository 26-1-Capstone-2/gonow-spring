package com.timemate.gonow.domain.member.dto;

import com.timemate.gonow.domain.member.constant.PriorityType;
import com.timemate.gonow.domain.member.constant.TransitType;
import com.timemate.gonow.domain.member.entity.MemberSetting;

public record SettingUpdateResponse(
        TransitType transitType,
        PriorityType priorityType


) {
    // MemberSetting -> UpdateSettingResponse로 전환
    public static SettingUpdateResponse from(MemberSetting setting) {
        return new SettingUpdateResponse(
                setting.getTransitType(),
                setting.getPriorityType()
        );
    }
}
