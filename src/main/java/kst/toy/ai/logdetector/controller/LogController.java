package kst.toy.ai.logdetector.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kst.toy.ai.logdetector.dto.LogRequestDto;
import kst.toy.ai.logdetector.service.LogService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @PostMapping()
    public void createLog(@RequestBody LogRequestDto dto) {
        logService.publish(dto);
    }
}