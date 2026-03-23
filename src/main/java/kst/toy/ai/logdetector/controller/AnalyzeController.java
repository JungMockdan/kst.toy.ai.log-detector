package kst.toy.ai.logdetector.controller;

import kst.toy.ai.logdetector.analyzer.Feature;
import kst.toy.ai.logdetector.domain.AnomalyResult;
import kst.toy.ai.logdetector.service.AnomalyService;
import kst.toy.ai.logdetector.service.FeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/****
 *
 * todo 실시간 탐질호 바뀌면서 계산식을 바꿔야 한다.
 * */

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
}