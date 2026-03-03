package com.wingstep.service.battle;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wingstep.service.domain.battle.JoinBattle;


@Mapper
public interface JoinBattleDao {

    int addJoinBattle(JoinBattle joinBattle);

    JoinBattle getJoinBattle(
        @Param("battleId") Integer battleId,
        @Param("userId") String userId
    );

    List<JoinBattle> getJoinBattleList(@Param("battleId") Integer battleId);

    int updateWinStatus(
        @Param("battleId") Integer battleId,
        @Param("userId") String userId,
        @Param("winStatus") Integer winStatus
    );

    int deleteJoinBattleByBattleId(@Param("battleId") Integer battleId);

    int getJoinBattleCountByTeam(
        @Param("battleId") Integer battleId,
        @Param("team") Integer team
   
    		
    		
    		);
    List<JoinBattle> getBattleResultList(@Param("battleId") Integer battleId);
}
