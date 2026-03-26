package kst.toy.ai.logdetector.analyzer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Feature {

    private String ip;
    private int requestCount;
    private double failureRate;
    private int distinctUrlCount;
    private double averageUrlLength;
    private int hourOfDay;
}