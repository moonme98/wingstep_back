package com.wingstep.service.domain.workrecord;

import java.time.LocalDate;

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
public class WorkRecordView {
    private int workRecordId;
    private String userId;
    private String nickname;
    private String courseName;
    private String battleName;
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
