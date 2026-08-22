package com.timemate.gonow.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private static final Duration CODE_TTL = Duration.ofMinutes(5); // 인증코드 유효기간(5분)
    private static final Duration VERIFIED_GRACE = Duration.ofMinutes(10); // 인증 성공 후 회원가입/비밀번호 재설정에 재사용 가능한 유예 시간(10분)
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60); // 동일 이메일 재요청 최소 간격 — 남발/스팸성 발송 방지(60초)

    private static final String CODE_KEY_PREFIX = "email-verification:code:"; // 인증코드 저장 Redis 키 접두사
    private static final String VERIFIED_KEY_PREFIX = "email-verification:verified:"; // 인증완료 표시 Redis 키 접두사
    private static final String COOLDOWN_KEY_PREFIX = "email-verification:cooldown:"; // 재전송 제한 Redis 키 접두사

    private static final SecureRandom RANDOM = new SecureRandom(); // 인증코드 난수 생성기

    private final StringRedisTemplate redisTemplate; // 인증코드/쿨다운/인증완료 상태 저장소(레디스)
    private final JavaMailSender mailSender; // SES SMTP 메일 발송기

    // 인증코드 발송 (재발송 시 기존 키를 그대로 덮어씀 — Redis SET이 곧 Upsert)
    public void sendCode(String email) {
        Long remainingSeconds = redisTemplate.getExpire(cooldownKey(email), TimeUnit.SECONDS);
        if (remainingSeconds != null && remainingSeconds > 0) {
            throw new IllegalStateException(remainingSeconds + "초 후 다시 시도해주세요.");
        }

        String code = generateCode();
        redisTemplate.opsForValue().set(codeKey(email), code, CODE_TTL);
        redisTemplate.opsForValue().set(cooldownKey(email), "1", RESEND_COOLDOWN);

        log.info("이메일 인증코드 발송 시도 - email: {}, code: {}", email, code); // 추적용 — SES 발송 실패해도 코드 확인 가능하도록 발송 전에 기록
        sendMail(email, code);
    }

    // 인증코드 확인
    public void confirmCode(String email, String code) {
        String savedCode = redisTemplate.opsForValue().get(codeKey(email));

        // Redis TTL로 자동 만료되므로 "요청 안 함"과 "만료됨"을 구분할 방법이 없다 — 하나의 메시지로 안내
        if (savedCode == null) {
            throw new IllegalStateException("인증코드가 만료되었거나 요청 내역이 없습니다. 다시 요청해주세요.");
        }
        if (!savedCode.equals(code)) {
            throw new IllegalArgumentException("인증코드가 일치하지 않습니다.");
        }

        redisTemplate.delete(codeKey(email)); // 확인된 인증코드는 바로 폐기(1회용)
        redisTemplate.opsForValue().set(verifiedKey(email), "true", VERIFIED_GRACE);
    }

    // 최근 인증 완료 여부 확인 (회원가입/비밀번호 재설정 진입 가드용)
    public boolean isRecentlyVerified(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(verifiedKey(email)));
    }

    // 인증 완료 상태 소진 (비밀번호 재설정처럼 반복 가능한 민감 작업 완료 직후 호출 — 유예시간 안에 같은 인증으로 재차 실행되는 것 방지)
    // 회원가입은 이메일 유니크 제약으로 자연히 1회성이 보장돼서 호출하지 않는다.
    public void invalidateVerification(String email) {
        redisTemplate.delete(verifiedKey(email));
    }

    // 이메일별 인증코드 Redis 키 생성
    private String codeKey(String email) {
        return CODE_KEY_PREFIX + email;
    }

    // 이메일별 인증완료 Redis 키 생성
    private String verifiedKey(String email) {
        return VERIFIED_KEY_PREFIX + email;
    }

    // 이메일별 재전송 제한 Redis 키 생성
    private String cooldownKey(String email) {
        return COOLDOWN_KEY_PREFIX + email;
    }

    // 6자리 인증코드 생성
    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private static final String FROM_ADDRESS = "noreply@gonow-api.uk"; // 인증 메일 발신자 주소

    // 유저의 메일함으로 인증코드 발송
    private void sendMail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage(); // 제목/본문만 담는 가장 단순한 메일 객체(첨부파일·HTML 불가)
        message.setFrom(FROM_ADDRESS); // 미지정 시 JavaMail이 로컬 계정 정보로 자동 채워서 SES가 거부함(도메인 인증된 주소만 발신 가능)
        message.setTo(email); // 수신자
        message.setSubject("[GoNow] 이메일 인증코드"); // 메일 제목
        message.setText("인증코드: " + code + "\n" + CODE_TTL.toMinutes() + "분 이내에 입력해주세요."); // 메일 본문
        mailSender.send(message); // 실제 SMTP 연결 맺고 SES로 전송(application.yml의 spring.mail 설정 사용)
    }
}
