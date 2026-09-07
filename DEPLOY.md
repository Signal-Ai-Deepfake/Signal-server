# 배포 가이드 (학교 서버 / Docker)

## 자동 배포 (CD)

`main`에 push되면 [.github/workflows/deploy.yml](.github/workflows/deploy.yml)이 자동으로 배포한다.

1. GitHub 러너가 이미지를 빌드해 `ghcr.io/signal-ai-deepfake/signal-server:<sha>`로 push
2. 서버에 SSH 접속 → 해당 커밋으로 `git reset --hard` → 이미지 pull → 컨테이너 교체
3. 헬스체크 실패 시 **이전 이미지로 자동 롤백**

서버가 1 vCPU / 2GB라서 서버에서 직접 빌드하면 배포 중 API가 응답 불능이 될 수 있다.
그래서 빌드는 러너에서 하고 서버는 pull만 한다.

PR을 열면 배포 없이 **빌드만 검증**한다.

### 엔티티에 필드/테이블이 추가된 배포

`prod` 프로필은 `ddl-auto: validate`라서 스키마를 먼저 반영해야 한다.
Actions → CD → **Run workflow**에서 `run_schema_update`를 체크하고 실행한다.
(push로 자동 실행된 배포는 이 단계를 건너뛰므로, 스키마가 안 맞으면 헬스체크에서 실패하고 롤백된다.)

### 서버에 필요한 것

`.env`만 서버에 있으면 된다 (git에 안 올라가므로 최초 1회 직접 생성).
CD는 `.env`를 건드리지 않으며, `git reset --hard`는 gitignore된 `.env`를 지우지 않는다.
단 **추적되는 파일을 서버에서 직접 수정하면 배포 때 덮어써진다.**

### 필요한 GitHub Secrets

| 이름 | 값 |
| --- | --- |
| `SSH_HOST` | 서버 주소 |
| `SSH_PORT` | SSH 포트 |
| `SSH_USER` | 접속 계정 |
| `SSH_PRIVATE_KEY` | 배포용 SSH 개인키 (공개키는 서버 `~/.ssh/authorized_keys`에 등록) |

GHCR 인증은 워크플로의 `GITHUB_TOKEN`을 서버에 전달해 처리하므로 별도 PAT가 필요 없다.

---

## 수동 배포

아래는 CD 없이 직접 배포하거나, CD가 실패했을 때 쓰는 절차다.

## 0. 사전 준비 (서버에 Docker 설치돼있어야 함)

```bash
docker --version
docker compose version
```

둘 다 안 되면 Docker 설치부터:

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
# 위 명령 실행 후 로그아웃 → 재로그인 필요
```

## 1. 코드 받기

```bash
git clone <레포_주소> signal-server
cd signal-server
```

## 2. `.env` 파일 올리기

`.env`는 git에 안 올라가므로(비밀정보 포함) 로컬에서 서버로 직접 복사해야 함.

```bash
scp .env <서버계정>@<서버주소>:~/signal-server/.env
```

scp로 옮기는 게 제일 안전함 (실제 비밀번호/키가 이 문서에는 안 적혀있음 — 로컬 `.env`에만 있음).

> 챗봇이 실제 LLM(Groq)으로 답하게 하려면 `.env`에 `GROQ_API_KEY`를 추가해야 함 (https://console.groq.com 에서 무료 발급). 없어도 서버는 정상 구동되며, 이 경우 챗봇은 자동으로 룰 기반 응답으로 동작함.

## 3. 최초 1회: DB 스키마 생성

`prod` 프로필은 `ddl-auto: validate`라서 빈 DB에는 그냥 못 뜸. 최초 1번만 `update` 모드로 스키마를 만들어줘야 함.

```bash
docker compose build app
docker compose run --rm -e SPRING_JPA_HIBERNATE_DDLAUTO=update app
```

> ⚠️ `docker compose build app`을 꼭 먼저 실행할 것. `docker compose run`은 이미지를 자동으로 새로 빌드하지 않아서, 방금 `git pull`한 새 코드가 아니라 예전에 캐시된 이미지로 실행될 수 있음 (스키마 변경사항이 반영 안 된 채로 돌아가서 계속 실패하는 원인이 됨).

로그에 아래처럼 뜨면 성공 → `Ctrl + C`로 종료:

```
Started SignalApplication in ... seconds
```

> ⚠️ 이 단계는 **컬럼/테이블이 추가되는 배포마다** 필요함 (최초 배포 포함). 코드 변경이 엔티티 필드 추가 없이 로직만 바뀐 거면 생략 가능.

## 4. 실행

```bash
docker compose up -d --build
```

## 5. 확인

```bash
docker compose ps
curl -i http://localhost:8080/swagger-ui/index.html
```

`200`이나 정상 응답 오면 배포 완료.

## 이후 재배포 (코드 수정 후)

엔티티에 새 필드/테이블이 생겼으면 (마이그레이션 필요):
```bash
git pull
docker compose build app
docker compose run --rm -e SPRING_JPA_HIBERNATE_DDLAUTO=update app
# Started 뜨면 Ctrl+C
docker compose up -d --build
```

로직만 바뀌고 스키마 변경이 없으면:
```bash
git pull
docker compose up -d --build
```

## 자주 쓰는 명령어

```bash
docker compose logs -f app      # 앱 로그 실시간으로 보기
docker compose restart app      # 앱만 재시작
docker compose down             # 전체 정지 (데이터는 volume에 남아있음)
docker compose down -v          # 전체 정지 + DB/업로드 데이터까지 삭제 (주의!)
```
