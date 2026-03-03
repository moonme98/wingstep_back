package com.wingstep.service.domain.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseUseResponse {
    
    // 코스 정보 (경로 포함)
    private Course course;
    
    // 생성된 운동기록 ID
    private int workRecordId;
}