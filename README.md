# 🔐 AuthLab (2FA 인증 시스템)

**AuthLab**은 보편적인 **이메일 인증(CSPRNG)** 방식과 보안성 및 효율성이 뛰어난 **TOTP(Google Authenticator)** 방식을 모두 구현한 프로젝트입니다.

서로 다른 두 가지 2차 인증 아키텍처를 직접 구현해보고, 트래픽 처리 과정에서의 구조적 차이를 비교/분석하기 위해 개발되었습니다.

## 📋 주요 기능 (Key Features)

### 1. 인증 및 인가
- **JWT (JSON Web Token)** 기반의 Stateless 인증 시스템
- Access Token & Refresh Token 발급 및 재발급 프로세스
- `PreAuthToken`을 활용한 **2단계 인증 중간 검증** 단계 구현 (임시 티켓 역할)
- Redis Blacklist를 활용한 **로그아웃** 처리

### 2. 이메일 인증 (CSPRNG 방식)
- `SecureRandom`을 이용한 암호화된 난수 생성
- Redis를 활용한 인증 코드 만료 시간(TTL) 관리
- 외부 SMTP 통신 지연(Latency) 상황 시뮬레이션 (I/O Bound 테스트 환경)

### 3. TOTP 인증 (Google Authenticator)
- **HMAC-SHA1** 알고리즘 기반의 시간 동기화 인증 (Time-based One-Time Password)
- 외부 네트워크 통신이 필요 없는 **순수 CPU 연산 기반**의 검증 로직

---

## 🛠 기술 스택 (Tech Stack)

### Backend
- **Java 17**
- **Spring Boot 3.5.9**
- **Spring Security 6**
- **Spring Data JPA**
- **Redis** (토큰 저장소 및 캐싱)
- **GoogleAuth Library** (TOTP 연산)

### Frontend
- **Next.js 14** (App Router)
- **TypeScript**
- **Tailwind CSS**
- **Docker & Docker Compose** (컨테이너 실행 환경)
- **Axios**

---

## 🏗 시스템 아키텍처 (Architecture)

### 인증 프로세스 (Authentication Flow)
1. **1차 로그인 (ID/PW):** - DB 검증 성공 시 `PreAuthToken`(2차 인증용 임시 토큰) 발급
2. **2차 인증 분기:**
   - **User A (Email):** 서버 난수 생성 → Redis 저장 → 이메일 발송 (I/O 대기 발생)
   - **User B (TOTP):** 앱 생성 코드 입력 → 서버 해시 연산 검증 (즉시 처리)
3. **최종 승인:** - 검증 성공 시 최종 `AccessToken` & `RefreshToken` 발급

---

## 📂 프로젝트 구조 (Project Structure)


```

AuthLab/
├── src/main/java/org/example/authlab/  # Spring Boot BE
│   ├── domain/auth/          # 인증 로직 (Controller, Service, JWT)
│   ├── domain/user/          # 유저 도메인 (Entity, Repository)
│   └── global/               # 전역 설정 (Config, Redis, Security)
├── auth-lab-front/           # Next.js FE
│   ├── app/                  # 화면 UI (Login, 2FA)
│   ├── docker-compose.yml    # Frontend 실행 설정
│   └──── public/
└── README.md

```
