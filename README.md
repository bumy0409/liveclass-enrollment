# 수강 신청 시스템 (BE-A)

## 프로젝트 개요

크리에이터(강사)가 강의를 개설하고, 클래스메이트(수강생)가 수강 신청하는 백엔드 API 서버입니다.

## 기술 스택

- **Java 17** / **Spring Boot 3.5.0**
- **Spring Data JPA** (Hibernate)
- **MySQL 8** (운영) / **H2** (개발 · 테스트)
- **Gradle**

## 실행 방법

### 방법 1. H2 인메모리 DB

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

## API 명세

### 공통

- **Base URL**: `http://localhost:8080`
- **인증**: 모든 요청에 `X-User-Id` 헤더 필요
- **Content-Type**: `application/json`

**공통 에러 응답**
```json
{
  "code": "에러코드",
  "message": "에러 메시지"
}
```

| HTTP Status | 설명 |
|-------------|------|
| 200 | 성공 |
| 201 | 생성 성공 |
| 204 | 삭제 성공 (응답 본문 없음) |
| 400 | 잘못된 요청 |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 409 | 충돌 (중복 신청, 정원 초과 등) |

---

### 강의 (Course)

#### POST /api/courses — 강의 등록

**Request**
```
POST /api/courses
X-User-Id: 1
Content-Type: application/json
```
```json
{
  "title": "Java 기초",
  "description": "Java 입문 강의입니다",
  "price": 50000,
  "maxCapacity": 30,
  "startDate": "2026-06-01",
  "endDate": "2026-08-31"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| title | String | ✅ | 강의 제목 |
| description | String | | 강의 설명 |
| price | int | ✅ | 가격 (0 이상) |
| maxCapacity | int | ✅ | 최대 수강 인원 (1 이상) |
| startDate | LocalDate | ✅ | 수강 시작일 (yyyy-MM-dd) |
| endDate | LocalDate | ✅ | 수강 종료일 (yyyy-MM-dd) |

**Response** `201 Created`
```json
{
  "id": 1,
  "title": "Java 기초",
  "description": "Java 입문 강의입니다",
  "price": 50000,
  "maxCapacity": 30,
  "currentEnrollmentCount": 0,
  "startDate": "2026-06-01",
  "endDate": "2026-08-31",
  "status": "DRAFT",
  "creatorId": 1,
  "createdAt": "2026-05-03T10:00:00"
}
```

---

#### GET /api/courses — 강의 목록 조회

**Request**
```
GET /api/courses?status=OPEN&page=0&size=20
X-User-Id: 1
```

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| status | String | | DRAFT / OPEN / CLOSED (없으면 전체) |
| page | int | | 페이지 번호 (기본값 0) |
| size | int | | 페이지 크기 (기본값 20) |

**Response** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "title": "Java 기초",
      "description": "Java 입문 강의입니다",
      "price": 50000,
      "maxCapacity": 30,
      "currentEnrollmentCount": 5,
      "startDate": "2026-06-01",
      "endDate": "2026-08-31",
      "status": "OPEN",
      "creatorId": 1,
      "createdAt": "2026-05-03T10:00:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

---

#### GET /api/courses/{courseId} — 강의 상세 조회

**Request**
```
GET /api/courses/1
X-User-Id: 1
```

**Response** `200 OK`
```json
{
  "id": 1,
  "title": "Java 기초",
  "description": "Java 입문 강의입니다",
  "price": 50000,
  "maxCapacity": 30,
  "currentEnrollmentCount": 5,
  "startDate": "2026-06-01",
  "endDate": "2026-08-31",
  "status": "OPEN",
  "creatorId": 1,
  "createdAt": "2026-05-03T10:00:00"
}
```

**에러 응답**
| 상황 | code | status |
|------|------|--------|
| 강의 없음 | COURSE_NOT_FOUND | 404 |

---

#### PATCH /api/courses/{courseId}/status — 강의 상태 변경

상태 전이: `DRAFT → OPEN → CLOSED` (역방향 불가)

**Request**
```
PATCH /api/courses/1/status
X-User-Id: 1
Content-Type: application/json
```
```json
{ "status": "OPEN" }
```

**Response** `200 OK`
```json
{
  "id": 1,
  "title": "Java 기초",
  "status": "OPEN",
  ...
}
```

**에러 응답**
| 상황 | code | status |
|------|------|--------|
| 강의 없음 | COURSE_NOT_FOUND | 404 |
| 유효하지 않은 전이 | INVALID_STATUS_TRANSITION | 400 |
| 개설자가 아님 | NOT_COURSE_CREATOR | 403 |

---

### 수강 신청 (Enrollment)

#### POST /api/enrollments — 수강 신청

**Request**
```
POST /api/enrollments
X-User-Id: 2
Content-Type: application/json
```
```json
{ "courseId": 1 }
```

**Response** `201 Created`
```json
{
  "id": 1,
  "courseId": 1,
  "userId": 2,
  "status": "PENDING",
  "confirmedAt": null,
  "createdAt": "2026-05-03T10:00:00"
}
```

**에러 응답**
| 상황 | code | status |
|------|------|--------|
| 강의 없음 | COURSE_NOT_FOUND | 404 |
| 신청 불가 상태 | COURSE_NOT_OPEN | 400 |
| 이미 신청함 | ALREADY_ENROLLED | 409 |
| 정원 초과 | COURSE_FULL | 409 |

---

#### PATCH /api/enrollments/{enrollmentId}/confirm — 결제 확정

PENDING → CONFIRMED 상태 변경 (외부 결제 시스템 연동 대체)

**Request**
```
PATCH /api/enrollments/1/confirm
X-User-Id: 2
```

**Response** `200 OK`
```json
{
  "id": 1,
  "courseId": 1,
  "userId": 2,
  "status": "CONFIRMED",
  "confirmedAt": "2026-05-03T10:05:00",
  "createdAt": "2026-05-03T10:00:00"
}
```

**에러 응답**
| 상황 | code | status |
|------|------|--------|
| 신청 없음 | ENROLLMENT_NOT_FOUND | 404 |
| 본인 신청 아님 | ENROLLMENT_NOT_OWNED | 403 |
| PENDING 아님 | INVALID_STATUS_TRANSITION | 400 |

---

#### PATCH /api/enrollments/{enrollmentId}/cancel — 수강 취소

- PENDING: 즉시 취소 가능
- CONFIRMED: 결제 후 7일 이내만 취소 가능
- 취소 시 대기열 1순위 자동 승격

**Request**
```
PATCH /api/enrollments/1/cancel
X-User-Id: 2
```

**Response** `200 OK`
```json
{
  "id": 1,
  "courseId": 1,
  "userId": 2,
  "status": "CANCELLED",
  "confirmedAt": "2026-05-03T10:05:00",
  "createdAt": "2026-05-03T10:00:00"
}
```

**에러 응답**
| 상황 | code | status |
|------|------|--------|
| 신청 없음 | ENROLLMENT_NOT_FOUND | 404 |
| 본인 신청 아님 | ENROLLMENT_NOT_OWNED | 403 |
| 이미 취소됨 | ALREADY_CANCELLED | 400 |
| 취소 기간 초과 | CANCELLATION_PERIOD_EXPIRED | 400 |

---

#### GET /api/enrollments/me — 내 수강 신청 목록

**Request**
```
GET /api/enrollments/me?page=0&size=20
X-User-Id: 2
```

**Response** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "courseId": 1,
      "userId": 2,
      "status": "CONFIRMED",
      "confirmedAt": "2026-05-03T10:05:00",
      "createdAt": "2026-05-03T10:00:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

---

#### GET /api/courses/{courseId}/enrollments — 강의별 수강생 목록 (크리에이터 전용)

PENDING, CONFIRMED 상태만 조회됨

**Request**
```
GET /api/courses/1/enrollments?page=0&size=20
X-User-Id: 1
```

**Response** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "courseId": 1,
      "userId": 2,
      "status": "CONFIRMED",
      "confirmedAt": "2026-05-03T10:05:00",
      "createdAt": "2026-05-03T10:00:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

**에러 응답**
| 상황 | code | status |
|------|------|--------|
| 강의 없음 | COURSE_NOT_FOUND | 404 |
| 개설자가 아님 | NOT_COURSE_CREATOR | 403 |

---

### 대기열 (Waitlist)

#### POST /api/waitlist — 대기열 등록

정원이 꽉 찬 강의에 대기 등록. position이 낮을수록 우선순위 높음

**Request**
```
POST /api/waitlist
X-User-Id: 4
Content-Type: application/json
```
```json
{ "courseId": 1 }
```

**Response** `201 Created`
```json
{
  "id": 1,
  "courseId": 1,
  "userId": 4,
  "position": 1,
  "createdAt": "2026-05-03T10:10:00"
}
```

**에러 응답**
| 상황 | code | status |
|------|------|--------|
| 강의 없음 | COURSE_NOT_FOUND | 404 |
| 신청 불가 상태 | COURSE_NOT_OPEN | 400 |
| 이미 수강 중 | ALREADY_ENROLLED | 409 |
| 이미 대기 중 | ALREADY_ON_WAITLIST | 409 |

---

#### DELETE /api/waitlist/{courseId} — 대기열 취소

**Request**
```
DELETE /api/waitlist/1
X-User-Id: 4
```

**Response** `204 No Content`

**에러 응답**
| 상황 | code | status |
|------|------|--------|
| 대기열에 없음 | NOT_ON_WAITLIST | 404 |

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

**직접 설계 및 구현한 부분**
 - ERD 설계 (Course / Enrollment / Waitlist 테이블 구조 및 관계 정의)
 - 도메인별 패키지 구조 설계 (course / enrollment / waitlist / common 분리)
 - 레이어드 아키텍처 설계 (Controller → Service → Repository)
 - 상태 변경 로직을 Service가 아닌 Entity 내부에 위치시키는 설계 결정 (상태 검증 누락 방지 목적)
 - API 엔드포인트 설계 및 HTTP 메서드 결정
 
 **AI 활용 부분 (Claude Code, GPT 5.5 Thinking)**
 - 반복적인 구현체(Controller, Repository, DTO 등) 코드 초안 생성
 - README 및 API 명세서 문서 작성
 - 동시성 문제 인지 후 비관적 락 방식을 선택하여 적용 (AI와 해결법을 논의한 뒤 비관적 락 채택)
