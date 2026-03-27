package kst.toy.ai.logdetector.controller;

import kst.toy.ai.logdetector.service.DashboardSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardSseService sseService;

    /**
     * SSE 구독 엔드포인트.
     * 브라우저가 EventSource('/dashboard/stream') 로 연결하면
     * 이후 이상 탐지 결과가 발생할 때마다 push 된다.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return sseService.subscribe();
    }
}
