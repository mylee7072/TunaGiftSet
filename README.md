# TunaGiftSet

한국형 참치 선물세트 쇼핑몰. Spring Boot(백엔드) + React/Vite(프론트엔드) + PostgreSQL.

이 문서는 **로컬 실행(두 가지 방식)** 을 다룹니다. 실제 운영 서버/도메인/HTTPS 배포는 아직 이 단계의 범위가
아닙니다 — 이번 단계는 어디까지나 "로컬 Docker로 전체 스택을 재현 가능하게" 만드는 배포 기반 구축입니다.

## 실행 방식 두 가지

이 프로젝트는 아래 두 방식을 **동시에** 지원합니다. 하나를 추가했다고 다른 하나가 깨지지 않습니다.

| 방식 | 언제 쓰나 | DB |
|---|---|---|
| A. IntelliJ(또는 `./gradlew bootRun`) + 로컬 PostgreSQL | 평소 백엔드 개발/디버깅 | 이미 로컬에 떠 있는 PostgreSQL (`localhost:5432`) |
| B. Docker Compose 전체 스택 | 프론트+백엔드+DB를 한 번에, 다른 사람 환경에서도 동일하게 재현 | Compose가 띄우는 별도 PostgreSQL 컨테이너 (A의 DB와는 별개 인스턴스) |

---

## A. 기존 방식 — IntelliJ + 로컬 PostgreSQL

지금까지와 동일합니다. 아래 환경변수만 셸/Run Configuration에 설정하면 됩니다.

```
DB_USERNAME=<로컬 postgres 계정>
DB_PASSWORD=<로컬 postgres 비밀번호>
JWT_SECRET=<아무 랜덤 문자열>
```

`DB_HOST`/`DB_PORT`/`DB_NAME`을 설정하지 않으면 각각 `localhost` / `5432` / `tunagiftset`으로
자동 적용됩니다 (기존과 완전히 동일). Frontend는 별도로 `frontend/`에서 `npm run dev`.

## B. 신규 방식 — Docker Compose 전체 스택

### 필요한 것

- Docker Desktop (Docker Engine + Compose v2)

### 1. 환경변수 준비

```bash
cp .env.example .env
```

`.env`를 열어 `change_me`로 되어 있는 값을 채웁니다. **`.env`는 git에 커밋되지 않습니다**
(`.gitignore`에 이미 등록됨). `TOSS_SECRET_KEY`/`VITE_TOSS_CLIENT_KEY`는 Toss **테스트 키**만
사용하세요 — Live 키는 이 단계에서 다루지 않습니다.

### 2. 실행

```bash
docker compose up -d --build
```

`compose.override.yaml`이 `-f` 없이 실행할 때 자동으로 함께 적용되어(Compose 표준 동작),
개발 편의용 DB 호스트 포트 매핑이 추가됩니다. 최초 실행은 Gradle 의존성 다운로드 때문에
몇 분 걸릴 수 있습니다.

### 3. 확인

| 서비스 | URL |
|---|---|
| Frontend | http://localhost:8081 |
| Backend API | http://localhost:8080/api/... |
| Backend Health | http://localhost:8080/actuator/health |
| PostgreSQL (dev 전용, 호스트에서 직접 접속 시) | `localhost:5433` |

```bash
docker compose ps                # 각 서비스 healthy 여부 확인
docker compose logs -f backend   # 백엔드 로그
docker compose logs -f frontend  # 프론트 로그
```

### 4. 종료

```bash
docker compose down        # 컨테이너만 정리, DB 데이터는 named volume에 보존됨
docker compose down -v     # ⚠️ DB 데이터까지 완전히 삭제 — 개발 DB를 초기화하고 싶을 때만
```

### 5. 프로덕션에 가까운 실행 (아직 실제 서버는 아님)

```bash
docker compose -f compose.yaml -f compose.prod.yaml up -d --build
```

이 명령은 `-f`를 명시했기 때문에 `compose.override.yaml`이 **자동 병합되지 않아** DB 포트가
호스트에 노출되지 않습니다. `SPRING_PROFILES_ACTIVE=prod`가 적용되어 `spring.jpa.hibernate.ddl-auto=validate`,
엄격한 CORS/JWT 필수값 등 운영 전제의 안전한 기본값이 켜집니다.

## 새 Docker 개발 DB와 기존 로컬 PostgreSQL은 별개입니다

`compose.yaml`의 `db` 서비스는 **완전히 새로운 빈 PostgreSQL 컨테이너**입니다. 이름(`tunagiftset`)만
같을 뿐, IntelliJ로 개발할 때 쓰던 로컬 PostgreSQL과는 다른 인스턴스이며 데이터도 공유되지 않습니다.
최초 기동 시 스키마는 비어 있고, 현재 백엔드가 `ddl-auto=update`(dev 프로파일)로 자동 생성합니다 —
Flyway/Liquibase 같은 마이그레이션 도구는 아직 없습니다 (완료 보고서의 "Migration 현황" 참고).

## Toss Client Key는 "빌드 타임" 값입니다

`VITE_TOSS_CLIENT_KEY`는 Vite가 **프론트엔드 빌드 시점**에 JS 번들에 값을 그대로 박아 넣습니다.
`.env`에서 이 값을 바꿨다면 컨테이너를 재시작하는 것만으로는 반영되지 않고, 반드시 이미지를
다시 빌드해야 합니다.

```bash
docker compose build frontend
docker compose up -d frontend
```

## 트러블슈팅

- **`db` 포트(5433)가 이미 사용 중** → `.env`의 `DB_HOST_PORT`를 다른 값으로 변경.
- **`backend`가 `unhealthy`로 계속 남아있음** → `docker compose logs backend`로 DB 연결/JWT_SECRET
  누락 여부 확인. `.env`의 필수값(`DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`)이 비어 있으면
  컨테이너가 즉시 기동 실패합니다 (일부러 그렇게 설계됨 — 운영에서 값 누락을 조용히 넘어가지 않기 위함).
- **이미지 업로드 상품 이미지가 컨테이너 재생성 후 사라짐** → `STORAGE_TYPE=local`일 때는
  `backend_uploads` named volume에 저장되므로 `docker compose down`(볼륨 미삭제) 후에는 유지됩니다.
  `down -v`를 쓰면 함께 삭제됩니다.

## 테스트

```bash
./gradlew.bat test          # 백엔드 (Windows)
cd frontend && npm test     # 프론트엔드
cd frontend && npm run lint
```

Docker 이미지 빌드는 테스트를 별도로 실행하지 않습니다(`bootJar -x test`) — 이미지를 빌드하기 전에
위 명령으로 테스트를 통과시키는 것이 여전히 정상 워크플로입니다.
