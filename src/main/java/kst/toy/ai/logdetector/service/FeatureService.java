package kst.toy.ai.logdetector.service;

import kst.toy.ai.logdetector.analyzer.Feature;
import kst.toy.ai.logdetector.domain.AccessLog;
import kst.toy.ai.logdetector.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeatureService {

    private final LogRepository logRepository;

    public List<Feature> extractFeatures() {

        List<String> ips = logRepository.findDistinctIps();
        List<Feature> features = new ArrayList<>();

        for (String ip : ips) {
            List<AccessLog> logs = logRepository.findByIp(ip);

            int total = logs.size();
            long failCount = logs.stream()
                    .filter(l -> l.getStatus() >= 400)
                    .count();

            double failureRate = total == 0 ? 0 : (double) failCount / total;

            int distinctUrlCount = (int) logs.stream()
                    .map(AccessLog::getUrl)
                    .distinct()
                    .count();

            double averageUrlLength = logs.isEmpty() ? 0.0 : logs.stream()
                    .mapToInt(l -> l.getUrl() != null ? l.getUrl().length() : 0)
                    .average()
                    .orElse(0.0);

            int hourOfDay = logs.stream()
                    .filter(l -> l.getTimestamp() != null)
                    .map(l -> l.getTimestamp().getHour())
                    .reduce((a, b) -> a) // take last hour, or you can use most frequent logic
                    .orElse(0);

            features.add(new Feature(ip, total, failureRate, distinctUrlCount, averageUrlLength, hourOfDay));
        }

        return features;
    }
// CSV 내보내기 메서드
    public void exportFeaturesToCsv(String outputFilePath) {
        List<Feature> features = extractFeatures();

        String header = "ip,requestCount,failureRate,distinctUrlCount,averageUrlLength,hourOfDay";

        try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
                java.nio.file.Path.of(outputFilePath),
                java.nio.charset.StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
        )) {
            writer.write(header);
            writer.newLine();
            for (Feature f : features) {
                writer.write(String.format("%s,%d,%.6f,%d,%.2f,%d",
                        f.getIp(), f.getRequestCount(), f.getFailureRate(), f.getDistinctUrlCount(), f.getAverageUrlLength(), f.getHourOfDay()));
                writer.newLine();
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to write feature CSV file", e);
        }
    }
}