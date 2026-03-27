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
    public Feature extractFeatureForIp(String ip) {
        List<AccessLog> logs = logRepository.findByIp(ip);
        if (logs.isEmpty()) return null;

        int total = logs.size();
        long failCount = logs.stream().filter(l -> l.getStatus() >= 400).count();
        double failureRate = (double) failCount / total;
        int distinctUrlCount = (int) logs.stream().map(AccessLog::getUrl).distinct().count();
        double averageUrlLength = logs.stream()
                .mapToInt(l -> l.getUrl() != null ? l.getUrl().length() : 0)
                .average().orElse(0.0);
        int hourOfDay = logs.stream()
                .filter(l -> l.getTimestamp() != null)
                .map(l -> l.getTimestamp().getHour())
                .reduce((a, b) -> a).orElse(0);
        return new Feature(ip, total, failureRate, distinctUrlCount, averageUrlLength, hourOfDay);
    }

// CSV 내보내기 메서드 (누적 저장: 신규 생성 또는 IP별 업데이트)
    public void exportFeaturesToCsv(String outputFilePath) {
        List<Feature> newFeatures = extractFeatures();
        java.nio.file.Path filePath = java.nio.file.Path.of(outputFilePath);
        
        // 기존 파일이 있으면 기존 데이터를 읽어서 merge
        java.util.Map<String, Feature> featureMap = new java.util.LinkedHashMap<>();
        
        if (java.nio.file.Files.exists(filePath)) {
            try {
                List<String> lines = java.nio.file.Files.readAllLines(filePath);
                // 첫 번째 라인(header) 제외하고 읽기
                for (int i = 1; i < lines.size(); i++) {
                    String[] parts = lines.get(i).split(",");
                    if (parts.length >= 6) {
                        Feature f = new Feature(
                            parts[0], // ip
                            Integer.parseInt(parts[1]), // requestCount
                            Double.parseDouble(parts[2]), // failureRate
                            Integer.parseInt(parts[3]), // distinctUrlCount
                            Double.parseDouble(parts[4]), // averageUrlLength
                            Integer.parseInt(parts[5]) // hourOfDay
                        );
                        featureMap.put(f.getIp(), f);
                    }
                }
            } catch (java.io.IOException e) {
                // 파일 읽기 실패시 새로 생성
            }
        }
        
        // 새 데이터로 기존 데이터 업데이트 (IP별 최신 데이터 유지)
        for (Feature f : newFeatures) {
            featureMap.put(f.getIp(), f);
        }

        String header = "ip,requestCount,failureRate,distinctUrlCount,averageUrlLength,hourOfDay";

        try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
                filePath,
                java.nio.charset.StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.WRITE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
        )) {
            writer.write(header);
            writer.newLine();
            for (Feature f : featureMap.values()) {
                writer.write(String.format("%s,%d,%.6f,%d,%.2f,%d",
                        f.getIp(), f.getRequestCount(), f.getFailureRate(), f.getDistinctUrlCount(), f.getAverageUrlLength(), f.getHourOfDay()));
                writer.newLine();
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to write feature CSV file", e);
        }
    }
}