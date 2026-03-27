# kst.toy.ai.log-detector
using LLM model for app that detact error log

## 🎯 프로젝트 소개

IP별 요청 로그를 실시간 분석해 **이상 트래픽을 감지**하는 하이브리드 AI 시스템입니다.

### 핵심 기능
- **Z-Score 기반 통계**: 요청 수 이상치 탐지 (평균 대비 편차)
- **AI 모델 (IsolationForest)**: 다중 특성 기반 이상 탐지
- **하이브리드 분류**: Z-score + AI 점수 결합 → 최종 Risk Level (LOW/MEDIUM/HIGH)
- **Fallback 안정성**: Flask 연결 실패 시에도 Z-score 기반 분류 유지

---

## 📌 주요 기술 스택

- **Backend**: Spring Boot 4.0.4 + Gradle
- **ML/Python**: Python 3.14.3 (uv 관리)
  - pandas, scikit-learn, joblib, flask
- **DB**: H2 (in-memory)
- **서버 포트**: Spring(8080), Flask(5000)

---

## 🚀 빠른 시작

### 1. 환경 설정
```bash
# Python 의존성 설치 (uv 기반)
uv add pandas scikit-learn joblib flask
```

### 2. 서버 실행
**Terminal 1: Flask ML API**
```bash
uv run python ml_api.py  # port 5000
```

**Terminal 2: Spring Boot**
```bash
./gradlew bootRun  # port 8080
```

### 3. API 호출
```powershell
# 이상 탐지 분석
curl -X POST http://localhost:8080/analyze

# Feature 데이터 내보내기
curl -X POST http://localhost:8080/analyze/features/export
```

---

## 📊 API 응답 예시

### GET /analyze

```json
[
  {
    "id": 1,
    "ip": "196.168.0.1",
    "score": 2.236,
    "aiScore": -0.174,
    "riskLevel": "HIGH",
    "requestCount": 105,
    "failureRate": 1.0,
    "detectedAt": "2026-03-27T10:26:32.586Z"
  }
]
```

**필드 설명**:
- `score`: Z-score (요청 수 기반 표준화 점수)
- `aiScore`: 머신러닝 모델 점수 (음수 = 이상 신호)
- `riskLevel`: 최종 위험도
  - `HIGH`: Z-score ≥ 3.0 OR AI 이상 감지
  - `MEDIUM`: Z-score ≥ 2.0
  - `LOW`: 정상

---

## 🧪 데모 시나리오

### 데이터 수집 및 모델 학습
```bash
# 1. 실제 로그 데이터 생성 (Spring이 자동으로 처리)
# 2. CSV로 Feature 추출
uv run python ml_training.py

# 출력:
# === IsolationForest 학습 결과 ===
# 총 데이터 수: 6
# 예측된 이상 수: 1
# 예측된 정상 수: 5
# 모델 저장 완료: isolation_forest_model.pkl
```

### 통합 테스트: 정상 vs 이상 비교

**정상 로그**: 196.168.0.2 ~ 196.168.0.6 (요청 1~3개)
- riskLevel: **LOW**
- Z-score: -0.46 (평균 이하)
- aiScore: ~0.17 (정상 신호)

**이상 로그**: 196.168.0.1 (요청 105개)
- riskLevel: **HIGH**
- Z-score: 2.24 (평균 상회)
- aiScore: -0.17 (이상 신호)

---

## 📈 성능 지표 (3월 27일 측정)

| 지표 | 값 |
|------|-----|
| 평균 응답 시간 | **158ms** |
| AI 예측 시간 | 10-50ms |
| 모델 정확도 | IsolationForest (1개 이상 탐지) |
| 안정성 | Flask 다운 시에도 Z-score 기반 분류 유지 ✅ |

---

## 🔧 문제 해결

### Flask 연결 실패 시
- Spring은 HTTP 200 유지 (Fallback)
- aiScore = 0.0으로 설정
- Z-score만으로 riskLevel 판정

### 데이터베이스 오류 (중복 IP)
- 동일 IP 재분석 시 기존 레코드 업데이트 (upsert 로직)
- 중복 저장으로 인한 unique 제약 오류 방지

---

## 📝 개발 진행 상황

전체 5단계 작업 완료율: **✅ 100%**

자세한 내용은 [# TODO.md](# TODO.md)를 참고하세요.