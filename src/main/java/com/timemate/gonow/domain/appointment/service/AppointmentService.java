package com.timemate.gonow.domain.appointment.service;

import com.timemate.gonow.domain.appointment.constant.AppointmentStatus;
import com.timemate.gonow.domain.appointment.dto.AppointmentCreateRequest;
import com.timemate.gonow.domain.appointment.dto.AppointmentCreateResponse;
import com.timemate.gonow.domain.appointment.dto.AppointmentJoinRequest;
import com.timemate.gonow.domain.appointment.dto.AppointmentJoinResponse;
import com.timemate.gonow.domain.appointment.dto.AppointmentResponse;
import com.timemate.gonow.domain.appointment.dto.AppointmentUpdateRequest;
import com.timemate.gonow.domain.appointment.dto.AppointmentUpdateResponse;
import com.timemate.gonow.domain.appointment.dto.DashboardResponse;
import com.timemate.gonow.domain.appointment.entity.Appointment;
import com.timemate.gonow.domain.appointment.entity.Participant;
import com.timemate.gonow.domain.appointment.repository.AppointmentRepository;
import com.timemate.gonow.domain.appointment.repository.ParticipantRepository;
import com.timemate.gonow.domain.common.Location;
import com.timemate.gonow.domain.common.Point;
import com.timemate.gonow.domain.appointment.constant.ParticipantStatus;
import com.timemate.gonow.domain.member.entity.Member;
import com.timemate.gonow.domain.member.repository.MemberRepository;
import com.timemate.gonow.global.fcm.FcmSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
    private final FcmSender fcmSender;

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

        // 방장 Participant 동시 생성
        Participant host = Participant.builder()
                .member(member)
                .appointment(appointment)
                .isHost(true)
                .transportType(request.transportType())
                .participantStatus(resolveInitialStatus(request.planDate())) // 당일 생성이면 READY, 그 외엔 SCHEDULED
                .build();

        participantRepository.save(host);

        return AppointmentCreateResponse.from(appointment, host);
    }

    // 초대코드로 참여
    @Transactional
    public AppointmentJoinResponse joinAppointment(Long memberId, AppointmentJoinRequest request) {
        Appointment appointment = appointmentRepository.findByInviteCode(request.inviteCode())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 초대 코드입니다."));

        if (appointment.getAppointmentStatus() == AppointmentStatus.FINISHED) {
            throw new IllegalStateException("이미 종료된 약속입니다.");
        }

        if (participantRepository.existsByAppointmentIdAndMemberId(appointment.getId(), memberId)) {
            throw new IllegalStateException("이미 참여 중인 약속입니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Participant participant = Participant.builder()
                .member(member)
                .appointment(appointment)
                .isHost(false)
                .transportType(request.transportType())
                .participantStatus(resolveInitialStatus(appointment.getPlanDate())) // 당일 생성이면 READY, 그 외엔 SCHEDULED
                .build();

        participantRepository.save(participant);

        return AppointmentJoinResponse.from(appointment, participant);
    }

    // 그룹 알람 수정 (방장 전용 — 목적지/날짜/시간/이동수단)
    @Transactional
    public AppointmentUpdateResponse updateAppointment(Long memberId, Long appointmentId, AppointmentUpdateRequest request) {
        Participant host = participantRepository.findHostWithAppointment(appointmentId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없거나 방장 권한이 없습니다."));

        if (host.getAppointment().getAppointmentStatus() == AppointmentStatus.ACTIVE) {
            throw new IllegalStateException("이미 이동 중인 참가자가 있는 약속은 수정할 수 없습니다.");
        }

        Location destination = new Location(
                request.destName(),
                request.destAddress(),
                new Point(request.destLat(), request.destLng())
        );

        host.getAppointment().update(request.planDate(), request.targetTime(), destination);
        host.updateTransportType(request.transportType());

        // 날짜 변경 시 참가자 상태 일괄 재조정 (SCHEDULED/READY/DEPARTING 대상)
        ParticipantStatus newStatus = resolveInitialStatus(request.planDate());
        participantRepository.bulkResetStatusByAppointmentId(appointmentId, newStatus);

        // 방장 제외 나머지 참가자들에게 FCM Data 발송 (날짜/상태 변경 알림)
        List<String> tokens = participantRepository.findFcmTokensByAppointmentIdExcluding(appointmentId, memberId);
        if (!tokens.isEmpty()) {
            fcmSender.sendAllData(tokens, Map.of(
                    "appointment_id", String.valueOf(appointmentId),
                    "participant_status", newStatus.name()
            ));
        }

        return AppointmentUpdateResponse.from(newStatus);
    }

    // 도착 예정 대시보드 조회
    public DashboardResponse getDashboard(Long memberId, Long appointmentId) {
        List<Participant> participants = participantRepository.findAllByAppointmentId(appointmentId);

        Participant me = participants.stream()
                .filter(p -> p.getMember().getId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 약속이거나 참여자가 아닙니다."));

        return DashboardResponse.from(me.getAppointment(), participants, memberId);
    }

    // 그룹 알람 조회
    public AppointmentResponse getAppointment(Long memberId, Long appointmentId) {
        List<Participant> participants = participantRepository.findAllByAppointmentId(appointmentId);

        Participant me = participants.stream()
                .filter(p -> p.getMember().getId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 약속이거나 참여자가 아닙니다."));

        return AppointmentResponse.from(me.getAppointment(), participants);
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
