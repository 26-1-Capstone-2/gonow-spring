package com.timemate.gonow.domain.appointment.service;

import com.timemate.gonow.domain.appointment.dto.AppointmentCreateRequest;
import com.timemate.gonow.domain.appointment.dto.AppointmentCreateResponse;
import com.timemate.gonow.domain.appointment.entity.Appointment;
import com.timemate.gonow.domain.appointment.entity.Participant;
import com.timemate.gonow.domain.appointment.repository.AppointmentRepository;
import com.timemate.gonow.domain.appointment.repository.ParticipantRepository;
import com.timemate.gonow.domain.common.Location;
import com.timemate.gonow.domain.common.Point;
import com.timemate.gonow.domain.appointment.constant.ParticipantStatus;
import com.timemate.gonow.domain.member.entity.Member;
import com.timemate.gonow.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AppointmentService {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final String CHAR_POOL = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 8;

    private final AppointmentRepository appointmentRepository;
    private final ParticipantRepository participantRepository;
    private final MemberRepository memberRepository;

    // 그룹 알람 생성 (방장 Participant 동시 생성)
    @Transactional
    public AppointmentCreateResponse createAppointment(Long memberId, AppointmentCreateRequest request) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Location destination = new Location(
                request.destName(),
                request.destAddress(),
                new Point(request.destLat(), request.destLng())
        );

        Appointment appointment = Appointment.builder()
                .inviteCode(generateInviteCode())
                .title(request.title())
                .planDate(request.planDate())
                .targetTime(request.targetTime())
                .destination(destination)
                .build();

        appointmentRepository.save(appointment);

        Location origin = new Location(
                request.originName(),
                request.originAddress(),
                new Point(request.originLat(), request.originLng())
        );

        // 방장 Participant 동시 생성
        Participant host = Participant.builder()
                .member(member)
                .appointment(appointment)
                .isHost(true)
                .origin(origin)
                .transportType(request.transportType())
                .participantStatus(resolveInitialStatus(request.planDate()))
                .build();

        participantRepository.save(host);

        return AppointmentCreateResponse.from(appointment, host);
    }

    // 그룹 알람 삭제
    @Transactional
    public void deleteAppointment(Long memberId, Long appointmentId) {
        // 1. 방장 권한 확인 및 Appointment 엔티티 확보
        Participant host = participantRepository.findHostWithAppointment(appointmentId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없거나 방장 권한이 없습니다."));

        // 2. 벌크 연산으로 참여자들 한 번에 삭제
        participantRepository.bulkDeleteByAppointmentId(appointmentId);

        // 3. 약속 삭제
        appointmentRepository.delete(host.getAppointment());
    }

    // 당일 생성이면 READY, 그 외엔 SCHEDULED
    private ParticipantStatus resolveInitialStatus(LocalDate planDate) {
        return planDate.isEqual(LocalDate.now()) ? ParticipantStatus.READY : ParticipantStatus.SCHEDULED;
    }

    private String generateInviteCode() {
        String inviteCode;

        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                int index = secureRandom.nextInt(CHAR_POOL.length());
                sb.append(CHAR_POOL.charAt(index));
            }
            inviteCode = sb.toString();
        } while(appointmentRepository.existsByInviteCode(inviteCode));

        return inviteCode;
    }
}
