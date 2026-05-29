package com.timemate.gonow.domain.appointment.service;

import com.timemate.gonow.domain.appointment.constant.AppointmentStatus;
import com.timemate.gonow.domain.appointment.constant.ParticipantStatus;
import com.timemate.gonow.domain.appointment.dto.ParticipantActiveUpdateRequest;
import com.timemate.gonow.domain.appointment.dto.ParticipantArriveResponse;
import com.timemate.gonow.domain.appointment.dto.ParticipantLocationUpdateResponse;
import com.timemate.gonow.domain.appointment.dto.ParticipantTransportUpdateRequest;
import com.timemate.gonow.domain.appointment.entity.Appointment;
import com.timemate.gonow.domain.appointment.entity.Participant;
import com.timemate.gonow.domain.appointment.repository.ParticipantRepository;
import com.timemate.gonow.domain.common.Point;
import com.timemate.gonow.domain.common.constant.GeoConstants;
import com.timemate.gonow.domain.common.dto.LocationUpdateRequest;
import com.timemate.gonow.domain.common.constant.TransportType;
import com.timemate.gonow.domain.member.constant.TransitType;
import com.timemate.gonow.domain.member.entity.MemberSetting;
import com.timemate.gonow.domain.member.repository.MemberSettingRepository;
import com.timemate.gonow.global.client.FlaskClient;
import com.timemate.gonow.global.client.dto.FlaskParticipantRequest;
import com.timemate.gonow.global.client.dto.FlaskParticipantResponse;
import com.timemate.gonow.global.client.dto.TransportMode;
import com.timemate.gonow.global.fcm.FcmSender;
import com.timemate.gonow.global.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ParticipantService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("a h시 m분", Locale.KOREAN);

    private final ParticipantRepository participantRepository;
    private final MemberSettingRepository memberSettingRepository;
    private final FlaskClient flaskClient;
    private final FcmSender fcmSender;

    // 개인 알람 스위치 ON/OFF (본인만)
    @Transactional
    public void updateActive(Long memberId, Long appointmentId, ParticipantActiveUpdateRequest request) {
        Participant participant = participantRepository.findByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 약속의 참여자가 아닙니다."));

        participant.updateActive(request.isActive());
    }

    // 이동 수단 변경 (일반 참가자 전용)
    @Transactional
    public void updateTransportType(Long memberId, Long appointmentId, ParticipantTransportUpdateRequest request) {
        Participant participant = participantRepository.findByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 약속의 참여자가 아닙니다."));

        if (participant.isHost()) {
            throw new IllegalArgumentException("방장은 그룹 알람 수정 API를 이용해주세요.");
        }

        participant.updateTransportType(request.transportType());
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

    // GPS 좌표 수신 + 상태 전이
    @Transactional
    public ParticipantLocationUpdateResponse updateLocation(Long memberId, Long appointmentId, LocationUpdateRequest request) {
        Participant participant = participantRepository.findWithAppointmentByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 약속의 참여자가 아닙니다."));

        MemberSetting setting = memberSettingRepository.findByMemberId(memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 회원 설정입니다."));


        Appointment appointment = participant.getAppointment();
        BigDecimal newLat = request.lat();
        BigDecimal newLng = request.lng();
        Point newPoint = new Point(newLat, newLng);

        // 목적지까지의 거리
        double distToDest = GeoUtils.calculateDistance(
                newLat.doubleValue(), newLng.doubleValue(),
                appointment.getDestination().point().lat().doubleValue(),
                appointment.getDestination().point().lng().doubleValue()
        );
        // 앵커로부터의 거리
        double distFromAnchor = participant.getCurrentPos() == null ? -1 :
                GeoUtils.calculateDistance(
                        newLat.doubleValue(), newLng.doubleValue(),
                        participant.getCurrentPos().lat().doubleValue(),
                        participant.getCurrentPos().lng().doubleValue()
                );

        boolean isNearDest = distToDest < GeoConstants.ARRIVAL_THRESHOLD_METERS; // 목적지 100m 이내?

        Integer interval = null;

        switch (participant.getParticipantStatus()) {
            case READY -> {
                boolean isFirstReceive = (distFromAnchor == -1); // 첫 좌표 수신?
                boolean isOutOfAnchor = (distFromAnchor >= GeoConstants.RECOMPUTE_THRESHOLD_METERS); // 앵커 500m 이탈?

                // 앵커 갱신 및 Q 계산이 필요한 조건 통합 (isPastAlarmTime 계산 전에 반드시 실행)
                if (isNearDest || isFirstReceive || isOutOfAnchor) {
                    participant.updateCurrentPos(newPoint);
                    interval = callFlaskAndUpdate(memberId, participant, appointment, newPoint, setting);
                }

                boolean isPastAlarmTime = !LocalDateTime.now().isBefore(participant.getDepartureAlarmTime()); // P >= Q?

                if (isNearDest) {
                    // 100m 이내 -> NEARDEST
                    participant.updateStatus(ParticipantStatus.NEARDEST);
                } else if (isPastAlarmTime) {
                    // P >= Q → DEPARTING, 300m 이탈 감지를 위해 주기를 타이트하게 갱신
                    participant.updateStatus(ParticipantStatus.DEPARTING);
                    interval = callFlaskAndUpdate(memberId, participant, appointment, newPoint, setting);
                }
                // P < Q → READY 유지
            }
            case NEARDEST -> {
                boolean isPastAlarmTime = !LocalDateTime.now().isBefore(participant.getDepartureAlarmTime()); // P >= Q?

                if (!isNearDest && !isPastAlarmTime) {
                    // 100m 벗어남 + P < Q → READY 복귀 (스쳐지나간 케이스)
                    participant.updateCurrentPos(newPoint);
                    participant.updateStatus(ParticipantStatus.READY);
                    interval = callFlaskAndUpdate(memberId, participant, appointment, newPoint, setting); // Q 재계산
                } else {
                    // 100m 이내 대기 중 → targetTime까지 남은 시간 기반으로 주기 조절 (플라스크 미호출)
                    // P >= Q면 100m 벗어나도 NEARDEST 고정 (알람 울리는 중)
                    // - 사용자 확인 → /arrive API → ARRIVED
                    // - targetTime 초과 → ArrivedTransitionScheduler가 자동 ARRIVED
                    long minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), appointment.getTargetTime());
                    if (minutesLeft > 30) interval = 120;
                    else if (minutesLeft > 10) interval = 60;
                    else interval = 30;
                }
            }
            case DEPARTING -> {
                boolean isDepartedFromAnchor = (distFromAnchor >= GeoConstants.DEPARTURE_THRESHOLD_METERS);

                if (isNearDest) {
                    // 100m 진입 → ARRIVED
                    participant.updateCurrentPos(newPoint);

                    participant.updateStatus(ParticipantStatus.ARRIVED);
                    finishAppointment(appointment);
                    sendGroupNotification(memberId, appointmentId, "도착 완료 알림",
                            participant.getMember().getNickname() + "님이 " + LocalDateTime.now().format(TIME_FORMATTER) + "에 도착했습니다.");
                } else if (isDepartedFromAnchor) {
                    // 300m 이탈 → MOVING
                    participant.updateCurrentPos(newPoint);

                    participant.updateStatus(ParticipantStatus.MOVING);
                    activateAppointment(appointment);
                    interval = callFlaskAndUpdate(memberId, participant, appointment, newPoint, setting);

                    String eta = participant.getEstimatedArrival().format(TIME_FORMATTER);
                    sendGroupNotification(memberId, appointmentId, "도착 예정 알림",
                            participant.getMember().getNickname() + "님이 " + eta + "에 도착 예정입니다.");
                }
                // 300m 미만이면 DEPARTING 유지, 앵커 보존
            }
            case MOVING -> {
                participant.updateCurrentPos(newPoint); // 대시보드용 위치 갱신
                interval = callFlaskAndUpdate(memberId, participant, appointment, newPoint, setting); // ETA 재계산

                if (isNearDest) {
                    // 100m 진입 → ARRIVED
                    participant.updateStatus(ParticipantStatus.ARRIVED);
                    finishAppointment(appointment);
                    sendGroupNotification(memberId, appointmentId, "도착 완료 알림",
                            participant.getMember().getNickname() + "님이 " + LocalDateTime.now().format(TIME_FORMATTER) + "에 도착했습니다.");
                }
            }
            default -> {
                // SCHEDULED, ARRIVED 상태에서는 무시
            }
        }

        return ParticipantLocationUpdateResponse.from(participant, appointment, interval, setting.getPreparationTime());
    }

    // 도착 확인 (사용자가 확인 버튼 눌렀을 때 ARRIVED로 전환)
    @Transactional
    public ParticipantArriveResponse arrive(Long memberId, Long appointmentId) {
        Participant participant = participantRepository.findWithAppointmentByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 약속의 참여자가 아닙니다."));

        if (participant.getParticipantStatus() != ParticipantStatus.NEARDEST) {
            throw new IllegalStateException("NEARDEST 상태에서만 도착 확인이 가능합니다.");
        }

        Appointment appointment = participant.getAppointment();

        participant.updateStatus(ParticipantStatus.ARRIVED);
        finishAppointment(appointment);
        sendGroupNotification(memberId, appointmentId, "도착 완료 알림",
                participant.getMember().getNickname() + "님이 " + LocalDateTime.now().format(TIME_FORMATTER) + "에 도착했습니다.");

        return ParticipantArriveResponse.from(appointment);
    }

    // WAITING → ACTIVE 전환
    private void activateAppointment(Appointment appointment) {
        if (appointment.getAppointmentStatus() == AppointmentStatus.WAITING) {
            appointment.updateStatus(AppointmentStatus.ACTIVE);
        }
    }

    // ACTIVE → FINISHED 전환 (전원 ARRIVED 시)
    private void finishAppointment(Appointment appointment) {
        activateAppointment(appointment);

        if (participantRepository.countNotArrivedByAppointmentId(appointment.getId()) == 0) {
            appointment.updateStatus(AppointmentStatus.FINISHED);
        }
    }

    // 나를 제외한 다른 참가자들에게 FCM Notification 발송
    private void sendGroupNotification(Long memberId, Long appointmentId, String title, String body) {
        List<String> tokens = participantRepository.findFcmTokensByAppointmentIdExcluding(appointmentId, memberId);
        fcmSender.sendAllNotification(tokens, title, body);
    }

    // 플라스크 호출 → interval 반환, MOVING이 아닐 때만 departureAlarmTime 저장
    // (MOVING에서는 DEPARTING 진입 시 확정된 departureAlarmTime을 덮어쓰지 않음, estimatedArrival은 항상 갱신)
    private Integer callFlaskAndUpdate(Long memberId, Participant participant, Appointment appointment, Point currentPoint, MemberSetting setting) {
        TransportMode transportMode = resolveTransportMode(participant.getTransportType(), setting.getTransitType());

        FlaskParticipantRequest request = new FlaskParticipantRequest(
                memberId,
                currentPoint.lat(),
                currentPoint.lng(),
                appointment.getDestination().point().lat(),
                appointment.getDestination().point().lng(),
                transportMode,
                setting.getPriorityType(),
                appointment.getTargetTime(),
                setting.getPreparationTime()
        );

        FlaskParticipantResponse response = flaskClient.calculateParticipantAlarm(request);
        if (participant.getParticipantStatus() != ParticipantStatus.MOVING) {
            participant.updateAlarmInfo(response.departureAlarmTime(), response.estimatedArrival());
        } else {
            participant.updateEstimatedArrival(response.estimatedArrival()); // ETA만 갱신 (대시보드용)
        }
        return response.interval();
    }

    // TransportType + TransitType → TransportMode 변환
    private TransportMode resolveTransportMode(TransportType transportType, TransitType transitType) {
        if (transportType == TransportType.DRIVING) return TransportMode.DRIVING;
        return switch (transitType) {
            case SUBWAY -> TransportMode.SUBWAY;
            case BUS -> TransportMode.BUS;
            case ALL -> TransportMode.ALL;
        };
    }
}
