package com.timemate.gonow.domain.appointment.service;

import com.timemate.gonow.domain.appointment.constant.AppointmentStatus;
import com.timemate.gonow.domain.appointment.constant.ParticipantStatus;
import com.timemate.gonow.domain.appointment.dto.ParticipantActiveUpdateRequest;
import com.timemate.gonow.domain.appointment.dto.ParticipantLocationUpdateResponse;
import com.timemate.gonow.domain.appointment.dto.ParticipantTransportUpdateRequest;
import com.timemate.gonow.domain.appointment.entity.Appointment;
import com.timemate.gonow.domain.appointment.entity.Participant;
import com.timemate.gonow.domain.appointment.repository.ParticipantRepository;
import com.timemate.gonow.domain.common.Point;
import com.timemate.gonow.domain.common.constant.GeoConstants;
import com.timemate.gonow.domain.common.dto.LocationUpdateRequest;
import com.timemate.gonow.global.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

        Appointment appointment = participant.getAppointment();
        BigDecimal newLat = request.lat();
        BigDecimal newLng = request.lng();
        Point newPoint = new Point(newLat, newLng);

        switch (participant.getParticipantStatus()) {
            case READY -> {
                double distToDest = GeoUtils.calculateDistance(
                        newLat.doubleValue(), newLng.doubleValue(),
                        appointment.getDestination().point().lat().doubleValue(),
                        appointment.getDestination().point().lng().doubleValue()
                );

                if (distToDest < GeoConstants.ARRIVAL_THRESHOLD_METERS) {
                    // 1. NEARDEST 우선: 목적지 100m 이내 → 도착 확인 대기
                    participant.updateCurrentPos(newPoint);
                    participant.updateStatus(ParticipantStatus.NEARDEST);
                //} else if (P >= Q) {
                //    // 2. TODO: DEPARTING 전환 + 앵커 확정 (플라스크 연동 후 departureAlarmTime 채워지면 활성화)
                } else if (participant.getCurrentPos() == null) {
                    // 3. 최초 좌표 수신 → 앵커 저장
                    participant.updateCurrentPos(newPoint);
                } else {
                    // 4. 앵커 존재 → 500m 이탈 시 앵커 갱신
                    double distFromAnchor = GeoUtils.calculateDistance(
                            newLat.doubleValue(), newLng.doubleValue(),
                            participant.getCurrentPos().lat().doubleValue(),
                            participant.getCurrentPos().lng().doubleValue()
                    );
                    if (distFromAnchor >= GeoConstants.RECOMPUTE_THRESHOLD_METERS) {
                        participant.updateCurrentPos(newPoint);
                        // TODO: 플라스크 호출 → ETA 계산 → departureAlarmTime 재계산
                    }
                    // 500m 미만이면 좌표 갱신 안 함 (앵커 보존)
                }
            }
            case DEPARTING -> {
                double distToDest = GeoUtils.calculateDistance(
                        newLat.doubleValue(), newLng.doubleValue(),
                        appointment.getDestination().point().lat().doubleValue(),
                        appointment.getDestination().point().lng().doubleValue()
                );

                if (distToDest < GeoConstants.ARRIVAL_THRESHOLD_METERS) {
                    // 출발 알람 후 이동 중 100m 진입 → 바로 ARRIVED (도착 확인 불필요)
                    participant.updateCurrentPos(newPoint);
                    participant.updateStatus(ParticipantStatus.ARRIVED);
                    // ARRIVED 시 Appointment ACTIVE + 전원 도착 시 FINISHED
                    if (appointment.getAppointmentStatus() == AppointmentStatus.WAITING) {
                        appointment.updateStatus(AppointmentStatus.ACTIVE);
                    }
                    if (participantRepository.countNotArrivedByAppointmentId(appointmentId) == 0) {
                        appointment.updateStatus(AppointmentStatus.FINISHED);
                    }
                } else {
                    double distFromAnchor = GeoUtils.calculateDistance(
                            newLat.doubleValue(), newLng.doubleValue(),
                            participant.getCurrentPos().lat().doubleValue(),
                            participant.getCurrentPos().lng().doubleValue()
                    );
                    if (distFromAnchor >= GeoConstants.DEPARTURE_THRESHOLD_METERS) {
                        // 앵커로부터 300m 이탈 → MOVING
                        participant.updateCurrentPos(newPoint);
                        participant.updateStatus(ParticipantStatus.MOVING);
                        // MOVING 시 Appointment ACTIVE로 전환
                        if (appointment.getAppointmentStatus() == AppointmentStatus.WAITING) {
                            appointment.updateStatus(AppointmentStatus.ACTIVE);
                        }
                    }
                    // 300m 미만이면 DEPARTING 유지, 좌표 갱신 안 함 (앵커 보존)
                }
            }
            case MOVING -> {
                double distToDest = GeoUtils.calculateDistance(
                        newLat.doubleValue(), newLng.doubleValue(),
                        appointment.getDestination().point().lat().doubleValue(),
                        appointment.getDestination().point().lng().doubleValue()
                );

                participant.updateCurrentPos(newPoint);
                // TODO: 플라스크 호출 → ETA 재계산 → estimatedArrival 갱신 (대시보드용)

                if (distToDest < GeoConstants.ARRIVAL_THRESHOLD_METERS) {
                    // 이동 중 목적지 100m 이내 진입 → 바로 ARRIVED (도착 확인 불필요)
                    participant.updateStatus(ParticipantStatus.ARRIVED);
                    // ARRIVED 시 Appointment ACTIVE + 전원 도착 시 FINISHED
                    if (appointment.getAppointmentStatus() == AppointmentStatus.WAITING) {
                        appointment.updateStatus(AppointmentStatus.ACTIVE);
                    }
                    if (participantRepository.countNotArrivedByAppointmentId(appointmentId) == 0) {
                        appointment.updateStatus(AppointmentStatus.FINISHED);
                    }
                }
            }
            case NEARDEST -> {
                double distToDest = GeoUtils.calculateDistance(
                        newLat.doubleValue(), newLng.doubleValue(),
                        appointment.getDestination().point().lat().doubleValue(),
                        appointment.getDestination().point().lng().doubleValue()
                );

                if (distToDest >= GeoConstants.ARRIVAL_THRESHOLD_METERS) {
                    // 100m 벗어남 (스쳐지나간 케이스)
                    participant.updateCurrentPos(newPoint);
                    //if (P >= Q) {
                    // TODO: DEPARTING 전환 (플라스크 연동 후 departureAlarmTime 채워지면 활성화)
                    //} else {
                    participant.updateStatus(ParticipantStatus.READY);
                    //}
                }
                // 100m 이내면 NEARDEST 유지, 좌표 갱신 안 함
                // - 사용자 확인 → /arrive API → ARRIVED
                // - 미확인 + P >= Q → 앱이 단계별 알람 처리 (서버 상태 변경 없음)
                // - 미확인 + targetTime 초과 → ArrivedTransitionScheduler가 자동 ARRIVED
            }
            default -> {
                // SCHEDULED, ARRIVED 상태에서는 무시
            }
        }

        return ParticipantLocationUpdateResponse.from(participant);
    }

    // 도착 확인 (사용자가 확인 버튼 눌렀을 때 ARRIVED로 전환)
    @Transactional
    public void arrive(Long memberId, Long appointmentId) {
        Participant participant = participantRepository.findWithAppointmentByAppointmentIdAndMemberId(appointmentId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 약속의 참여자가 아닙니다."));

        if (participant.getParticipantStatus() != ParticipantStatus.NEARDEST) {
            throw new IllegalStateException("NEARDEST 상태에서만 도착 확인이 가능합니다.");
        }

        Appointment appointment = participant.getAppointment();
        participant.updateStatus(ParticipantStatus.ARRIVED);

        // ARRIVED 시 Appointment ACTIVE + 전원 도착 시 FINISHED
        if (appointment.getAppointmentStatus() == AppointmentStatus.WAITING) {
            appointment.updateStatus(AppointmentStatus.ACTIVE);
        }
        if (participantRepository.countNotArrivedByAppointmentId(appointmentId) == 0) {
            appointment.updateStatus(AppointmentStatus.FINISHED);
        }
    }

}
