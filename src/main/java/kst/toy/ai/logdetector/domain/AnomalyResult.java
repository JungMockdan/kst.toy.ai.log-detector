package kst.toy.ai.logdetector.domain;

import jakarta.persistence.*;
import kst.toy.ai.logdetector.domain.enm.RiskLevel;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String ip;

    private double score; // Z-score

    private String riskLevel; // LOW / MEDIUM / HIGH

    private int requestCount;

    private double failureRate;

    private LocalDateTime detectedAt;

    @Override
    public String toString() {
        return "AnomalyResult{" +
                "id=" + id +
                ", ip='" + ip + '\'' +
                ", score=" + score +
                ", riskLevel='" + riskLevel + '\'' +
                ", requestCount=" + requestCount +
                ", failureRate=" + failureRate +
                ", detectedAt=" + detectedAt +
                '}';
    }
}