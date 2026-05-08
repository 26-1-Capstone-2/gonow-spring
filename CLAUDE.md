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

### 패키지 구조
```
com.timemate.gonow/
├── GonowApplication.java
├── domain/
│   ├── common/       # 공용 Embeddable 값 타입 (Location, Point)
│   ├── member/       # 회원/설정 (Controller, Service, Repository, Entity, DTO, Constant 포함)
│   ├── appointment/  # 약속 및 참여자 (Entity, Constant)
│   ├── journey/      # 여정 (Entity, Constant)
│   └── place/        # 장소 (Controller, Service, Repository, Entity, DTO, Constant 포함)
└── global/
    ├── auth/         # JWT 필터(JwtTokenFilter) 및 토큰 프로바이더(JwtTokenProvider)
    ├── config/       # SecurityConfig, RestClientConfig
    ├── controller/   # AuthController, HealthController
    ├── dto/          # LoginRequest, LoginResponse
    ├── entity/       # BaseTimeEntity (createdAt, updatedAt, touch())
    ├── exception/    # GlobalExceptionHandler
    ├── response/     # ApiResult (success/fail 팩토리 메서드)
    └── service/      # AuthService
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
| POST | `/api/auth/logout` | 필요 | 로그아웃 (클라이언트 토큰 삭제, 스켈레톤) |
| POST | `/api/members` | 불필요 | 회원가입 (MemberSetting 기본값 동시 생성) |
| GET | `/api/members/check?email=` | 불필요 | 이메일 중복 확인 |
| GET | `/api/members/check?nickname=` | 불필요 | 닉네임 중복 확인 |
| GET | `/api/members/me` | 필요 | 내 프로필 조회 (Member + MemberSetting 통합) |
| PATCH | `/api/members/me/nickname` | 필요 | 닉네임 변경 |
| PATCH | `/api/members/me/password` | 필요 | 비밀번호 변경 |
| PATCH | `/api/members/me/home` | 필요 | 귀가지 등록/수정 |
| PATCH | `/api/members/me/setting` | 필요 | 멤버 설정 변경 (transitType, priorityType, preparationTime) |
| DELETE | `/api/members/me` | 필요 | 회원 탈퇴 (스켈레톤) |
| GET | `/api/places` | 필요 | 장소 목록 조회 (`?place_type=HOME\|DEST`, 미전달 시 전체) |
| POST | `/api/places` | 필요 | 장소 저장 (동일 주소 존재 시 updatedAt만 갱신 — Upsert) |
| DELETE | `/api/places/{placeId}` | 필요 | 장소 삭제 (소유자 검증 포함) |

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
- `IllegalArgumentException` → 400 Bad Request (비즈니스 규칙 위반)
- `IllegalStateException` → 400 Bad Request (비즈니스 규칙 위반)
- `Exception` → 500 Internal Server Error

### 알람 메커니즘 (Universal Journey State Machine)

@.claude/universal_journey_state_machine.md

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
| Member | domain/member/entity | email, password, nickname, location(name+address+Point, NOT NULL) | O (MemberController, MemberService) |
| MemberSetting | domain/member/entity | transitType, priorityType, preparationTime | O (MemberSettingController, MemberSettingService) |
| Appointment | domain/appointment/entity | title, destination, targetTime, appointmentStatus | X |
| Participant | domain/appointment/entity | member_id, appointment_id, isHost, participantStatus | X |
| Journey | domain/journey/entity | member_id, journeyType, destination, targetTime, journeyStatus | X |
| Place | domain/place/entity | member_id, placeType, location(name+address+Point) | O (PlaceController, PlaceService, PlaceRepository) |

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

### Enum 상수 목록

- `TransitType`: ALL, SUBWAY, BUS (회원 선호 교통수단)
- `PriorityType`: MIN_TIME, MIN_TRANSFER, MIN_WALK (경로 우선순위)
- `AppointmentStatus`: READY, ACTIVE, FINISHED
- `ParticipantStatus`: READY, MOVING, ARRIVED
- `JourneyStatus`: READY, MOVING, ARRIVED
- `JourneyType`: HOME, PERSONAL
- `PlaceType`: HOME, DEST

## 개발 환경 설정

### DB 연결 정보 (로컬)
- URL: `jdbc:mysql://localhost:3306/mydb`
- User: `root` / Password: `pwd1234`
- DDL: `create` (서버 기동 시 테이블 재생성됨 — 데이터 초기화 주의)

### JWT 설정
- 만료 시간: 3000분 (50시간)
- Subject: Member ID (Long)
- 알고리즘: HMAC SHA

### HTTP 테스트 파일
`src/test/http/` 디렉토리에 IntelliJ HTTP Client용 시나리오 파일이 있다.
- `member.http`: 각 API를 독립적으로 테스트 가능한 시나리오 (회원가입, 로그인, 프로필 조회, 정보 변경, 귀가지, 설정 등)
- `member-error.http`: 회원 관련 에러 케이스 (E-1 ~ E-18)
- `place.http`: 장소 API 시나리오 (목록 조회, 저장, 삭제)
- `place-error.http`: 장소 관련 에러 케이스

## 구현 현황

### 완료
- JWT 기반 인증 시스템 (JwtTokenProvider, JwtTokenFilter)
- Spring Security 통합 (STATELESS, CORS/CSRF 비활성화)
- 글로벌 예외 처리 (GlobalExceptionHandler)
- 표준화된 API 응답 포맷 (SuccessResult, ErrorResult) + SNAKE_CASE JSON 직렬화
- BaseTimeEntity (createdAt, updatedAt JPA Auditing + touch() 메서드)
- 회원(Member): 회원가입, 로그인, 프로필 조회, 닉네임/비밀번호 변경, 탈퇴(스켈레톤)
- 귀가지: 등록/수정
- 멤버 설정(MemberSetting): 회원가입 시 자동 생성, 설정 변경
- 이메일/닉네임 중복 확인
- 장소(Place): 목록 조회(타입 필터링), Upsert 저장(동일 주소 시 touch), 삭제(소유자 검증)

### 미구현 (엔티티만 존재)
- Appointment(약속), Participant(참여자) — Controller/Service/Repository 없음
- Journey(여정) — Controller/Service/Repository 없음
- Refresh Token
- Redis 캐싱 활용
- QueryDSL 복잡 쿼리
- 외부 API 연동 (지도, 경로 최적화 등)
- 회원 탈퇴 실제 삭제 로직

## 새 도메인 추가 시 체크리스트

1. `domain/{도메인명}/entity/` — JPA Entity 작성
2. `domain/{도메인명}/constant/` — Enum 상수 작성
3. `domain/{도메인명}/repository/` — Spring Data JPA Repository
4. `domain/{도메인명}/service/` — 비즈니스 로직 (`@Transactional` 필수)
5. `domain/{도메인명}/dto/` — 요청/응답 DTO (Record 타입)
6. `domain/{도메인명}/controller/` — REST Controller
7. 인증이 필요 없는 엔드포인트는 `SecurityConfig`의 `permitAll()` 목록에 추가

## 데이터베이스 스키마

@.claude/db-schema.md
