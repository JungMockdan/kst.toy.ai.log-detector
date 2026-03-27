package kst.toy.ai.logdetector.domain;

import kst.toy.ai.logdetector.domain.enm.RiskLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DetectionResult {

    private String ip;
    private RiskLevel riskLevel;
    private double score;
    private double aiScore;
    private long requestCount;
    private double failureRate;
}