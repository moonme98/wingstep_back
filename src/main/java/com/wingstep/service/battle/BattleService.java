package com.wingstep.service.battle;

import java.util.List;

import com.wingstep.service.domain.battle.Battle;
import com.wingstep.service.domain.battle.BattleDetail;
import com.wingstep.service.domain.battle.BattleTop;
import com.wingstep.service.domain.battle.JoinBattle;

public interface BattleService {

    // ===== BATTLE =====
    void addBattle(Battle battle);
    Battle getBattle(Integer battleId);
    List<Battle> getBattleList(String searchKeyword, String searchStartDate, Integer status, Integer mode, Integer genderType, Double userLat,
            Double userLng,   Integer offset,
            Integer limit);
    void updateBattle(Battle battle);
    void updateBattleStatus(Integer battleId, Integer status);
    void deleteBattle(Integer battleId);
    
 // 1) 대결 시작 (코스에서 '시작' 눌렀을 때)
    void startBattle(Integer battleId, String userId);

    // 2) 대결 종료 + 결과 계산
    void finishBattle(Integer battleId, Integer workRecordId);
    
    //24시간 48시간
    void expireRecruitingBattles();
    
    BattleDetail getBattleDetail(Integer battleId, String loginUserId);

    // 내가 참가한 대결 리스트
    List<Battle> getMyBattleList(String userId, Integer status,    Integer offset,
            Integer limit);

    // 대결의 전당 TOP10
    List<BattleTop> getTopBattle(Integer mode, Integer genderType);
    
    

    // ===== JOIN_BATTLE =====
    void addJoinBattle(JoinBattle joinBattle);
    JoinBattle getJoinBattle(Integer battleId, String userId);
    List<JoinBattle> getJoinBattleList(Integer battleId);
    void updateWinStatus(Integer battleId, String userId, Integer winStatus);
    void deleteJoinBattleByBattleId(Integer battleId);
    List<JoinBattle> getBattleResultList(Integer battleId);
    
   



}
