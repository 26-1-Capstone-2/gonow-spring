# 데이터베이스 스키마

## 1. member (회원) 테이블
유저의 기본 정보와 계정 상태를 관리

| 컬럼 한글명 | 컬럼 영문명 | 데이터 타입 | 제약 조건 | 설명 |
|---|---|---|---|---|
| 멤버 ID | member_id | BIGINT | PK, AUTO_INCREMENT | 고유 식별자 |
| 이메일 | email | VARCHAR(255) | UNIQUE, NOT NULL | - |
| 비밀번호 | password | VARCHAR(255) | NOT NULL | 암호화된 비밀번호 |
| 닉네임 | nickname | VARCHAR(50) | UNIQUE, NOT NULL | - |
| 집 이름 | home_name | VARCHAR(255) | NOT NULL | 예: '우리집', '본가' |
| 집 주소 | home_address | VARCHAR(255) | NOT NULL | - |
| 집 위도 | home_lat | DECIMAL(10, 8) | NOT NULL | - |
| 집 경도 | home_lng | DECIMAL(11, 8) | NOT NULL | - |

---

## 2. member_setting (사용자 설정) 테이블

| 컬럼 한글명 | 컬럼 영문명 | 데이터 타입 | 제약 조건 | 설명 |
|---|---|---|---|---|
| 설정 ID | setting_id | BIGINT | PK, AUTO_INCREMENT | 고유 식별자 |
| 멤버 ID | member_id | BIGINT | FK(member), UNIQUE, NOT NULL | member와 1:1 관계 |
| 선호 교통수단 | preferred_transit | ENUM | DEFAULT 'ALL', NOT NULL | 필터 1: ALL(상관없음), SUBWAY(지하철), BUS(버스) |
| 경로 우선순위 | priority_type | ENUM | DEFAULT 'MIN_TIME', NOT NULL | 필터 2: MIN_TIME(최단 시간), MIN_TRANSFER(최소 환승), MIN_WALK(최소 도보) |
| 여유시간 | preparation_time | INT | NOT NULL | 사용자가 실제로 나갈 준비를 하는 준비시간 및 여유시간 (단위: 분) |

---

## 3. place (장소) 테이블
유저가 검색한 목적지/귀가지 장소를 관리

| 컬럼 한글명 | 컬럼 영문명 | 데이터 타입 | 제약 조건 | 설명 |
|---|---|---|---|---|
| 장소 ID | place_id | BIGINT | PK, AUTO_INCREMENT | 고유 식별자 |
| 멤버 ID | member_id | BIGINT | FK(member), NOT NULL | member와 1:N 관계 (NOT UNIQUE) |
| 장소 타입 | place_type | ENUM | NOT NULL | HOME(최근 귀가지), DEST(최근 목적지) |
| 장소 이름 | name | VARCHAR(255) | NOT NULL | 예: '우리 집', '학교', '회사' |
| 주소 | address | VARCHAR(255) | NOT NULL | - |
| 위도 | lat | DECIMAL(10, 8) | NOT NULL | - |
| 경도 | lng | DECIMAL(11, 8) | NOT NULL | - |

---

## 4. appointment (약속) 테이블
그룹 만남 모드의 중앙 정보를 관리하는 껍데기

| 컬럼 한글명 | 컬럼 영문명 | 데이터 타입 | 제약 조건 | 설명 |
|---|---|---|---|---|
| 약속 ID | appointment_id | BIGINT | PK, AUTO_INCREMENT | 고유 식별자 |
| 초대 코드 | invite_code | VARCHAR(255) | UNIQUE, NOT NULL | 서버에서 생성 (영대문자+숫자 8자리, O/I/0/1 제외) |
| 약속 제목(후순위) | title | VARCHAR(255) | - | 예: 면접 스터디 |
| 목적지 이름 | dest_name | VARCHAR(255) | NOT NULL | 약속 장소 이름 |
| 목적지 주소 | dest_address | VARCHAR(255) | NOT NULL | 약속 장소 주소 |
| 목적지 위도 | dest_lat | DECIMAL(10, 8) | NOT NULL | - |
| 목적지 경도 | dest_lng | DECIMAL(11, 8) | NOT NULL | - |
| 약속 예정 날짜 | plan_date | DATE | NOT NULL | - |
| 목표 시간 | target_time | DATETIME | NOT NULL | - |
| 약속 상태 | status | ENUM | NOT NULL, DEFAULT 'WAITING' | WAITING(방을 만든 직후), ACTIVE(한명이라도 이동 중이면), FINISHED(모든 친구가 도착하면) |

---

## 5. participant (참여자 / 이동 정보) 테이블
약속에 참여한 인원별 실시간 상태와 ETA 데이터를 담는다

| 컬럼 한글명 | 컬럼 영문명 | 데이터 타입 | 제약 조건 | 설명 |
|---|---|---|---|---|
| 참석자 ID | participant_id | BIGINT | PK, AUTO_INCREMENT | 고유 식별자 |
| 멤버 ID | member_id | BIGINT | FK(member), NOT NULL, 복합 UK | 약속 ID랑 복합 UK를 이룸 |
| 약속 ID | appointment_id | BIGINT | FK(meeting), NOT NULL, 복합 UK | 멤버 ID랑 복합 UK를 이룸 |
| 방장 여부 | is_host | BOOLEAN | NOT NULL, DEFAULT FALSE | TRUE면 방장(권한 있음), FALSE면 일반 참여자 |
| 현재 위도 | current_lat | DECIMAL(10, 8) | - | 유저의 실시간 위도 |
| 현재 경도 | current_lng | DECIMAL(11, 8) | - | 유저의 실시간 경도 |
| 도착 예정 시간 | estimated_arrival | DATETIME | - | 서버가 계산한 도착 예정 시간 |
| 출발 알람 시각 | departure_alarm_time | DATETIME | - (SCHEDULED 상태에서는 null, READY 최초 GPS 수신 시 확정) | 서버가 계산한 출발 알람 시각 |
| 이동 수단 | transport_type | ENUM | NOT NULL | DRIVING(자가용), TRANSIT(대중교통) |
| 참여자 상태 | status | ENUM | NOT NULL, DEFAULT 'SCHEDULED' | SCHEDULED(예약), READY(대기), DEPARTING(출발 준비), MOVING(이동 중), ARRIVED(도착) |
| 알람 활성화 여부 | is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | 개인 알람 ON/OFF |

- 방장이 친구를 초대 → insert
- 방장이 친구를 내보냄 / 친구가 스스로 나감 → delete
- 약속  시간 이후 1시간이 지나도 친구가 도착 안하면 → delete
---

## 6. journey (여정) 테이블
개인/귀가 모드 데이터 관리

| 컬럼 한글명 | 컬럼 영문명 | 데이터 타입 | 제약 조건 | 설명 |
|---|---|---|---|---|
| 여정 ID | journey_id | BIGINT | PK, AUTO_INCREMENT | 고유 식별자 |
| 멤버 ID | member_id | BIGINT | FK(member), NOT NULL | 여정의 주인인 유저 ID |
| 여정 제목(후순위) | title | VARCHAR(255) | - | 예: 기차역, 알바 가는 길 |
| 여정 타입 | journey_type | ENUM | NOT NULL | HOME(귀가 모드), PERSONAL(개인 모드) |
| 현재 위도 | current_lat | DECIMAL(10, 8) | - | 유저의 실시간 위도 |
| 현재 경도 | current_lng | DECIMAL(11, 8) | - | 유저의 실시간 경도 |
| 목적지 이름 | dest_name | VARCHAR(255) | NOT NULL | - |
| 목적지 주소 | dest_address | VARCHAR(255) | NOT NULL | - |
| 목적지 위도 | dest_lat | DECIMAL(10, 8) | NOT NULL | - |
| 목적지 경도 | dest_lng | DECIMAL(11, 8) | NOT NULL | - |
| 이동 수단 | transport_type | ENUM | NOT NULL | DRIVING(자가용), TRANSIT(대중교통) |
| 여정 예정 날짜 | plan_date | DATE | NOT NULL | - |
| 막차 여부 | is_last_mode | BOOLEAN | NOT NULL | TRUE면 막차 시간 기준, FALSE면 직접 입력 기준 |
| 목표 시간 | target_time | DATETIME | NOT NULL | 직접 입력값 혹은 API로 가져온 막차 시각 |
| 도착 예정 시간 | estimated_arrival | DATETIME | - | 서버가 계산한 도착 예정 시간 |
| 출발 알람 시각 | departure_alarm_time | DATETIME | - (SCHEDULED 상태에서는 null, READY 최초 GPS 수신 시 확정) | 서버가 계산한 출발 알람 시각 |
| 반복 요일 | repeat_days | INT | NOT NULL | 요일 반복 비트마스크 (0: 반복 안 함) |
| 여정 알람 스위치 | is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | 여정 활성화 ON/OFF |
| 여정 상태 | status | ENUM | NOT NULL, DEFAULT 'SCHEDULED' | SCHEDULED(예약), READY(대기), DEPARTING(출발 준비), MOVING(이동 중), ARRIVED(도착) |

## 요일 반복 비트 마스크

- 월: 1, 화: 2, 수: 4, 목: 8, 금: 16, 토: 32, 일: 64
- **평일 전체:** 1 + 2 + 4 + 8 + 16 = 31
- **주말 전체:** 32 + 64 = 96
- **매일 반복:** 1 + 2 + 4 + 8 + 16 + 32 + 64 = 127