package kst.toy.ai.logdetector.service;

import kst.toy.ai.logdetector.domain.AnomalyResult;
import kst.toy.ai.logdetector.domain.DetectionResult;
import kst.toy.ai.logdetector.domain.enm.RiskLevel;
import kst.toy.ai.logdetector.repository.AnomalyResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

//실시간 탐지 서비스
@Service
@RequiredArgsConstructor
public class RealTimeDetectionService {

    private final AnomalyResultRepository resultRepository;
    private final AlertDedupService alertDedupService;

    /*public void detect(String ip, int status, long count) {

        double failureScore = (status >= 400) ? 1.0 : 0.0;
        RiskLevel risk = classifyRisk(count, failureScore);
        if (risk.equals(RiskLevel.LOW)) return;


        // 중복 탐지 방지
        if (!alertDedupService.isFirstDetection(ip)) return;

        double score = calculateScore(count, failureScore);

        AnomalyResult result = AnomalyResult.builder()
                .ip(ip)
                .score(score)
                .riskLevel(risk)
                .requestCount((int) count)
                .failureRate(failureScore)
                .detectedAt(LocalDateTime.now())
                .build();

        resultRepository.save(result);

        System.out.println("🚨 DETECTED: " + ip + " risk=" + risk + " score=" + score);
    }*/
    public void detect(String ip, int status, long count) {

        DetectionResult result = analyze(ip, status, count);

        if (count < 50) return;
        if (result.getRiskLevel() == RiskLevel.LOW) return;

        // 1. Redis는 항상 최신 상태
        alertDedupService.saveAlertToRedis(result);

        // 2. DB는 insert or update
        saveOrUpdate(result);
    }

    public void saveOrUpdate(DetectionResult result) {

        Optional<AnomalyResult> optional =
                resultRepository.findTopByIpOrderByDetectedAtDesc(result.getIp());

        if (optional.isEmpty()) {
            // 최초 저장
            saveNew(result);
            return;
        }

        AnomalyResult existing = optional.get();

        // 위험도 비교
        if (isHigherRisk(result, existing)) {
            update(existing, result);
        }
    }


    private boolean isHigherRisk(DetectionResult newResult, AnomalyResult existing) {

        // 1. RiskLevel 우선
        if (newResult.getRiskLevel().ordinal() >
                RiskLevel.valueOf(existing.getRiskLevel()).ordinal()) {
            return true;
        }

        // 2. Score 비교
        return newResult.getScore() > existing.getScore();
    }

    private void update(AnomalyResult entity, DetectionResult result) {

        entity.setRiskLevel(result.getRiskLevel().name());
        entity.setScore(result.getScore());
        entity.setRequestCount((int) result.getRequestCount());
        entity.setFailureRate(result.getFailureRate());
        entity.setDetectedAt(LocalDateTime.now());

        resultRepository.save(entity);
    }
    private void saveNew(DetectionResult result) {

        AnomalyResult entity = AnomalyResult.builder()
                .ip(result.getIp())
                .riskLevel(result.getRiskLevel().name())
                .score(result.getScore())
                .requestCount((int) result.getRequestCount())
                .failureRate(result.getFailureRate())
                .detectedAt(LocalDateTime.now())
                .build();

        resultRepository.save(entity);
    }




    private DetectionResult analyze(String ip, int status, long count) {
        double failureRate = (status >= 400) ? 1.0 : 0.0;

        RiskLevel risk = classifyRisk(count, failureRate);
        double score = calculateScore(count, failureRate);

        return DetectionResult.builder()
                .ip(ip)
                .riskLevel(risk)
                .score(score)
                .requestCount(count)
                .failureRate(failureRate)
                .build();
    }

    private RiskLevel classifyRisk(long count, double failureScore) {

        if (count >= 100) return RiskLevel.HIGH;
        if (count >= 50) return RiskLevel.MEDIUM;

        if (failureScore > 0) return RiskLevel.MEDIUM;

        return RiskLevel.LOW;
    }

    private double calculateScore(long count, double failureScore) {
        return (count * 0.8) + (failureScore * 20);
    }

    private void saveToDatabase(DetectionResult result) {

        AnomalyResult entity = AnomalyResult.builder()
                .ip(result.getIp())
                .riskLevel(result.getRiskLevel().name())
                .score(result.getScore())
                .requestCount((int) result.getRequestCount())
                .failureRate(result.getFailureRate())
                .detectedAt(LocalDateTime.now())
                .build();

        resultRepository.save(entity);
    }
}