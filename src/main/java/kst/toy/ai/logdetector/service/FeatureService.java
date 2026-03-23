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

            features.add(new Feature(ip, total, failureRate));
        }

        return features;
    }
}