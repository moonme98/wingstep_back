
package com.wingstep.service.domain.workrecord;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class Measurement {
    private int measurementId;
    private int workRecordId;
    private LocalDateTime measurementDatetime;
    private String measurementLocation; // 반드시 위도/경도 "37.5312, 85.0213" 형식으로 입력 
    private double speed;
    private double distance;
    private int steps;
    private int heartrate;
    private  int calorie;

    // getter/setter 생략
}
