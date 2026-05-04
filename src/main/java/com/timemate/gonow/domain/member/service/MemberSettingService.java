package com.timemate.gonow.domain.member.service;

import com.timemate.gonow.domain.member.dto.SettingUpdateRequest;
import com.timemate.gonow.domain.member.dto.SettingUpdateResponse;
import com.timemate.gonow.domain.member.entity.MemberSetting;
import com.timemate.gonow.domain.member.repository.MemberSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberSettingService {
    private final MemberSettingRepository memberSettingRepository;

    // 멤버 설정 등록/변경
    @Transactional
    public SettingUpdateResponse updateSetting(Long memberId, SettingUpdateRequest request) {
        MemberSetting setting = memberSettingRepository.findByMemberId(memberId).orElseThrow(
                () -> new IllegalArgumentException("사용자 정보 또는 설정 정보를 찾을 수 없습니다."));

        setting.updateSetting(request.transitType(), request.priorityType());

        return SettingUpdateResponse.from(setting);
    }
}
