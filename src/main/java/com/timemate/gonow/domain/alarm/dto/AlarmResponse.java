package com.timemate.gonow.domain.alarm.dto;

import com.timemate.gonow.domain.alarm.constant.AlarmType;
import com.timemate.gonow.domain.appointment.constant.AppointmentStatus;
import com.timemate.gonow.domain.common.constant.TransportType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AlarmResponse(
        AlarmType alarmType,                 // PERSONAL, HOME, GROUP
        Long journeyId,                      // PERSONAL/HOME이면 값, GROUP이면 null
        Long appointmentId,                  // GROUP이면 값, 나머지 null
        String destName,                     // 목적지 이름
        LocalDate planDate,                  // 계획한 날짜
        LocalDateTime targetTime,            // 목표 시각 -> 막차 모드이면 null
        LocalDateTime departureAlarmTime,    // 출발 알람 시각
        TransportType transportType,         // 자가용/대중교통
        boolean isActive,                    // 스위치 ON/OFF
        boolean isLastMode,                  // HOME이면 true, 나머지 false
        Integer repeatDays,                  // PERSONAL/HOME이면 값, GROUP이면 null
        AppointmentStatus appointmentStatus, // GROUP이면 값, 나머지 null
        Integer participantCount,            // GROUP이면 값, 나머지 null
        String myStatus                      // 나의 상태 (SCHEDULED, READY, DEPARTING, MOVING, NEARDEST, ARRIVED)
) {}
