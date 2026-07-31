# Signal Server

**AI 기반 딥페이크 피해 예방·탐지·대응 솔루션 Signal**의 백엔드 서버입니다.
(Spring Boot 3.4 / Java 21 / Gradle)

> 2026 전국마이스터고 스타프로젝트 — 팀 소마어스 (광주소프트웨어마이스터고등학교)

## 프로젝트 소개

생성형 AI 기술의 발전으로 누구나 손쉽게 타인의 얼굴을 합성한 딥페이크 이미지·영상을 만들 수 있게 되면서, 딥페이크를 악용한 성범죄·명예훼손·사기 피해가 빠르게 늘고 있습니다. 그러나 대부분의 사용자는 자신의 사진이 악용될 수 있다는 사실조차 인지하지 못하며, 피해가 발생한 이후에도 어디에 신고해야 하는지 몰라 대응 시점을 놓치는 경우가 많습니다.

Signal은 사용자가 SNS에 사진을 올리기 전 위험 요소를 진단하는 **예방**, 이미 유포된 사진의 도용·합성 여부를 확인하는 **탐지**, 피해 발생 시 상담부터 신고, 기관 연결까지 지원하는 **대응**을 하나로 통합한 원스톱 서비스를 목표로 합니다.

### 시스템 구성

| 모듈 | 기능 |
| --- | --- |
| 예방 | 사진 위험도 사전 진단(AI), 이미지 보호 처리(적대적 노이즈 삽입) |
| 탐지 | 얼굴 모니터링(도용 추적), 정밀 딥페이크 탐지 |
| 대응 | 익명 상담 챗봇, 신고 문서 자동 작성, 실제 기관 연결 |

### 팀 소마어스

| 이름 | 역할 |
| --- | --- |
| 류수연 | 팀장 / App 개발 |
| 이효은 | 프론트엔드 |
| 정종윤 | 백엔드 |
| 추혜인 | 디자인 |

## 실행

```bash
./gradlew bootRun
```

- 기본 프로필 `local`: MySQL `localhost:3306/signal` (기본 계정 root/1234, `DB_USERNAME`/`DB_PASSWORD` 환경변수로 변경 가능)
- 운영 프로필 `prod`: 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` 필요
- 배포 관련 상세 내용은 [DEPLOY.md](./DEPLOY.md) 참고

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
3. PATCH /api/v1/auth/password             { email, verificationToken, newPassword, newPasswordConfirm }
   → 비밀번호 변경 완료 (토큰은 1회용, 10분 유효)
```

회원가입도 동일 플로우로 `purpose: "SIGNUP"` 사용 → 발급받은 verificationToken을 `/auth/signup`에 포함.

## API 개요

| 도메인 | 엔드포인트 | 설명 |
| --- | --- | --- |
| Auth | `/api/v1/auth/**` | 로그인 / 회원가입 / 토큰 재발급 / 로그아웃 / 인증번호·비밀번호 재설정 |
| User | `/api/v1/users/**` | 내 정보 조회·수정·탈퇴, 프로필 이미지 업로드 |
| RiskAssessment | `/api/v1/risk-assessments` | 사진 위험도 사전 진단 (목록/단건 조회 포함) |
| Protection | `/api/v1/protections` | 이미지 보호 처리(노이즈 삽입), 결과 다운로드 |
| Monitoring | `/api/v1/monitorings` | 얼굴 모니터링(도용 추적), 탐지 결과 조회/삭제 |
| DeepfakeDetection | `/api/v1/deepfake-detections` | 정밀 딥페이크 탐지 (비로그인 5회 제한) |
| Chat | `/api/v1/chat/sessions/**` | 익명 상담 챗봇 세션·메시지·상담 요약 |
| Report | `/api/v1/reports/**` | 신고 문서 자동 작성, 증거 업로드, 최종 제출 |
| Agency | `/api/v1/agencies` | 실제 기관 연결(딥링크), 연결 이력 기록 |
| Health | `/health` | 인증 없이 접근 가능한 헬스체크 |

각 AI 연동 기능(위험도 분석, 이미지 보호, 얼굴 모니터링, 딥페이크 탐지, 챗봇)은 인터페이스로 분리되어 있으며, 현재는 `Stub*`/`RuleBased*` 구현체로 동작합니다 (AI 서버 연동 전 임시 구현).

## 패키지 구조

```
com.signal
├── global
│   ├── config      # SecurityConfig 등
│   ├── security    # JwtProvider, JwtAuthenticationFilter
│   ├── exception   # ErrorCode, SignalException, GlobalExceptionHandler
│   ├── response    # 공통 응답/에러 포맷
│   ├── file        # 파일 업로드 공통 처리
│   ├── mail        # 인증번호 메일 발송
│   ├── util        # 공통 유틸
│   └── health      # 헬스체크
└── domain
    ├── auth               # 로그인/회원가입/토큰
    ├── user               # 회원 정보
    ├── riskassessment     # 사진 위험도 진단
    ├── protection         # 이미지 보호 처리
    ├── monitoring         # 얼굴 모니터링
    ├── deepfakedetection  # 정밀 딥페이크 탐지
    ├── chat               # 익명 상담 챗봇
    ├── report             # 신고 문서 자동 작성
    └── agency             # 실제 기관 연결
```

각 도메인은 `controller / service / dto / entity / repository` 구조를 따르며, AI 연동이 필요한 도메인은 전략 인터페이스 디렉터리(`analyzer` / `protector` / `monitor` / `detector` / `engine` / `generator`)를 별도로 둡니다.
