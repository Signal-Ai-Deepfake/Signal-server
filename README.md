# Signal Server

딥페이크 피해 예방·대응 서비스 Signal 백엔드 (Spring Boot 3.4 / Java 21 / Gradle)

## 실행

```bash
./gradlew bootRun
```

- 기본 프로필 `local`: MySQL `localhost:3306/signal` (기본 계정 root/1234, `DB_USERNAME`/`DB_PASSWORD` 환경변수로 변경 가능)
- 운영 프로필 `prod`: 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` 필요

### DB 준비

```sql
CREATE DATABASE signal DEFAULT CHARACTER SET utf8mb4;
```

> Gradle Wrapper가 없다면 IntelliJ에서 프로젝트 열면 자동 생성되거나, `gradle wrapper` 실행.

### Gmail 인증번호 발송 설정

1. Google 계정 → 보안 → **2단계 인증** 활성화
2. [앱 비밀번호](https://myaccount.google.com/apppasswords) 발급 (16자리)
3. 환경변수 설정 후 실행:

```bash
export MAIL_USERNAME=본인주소@gmail.com
export MAIL_PASSWORD=발급받은앱비밀번호   # 일반 로그인 비밀번호 아님!
export JWT_SECRET=$(openssl rand -base64 48)
```

> IntelliJ에서는 Run Configuration → Environment variables에 넣으면 됨.

### 비밀번호 재설정 플로우

```
1. POST /api/v1/auth/verification/send    { email, purpose: "PASSWORD_RESET" }
   → Gmail로 6자리 인증번호 발송 (5분 유효)
2. POST /api/v1/auth/verification/verify  { email, code, purpose: "PASSWORD_RESET" }
   → 성공 시 { verificationToken } 반환 → 프론트는 재설정 화면으로 이동
3. PATCH /api/v1/auth/password            { email, verificationToken, newPassword, newPasswordConfirm }
   → 비밀번호 변경 완료 (토큰은 1회용, 10분 유효)
```

회원가입도 동일 플로우로 `purpose: "SIGNUP"` 사용 → 발급받은 verificationToken을 `/auth/signup`에 포함.

## 구현된 것

- JWT (Access/Refresh) 인증 필터 + Security 설정
- 공통 에러 응답 (`ErrorCode` + `GlobalExceptionHandler`)
- Auth: 로그인 / 회원가입 / 토큰 재발급 (`/api/v1/auth/**`)
- 인증번호 메일 발송·확인 (`/auth/verification/*`) + 비밀번호 재설정 (`/auth/password`)
- User 엔티티 + 리포지토리

## 패키지 구조

```
com.signal
├── global
│   ├── config        # SecurityConfig
│   ├── security/jwt  # JwtProvider, JwtAuthenticationFilter
│   ├── exception     # ErrorCode, SignalException, GlobalExceptionHandler
│   └── response      # ErrorResponse
└── domain
    ├── auth          # controller / service / dto
    └── user          # entity / repository
```

## TODO (API 명세서 기준)

- [x] `/auth/verification/*` 인증번호 발송·확인
- [x] `/auth/password` 비밀번호 재설정
- [ ] `/users/me/profile-image` 프로필 업로드 (S3 등)
- [x] `/risk-assessments` 사진 위험도 사전 진단 (AI 서버 연동) — 현재는 `RiskAnalyzer` 스텁 구현체 (`StubRiskAnalyzer`)
- [x] `/protections` 이미지 보호 처리 (노이즈 삽입) — 현재는 `ImageProtector` 스텁 구현체 (`StubImageProtector`, `@Async`)
- [x] `/monitorings` 얼굴 모니터링 (도용 추적) — 현재는 `FaceMonitor` 스텁 구현체 (`StubFaceMonitor`, `@Async`)
- [x] `/deepfake-detections` 정밀 딥페이크 탐지 — 현재는 `DeepfakeDetector` 스텁 구현체 (`StubDeepfakeDetector`, `@Async`)
- [x] `/chat` 익명 상담 챗봇 — 현재는 `ChatEngine` 룰 기반 스텁 구현체 (`RuleBasedChatEngine`)
- [x] `/reports` 신고 문서 자동 작성 — 현재는 `ReportDocumentGenerator` 스텁 구현체 (`StubReportDocumentGenerator`, URL만 생성)
- [ ] `/agencies` 실제 기관 연결
