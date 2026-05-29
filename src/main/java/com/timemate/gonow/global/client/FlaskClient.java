package com.timemate.gonow.global.client;

import com.timemate.gonow.global.client.dto.FlaskJourneyRequest;
import com.timemate.gonow.global.client.dto.FlaskJourneyResponse;
import com.timemate.gonow.global.client.dto.FlaskParticipantRequest;
import com.timemate.gonow.global.client.dto.FlaskParticipantResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface FlaskClient {

    @PostExchange("/internal/alarm/journey")
    FlaskJourneyResponse calculateJourneyAlarm(@RequestBody FlaskJourneyRequest request);

    @PostExchange("/internal/alarm/appointment")
    FlaskParticipantResponse calculateParticipantAlarm(@RequestBody FlaskParticipantRequest request);
}
