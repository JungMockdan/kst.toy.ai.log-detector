package kst.toy.ai.logdetector.controller;

import kst.toy.ai.logdetector.analyzer.Feature;
import kst.toy.ai.logdetector.domain.AnomalyResult;
import kst.toy.ai.logdetector.service.AnomalyService;
import kst.toy.ai.logdetector.service.FeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/analyze")
@RequiredArgsConstructor
public class AnalyzeController {

    private final FeatureService featureService;
    private final AnomalyService anomalyService;

    @PostMapping
    public List<AnomalyResult> analyze() {

        List<Feature> features = featureService.extractFeatures();

        return anomalyService.detect(features);
    }

    @GetMapping("/features")
    public List<Feature> getFeatures() {
        return featureService.extractFeatures();
    }

    @PostMapping("/features/export")
    public String exportFeatures(@RequestParam(defaultValue = "feature-data.csv") String fileName) {
        String outputPath = java.nio.file.Paths.get(System.getProperty("user.dir"), fileName).toString();
        featureService.exportFeaturesToCsv(outputPath);
        return "Exported feature CSV to " + outputPath;
    }
}