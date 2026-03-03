package com.wingstep.service.battle;

import com.wingstep.service.domain.battle.Battle;
import com.wingstep.service.domain.battle.BattleTop;
import com.wingstep.service.domain.workrecord.WorkRecord;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface BattleDao {

    int addBattle(Battle battle);
    
    boolean isValidCourseForBattle(@Param("courseId") int courseId,
            @Param("userId") String userId);


    Battle getBattle(@Param("battleId") Integer battleId);

    List<Battle> getBattleList(
        @Param("status") Integer status,
        @Param("mode") Integer mode,
        @Param("genderType") Integer genderType,
        @Param("searchKeyword") String searchKeyword,
        @Param("searchStartDate") String searchStartDate,
        @Param("userLat") Double userLat,
        @Param("userLng") Double userLng,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );
    
    List<Battle> getMyBattleList(
            @Param("userId") String userId,
            @Param("status") Integer status,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
        );

        // 대결의 전당 TOP10
        List<BattleTop> getTopBattle(
            @Param("mode") Integer mode,
            @Param("genderType") Integer genderType
        );

    int updateBattle(Battle battle);

    int updateBattleStatus(
        @Param("battleId") Integer battleId,
        @Param("status") Integer status
    );

    int deleteBattle(@Param("battleId") Integer battleId);

    int getJoinBattleCount(@Param("battleId") Integer battleId);
    
    int countFinishedWorkRecordsByBattleId(int battleId);
    WorkRecord getFinishedWorkRecordByBattleIdAndUserId(Map<String, Object> map);

    Integer getLatestValidWorkRecordId(@Param("userId") String userId);


    
    List<WorkRecord> getBattleWorkRecordList(Integer battleId);
    
 // 기준 기록 1개 조회
    WorkRecord getWorkRecordById(@Param("workRecordId") Integer workRecordId);


    
    
    
    
}
