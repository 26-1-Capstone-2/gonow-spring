package com.timemate.gonow.domain.member.service;

import com.timemate.gonow.domain.member.dto.ArrivalSoundUpdateRequest;
import com.timemate.gonow.domain.member.dto.SettingUpdateRequest;
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
    public void updateSetting(Long memberId, SettingUpdateRequest request) {
        MemberSetting setting = memberSettingRepository.findByMemberId(memberId).orElseThrow(
                () -> new IllegalArgumentException("사용자 정보 또는 설정 정보를 찾을 수 없습니다."));

        setting.updateSetting(request.transitType(), request.priorityType(), request.preparationTime());
    }

    // 도착 예정/완료 알림 소리 모드 변경 — 부분 업데이트(둘 다 null이면 바꿀 게 없는 무의미한 요청이라 차단)
    // null 하나만 오면 그 필드는 유지되도록 MemberSetting.updateArrivalSoundMode에서 처리
    @Transactional
    public void updateArrivalSoundMode(Long memberId, ArrivalSoundUpdateRequest request) {
        if (request.arrivalExpectedSoundMode() == null && request.arrivalCompleteSoundMode() == null) {
            throw new IllegalArgumentException("변경할 소리 모드를 최소 하나는 지정해야 합니다.");
        }

        MemberSetting setting = memberSettingRepository.findByMemberId(memberId).orElseThrow(
                () -> new IllegalArgumentException("사용자 정보 또는 설정 정보를 찾을 수 없습니다."));

        setting.updateArrivalSoundMode(request.arrivalExpectedSoundMode(), request.arrivalCompleteSoundMode());
    }
}
