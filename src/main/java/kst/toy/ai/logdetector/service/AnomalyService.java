package kst.toy.ai.logdetector.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import kst.toy.ai.logdetector.analyzer.Feature;
import kst.toy.ai.logdetector.domain.AnomalyResult;
import kst.toy.ai.logdetector.domain.enm.RiskLevel;
import kst.toy.ai.logdetector.repository.AnomalyResultRepository;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyService {

    private final AnomalyResultRepository resultRepository;
    private final RestTemplate restTemplate = new RestTemplate();

/**
 * 이상 탐지 로직
 * Rule-based 방식임
 * [Feature]
            ↓
    평균 계산
  ↓
    표준편차 계산
  ↓
    각 IP별 Z-score 계산
  ↓
    임계값 초과 여부 판단
  ↓
    이상 IP 반환
 */
    public List<AnomalyResult> detect(List<Feature> features) {

//        전체 IP들의 평균 요청 수 계산
        double mean = features.stream()
                .mapToInt(Feature::getRequestCount)
                .average()
                .orElse(0);

        double std = calculateStd(features, mean);
        return features.stream()
                .map(f -> {
                    double z = calculateZScore(f.getRequestCount(), mean, std);
                    RiskLevel risk = classifyRisk(z);

                    // AI 예측 추가
                    Map<String, Object> aiResult = predictAnomalyWithAI(f);
                    boolean aiAnomaly = (boolean) aiResult.get("is_anomaly");
                    double aiScore = (double) aiResult.get("anomaly_score");

                    // Z-score와 AI 결합: 둘 중 하나라도 이상이면 HIGH
                    RiskLevel finalRisk = (risk == RiskLevel.HIGH || aiAnomaly) ? RiskLevel.HIGH : risk;

                    return AnomalyResult.builder()
                            .ip(f.getIp())
                            .score(z)
                            .aiScore(aiScore)
                            .riskLevel(finalRisk.name())
                            .requestCount(f.getRequestCount())
                            .failureRate(f.getFailureRate())
                            .detectedAt(LocalDateTime.now())
                            .build();
                })
                .map(resultRepository::save)
                .toList();
    }


    /**
     * 위험도 분류
     * */
    private RiskLevel classifyRisk(double z) {
        double absZ = Math.abs(z);

        if (absZ >= 3.0) return RiskLevel.HIGH;
        if (absZ >= 2.0) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }


    /**
     * Z-score
     *    평균 대비 얼마나 벗어났는가
    * */
    private double calculateZScore(double value, double mean, double std) {
        if (std == 0) return 0;
        return (value - mean) / std;
    }

    /**
     *
     * 표준편차 계산
     * 데이터가 평균에서 얼마나 퍼져 있는지 측정
     * Z-score	의미
     * 0	평균
     * 1	평균보다 조금 큼
     * 2	꽤 큼
     * 3 이상	매우 비정상
     * */
    private double calculateStd(List<Feature> features, double mean) {

        double variance = features.stream()
                .mapToDouble(f -> Math.pow(f.getRequestCount() - mean, 2))
                .average()
                .orElse(0);

        return Math.sqrt(variance);
    }

    /**
     * AI 기반 이상 예측 (Python ML API 호출)
     */
    private Map<String, Object> predictAnomalyWithAI(Feature feature) {
        try {
            String url = "http://localhost:5000/predict";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Feature 데이터를 JSON으로 변환
            Map<String, Object> requestBody = Map.of(
                "features", new double[]{
                    feature.getRequestCount(),
                    feature.getFailureRate(),
                    feature.getDistinctUrlCount(),
                    feature.getAverageUrlLength(),
                    feature.getHourOfDay()
                }
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("AI prediction failed: {}", e.getMessage());
            return Map.of("is_anomaly", false, "anomaly_score", 0.0); // fallback
        }
    }
}