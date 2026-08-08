# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

**GoNow** (TimeMate) — 실시간 위치 기반 약속/여정 관리 플랫폼. 모바일 앱(안드로이드/iOS)을 대상으로 하는 Spring Boot REST API 서버.

## 빌드 및 실행 명령어

```bash
# 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 단일 테스트 클래스 실행
./gradlew test --tests "com.timemate.gonow.GonowApplicationTests"

# 애플리케이션 실행 (MySQL 먼저 기동 필요)
./gradlew bootRun

# MySQL 컨테이너 기동
docker compose up -d

# MySQL 컨테이너 종료
docker compose down
```

## 기술 스택

- **Java 21** / **Spring Boot 4.0.6** / **Gradle**
- **인증**: Spring Security + JWT (JJWT 0.13)
- **ORM**: JPA/Hibernate + QueryDSL 7.1
- **DB**: MySQL 8.4 (Docker), Redis
- **외부 API 호출**: Spring RestClient

## 아키텍처

### 계층 구조
```
Controller → Service → Repository → Entity → MySQL
```

### 서버 간 통신 구조 (C 방식)
좌표 계산(ETA, 출발 알람 시각 역산)이 필요한 경우 아래 흐름을 따른다.

```
프론트 → 스프링(DB 저장) → 플라스크(지도 API 호출 + 계산) → 스프링(결과 저장) → 프론트
```

- **프론트**: GPS/지오코딩으로 현재 위치·목적지 좌표 획득 후 스프링에 전달
- **스프링**: DB 저장 및 플라스크 요청/응답 중계 (게이트웨이 역할)
- **플라스크**: ODsay/카카오맵 등 외부 지도 API 호출 + ETA·출발 알람 시각 계산
- 플라스크는 스프링만 바라보고, 스프링은 프론트만 바라본다 (플라스크 ↔ 프론트 직접 통신 없음)

### 출발 알람 시각 계산 정책

#### 출발지 선택 정책
알람 생성 시점에는 출발지를 저장하지 않는다. 출발지는 당일 READY 전환 시 GPS로 측정한 현재 위치로 최초 확정된다. 엔티티에 origin 필드 없음 — current_lat/lng만 사용.

#### 출발지 기준 시점별 정책
| 시점 | 출발지 기준 | GPS 사용 | 비고 |
|---|---|---|---|
| 알람 생성 시점 | 없음 (미정) | X | 출발지 없이 생성 |
| 당일 새벽 4시 (READY 전환) | 실제 현재 위치 | O (1회) | 정확한 값으로 확정, FCM 통보 |
| READY 상태 500m 이탈마다 | 실제 현재 위치 | O (연속) | 실시간 재계산 |

#### 이동 수단별 정확도
- **대중교통**: `target_time` 기준 시간표로 계산 → 생성 시점부터 비교적 정확
- **자가용**: 요청 시점의 실시간 교통 기반 (미래 시점은 통계 기반 예측) → 당일 READY 전환 시 재계산이 실질적 의미

#### 막차 모드 생성 시점 검증
- 귀가 여정 생성/수정(`JourneyService.validateLastTrainNotAlreadyMissed`) 시, `is_last_mode=true` + `plan_date=오늘`(즉시 READY 전환)이면서 현재 시각이 새벽 01시~`scheduler.day-boundary-hour`(기본 4시) 사이면 400 에러("오늘 밤 막차는 이미 지났습니다.") 즉시 반환
- 플라스크의 막차 탐색 창이 23시~다음날 01시로 고정이라, 위치 정보 없이도 이 시간대엔 무조건 막차가 지난 것으로 판단 가능(생성 시점엔 출발지 GPS가 없어 플라스크 호출 자체가 불가하지만, 이 판단은 GPS가 필요 없음)
- `plan_date`가 미래인 경우는 검증 대상 아님 — 실제 계산은 그날 READY 전환 후 이뤄지므로 지금 시각과 무관
- 이 검증은 "지금 당장 만드는" 케이스에 대한 즉시 피드백용 보조 장치이며, `/location` 첫 GPS 수신 시점의 동일 검증(플라스크 `alarm.py`)이 모든 경우(미래 생성, 반복 여정 등)를 커버하는 최종 안전망

#### 스케줄러
- **매일 새벽 4시** `@Scheduled` cron으로 실행
- 당일(`plan_date = 오늘`) `SCHEDULED` 상태 여정/약속 → `READY` 일괄 전환 (출발지는 앱이 첫 GPS 보낼 때 확정)
- 반복 여정(`repeat_days` 비트마스크로 오늘 요일 포함) + `plan_date <= 오늘` → `ARRIVED` 포함 `READY` 전환
- **`isActive` 무관하게 항상 READY로 전환** — 스위치 OFF여도 상태는 오늘에 맞게 갱신

#### status와 isActive 분리 원칙
- **`status`** — 서버/스케줄러가 관리하는 팩트 ("오늘 있는 약속인가")
- **`isActive`** — 사용자가 관리하는 의도 ("알람을 울릴 것인가")
- **GPS/지오펜싱 작동 조건**: `status = READY AND isActive = true` — 앱이 두 값을 보고 판단
- 사용자가 `isActive = false` → 상태는 스케줄러가 READY로 올려놓지만 앱은 GPS를 깨우지 않음
- 사용자가 나중에 `isActive = true`로 켜면 → 이미 READY 상태이므로 앱이 즉시 GPS 가동 가능

#### 알람 수정 시 재계산 정책
- 개인/귀가: 알람 수정(PUT/PATCH) 시 `departureAlarmTime`과 현재 위치 앵커(`current_lat/lng`)를 `null`로 리셋 → 다음 GPS 수신 시 READY 단계부터 재계산 유도(`JourneyService`에서 `updateDepartureAlarmTime(null)` + `updateCurrentPoint(null)` 호출)
- 그룹(방장의 약속 정보 수정, `AppointmentService.updateAppointment()`): 날짜/시간/목적지가 실제로 바뀐 경우에만 참가자 전원을 리셋한다 — `bulkResetAlarmInfoByAppointmentId`(앵커) + `bulkResetStatusByAppointmentId`(SCHEDULED/READY/DEPARTING 상태 재조정, MOVING 이상 제외) + 방장 제외 전원에게 `participant_status` FCM 발송. 참가자별 이동수단은 서로 독립적으로 계산되므로(다른 참가자 경로에 영향 없음), 방장이 자기 이동수단만 바꾼 경우엔 참가자 전원을 건드리지 않고 방장 본인 앵커만 리셋한다(변경 여부는 `isScheduleChanged()`로 비교 — 위도/경도는 `BigDecimal` scale 문제 때문에 `equals()` 대신 `compareTo()`로 비교)
- 그룹(참가자 본인 이동수단 변경, `ParticipantService.updateTransportType()`): 실제로 값이 바뀐 경우에만 본인 앵커를 리셋
- 프론트는 방장 수정 FCM(`participant_status: READY`) 수신 시 알람이 이미 실행 중이어도 무조건 `alarmService.start()`로 재시작한다(`app/_layout.tsx`) — 방장은 저장 즉시 로컬에서 강제 재시작되는데 참가자는 다음 자연 폴링(최대 인터벌만큼 지연)까지 갱신이 안 되던 비대칭을 해소한 것. 참가자 본인 이동수단 변경 시에도 저장 즉시 동일하게 `alarmService.start()`로 재시작한다. 이미 실행 중이던 알람을 재시작할 때 `AlarmManager.start()`는 호출부가 `isActive`를 명시하지 않으면 기존 runner의 `isActive`(참가자 개인 알람 스위치)를 그대로 이어받아, 꺼둔 알람이 재시작 때마다 강제로 켜지는 걸 방지한다.

### 패키지 구조
```
com.timemate.gonow/
├── GonowApplication.java
├── domain/
│   ├── common/       # 공용 Embeddable 값 타입 (Location, Point), TransportType Enum
│   ├── member/       # 회원/설정 (Controller, Service, Repository, Entity, DTO, Constant 포함)
│   ├── appointment/  # 약속/참여자 (Controller, Service, Repository, Entity, DTO, Constant 포함)
│   ├── journey/      # 여정 (Controller, Service, Repository, Entity, DTO, Constant 포함)
│   ├── place/        # 장소 (Controller, Service, Repository, Entity, DTO, Constant 포함)
│   └── alarm/        # 알람 조회 (Controller, Service, DTO, Constant 포함 — 별도 Entity 없음)
└── global/
    ├── auth/         # JWT 필터(JwtTokenFilter), 토큰 프로바이더(JwtTokenProvider), @MemberId 어노테이션
    ├── client/       # FlaskClient (HTTP Interface), dto/ (FlaskJourneyRequest/Response, FlaskParticipantRequest/Response, TransportMode)
    ├── config/       # SecurityConfig, RestClientConfig (flask.url baseUrl 설정), QueryDslConfig, FirebaseConfig
    ├── controller/   # AuthController, HealthController
    ├── dto/          # LoginRequest, LoginResponse
    ├── entity/       # BaseTimeEntity (createdAt, updatedAt, touch())
    ├── exception/    # GlobalExceptionHandler
    ├── response/     # ApiResult (success/fail 팩토리 메서드)
    ├── scheduler/    # ReadyTransitionScheduler/Service, ArrivedTransitionScheduler/Service
    ├── service/      # AuthService
    ├── fcm/          # FcmSender (Data/Notification 다중 발송)
    └── util/         # GeoUtils (Haversine 거리 계산)
```

### 보안 흐름

모든 요청은 `JwtTokenFilter`를 통과한다. 필터는 `Authorization: Bearer {token}` 헤더를 파싱하여 `JwtTokenProvider`로 검증하고, 유효하면 `SecurityContext`에 인증 정보를 주입한다. 무효 토큰은 즉시 401을 반환한다.

**공개 엔드포인트** (인증 불필요):
- `GET /health`
- `POST /api/auth/login`
- `POST /api/members` (회원가입)
- `GET /api/members/check?email=` (이메일 중복 확인)
- `GET /api/members/check?nickname=` (닉네임 중복 확인)

### 현재 구현된 API 엔드포인트

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| GET | `/health` | 불필요 | 헬스 체크 |
| POST | `/api/auth/login` | 불필요 | 로그인 (JWT 발급) |
| POST | `/api/auth/logout` | 필요 | 로그아웃 (서버가 FCM 토큰을 null로 처리, 클라이언트 토큰 삭제는 앱 담당) |
| POST | `/api/members` | 불필요 | 회원가입 (MemberSetting 기본값 동시 생성) |
| GET | `/api/members/check?email=` | 불필요 | 이메일 중복 확인 |
| GET | `/api/members/check?nickname=` | 불필요 | 닉네임 중복 확인 |
| GET | `/api/members/me` | 필요 | 내 프로필 조회 (Member + MemberSetting 통합) |
| PATCH | `/api/members/me/nickname` | 필요 | 닉네임 변경 |
| PATCH | `/api/members/me/password` | 필요 | 비밀번호 변경 |
| PATCH | `/api/members/me/fcm-token` | 필요 | FCM 토큰 등록/갱신 |
| PATCH | `/api/members/me/home` | 필요 | 귀가지 등록/수정 |
| PATCH | `/api/members/me/setting` | 필요 | 멤버 설정 변경 (transitType, priorityType, preparationTime) |
| DELETE | `/api/members/me` | 필요 | 회원 탈퇴 (스켈레톤) |
| GET | `/api/places` | 필요 | 장소 목록 조회 (`?place_type=HOME\|DEST`, 미전달 시 전체) |
| POST | `/api/places` | 필요 | 장소 저장 (동일 주소 존재 시 updatedAt만 갱신 — Upsert) |
| DELETE | `/api/places/{placeId}` | 필요 | 장소 삭제 (소유자 검증 포함) |
| POST | `/api/journeys/personal` | 필요 | 개인 여정 생성 |
| PUT | `/api/journeys/personal/{journeyId}` | 필요 | 개인 여정 수정 (소유자 검증 포함) |
| DELETE | `/api/journeys/{journeyId}` | 필요 | 여정 삭제 (소유자 검증 포함, PERSONAL/HOME 공통) |
| PATCH | `/api/journeys/{journeyId}/active` | 필요 | 여정 알람 스위치 ON/OFF |
| POST | `/api/journeys/home` | 필요 | 귀가 여정 생성 (막차/데드라인 공통, is_last_mode로 분기) |
| PUT | `/api/journeys/home/{journeyId}` | 필요 | 귀가 여정 수정 (소유자 검증 포함) |
| POST | `/api/appointments` | 필요 | 그룹 알람 생성 (방장 Participant 동시 생성, 초대코드 서버 자동 생성) |
| POST | `/api/appointments/join` | 필요 | 초대코드로 참여 (FINISHED 상태 차단, 중복 참여 차단) |
| DELETE | `/api/appointments/{appointmentId}` | 필요 | 그룹 알람 삭제 (방장 전용, 모든 Participant 벌크 삭제) |
| PATCH | `/api/appointments/{appointmentId}/participants/active` | 필요 | 참가자 개인 알람 스위치 ON/OFF (본인만) |
| DELETE | `/api/appointments/{appointmentId}/participants/{targetMemberId}` | 필요 | 참가자 탈퇴(본인) 또는 추방(방장) |
| GET | `/api/alarms` | 필요 | 알람 조회 (`?date=` 날짜별 혼합 조회 또는 `?type=PERSONAL\|HOME\|GROUP` 타입별 조회, 둘 중 하나 필수) |
| GET | `/api/journeys/{journeyId}` | 필요 | 여정 상세 조회 (개인/귀가 공통, `journeyType`+`isLastMode`로 프론트 분기) |
| GET | `/api/appointments/{appointmentId}` | 필요 | 그룹 알람 상세 조회 (참여자 본인만, 참가자 목록 포함) |
| GET | `/api/appointments/{appointmentId}/dashboard` | 필요 | 도착 예정 대시보드 조회 (참여자 본인만, 참가자별 participantStatus 포함, estimatedArrival은 최초엔 null, 참가자 GPS 수신(플라스크 연동) 후 채워짐) |
| PATCH | `/api/appointments/{appointmentId}` | 필요 | 그룹 알람 수정 (방장 전용 — 목적지/날짜/시간/이동수단) |
| PATCH | `/api/appointments/{appointmentId}/participants/transport` | 필요 | 이동 수단 변경 (일반 참가자 전용, 방장 호출 시 에러) |
| PATCH | `/api/journeys/{journeyId}/location` | 필요 | 여정 GPS 좌표 수신 + 상태 전이 (응답: journeyStatus, departureAlarmTime, interval, preparationTime, whichStation, boardingTime) |
| PATCH | `/api/journeys/{journeyId}/arrive` | 필요 | 여정 도착 확인 (NEARDEST → ARRIVED, 사용자 확인 버튼) |
| PATCH | `/api/appointments/{appointmentId}/participants/location` | 필요 | 참가자 GPS 좌표 수신 + 상태 전이 (응답: participantStatus, appointmentStatus, departureAlarmTime, estimatedArrival, interval, preparationTime, whichStation, boardingTime) |
| PATCH | `/api/appointments/{appointmentId}/participants/arrive` | 필요 | 참가자 도착 확인 (NEARDEST → ARRIVED, 응답: appointmentStatus) |

### API 응답 형식

모든 응답은 `ApiResult<T>`로 일관된 형식을 유지한다.
JSON 직렬화는 `spring.jackson.property-naming-strategy: SNAKE_CASE` 전역 설정으로 snake_case 변환된다.

```java
// 성공(데이터 있음): ApiResult.success(message, data)
// 성공(데이터 없음): ApiResult.success(message)   → data: null
// 실패: GlobalExceptionHandler가 ApiResult.fail(message) 자동 반환
```

**API 응답 data 반환 원칙:**
- **data 반환**: 조회(GET) API, 또는 서버가 생성한 ID처럼 클라이언트가 알 수 없는 값 (예: 장소 저장 → place_id)
- **data: null**: 변경(PATCH/DELETE) API — 클라이언트가 입력값을 이미 알고 있으므로 불필요

**GlobalExceptionHandler 처리 케이스:**
- `MethodArgumentNotValidException` → 400 Bad Request (Bean Validation 실패)
- `HttpMessageNotReadableException` → 400 Bad Request (잘못된 JSON 형식)
- `MethodArgumentTypeMismatchException` → 400 Bad Request (잘못된 쿼리 파라미터/경로 변수)
- `IllegalArgumentException` → 400 Bad Request (비즈니스 규칙 위반)
- `IllegalStateException` → 400 Bad Request (상태 전이 규칙 위반)
- `DataIntegrityViolationException` → 400 Bad Request (DB 제약 조건 위반 — UNIQUE, NOT NULL, FK)
- `ResourceAccessException` → 503 Service Unavailable (플라스크 서버 연결 실패)
- `ConstraintViolationException` → 400 Bad Request (`@Validated` + `@RequestParam`/`@PathVariable`에 직접 붙인 검증 실패 — `@RequestBody`+`@Valid`의 `MethodArgumentNotValidException`과는 별개)
- `Exception` → 500 Internal Server Error

### 알람 메커니즘 (Universal Journey State Machine)

상세 상태 전이 규칙: `docs/spec/journey-state-machine.md` 참고 (상태머신/알람 로직 작업 시 확인)

### 알람 구현 방식 결정 사항

#### 실시간 통신 방식
- **현재**: 모든 API를 HTTP로 구현
- **대시보드**: 프론트가 폴링 방식으로 구현 (팀원 결정)
- **FCM**: 구현 완료 — `FcmSender` (Data/Notification 두 가지 메서드)

#### GPS 폴링 주기 조절
- `/location` 응답에 `interval`(초) 포함 — 플라스크 호출 시에만 값 설정, 미호출 시 `null`
- 프론트는 `interval != null`이면 폴링 주기를 해당 값으로 갱신, `null`이면 마지막 주기 유지
- 새벽 4시 `ReadyTransitionService`에서 FCM Data 토큰별 개별 발송 → 프론트가 GPS 폴링 시작
- FCM Data 페이로드: `journey_ids: "1,3"` (해당 유저의 당일 여정 ID 콤마 문자열) + `appointment_ids: "2,4"` (해당 유저의 당일 약속 ID 콤마 문자열, 없으면 미포함)

#### 알람 울리기 방식
| 방식 | 작동 조건 | 용도 |
|------|----------|------|
| expo-notifications (로컬 알림) | 앱 꺼져 있어도 작동 (OS 등록) | `departure_alarm_time` + `preparationTime` 기반 단계별 알람 시퀀스 (1→4단계) |
| FCM Data | 앱 포그라운드/백그라운드 | ① 새벽 4시 READY 전환 → GPS 가동 트리거 (`journey_ids`, `appointment_ids`) ② 그룹 참가자/약속 정보 변경 동기화 (`sync_event`, 아래 표 참고) |
| FCM Notification | 앱 완전 종료 포함 | 그룹 알람 도착 예정/완료 알림 (MOVING 진입, ARRIVED 진입 시) — 안드로이드 알림 채널을 `ArrivalChannel` enum으로 분리해서 실어 보냄(아래 FCM 그룹 알람 메시지 절 참고) |

#### 단계별 출발 알람 (프론트 처리)
- 서버가 `/location` 응답으로 `departureAlarmTime` + `preparationTime` 전달
- 프론트가 두 값을 조합해 로컬 알림 등록:
  - 1단계: `departureAlarmTime - preparationTime` (지금 바로 나가야 함)
  - 2~4단계: `preparationTime`을 25%씩 소진하는 시점에 알림

#### FCM 그룹 알람 메시지 (ParticipantService)
| 시점 | 제목 | 내용 |
|------|------|------|
| MOVING 진입 (DEPARTING→MOVING) | 도착 예정 알림 | XXX님이 오전/오후 X시 X분에 도착 예정입니다. |
| ARRIVED (NEARDEST→ARRIVED, 확인 버튼) | 도착 완료 알림 | XXX님이 오전/오후 X시 X분에 도착했습니다. |
| ARRIVED (DEPARTING→ARRIVED, 100m 자동) | 도착 완료 알림 | XXX님이 오전/오후 X시 X분에 도착했습니다. |
| ARRIVED (MOVING→ARRIVED, 100m 자동) | 도착 완료 알림 | XXX님이 오전/오후 X시 X분에 도착했습니다. |

- `isActive = false`인 참가자에게는 위 알림을 발송하지 않음 (`ParticipantRepository.findFcmTokensByAppointmentIdExcluding` 쿼리에 `isActive = true` 조건 포함)
- 도착 예정/완료 알림은 `ArrivalChannel` enum(`EXPECTED`/`COMPLETE`)으로 안드로이드 알림 채널을 분리해서 `FcmSender.sendAllNotification(tokens, title, body, channelId)`에 실어 보냄 — `AndroidConfig`(백그라운드/종료 상태 OS 자동 표시용)와 `data.channel_id`(포그라운드 프론트 수동 재표시용) 양쪽에 실림. 채널ID 문자열은 프론트 `notifications.ts`의 `CHANNEL_BASE`와 반드시 일치해야 함(원래는 출발 단계별 채널을 그대로 재사용해서 소리가 섞이던 버그가 있었음 — 채널 분리로 해결).

#### FCM Data 그룹 참가자/약속 동기화

그룹 알람 화면을 열어둔 다른 참가자에게 변경사항을 실시간 반영하기 위한 용도(상세화면 재조회·강제 종료·목록 새로고침 트리거). 위 표의 도착 알림(Notification)과 달리 화면에 직접 뭔가 표시하지 않고, 프론트가 신호를 받아 API를 다시 호출하게 만드는 것이 목적이다.

| 시점 | sync_event / 필드 | 발송 대상 | 발송 메서드 |
|------|-------------------|-----------|-------------|
| 참가자 참여(join) | `participants_changed` | 기존 참가자 전원(신규 참여자 제외) | `AppointmentService.joinAppointment()` → `sendAllData` |
| 참가자 탈퇴/추방 | `participants_changed` | 남은 참가자 전원(요청자 제외) | `ParticipantService.deleteParticipant()` → `sendAllData` |
| 참가자 추방(대상자 전용) | `removed_from_appointment` | 쫓겨난 당사자 1명 | `ParticipantService.deleteParticipant()` → `sendData`(단건) |
| 이동수단 변경 | `participants_changed` | 본인 제외 나머지 | `ParticipantService.updateTransportType()` → `sendAllData` |
| 방장의 약속 정보 수정(목적지/날짜/시간/이동수단) | `participant_status`(READY/SCHEDULED) | 방장 제외 나머지 | `AppointmentService.updateAppointment()` → `sendAllData` |
| 약속 삭제 | `appointment_deleted` | 방장 제외 나머지 | `AppointmentService.deleteAppointment()` → `sendAllData` |

- 모든 이벤트에 `appointment_id` 필드 포함. `findFcmTokensByAppointmentIdExcluding` 사용 시 위와 동일하게 `isActive = false` 참가자는 제외됨(단, `removed_from_appointment` 단건 발송은 이 쿼리를 쓰지 않으므로 `isActive`와 무관하게 항상 발송)
- 약속 삭제/추방 시 OS에 이미 등록된 로컬 단계별 알람 취소는 아직 미구현 (`BUGS.md` 버그14 참고)

### 도메인 설계 원칙

- **단방향 연관관계**: 모든 엔티티는 단방향 참조만 사용 (역방향 참조 없음)
- **Embeddable 값 타입**: `Location`(name + address + Point), `Point`(lat + lng)를 엔티티에 재사용. `Location`의 `@Column` 제약은 Location/Point 선언부에 정의하고, 컬럼명 변경이 필요한 경우(Member의 home_*)에만 `@AttributeOverride` 사용.
- **Enum 상태 관리**: `AppointmentStatus`, `JourneyStatus`, `ParticipantStatus` 등
- **Record DTO**: Java Record 타입으로 불변 DTO 정의
- **생성자 기본값**: `@Builder` 생성자에서 `Objects.requireNonNullElse`로 기본값 처리 — 필드 초기화 대신 생성자 초기화를 사용한다. 파라미터는 래퍼 타입(Boolean, Integer 등)으로 받고 필드는 원시 타입(boolean, int)으로 유지한다.
- **BaseTimeEntity**: 모든 엔티티가 상속하는 감사 엔티티. `createdAt`, `updatedAt` 자동 관리. `touch()` 메서드로 `updatedAt`을 현재 시각으로 갱신 (Upsert 시 활용).
- **Upsert 패턴**: 동일 조건(member + 주소 등) 중복 시 새 레코드 생성 대신 기존 레코드의 `touch()`로 최신화하는 방식 사용 (PlaceService 참고).

### 엔티티 현황

| 엔티티 | 위치 | 주요 필드 | Controller/Service 존재 |
|--------|------|-----------|------------------------|
| Member | domain/member/entity | email, password, nickname, home(Location, NOT NULL), fcmToken(nullable) | O (MemberController, MemberService) |
| MemberSetting | domain/member/entity | transitType, priorityType, preparationTime | O (MemberSettingController, MemberSettingService) |
| Appointment | domain/appointment/entity | inviteCode, title, destination(Location), planDate, targetTime, appointmentStatus | O (AppointmentController, AppointmentService) |
| Participant | domain/appointment/entity | member, appointment, isHost, transportType, participantStatus, isActive | O (ParticipantController, ParticipantService) |
| Journey | domain/journey/entity | member, journeyType, isLastMode, planDate, destination(Location), transportType, targetTime(막차 모드에서는 nullable), repeatDays, isActive, journeyStatus | O (JourneyController, JourneyService) |
| Place | domain/place/entity | member, placeType, location(Location) | O (PlaceController, PlaceService) |

### MemberSetting 생성 규칙

- 회원가입(`signUp()`) 시 `MemberSetting`이 기본값(transitType=ALL, priorityType=MIN_TIME)으로 **자동 생성**된다. 단, `preparationTime`은 회원가입 시 클라이언트가 직접 입력하므로 기본값 없음.
- 클라이언트가 별도로 설정 생성 요청을 보낼 필요 없음.
- `PATCH /api/members/me/setting`은 항상 수정(update)만 수행한다.

### MemberSettingRepository 조회 메서드

- `findByMemberId(Long memberId)` — 기본 조회 (Member LAZY 로드)
- `findWithMemberByMemberId(Long memberId)` — `@EntityGraph`로 Member까지 한 번에 조회 (N+1 방지). `getMyProfile()`에서 사용.

### PlaceRepository 조회 메서드

- `findAllByMember(Long memberId)` — 전체 목록 조회, `updatedAt DESC` 정렬
- `findAllByMemberAndType(Long memberId, PlaceType placeType)` — 타입 필터링 조회, `updatedAt DESC` 정렬
- `findByMemberAndAddress(Long memberId, String address)` — Upsert 중복 확인용 (동일 회원 + 주소 조회)
- `findByIdAndMemberId(Long placeId, Long memberId)` — 삭제 전 소유자 검증용

### ParticipantRepository 조회 메서드

- `findHostWithAppointment(appointmentId, memberId)` — 방장 권한 확인 + Appointment fetch join (`@Query`)
- `findByAppointmentIdAndMemberId(appointmentId, memberId)` — 본인 Participant 조회 (탈퇴/추방/알람스위치용)
- `findWithAppointmentByAppointmentIdAndMemberId(appointmentId, memberId)` — 본인 Participant + Appointment fetch join (`@EntityGraph`, 상세 조회용)
- `findAllByAppointmentId(appointmentId)` — 전체 참가자 조회 + Member·Appointment fetch join (`@EntityGraph`, 상세 조회용)
- `existsByAppointmentIdAndMemberId(appointmentId, memberId)` — 중복 참여 확인
- `findAllByAppointmentIdAndMemberIdIn(appointmentId, memberIds)` — 탈퇴/추방 시 요청자+대상자 한 번에 조회
- `bulkDeleteByAppointmentId(appointmentId)` — 약속 삭제 시 전체 참가자 벌크 삭제
- `findAllByMemberId(memberId)` — 내가 참여한 약속 전체 조회 + Appointment fetch join (알람 타입별 조회용)
- `findAllByMemberIdAndPlanDate(memberId, planDate)` — 내가 참여한 약속 날짜별 조회 + Appointment fetch join (알람 날짜별 조회용)
- `countByAppointmentId(appointmentId)` — 약속별 참가자 수 조회
- `bulkUpdateToReady(today)` — 당일 SCHEDULED → READY 벌크 전환 (새벽 4시 스케줄러용)
- `bulkResetAlarmInfoByAppointmentId(appointmentId)` — 약속 수정 시 참가자 departureAlarmTime + currentPos 일괄 리셋 (앵커 초기화 → 플라스크 재호출 유도)
- `bulkResetStatusByAppointmentId(appointmentId, newStatus)` — 약속 수정 시 SCHEDULED/READY/DEPARTING 참가자 상태 일괄 재조정 (MOVING 이상은 건드리지 않음)
- `countNotArrivedByAppointmentId(appointmentId)` — ARRIVED 아닌 참가자 수 조회 (전원 도착 여부 확인, FINISHED 전환 판단용)
- `findFcmTokensByAppointmentIdExcluding(appointmentId, excludeMemberId)` — 특정 회원 제외 나머지 참가자 FCM 토큰 조회 (null 토큰·`isActive=false` 제외)
- `findAppointmentIdsWithOverdueParticipants(today, now)` — NEARDEST + targetTime 초과 참가자가 속한 약속 ID 조회 (즉시 ARRIVED 자동 전환 대상)
- `bulkUpdateToArrivedByAppointmentIds(appIds)` — 약속 ID 목록 기준 NEARDEST → ARRIVED 벌크 전환
- `findAppointmentIdsWithActiveOverdueParticipants(today, oneHourAgo)` — READY/DEPARTING/MOVING + targetTime+1시간 초과 참가자가 속한 약속 ID 조회 (지각 정리 대상)
- `bulkUpdateActiveToArrivedByAppointmentIds(appIds)` — 약속 ID 목록 기준 READY/DEPARTING/MOVING → ARRIVED 벌크 전환 (지각 정리)
- `findTokenToAppointmentIdsForReadyTransition(today)` — 스케줄러: 당일 READY 전환 대상 참가자를 FCM 토큰별로 그룹화해 (token → appointmentId 콤마 문자열) 맵 조회

### Enum 상수 목록

- `TransitType`: ALL, SUBWAY, BUS (회원 선호 교통수단, domain/member/constant)
- `PriorityType`: MIN_TIME, MIN_TRANSFER, MIN_WALK (경로 우선순위, domain/member/constant)
- `TransportType`: DRIVING, TRANSIT (여정/참여자 이동 수단, domain/common/constant)
- `TransportMode`: DRIVING, SUBWAY, BUS, ALL (플라스크 요청 전용 — TransportType+TransitType 조합, global/client/dto)
- `AppointmentStatus`: WAITING, ACTIVE, FINISHED
- `ParticipantStatus`: SCHEDULED, READY, DEPARTING, MOVING, NEARDEST, ARRIVED (기본값: SCHEDULED)
- `JourneyStatus`: SCHEDULED, READY, DEPARTING, MOVING, NEARDEST, ARRIVED (기본값: SCHEDULED)
- `JourneyType`: HOME, PERSONAL
- `PlaceType`: HOME, DEST
- `AlarmType`: PERSONAL, HOME, GROUP (알람 조회용, domain/alarm/constant)
- `ArrivalChannel`: EXPECTED("gonow-arrival-expected"), COMPLETE("gonow-arrival-complete") (그룹 도착 예정/완료 FCM 알림의 안드로이드 채널ID, domain/appointment/constant — 프론트 notifications.ts의 CHANNEL_BASE와 문자열이 반드시 일치해야 함)

### DTO 네이밍 규칙
- `XxxSaveResponse`: 생성/수정 공통 최소 응답 (`JourneySaveResponse` — `journeyId` + `journeyStatus`)
- `XxxResponse`: 상세 조회 응답 (`JourneyResponse`, `AppointmentResponse`, `DashboardResponse`)
- `XxxCreateResponse`: 생성 전용 응답 (`AppointmentCreateResponse` — `appointmentId` + `inviteCode`)
- `XxxArriveResponse`: 도착 확인 응답 (`ParticipantArriveResponse` — `appointmentStatus`)
- 중첩 record (`ParticipantInfo`, `ParticipantDashboard`): `public` 필수 (Jackson 직렬화)
- 팩토리 메서드: 단일/다중 파라미터 무관하게 모두 `from()` 으로 통일

## 개발 환경 설정

### DB 연결 정보 (로컬)
- URL: `jdbc:mysql://localhost:3306/mydb`
- User: `root` / Password: `pwd1234`
- DDL: `create` (서버 기동 시 테이블 재생성됨 — 데이터 초기화 주의)

### 플라스크 연동 설정
- `flask.url`은 현재 프로필(local/prod) 분리 없이 `application.yml`에 단일 값으로 박혀 있음 — **운영(prod) 값(`http://172.31.34.244:5000`, 플라스크와 같은 EC2의 프라이빗 IP)이 기본값**이라, 로컬에서 WSL 플라스크(`localhost:5000`)로 테스트하려면 이 값을 직접 `http://localhost:5000`으로 바꿔서 실행해야 함(커밋 금지, 로컬 전용 임시 변경)
- 같은 EC2 안에서도 스프링·플라스크가 서로 다른 Docker 컨테이너라 `localhost`로는 서로 못 찾음(컨테이너별 네트워크 네임스페이스 분리) — 그래서 운영 값이 `localhost`가 아니라 호스트의 프라이빗 IP로 되어 있음
- `RestClientConfig`에서 `flask.url`을 `baseUrl`로 설정 → `FlaskClient` 빈 생성
- 플라스크 엔드포인트:
  - `POST /internal/alarm/journey` — 개인/귀가용 (`FlaskJourneyRequest` → `FlaskJourneyResponse`)
  - `POST /internal/alarm/appointment` — 그룹(약속)용 (`FlaskParticipantRequest` → `FlaskParticipantResponse`)
- 요청 필드:
  - 공통: `memberId`, `currentLat/Lng`, `destLat/Lng`, `transportMode(TransportMode enum)`, `priorityType`, `targetTime`, `preparationTime`
  - 개인/귀가 전용: `isLastMode` 포함
- 폴백 정책: BUS/SUBWAY 검색 결과 없으면 ALL로 폴백 (플라스크 내부 처리)
- 응답 필드:
  - 개인/귀가(`FlaskJourneyResponse`): `targetTime`(막차 모드에서만, 데드라인 모드는 null), `departureAlarmTime`, `interval`, `whichStation`, `boardingTime`
  - 그룹(`FlaskParticipantResponse`): `departureAlarmTime`, `estimatedArrival`, `interval`, `whichStation`, `boardingTime`
- `TransportMode` enum: `DRIVING`, `SUBWAY`, `BUS`, `ALL` — `TransportType` + `TransitType` 조합을 단일 값으로 변환해서 전달
- 플라스크 미기동 시 `ResourceAccessException` → 503 반환
- **ODsay `-98` 에러** (출발지↔목적지 700m 이내): 플라스크가 도보 기준 폴백으로 핸들링해야 함 — `departureAlarmTime` + 막차 모드 시 `targetTime` 계산해서 반환 필요

### Firebase 설정
- `firebase.credential: classpath:firebase-service-account.json` — JAR 안에 포함되어야 함
- `firebase-service-account.json`은 `.gitignore`로 git 제외 (민감 정보)
- **로컬**: `src/main/resources/`에 직접 파일 배치
- **EC2(prod)**: GitHub Secret `FIREBASE_SERVICE_ACCOUNT`에 JSON 내용 저장 → CI/CD `deploy.yml`에서 빌드 전 파일 복원 후 JAR에 포함
- `firebase-admin` 의존성에서 `jackson-dataformat-xml` 전이 의존성 exclude 필수 — 미제거 시 Spring이 XML 응답을 JSON보다 우선하는 부작용 발생

### JWT 설정
- 만료 시간: 3000분 (50시간)
- Subject: Member ID (Long)
- 알고리즘: HMAC SHA

### HTTP 테스트 파일
`src/test/http/` 디렉토리에 IntelliJ HTTP Client용 시나리오 파일이 있다.
- `member.http`: 회원가입, 로그인, 프로필 조회, 정보 변경, 귀가지, 설정 등
- `member-error.http`: 회원 관련 에러 케이스
- `place.http`: 장소 API 시나리오 (목록 조회, 저장, 삭제)
- `place-error.http`: 장소 관련 에러 케이스
- `journey.http`: 개인/귀가 여정 생성, 상세 조회, 수정, 삭제, 알람 스위치
- `journey-error.http`: 여정 관련 에러 케이스
- `appointment.http`: 그룹 알람 생성, 참여, 조회, 대시보드, 수정, 삭제 등
- `appointment-error.http`: 그룹 알람 관련 에러 케이스
- `alarm.http`: 알람 목록 조회 (날짜별/타입별), 상세 조회
- `location.http`: GPS 상태 전이 시나리오 (개인 여정 + 그룹 참가자) — 플라스크 서버 기동 필요
- `scheduler.http`: 스케줄러 테스트 시나리오 (회원가입 → 로그인 → 여정/약속 생성 → READY 전환 확인)
- 각 파일은 독립 실행 가능 (자체 회원가입/로그인 포함), 서버 재기동(DB create) 후 실행
- 날짜는 `2026-12-31`로 고정 (과거 날짜 검증 회피용), `scheduler.http`/`alarm.http`는 오늘 날짜 사용

## 구현 현황

### 완료
- 인증: JWT (JwtTokenProvider, JwtTokenFilter), Spring Security (STATELESS)
- 글로벌 예외 처리 (GlobalExceptionHandler), 표준 응답 포맷 (ApiResult + SNAKE_CASE)
- 회원: 회원가입/로그인/프로필/닉네임·비밀번호 변경/FCM 토큰/귀가지/설정/탈퇴(스켈레톤)
- 장소: 목록 조회(타입 필터링), Upsert 저장, 삭제
- 개인/귀가 여정: 생성·수정·삭제·상세 조회·알람 스위치
- 그룹 알람: 생성(초대코드 자동)·참여·수정·삭제·상세 조회·대시보드
- 참가자: 알람 스위치·탈퇴(본인)/추방(방장)·이동수단 변경
- 알람 조회: 날짜별(`?date=`) 혼합 조회, 타입별(`?type=PERSONAL|HOME|GROUP`) 조회
- 스케줄러: 새벽 4시 SCHEDULED/ARRIVED → READY 벌크 전환 + FCM Data 발송, 매분 정각 NEARDEST+targetTime 초과 → 자동 ARRIVED, READY/DEPARTING/MOVING 상태가 targetTime+1시간 초과해도 ARRIVED에 도달 못 하면 자동 ARRIVED(지각 정리, 여정·참가자 공통)
- 여정/참가자 GPS 상태 전이 (`/location`, `/arrive`) — 상태 머신 전체 구현 완료
- FCM: `FcmSender`(Data/Notification), `FirebaseConfig` — READY 트리거·그룹 도착 알림·그룹 참가자/약속 실시간 동기화
- 플라스크 연동: `FlaskJourneyRequest/Response`, `FlaskParticipantRequest/Response`, memberId 포함 — 실제 통신 테스트 완료 (2026-06-03, `docs/status/frontend-impl-status.md` 참고). 이후 `flask.url`이 탈퇴한 팀원 개인 서버를 계속 가리키고 있어 한동안 연동이 조용히 실패했던 이력 있음 — 2026-07-31 프라이빗 IP로 재수정 후 재검증 완료 (`docs/history/resolved-bugs.md` 참고)
- NEARDEST 상태 interval 서버 자체 계산 (시간 기반: 30분↑→120초, 10~30분→60초, 10분↓→30초)

### 미구현
- Refresh Token
- Redis (현재 위치 실시간 저장, Refresh Token 저장 용도)
- 회원 탈퇴 실제 삭제 로직

### 설계 확정 사항
- `Appointment.isActive` 제거 — 방 전체 스위치 불필요, 삭제로 대체
- `Participant.isActive` 유지 — 참가자 개인 알람 ON/OFF (참여 보류 용도)
- FINISHED 상태 약속 재사용 없음 — 새 초대코드로 새 방 생성
- 조회 시 `status != FINISHED` 필터링 (자동 삭제 없음, 이력 보존)
- 현재 위치(current_lat/lng)는 향후 Redis로 이전 예정, 현재는 MySQL
- Appointment ACTIVE 트리거: MOVING 또는 ARRIVED 진입 시만 (DEPARTING 제외 — 아직 출발 전)

## 문서 관리 원칙

#### 내용 동기화
- 도메인 로직(상태 전이, 스케줄러, API 응답 필드, DB 스키마, Repository 메서드)을 변경하면 관련 문서(`CLAUDE.md` 해당 섹션, `docs/spec/*`)도 같은 작업에서 함께 갱신을 검토한다.
- 확신이 없거나 누적된 변경이 많으면 `/sync-docs` 스킬로 문서-코드 및 문서 간 정합성을 전체 점검할 수 있다.
- **`/sync-docs`의 점검 대상은 `spec/`·`status/`·`testing/`·`history/`뿐**이다(전부 실제 자바 소스코드 기준으로 맞춰야 하는 문서). `planning/`·`reference/`는 자바 코드 정합성과 무관한 문서라 대상이 아니다 — 아래 항목 참고.

#### 새 문서는 어디에 둘지
- `docs/spec/` — 지금 유효한 설계/스펙 (DB 스키마, 상태머신 등 자주 안 바뀌는 것)
- `docs/status/` — 특정 시점 상태 스냅샷 (구현 현황, 테스트 결과 등 주기적으로 갱신되는 것)
- `docs/testing/` — 테스트 절차/가이드
- `docs/history/` — 이미 끝났거나 대체된 기록 (해결된 버그, 지난 변경 이력, 구버전 설계안 — 참고용 아카이브)
- `docs/planning/` — "무엇을 할지" 정하는 유동적인 계획/아이디어 자료 (대회 참가 규정, 기능 확장 아이디어 등). 확정된 로드맵이 아니고, 자바 코드와 맞춰야 할 의무도 없음.
- `docs/reference/` — 구현 여부와 무관하게 반복 참조하는 안정적인 외부 스펙/개발 도구 가이드 (외부 API 딥링크 스펙, MCP 서버 사용법 등). `planning/`과 달리 "결정"이 아니라 "사실/사용법" 성격이라 잘 안 바뀜.
- 위 6개 모두 `docs/` 하위 카테고리다. 루트는 `CLAUDE.md`(상시 로드)와 `BUGS.md`(지금도 계속 갱신되는 활성 이슈 트래커)만 유지한다. 그 외 새 md는 원칙적으로 루트가 아니라 `docs/` 하위에 만든다.

#### 새 파일 생성 vs 기존 파일에 추가
- 관련 있는 내용이면 새 파일보다 기존 파일에 섹션을 추가하는 쪽을 우선 고려한다 (파일 수가 늘어날수록 관리 부담도 늘어난다).
- 다음 경우에만 파일을 분리한다: 기존 파일이 과도하게 커져서(대략 150~200줄 이상) 한 문서 안에 이질적인 내용이 섞이거나, 문서 성격(스펙/상태/가이드/이력)이 명백히 다를 때.
- 반대로 성격이 같고 항상 같이 읽히는 문서라면 합친다.

#### `@import` 규칙
- `CLAUDE.md`에서 `@경로`로 문서를 끌어오면 그 문서는 **매 세션 자동으로 전부 로드**된다 — 아주 작고(수십 줄 이하) 모든 세션에 보편적으로 유용한 "색인" 성격의 문서에만 사용한다 (현재는 `docs/README.md`가 유일하게 이 조건을 만족).
- 그 외 문서는 `@` 없이 경로 텍스트로만 언급해서, 필요할 때만 Read로 불러온다. 기본값은 온디맨드이고, `@import`는 예외적으로만 쓴다.

#### 문서 추가/이동/삭제 시 반드시 할 것
- `docs/README.md` 색인에 새 행 추가/경로 수정을 함께 반영한다.
- 애매한 경우 매번 되묻기보다 위 기준으로 스스로 판단하고, 판단 이유를 한 줄로 남긴다.

## 새 도메인 추가 시 체크리스트

1. `domain/{도메인명}/entity/` — JPA Entity 작성
2. `domain/{도메인명}/constant/` — Enum 상수 작성
3. `domain/{도메인명}/repository/` — Spring Data JPA Repository
4. `domain/{도메인명}/service/` — 비즈니스 로직 (`@Transactional` 필수)
5. `domain/{도메인명}/dto/` — 요청/응답 DTO (Record 타입)
6. `domain/{도메인명}/controller/` — REST Controller
7. 인증이 필요 없는 엔드포인트는 `SecurityConfig`의 `permitAll()` 목록에 추가

## 데이터베이스 스키마

전체 테이블 정의: `docs/spec/db-schema.md` 참고 (엔티티/테이블 작업 시 확인)

## 프론트엔드 구현 현황

최신 현황: `docs/status/frontend-impl-status.md` 참고 (프론트-백엔드 연동 작업 시 확인)

## 백그라운드/FCM 테스트 가이드

테스트 절차: `docs/testing/TEST_GUIDE.md` 참고 (GPS 폴링/FCM 테스트 시 확인)

## 문서 색인

@docs/README.md
