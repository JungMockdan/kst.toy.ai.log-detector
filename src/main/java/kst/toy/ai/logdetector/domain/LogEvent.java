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
}
