# 수강 신청 시스템 (BE-A)

## 프로젝트 개요

크리에이터(강사)가 강의를 개설하고, 클래스메이트(수강생)가 수강 신청하는 백엔드 API 서버입니다.

## 기술 스택

- **Java 17** / **Spring Boot 3.5.0**
- **Spring Data JPA** (Hibernate)
- **MySQL 8** (운영) / **H2** (개발 · 테스트)
- **Gradle**

## 실행 방법

### 방법 1. H2 인메모리 DB (MySQL 설치 불필요, 가장 빠름)

```bash
./gradlew bootRun --args='--spring.profiles.active=h2'
```

- DB 설치 없이 바로 실행 가능
- 서버 재시작 시 데이터 초기화됨
- H2 콘솔: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:liveclass`)

### 방법 2. 로컬 MySQL

```bash
# MySQL에 데이터베이스 생성
CREATE DATABASE liveclass;

# src/main/resources/application.yml에서 username, password 수정 후 실행
./gradlew bootRun
```

기본 설정: `localhost:3306/liveclass`, username=`root`, password=`password`

### 방법 3. Docker Compose

```bash
docker compose up
```

서버 포트: `8080`

## 인증 방식

별도 JWT 없이 `X-User-Id` 헤더로 사용자를 식별합니다.

```
X-User-Id: 1
```

## API 목록 및 예시

### 강의(Course)

| Method | URL | 설명 |
|--------|-----|------|
| POST | /api/courses | 강의 등록 |
| GET | /api/courses | 강의 목록 조회 (상태 필터, 페이지네이션) |
| GET | /api/courses/{courseId} | 강의 상세 조회 |
| PATCH | /api/courses/{courseId}/status | 강의 상태 변경 |

**강의 등록**
```json
POST /api/courses
X-User-Id: 1

{
  "title": "Java 기초",
  "description": "Java 입문 강의입니다",
  "price": 50000,
  "maxCapacity": 30,
  "startDate": "2026-06-01",
  "endDate": "2026-08-31"
}
```

**강의 상태 변경** (`DRAFT → OPEN → CLOSED`)
```json
PATCH /api/courses/1/status
X-User-Id: 1

{ "status": "OPEN" }
```

### 수강 신청(Enrollment)

| Method | URL | 설명 |
|--------|-----|------|
| POST | /api/enrollments | 수강 신청 (PENDING) |
| PATCH | /api/enrollments/{id}/confirm | 결제 확정 (CONFIRMED) |
| PATCH | /api/enrollments/{id}/cancel | 수강 취소 |
| GET | /api/enrollments/me | 내 수강 신청 목록 |
| GET | /api/courses/{courseId}/enrollments | 강의별 수강생 목록 (크리에이터 전용) |

**수강 신청**
```json
POST /api/enrollments
X-User-Id: 2

{ "courseId": 1 }
```

### 대기열(Waitlist)

| Method | URL | 설명 |
|--------|-----|------|
| POST | /api/waitlist | 대기열 등록 |
| DELETE | /api/waitlist/{courseId} | 대기열 취소 |

## 데이터 모델 (ERD)

```
course
├── id (PK)
├── title
├── description
├── price
├── max_capacity
├── start_date / end_date
├── creator_id
├── status (DRAFT / OPEN / CLOSED)
└── created_at / updated_at

enrollment
├── id (PK)
├── course_id (FK)
├── user_id
├── status (PENDING / CONFIRMED / CANCELLED)
├── confirmed_at
└── created_at / updated_at

waitlist
├── id (PK)
├── course_id (FK)
├── user_id
├── position (대기 순서)
└── created_at
```

## 요구사항 해석 및 가정

- **인증**: 실제 인증 시스템 없이 `X-User-Id` 헤더로 사용자 식별 (과제 허용 사항)
- **결제**: 외부 결제 연동 없이 `/confirm` API 호출로 상태 변경
- **취소 기간**: CONFIRMED 후 7일 이내만 취소 가능 (`application.yml`에서 설정 변경 가능)
- **대기열 승격**: 수강 취소 시 대기열 1순위가 자동으로 PENDING 상태로 신청됨

## 설계 결정과 이유

### 동시성 제어: 비관적 락(Pessimistic Lock)
정원 마지막 자리에 여러 사용자가 동시 신청하는 race condition을 방지하기 위해 수강 신청 시 `SELECT FOR UPDATE`로 Course 행을 잠급니다.
낙관적 락 대비 재시도 로직이 불필요하고, 정원 초과가 절대 발생하지 않음을 보장합니다.

### 상태 전이 검증을 엔티티 내부에서 처리
`Course.transitionTo()`, `Enrollment.confirm()`, `Enrollment.cancel()` 처럼 상태 변경 로직을 엔티티에 위치시켜 서비스 계층에서 실수로 상태 검증을 빠뜨리는 것을 방지합니다.

## 미구현 / 제약사항

- 실제 사용자 테이블 없음 (userId를 헤더로만 관리)
- 실제 결제 시스템 연동 없음
- 대기열 취소 후 position 재정렬 없음 (신규 등록 시 MAX+1 방식으로 순서 유지)

## 테스트 실행 방법

```bash
# 전체 테스트 (H2 인메모리 DB 사용, MySQL 불필요)
./gradlew test

# 테스트 리포트
open build/reports/tests/test/index.html
```

| 테스트 클래스 | 테스트 수 | 설명 |
|---|---|---|
| CourseServiceTest | 5 | 강의 CRUD, 상태 전이 |
| EnrollmentServiceTest | 9 | 수강 신청 전체 시나리오 |
| EnrollmentConcurrencyTest | 2 | 동시 신청 race condition 검증 |
| WaitlistServiceTest | 4 | 대기열 등록/취소 |

## AI 활용 범위

Claude Code(claude-sonnet-4-6)를 사용하여 초기 코드 구조 및 보일러플레이트 생성에 활용했습니다.
비관적 락 전략, 상태 전이 설계, 대기열 승격 로직, 동시성 테스트 설계는 직접 검토하고 수정했습니다.
