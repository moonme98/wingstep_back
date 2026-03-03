package com.wingstep.service.domain.battle;


import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Battle {

    // BATTLE_ID
    private Integer  battleId;

    // COURSE_ID
    private Integer courseId;

    // USER_ID (생성자 ID)
    private String userId;

    // WORK_RECORD_ID
    private Integer workRecordId;

    // BATTLE_NAME
    private String battleName;

    // MODE : 0 개인전, 1 팀전
    private Integer mode;

    // STATUS : 0 모집중, 1 모집완료, 2 종료
    private Integer status;

    // START_DATETIME
    private LocalDateTime startDatetime;

    // END_DATETIME
    private LocalDateTime endDatetime;

    // TEAM_SIZE (2~10)
    private Integer teamSize;

    // GENDER_TYPE : 0 혼성, 1 여, 2 남
    private Integer genderType;
    
    //코스 이미지
    private String courseImages;
    
    private String courseName;     // 코스명
    private String ownerNickname; 
    
private Integer joinCount;      // 현재 참가 인원 수
private Boolean alreadyJoined;  // 로그인 유저가 이미 참가했는지
    
    
    
    
}
