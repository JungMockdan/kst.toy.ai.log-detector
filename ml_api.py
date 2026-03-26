from flask import Flask, request, jsonify
import joblib
import numpy as np

app = Flask(__name__)

# 학습된 모델 로드
model = joblib.load('isolation_forest_model.pkl')

@app.route('/predict', methods=['POST'])
def predict():
    # JSON 입력: {"features": [requestCount, failureRate, distinctUrlCount, averageUrlLength, hourOfDay]}
    data = request.get_json()
    features = np.array(data['features']).reshape(1, -1)  # 1행 5열로 변환

    # 예측
    prediction = model.predict(features)[0]  # -1 or 1
    score = model.decision_function(features)[0]  # 이상 점수

    # 결과: -1이면 이상, 1이면 정상
    result = {
        'prediction': int(prediction),  # -1: 이상, 1: 정상
        'anomaly_score': float(score),  # 낮을수록 이상
        'is_anomaly': prediction == -1
    }
    return jsonify(result)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)