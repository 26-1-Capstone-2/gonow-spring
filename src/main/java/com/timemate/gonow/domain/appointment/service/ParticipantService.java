package com.timemate.gonow.domain.appointment.service;

import com.timemate.gonow.domain.appointment.dto.ParticipantActiveUpdateRequest;
import com.timemate.gonow.domain.appointment.entity.Participant;
import com.timemate.gonow.domain.appointment.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ParticipantService {
    private final ParticipantRepository participantRepository;

    // 개인 알람 스위치 ON/OFF (본인만)
    @Transactional
    public void updateActive(Long memberId, Long appointmentId, ParticipantActiveUpdateRequest request) {
        Participant participant = participantRepository.findByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 참여 정보를 찾을 수 없습니다."));

        participant.updateActive(request.isActive());
    }

    // 탈퇴(본인) & 추방(방장) 공통
    @Transactional
    public void deleteParticipant(Long memberId, Long appointmentId, Long targetMemberId) {
        // [조회] IN 절로 최대 2건 조회
        List<Participant> participants = participantRepository.findAllByAppointmentIdAndMemberIdIn(appointmentId, List.of(memberId, targetMemberId));

        // [검증 1] '나'의 정보가 리스트에 없으면? -> 내가 이 방 멤버가 아님!
        Participant requester = participants.stream()
                .filter(p -> p.getMember().getId().equals(memberId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("본인이 해당 그룹의 멤버가 아닙니다."));

        // [검증 2] '타겟'의 정보가 리스트에 없으면? -> 상대가 이미 나갔거나 존재 안 함!
        Participant target = participants.stream()
                .filter(p -> p.getMember().getId().equals(targetMemberId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("참여자를 찾을 수 없습니다."));

        if (memberId.equals(targetMemberId)) {
            // [탈퇴] 방장은 탈퇴 불가
            if (requester.isHost()) throw new IllegalArgumentException("방장은 스스로 탈퇴할 수 없습니다. 알람을 삭제해 주세요.");
        } else {
            // [추방] 방장만 가능
            if (!requester.isHost()) throw new IllegalArgumentException("방장 권한이 없습니다.");
        }

        participantRepository.delete(target);
    }
}
