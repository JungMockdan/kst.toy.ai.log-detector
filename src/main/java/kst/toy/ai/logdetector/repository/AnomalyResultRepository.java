package kst.toy.ai.logdetector.repository;

import kst.toy.ai.logdetector.domain.AnomalyResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AnomalyResultRepository extends JpaRepository<AnomalyResult, Long> {
    @Query("SELECT a FROM AnomalyResult a WHERE a.detectedAt >= :time")
    List<AnomalyResult> findRecent(LocalDateTime time);

    Optional<AnomalyResult> findTopByIpOrderByDetectedAtDesc(String ip);
}