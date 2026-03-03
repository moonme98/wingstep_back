
package com.wingstep.service.domain.workrecord;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.Builder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class WorkRecord {
    private int workRecordId;
    private String userId;
    private Integer courseId;
    private Integer battleId;
    private boolean isMeasure;
    private LocalDate workRecordDate;
    private double distance;
    private double maxSpeed;
    private double avgSpeed;
    private int maxHeartrate;
    private int avgHeartrate;
    private double avgPace; 
    private int cadence;
    private int calorie;

    // getter/setter 생략
}
