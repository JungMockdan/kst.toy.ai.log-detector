package kst.toy.ai.logdetector.controller;

import kst.toy.ai.logdetector.domain.AnomalyResult;
import kst.toy.ai.logdetector.repository.AnomalyResultRepository;
import kst.toy.ai.logdetector.service.RedisWindowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/results")
@RequiredArgsConstructor
public class ResultController {

    private final AnomalyResultRepository repository;
    private final RedisWindowService redisService;

    @GetMapping
    public List<AnomalyResult> getResults() {
        return repository.findAll();
    }
    //최신 탐지 조회 (1분) : db 저장된 것중.
    @GetMapping("/latest")
    public List<AnomalyResult> getLatest() {
//        return repository.findAll(Sort.by(Sort.Direction.DESC, "detectedAt"));
        return repository.findRecent(LocalDateTime.now().minusMinutes(1));
    }

    // redis alert 로 저장된 내용 조회
    @GetMapping("/active-alerts")
    public Map<String, Object> activeIps() {
        return redisService.getActiveIps();
    }
}