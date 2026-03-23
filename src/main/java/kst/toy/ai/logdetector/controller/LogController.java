package kst.toy.ai.logdetector.controller;

import kst.toy.ai.logdetector.domain.AccessLog;
import kst.toy.ai.logdetector.dto.LogRequestDto;
import kst.toy.ai.logdetector.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;



//    @PostMapping
//    public AccessLog createLog(@RequestBody LogRequestDto request) {
//        return logService.save(request);
//    }

    @PostMapping()
    public void createLog(@RequestBody LogRequestDto dto) {
        logService.publish(dto);
    }
}