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
public class JoinBattle {

    // BATTLE_ID (PK1, FK)
    private int battleId;

    // USER_ID (PK2, FK)
    private String userId;

    // WIN_STATUS : 0 승, 1 패
    private Integer winStatus;

    // TEAM : 0 개인전, 1:1팀, 2:2팀
    private Integer team;
    
    private String nickname; 
    
    private LocalDateTime finishTime;
    
 // 로그인 사용자가 이 배틀에 참가했는지 여부
    private Boolean alreadyJoined;

		
	
    
}
