import pandas as pd
from sklearn.ensemble import IsolationForest
from sklearn.metrics import classification_report, confusion_matrix
import joblib
import numpy as np

# 1. CSV 데이터 로드 (Java에서 생성된 feature-data.csv)
# 이 데이터는 IP별 feature: requestCount, failureRate, distinctUrlCount, averageUrlLength, hourOfDay
data = pd.read_csv('feature-data.csv')

# 2. Feature 선택 (IP 제외, 숫자형 feature만)
# 왜? IP는 문자열이므로 ML 모델에 직접 넣을 수 없음. 나중에 IP별로 매핑해서 사용.
features = data[['requestCount', 'failureRate', 'distinctUrlCount', 'averageUrlLength', 'hourOfDay']]

# 3. IsolationForest 모델 선택 이유:
# - 비지도 학습: 라벨(정상/이상)이 없어도 학습 가능 (실제 로그에서는 이상 라벨이 드물기 때문)
# - 이상 탐지에 특화: 정상 데이터만으로 학습, 이상 점수(-1: 이상, 1: 정상) 출력
# - 간단하고 빠름: 토이 프로젝트에 적합
model = IsolationForest(contamination=0.1, random_state=42)  # contamination: 예상 이상 비율 10%

# 4. 모델 학습
# fit()으로 정상 패턴 학습
model.fit(features)

# 5. 예측 (학습 데이터에 대해 이상 점수 계산)
# predict(): -1 (이상), 1 (정상)
predictions = model.predict(features)

# decision_function(): 이상 점수 (낮을수록 이상)
scores = model.decision_function(features)

# 6. 결과 확인 (학습 데이터 평가)
# 실제로는 테스트 데이터로 평가해야 하지만, 여기서는 데모용
print("=== IsolationForest 학습 결과 ===")
print(f"총 데이터 수: {len(data)}")
print(f"예측된 이상 수: {np.sum(predictions == -1)}")
print(f"예측된 정상 수: {np.sum(predictions == 1)}")

# 점수 분포 확인
print(f"\n이상 점수 범위: {scores.min():.3f} ~ {scores.max():.3f}")
print(f"평균 점수: {scores.mean():.3f}")

# 7. 모델 저장 (Java에서 로드할 수 있도록)
# joblib: scikit-learn 모델을 파일로 저장
joblib.dump(model, 'isolation_forest_model.pkl')
print("\n모델 저장 완료: isolation_forest_model.pkl")

# 8. CSV에 예측 결과 추가 (선택: 검증용)
data['anomaly_score'] = scores
data['prediction'] = predictions
data.to_csv('feature-data-with-predictions.csv', index=False)
print("예측 결과 CSV 저장: feature-data-with-predictions.csv")

# 9. 추가 학습 팁 (납득용)
# - contamination: 실제 이상 비율에 맞춰 조정 (예: 0.05 = 5%)
# - random_state: 재현성 위해 고정
# - 평가: 실제 이상 라벨이 있다면 classification_report 사용
#   예: print(classification_report(true_labels, predictions))

print("\n=== 학습 완료 ===")
print("이제 Java에서 이 모델을 로드해서 실시간 예측에 사용하세요!")