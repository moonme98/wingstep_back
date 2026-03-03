// com.wingstep.service.domain.BattleTop

package com.wingstep.service.domain.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattleTop {

    private String userId;    // USERS.USER_ID
    private String nickname;  // USERS.USER_NAME (닉네임/이름)
    private int winCount;     // 승리 횟수
    private Integer level;     // null 가능하면 Integer
    private String  avatarUrl; // 아바타 이미지 경로 (null이면 기본 이미지 사용)
    
}
