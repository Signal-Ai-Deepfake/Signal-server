# 배포 가이드 (학교 서버 / Docker)

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

## 3. 최초 1회: DB 스키마 생성

`prod` 프로필은 `ddl-auto: validate`라서 빈 DB에는 그냥 못 뜸. 최초 1번만 `update` 모드로 스키마를 만들어줘야 함.

```bash
docker compose run --rm -e SPRING_JPA_HIBERNATE_DDLAUTO=update app
```

로그에 아래처럼 뜨면 성공 → `Ctrl + C`로 종료:

```
Started SignalApplication in ... seconds
```

> ⚠️ 이 단계는 **최초 배포 때 딱 한 번만** 하면 됨. 이후 재배포/재시작 시에는 다시 안 해도 됨 (스키마가 이미 있으니까 4번 단계만 반복).

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
