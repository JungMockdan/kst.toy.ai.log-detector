package kst.toy.ai.logdetector.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    private double aiScore; // AI anomaly score

    private String riskLevel; // LOW / MEDIUM / HIGH

    private int requestCount;

    private double failureRate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime detectedAt;

    @Override
    public String toString() {
        return "AnomalyResult{" +
                "id=" + id +
                ", ip='" + ip + '\'' +
                ", score=" + score +
                ", aiScore=" + aiScore +
                ", riskLevel='" + riskLevel + '\'' +
                ", requestCount=" + requestCount +
                ", failureRate=" + failureRate +
                ", detectedAt=" + detectedAt +
                '}';
    }
}