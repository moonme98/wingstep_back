// src/main/java/com/wingstep/service/domain/battle/BattleDetail.java
package com.wingstep.service.domain.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 대결 상세 응답용 DTO
 * - 기존 Battle 도메인은 그대로 두고
 * - 상세 화면에 필요한 추가 정보(joinCount 등)만 감싼다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattleDetail {

    // 순수 BATTLE 도메인
    private Battle battle;

    // 현재 참가 인원 수
    private int joinCount;
    
    // TODO: 나중에 필요해지면 여기에만 추가
    // private boolean joined;
    // private boolean canJoin;
    // private boolean canStart;
    // private String creatorNickname;
    // ...
}
