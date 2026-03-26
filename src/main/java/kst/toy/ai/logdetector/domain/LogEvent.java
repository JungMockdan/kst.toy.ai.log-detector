package kst.toy.ai.logdetector.domain;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEvent {

    private String ip;
    private String url;
    private int status;
    private long timestamp;

    // ===== 추가 =====

    private String eventId;     // 추적 + 중복 방지
    private long createdAt;    // Producer 생성 시각
    private long consumedAt;   // Consumer 수신 시각
}
