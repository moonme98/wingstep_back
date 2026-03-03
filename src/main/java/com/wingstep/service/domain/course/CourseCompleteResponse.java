package com.wingstep.service.domain.course;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseCompleteResponse {
    // 이동할 페이지 구분 (GENERAL: 운동기록상세, BATTLE: 대결상세)
    private String type; 
    
    // 이동할 ID (workRecordId 또는 battleId)
    private int targetId;
}