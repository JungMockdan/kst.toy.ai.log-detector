package kst.toy.ai.logdetector.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogRequestDto {

    private String ip;
    private String url;
    private int status;
}