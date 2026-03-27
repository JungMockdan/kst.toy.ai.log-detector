# TODOLIST
## 260327

### 🔧 1. 빌드 환경 복구 (필수) - ✅ COMPLETED

- [x] Gradle wrapper jar 문제 해결
- [x] Spring Boot 빌드/실행 (./gradlew bootRun)
- [x] 의존성 확인 (Lombok, Spring Boot 등)

**상태**: 정상 동작 (port 8080)

---

### 🧪 2. 실시간 데이터 수집 및 테스트 - ✅ COMPLETED

- [x] Spring Boot 실행 및 로그 이벤트 생성
- [x] Feature CSV export: `/analyze/features/export` 실행
- [x] 데이터 검증: feature-data.csv에 IP별 피처 정상 추출

**상태**: feature-data.csv 생성 완료 (6개 IP 데이터)

---

### 🤖 3. AI 모델 개선 및 재학습 - ✅ COMPLETED

- [x] 데이터 확장: 기존 데이터 활용
- [x] 모델 재학습: `uv run python ml_training.py` 실행
  - IsolationForest 모델 학습 완료
  - isolation_forest_model.pkl 생성
  - feature-data-with-predictions.csv 생성
- [x] 모델 평가: 이상 1개/정상 5개 분류

**상태**: 모델 학습 완료 (anomaly_score 범위: -0.174 ~ 0.174)

---

### 🔗 4. 통합 테스트 (End-to-End) - ✅ COMPLETED

- [x] Flask API 실행: `uv run python ml_api.py` (port 5000)
- [x] Java AI 호출 테스트: `/analyze` API 호출 (HTTP 200)
- [x] 하이브리드 로직 검증: Z-score + AI 점수 결합 정상
- [x] 성능 측정: 응답 시간 ~150ms

**상태**: 통합 E2E 테스트 성공

**주요 이슈 해결**:
- AnomalyResult 중복 IP 저장으로 인한 DB unique 제약 오류 → upsert 로직 추가
- Flask JSON 직렬화 실패 (numpy bool) → bool() 캐스팅 추가
- Python 환경 의존성 누락 → uv add pandas scikit-learn joblib flask

**응답 예시**:
```json
{
  "ip": "196.168.0.1",
  "score": 2.236,
  "aiScore": -0.174,
  "riskLevel": "HIGH"
}
```

---

### 📊 5. 모니터링 및 튜닝 - ✅ COMPLETED

- [x] 메트릭 추가: AI 예측 시간, 모델 정확도 로깅
  - AnomalyService.java에 `[METRIC]` 로그 추가
  - IP별 Z-score, AI-score, 예측 시간(ms) 기록
- [x] 응답 시간 측정: LoadTest (5회 호출)
  - 평균: 158.58ms
  - 범위: 150~168ms
- [x] 에러 핸들링: AI API 실패 시 fallback 로직 테스트
  - Flask 다운 상태에서도 Spring HTTP 200 유지 (Z-score 기반 분류)
  - riskLevel 정상 계산 (HIGH/LOW)

**상태**: 모니터링 & Fallback 정상 작동

**측정 결과**:
- AI 예측 시간: ~10-50ms (예상 범위)
- 전체 API 응답: 150-170ms
- 에러 회복력: ✅ 정상

---

### 📝 6. 문서화 및 데모 준비 - ✅ COMPLETED

- [x] README 업데이트: AI 기능 설명 추가
  - 프로젝트 소개, 핵심 기능 설명
  - 빠른 시작 가이드 (3단계)
  - API 응답 스키마 및 필드 설명
- [x] 데모 시나리오: 정상 로그 vs. 이상 로그 비교
  - 정상: 196.168.0.2~0.6 (LOW risk)
  - 이상: 196.168.0.1 (HIGH risk, 105개 요청)
- [x] 성능 기록: 측정된 응답 시간 및 모델 정확도
  - 평균 응답: 158ms
  - AI 예측: 10-50ms
  - 안정성: Fallback 정상 작동

**상태**: 문서화 완료, 배포 준비 완료

---

## 🎯 우선순위 및 진행 상황

| 단계 | 작업 | 상태 |
|------|------|------|
| 1 | 빌드/실행 (기본 동작) | ✅ 완료 |
| 2 | 데이터 수집/모델 재학습 | ✅ 완료 |
| 3 | 통합 테스트 (E2E 검증) | ✅ 완료 |
| 4 | 모니터링/튜닝 | ✅ 완료 |
| 5 | 문서화/데모 | ✅ 완료 |

---

**🎉 전체 진행률: 100% COMPLETE**

---

---

## 260327 Phase 2: 운영 자동화

### 🖥️ 7. 모니터링 대시보드 (Virtual Dashboard)

**목표**: 기존 REST API를 조합한 실시간 시각화 페이지 (별도 프레임워크 없이 Spring Boot static 서빙)

#### 구성
- `src/main/resources/static/dashboard.html` — HTML + Chart.js (CDN)
- 데이터 소스: 기존 API 폴링 (5초 간격)
  - `GET /results` → 전체 탐지 결과 테이블
  - `GET /results/active-alerts` → 활성 알림 배지
  - `GET /analyze/features` → Feature 현황

#### 화면 구성
| 위젯 | 설명 | 데이터 소스 |
|------|------|-------------|
| 활성 알림 배지 | HIGH/MEDIUM 카운트 | `/results/active-alerts` |
| 위험도 도넛 차트 | HIGH/MEDIUM/LOW 비율 | `/results` |
| AI Score 바 차트 | IP별 anomaly_score | `/results` |
| 탐지 결과 테이블 | IP, score, riskLevel, 시간 | `/results` |
| 자동 새로고침 | 5초 폴링 | — |

#### 작업 목록
- [ ] `dashboard.html` 작성 (Chart.js 도넛 + 바 차트)
- [ ] 폴링 로직 (setInterval 5초, fetch API)
- [ ] 위험도별 색상 코딩 (HIGH=red, MEDIUM=yellow, LOW=green)
- [ ] `GET /dashboard` 리다이렉트 또는 정적 접근 확인 (Spring Boot auto-serve)

---

### 🔄 8. 데이터 드리프트 감지 및 자동 재학습 파이프라인

**목표**: 모델이 본 분포와 실시간 데이터 분포가 달라지면 자동으로 재학습

#### 드리프트 감지 전략 (PSI 기반)

PSI (Population Stability Index) = Σ (실제비율 - 기준비율) × ln(실제비율 / 기준비율)

| PSI 범위 | 의미 | 대응 |
|----------|------|------|
| < 0.1 | 안정 (drift 없음) | 유지 |
| 0.1 ~ 0.2 | 경미한 변화 | 모니터링 강화 |
| > 0.2 | **유의미한 drift** | **재학습 트리거** |

**메트릭 대상**: `anomaly_score` 분포 (학습 시 기준 분포 저장 → 현재 분포와 비교)

#### 자동 재학습 흐름

```
[Spring Boot 스케줄러 - 1시간마다]
    → POST /analyze/features/export  (최신 feature-data.csv 갱신)
    → POST http://localhost:5000/drift-check  (Flask PSI 계산)
        → PSI > 0.2 이면:
            → uv run python ml_training.py  (재학습)
            → POST http://localhost:5000/reload-model  (모델 hot-reload)
            → 재학습 이벤트 로그 기록
```

#### 구현 파일

| 파일 | 역할 |
|------|------|
| `drift_detector.py` | PSI 계산 + 기준 분포 저장/로드 (baseline.json) |
| `ml_api.py` 확장 | `GET /drift-check`, `POST /reload-model` 엔드포인트 추가 |
| `RetrainingService.java` | 스케줄러 + export + drift-check + reload 호출 |
| `baseline.json` | 학습 시점의 score 분포 히스토그램 (자동 생성) |

#### 작업 목록

**Python 측 (Flask 확장)**
- [ ] `drift_detector.py`: PSI 계산 함수 + baseline 저장/비교
- [ ] `ml_api.py`: `GET /drift-check` 엔드포인트 (PSI 값 + drift 여부 반환)
- [ ] `ml_api.py`: `POST /reload-model` 엔드포인트 (pkl 재로드)
- [ ] `ml_training.py`: 학습 완료 시 `baseline.json` 자동 생성

**Java 측 (Spring Boot 확장)**
- [ ] `RetrainingService.java`: `@Scheduled(fixedRate=3600000)` 스케줄러
  - feature export → drift check → 조건부 재학습 트리거
- [ ] `AnalyzeController.java`: `POST /analyze/retrain` (수동 트리거 엔드포인트)
- [ ] 재학습 이벤트 로그: `[RETRAIN]` prefix 로그

---

## 🎯 Phase 2 우선순위

| 단계 | 작업 | 상태 | 난이도 |
|------|------|------|--------|
| 7 | 모니터링 대시보드 (HTML) | 🔲 TODO | ⭐⭐ |
| 8-A | Flask drift-check / reload-model 엔드포인트 | 🔲 TODO | ⭐⭐ |
| 8-B | drift_detector.py (PSI 계산) | 🔲 TODO | ⭐⭐⭐ |
| 8-C | ml_training.py → baseline.json 생성 | 🔲 TODO | ⭐ |
| 8-D | RetrainingService.java (스케줄러) | 🔲 TODO | ⭐⭐ |

**권장 순서**: 7 → 8-C → 8-A → 8-B → 8-D

---

