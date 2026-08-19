package com.timemate.gonow.domain.appointment.dto;

import com.timemate.gonow.domain.member.constant.AlarmSoundMode;

public interface ParticipantTokenSoundModeProjection {
    String getFcmToken();
    AlarmSoundMode getArrivalExpectedSoundMode();
    AlarmSoundMode getArrivalCompleteSoundMode();
}
