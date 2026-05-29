package com.timemate.gonow.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Admin SDK 초기화 설정 클래스
 * <p>
 * 역할:
 * - 서버 시작 시 Firebase를 단 한 번 초기화한다.
 * - application.yml의 firebase.credential 경로에서 서비스 계정 JSON 파일을 읽어
 *   Google 인증 정보(GoogleCredentials)를 생성하고 FirebaseApp에 등록한다.
 * - 이 초기화가 완료되어야 FcmService에서 FCM 푸시 알림을 전송할 수 있다.
 * <p>
 * 주의:
 * - firebase-service-account.json은 Firebase 콘솔에서 발급한 서비스 계정 키 파일이다.
 * - 외부에 절대 노출되면 안 되므로 .gitignore에 반드시 등록해야 한다.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    // application.yml의 firebase.credential 값 주입 (classpath:firebase-service-account.json)
    @Value("${firebase.credential}")
    private Resource credential;

    // 스프링 빈 초기화 직후 자동 실행
    @PostConstruct
    public void initialize() throws IOException {
        // 전역적으로 파이어베이스 앱이 하나도 없을 때만 최초 1번 가동
        if (FirebaseApp.getApps().isEmpty()) {
            // try-with-resources: 파싱 도중 예외가 발생해도 스트림이 반드시 닫힘
            try (InputStream stream = credential.getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(stream))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase 초기화 완료");
            }
        } else {
            // 테스트 코드나 hot-swap 시점에 이 로그가 찍히며 서버의 폭발을 막아줍니다.
            log.info("Firebase 이미 초기화됨 — 건너뜀");
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }
}
