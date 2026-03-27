package kst.toy.ai.logdetector.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import kst.toy.ai.logdetector.analyzer.Feature;
import kst.toy.ai.logdetector.domain.AnomalyResult;
import kst.toy.ai.logdetector.domain.DetectionResult;
import kst.toy.ai.logdetector.domain.enm.RiskLevel;
import kst.toy.ai.logdetector.repository.AnomalyResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealTimeDetectionService {

    private final AnomalyResultRepository resultRepository;
    private final AlertDedupService alertDedupService;
    private final DashboardSseService dashboardSseService;
    private final RedisWindowService redisWindowService;
    private final FeatureService featureService;
    private final RestTemplate restTemplate = new RestTemplate();

    public void detect(String ip, int status, long count) {
        DetectionResult result = analyze(ip, status, count);

        // LOW도 저장하여 /logs와 /analyze 경로의 데이터 일관성을 유지한다.
        if (result.getRiskLevel() != RiskLevel.LOW) {
            alertDedupService.saveAlertToRedis(result);
        }
        saveOrUpdate(result);
    }

    public void saveOrUpdate(DetectionResult result) {
        Optional<AnomalyResult> optional =
                resultRepository.findTopByIpOrderByDetectedAtDesc(result.getIp());

        if (optional.isEmpty()) {
            saveNew(result);
            return;
        }

        AnomalyResult existing = optional.get();
        if (isHigherRisk(result, existing)) {
            update(existing, result);
        }
    }

    private boolean isHigherRisk(DetectionResult newResult, AnomalyResult existing) {
        if (newResult.getRiskLevel().ordinal() >
                RiskLevel.valueOf(existing.getRiskLevel()).ordinal()) {
            return true;
        }
        return newResult.getScore() > existing.getScore();
    }

    private void update(AnomalyResult entity, DetectionResult result) {
        entity.setRiskLevel(result.getRiskLevel().name());
        entity.setScore(result.getScore());
        entity.setAiScore(result.getAiScore());
        entity.setRequestCount((int) result.getRequestCount());
        entity.setFailureRate(result.getFailureRate());
        entity.setDetectedAt(LocalDateTime.now());

        AnomalyResult saved = resultRepository.save(entity);
        dashboardSseService.broadcast(saved);
    }

    private void saveNew(DetectionResult result) {
        AnomalyResult entity = AnomalyResult.builder()
                .ip(result.getIp())
                .riskLevel(result.getRiskLevel().name())
                .score(result.getScore())
                .aiScore(result.getAiScore())
                .requestCount((int) result.getRequestCount())
                .failureRate(result.getFailureRate())
                .detectedAt(LocalDateTime.now())
                .build();

        AnomalyResult saved = resultRepository.save(entity);
        dashboardSseService.broadcast(saved);
    }

    private DetectionResult analyze(String ip, int status, long count) {
        // 1. DB에서 IP별 full feature 추출
        Feature feature = featureService.extractFeatureForIp(ip);
        int featureCount = feature != null ? feature.getRequestCount() : (int) count;
        double failureRate = feature != null ? feature.getFailureRate() : (status >= 400 ? 1.0 : 0.0);

        // 2. 캐시된 mean/std로 Z-score 계산 (AnomalyService 배치 분석과 동일 공식)
        double mean = redisWindowService.getCachedMean();
        double std  = redisWindowService.getCachedStd();
        double z;
        if (mean >= 0 && std > 0) {
            z = (featureCount - mean) / std;
        } else {
            // 배치 분석 전이면 절대값 fallback
            z = featureCount >= 100 ? 4.0 : featureCount >= 50 ? 2.5 : 0.0;
        }
        RiskLevel risk = classifyRisk(z);

        // 3. AI 예측 (AnomalyService와 동일 엔드포인트)
        Map<String, Object> aiResult = feature != null
                ? predictWithAI(feature)
                : Map.of("is_anomaly", false, "anomaly_score", 0.0);
        boolean aiAnomaly = Boolean.TRUE.equals(aiResult.get("is_anomaly"));
        double aiScore = aiResult.get("anomaly_score") instanceof Number n ? n.doubleValue() : 0.0;

        // 4. Z-score + AI 조합 (AnomalyService와 동일)
        RiskLevel finalRisk = (risk == RiskLevel.HIGH || aiAnomaly) ? RiskLevel.HIGH : risk;

        return DetectionResult.builder()
                .ip(ip)
                .riskLevel(finalRisk)
                .score(z)
                .aiScore(aiScore)
                .requestCount(featureCount)
                .failureRate(failureRate)
                .build();
    }

    private RiskLevel classifyRisk(double z) {
        double absZ = Math.abs(z);
        if (absZ >= 3.0) return RiskLevel.HIGH;
        if (absZ >= 2.0) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    private Map<String, Object> predictWithAI(Feature feature) {
        try {
            String url = "http://localhost:5000/predict";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = Map.of(
                    "features", new double[]{
                            feature.getRequestCount(),
                            feature.getFailureRate(),
                            feature.getDistinctUrlCount(),
                            feature.getAverageUrlLength(),
                            feature.getHourOfDay()
                    }
            );
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForEntity(url, entity, Map.class).getBody();
            return response != null ? response : Map.of("is_anomaly", false, "anomaly_score", 0.0);
        } catch (Exception e) {
            log.warn("[AI] real-time prediction failed: {}", e.getMessage());
            return Map.of("is_anomaly", false, "anomaly_score", 0.0);
        }
    }
}
