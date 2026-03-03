package com.wingstep;


import com.wingstep.service.battle.BattleDao;
import com.wingstep.service.domain.battle.Battle;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * BATTLE DAO 단위 테스트
 * -----------------------------
 * - MyBatis 매퍼 XML이 제대로 동작하는지 단독 확인
 */
@SpringBootTest
class BattleDaoTest {

    @Autowired
    private BattleDao battleDao;

    //@Test
    void testGetBattle() {
        int battleId = 1; // 테스트용 ID
        Battle battle = battleDao.getBattle(battleId);

        if (battle == null) {
            System.out.println("DAO : battleId=" + battleId + " 결과 없음");
        } else {
            System.out.println("DAO : 조회 성공 => " + battle.getBattleName());
        }
    }
}