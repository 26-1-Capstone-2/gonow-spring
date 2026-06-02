package com.timemate.gonow.global.fcm;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmSender {
    private final FirebaseMessaging firebaseMessaging; // FirebaseConfig의 빈 주입

    // 단일 기기 Data 메시지 발송 (토큰별 개인화 데이터 — 스케줄러 READY 전환 시 사용)
    public void sendData(String token, Map<String, String> data) {
        if (!StringUtils.hasText(token)) {
            log.warn("유효한 FCM 토큰 없음 — Data 단건 발송 건너뜀");
            return;
        }

        Message message = Message.builder()
                .setToken(token)
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .build();

        try {
            firebaseMessaging.send(message);
            log.info("FCM Data 단건 발송 완료 — token={}, payload={}", token, data);
        } catch (FirebaseMessagingException e) {
            log.error("FCM Data 단건 발송 실패 — token={}, error={}", token, e.getMessage());
        }
    }

    // 다중 기기 Data 메시지 발송 (동일 페이로드 — 약속 수정 시 참가자 일괄 알림)
    public void sendAllData(Collection<String> tokens, Map<String, String> data) {
        List<String> validTokens = tokens.stream()
                .filter(StringUtils::hasText)
                .toList();

        if (validTokens.isEmpty()) {
            log.warn("유효한 FCM 토큰 없음 — Data 다중 발송 건너뜀");
            return;
        }

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(validTokens)
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .build();

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.info("FCM Data 다중 발송 완료 — 성공: {}건, 실패: {}건, payload={}", response.getSuccessCount(), response.getFailureCount(), data);
        } catch (FirebaseMessagingException e) {
            log.error("FCM Data 다중 발송 실패 — {}", e.getMessage());
        }
    }

    // 다중 기기 Notification 메시지 발송 (상단바 알림 — 소리/진동 포함)
    public void sendAllNotification(Collection<String> tokens, String title, String body) {
        List<String> validTokens = tokens.stream()
                .filter(StringUtils::hasText)
                .toList();

        if (validTokens.isEmpty()) {
            log.warn("유효한 FCM 토큰 없음 — Notification 발송 건너뜀");
            return;
        }

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(validTokens)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.info("FCM Notification 발송 완료 — 성공: {}건, 실패: {}건",
                    response.getSuccessCount(), response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            log.error("FCM Notification 발송 실패 — {}", e.getMessage());
        }
    }
}
