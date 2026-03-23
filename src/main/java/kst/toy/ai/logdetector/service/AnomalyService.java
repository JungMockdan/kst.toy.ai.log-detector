package kst.toy.ai.logdetector.service;

import kst.toy.ai.logdetector.analyzer.Feature;
import kst.toy.ai.logdetector.domain.AnomalyResult;
import kst.toy.ai.logdetector.domain.enm.RiskLevel;
import kst.toy.ai.logdetector.repository.AnomalyResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyService {

    private final AnomalyResultRepository resultRepository;

/**
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
        List<AnomalyResult> result = features.stream()
                .map(f -> {
                    double z = calculateZScore(f.getRequestCount(), mean, std);
                    RiskLevel risk = classifyRisk(z);
                    f.toString();

                    return AnomalyResult.builder()
                            .ip(f.getIp())
                            .score(z)
                            .riskLevel(risk.name())
                            .requestCount(f.getRequestCount())
                            .failureRate(f.getFailureRate())
                            .detectedAt(LocalDateTime.now())
                            .build();
                })
                .map(resultRepository::save)
                .toList();
        return result;
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
}