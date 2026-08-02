package com.timemate.gonow.global.fcm;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
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

    // 단일 기기 Data 메시지 발송 (토큰별 개인화 데이터 — 스케줄러 READY 전환 시, 참가자 추방 시 사용)
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
    // channelId: 안드로이드 알림 채널 ID. 프론트(notifications.ts)에 미리 만들어져 있는 채널과
    // 문자열이 반드시 일치해야 함 — 앱 배경/종료 상태에선 AndroidConfig의 channelId로 OS가 직접 사용하고,
    // 포그라운드 상태에선 프론트가 data.channel_id를 읽어 notifee로 수동 재표시할 때 사용함(둘 다 필요).
    public void sendAllNotification(Collection<String> tokens, String title, String body, String channelId) {
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
                .putData("channel_id", channelId)
                .setAndroidConfig(AndroidConfig.builder()
                        .setNotification(AndroidNotification.builder()
                                .setChannelId(channelId)
                                .build())
                        .setPriority(AndroidConfig.Priority.HIGH)
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
