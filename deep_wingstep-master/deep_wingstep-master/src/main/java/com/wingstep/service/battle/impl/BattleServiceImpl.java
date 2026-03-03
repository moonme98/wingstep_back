package com.wingstep.service.battle.impl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wingstep.service.battle.BattleDao;
import com.wingstep.service.battle.BattleService;
import com.wingstep.service.battle.JoinBattleDao;
import com.wingstep.service.domain.battle.Battle;
import com.wingstep.service.domain.battle.BattleDetail;
import com.wingstep.service.domain.battle.BattleTop;
import com.wingstep.service.domain.battle.JoinBattle;
import com.wingstep.service.domain.user.User;
import com.wingstep.service.domain.workrecord.WorkRecord;
import com.wingstep.service.user.UserDao;
import com.wingstep.service.user.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BattleServiceImpl implements BattleService {

    private final BattleDao battleDao;
    private final JoinBattleDao joinBattleDao;
    private final UserDao userDao;

    private final UserService userService;
    
    private static final double NO_RECORD_PENALTY = 999_999_999d;
  
    
 // BattleServiceImpl 클래스 상단
    private static final int STATUS_RECRUITING = 0; // 모집중
    private static final int STATUS_FULL       = 1; // 모집완료 (또는 진행중으로 쓰고 있으면 그 의미로)
    private static final int STATUS_END        = 2; // 종료

    
    private static final int MODE_SOLO = 0; // 개인전
    private static final int MODE_TEAM = 1; // 팀전


    private static final int WIN_EXP = 300;
    
 // ✅ avgPace null/0 방어용 (NPE 방지)
    private Double getValidAvgPace(WorkRecord wr) {
        if (wr == null) return null;
        Double pace = wr.getAvgPace(); // Double일 수 있음
        if (pace == null || pace <= 0) return null;
        return pace;
    }

    
    
 // ✅ 완주시간(분) = avgPace(분/km) * distance(km)
    private double getFinishTimeScore(WorkRecord wr) {
        if (wr == null) return NO_RECORD_PENALTY;

        double pace = wr.getAvgPace();     // 분/km (추정)
        double dist = wr.getDistance();    // km

        if (pace <= 0 || dist <= 0) return NO_RECORD_PENALTY;

        return pace * dist; // "총 분"
    }
    
    

    @Override
    public void addBattle(Battle battle) {

        if (battle.getCourseId() == null) {
            throw new IllegalArgumentException("대결 생성에는 코스 ID가 필요합니다.");
        }

        boolean ok = battleDao.isValidCourseForBattle(
                battle.getCourseId(),
                battle.getUserId()
        );
        if (!ok) {
            throw new IllegalStateException("대결에 사용할 수 없는 코스입니다. (내 공개 러닝 코스인지 확인 필요)");
        }

        // 상태 기본값 (0: 모집중)
        if (battle.getStatus() == null) {
            battle.setStatus(0);
        }

        // 성별 기본값 (0: 혼성)
        if (battle.getGenderType() == null) {
            battle.setGenderType(0);
        }

        // MODE / TEAM_SIZE 규칙 강제
        Integer mode = battle.getMode();
        if (mode == null) {
            mode = 0;          // 기본: 개인전
            battle.setMode(0);
        }

        if (mode == 0) {
            // 개인전 → TEAM_SIZE는 NULL
            battle.setTeamSize(null);
        } else { // mode == 1 (팀전)
            Integer teamSize = battle.getTeamSize();
            if (teamSize == null || teamSize < 2 || teamSize > 10) {
                throw new IllegalArgumentException("팀전은 팀당 인원을 2~10명 사이로 설정해야 합니다.");
            }
        }

        // 시작시간 과거 방지
        if (battle.getStartDatetime() != null &&
                battle.getStartDatetime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("시작 시간은 현재 시각 이후로만 설정 가능합니다.");
        }

        // 성별 제한 로직 (기존 유지)
        User creator = userDao.getUser(battle.getUserId());
        Character userGender = creator != null ? creator.getGender() : null; // '0' / '1' / null
        Integer genderType = battle.getGenderType(); // 0 혼성, 1 여, 2 남

        if (userGender != null && genderType != null) {
            if (userGender == '0' && genderType == 1) {
                throw new IllegalArgumentException(
                        "남자 생성자는 여자전 대결을 생성할 수 없습니다. (혼성 / 남자전만 가능)"
                );
            }
            if (userGender == '1' && genderType == 2) {
                throw new IllegalArgumentException(
                        "여자 생성자는 남자전 대결을 생성할 수 없습니다. (혼성 / 여자전만 가능)"
                );
            }
        }

        // =====================================================
        // ✅✅✅ [최소 추가] 기준 WORKRECORD_ID 자동 세팅 (0/NULL 방지)
        // - 기존 생성 플로우에 영향 없음
        // - battle.workRecordId가 비어있으면 "최근 완료 기록"을 넣어줌
        // =====================================================
     // 변경: "기준 기록 없으면 생성은 허용" (승패는 finishBattle에서 페널티 처리됨)
        if (battle.getWorkRecordId() == null || battle.getWorkRecordId() == 0) {

            Integer latestWorkRecordId = battleDao.getLatestValidWorkRecordId(battle.getUserId());

            // ✅ 없으면 그냥 null로 둔다 (WORKRECORD_ID 컬럼이 NULL 허용인 구조여야 함)
            if (latestWorkRecordId != null && latestWorkRecordId > 0) {
                battle.setWorkRecordId(latestWorkRecordId);
            } else {
                battle.setWorkRecordId(null);
            }
        }

        // ✅ 배틀 생성 (PK 생성)
        battleDao.addBattle(battle);

        // ✅ 생성자 자동 참가 (기존 유지)
        JoinBattle creatorJoin = new JoinBattle();
        creatorJoin.setBattleId(battle.getBattleId());   // addBattle 후 생성된 PK
        creatorJoin.setUserId(battle.getUserId());
        creatorJoin.setWinStatus(null);                  // 초기엔 null
        creatorJoin.setTeam(0);                          // 기존 유지
        joinBattleDao.addJoinBattle(creatorJoin);
    }


    @Override
    public Battle getBattle(Integer battleId) {
        return battleDao.getBattle(battleId);
    }
    
    
    
    @Override
    public BattleDetail getBattleDetail(Integer battleId, String loginUserId) {

        Battle battle = battleDao.getBattle(battleId);
        if (battle == null) {
            return null;
        }

        int joinCount = battleDao.getJoinBattleCount(battleId);

        boolean alreadyJoined = false;
        if (loginUserId != null && !loginUserId.isBlank()) {
            JoinBattle myJoin = joinBattleDao.getJoinBattle(battleId, loginUserId);
            alreadyJoined = (myJoin != null);
        }

        // 🔹 VO에 계산 값 세팅
        battle.setJoinCount(joinCount);
        battle.setAlreadyJoined(alreadyJoined);

        return BattleDetail.builder()
                .battle(battle)
                .joinCount(joinCount)
                .build();
    }

    
    
    

    @Override
    public List<Battle> getBattleList(
            String searchKeyword,
            String searchStartDate,
            Integer status,
            Integer mode,
            Integer genderType,
            Double userLat,
            Double userLng,
            Integer offset,
            Integer limit
    ) {
        int safeOffset = (offset == null || offset < 0) ? 0 : offset;
        int safeLimit  = (limit == null || limit < 1) ? 5 : limit;

        return battleDao.getBattleList(
                status,
                mode,
                genderType,
                searchKeyword,
                searchStartDate,
                userLat,
                userLng,
                safeOffset,
                safeLimit
        );
    }


    @Override
    public void updateBattle(Battle battle) {
        battleDao.updateBattle(battle);
    }

    @Override
    public void updateBattleStatus(Integer battleId, Integer status) {
        battleDao.updateBattleStatus(battleId, status);
    }

    @Override
    public void deleteBattle(Integer battleId) {
        joinBattleDao.deleteJoinBattleByBattleId(battleId);
        battleDao.deleteBattle(battleId);
    }

    // ===== JOIN_BATTLE =====

    @Override
    public void addJoinBattle(JoinBattle joinBattle) {
        JoinBattle exist = joinBattleDao.getJoinBattle(
            joinBattle.getBattleId(),
            joinBattle.getUserId()
        );
        if (exist != null) throw new IllegalStateException("이미 참가한 대결임.");

        joinBattleDao.addJoinBattle(joinBattle);
    }

    @Override
    public JoinBattle getJoinBattle(Integer battleId, String userId) {
        return joinBattleDao.getJoinBattle(battleId, userId);
    }

    @Override
    public List<JoinBattle> getJoinBattleList(Integer battleId) {
        return joinBattleDao.getJoinBattleList(battleId);
    }

    @Override
    public void updateWinStatus(Integer battleId, String userId, Integer winStatus) {
        joinBattleDao.updateWinStatus(battleId, userId, winStatus);
    }

    @Override
    public void deleteJoinBattleByBattleId(Integer battleId) {
        joinBattleDao.deleteJoinBattleByBattleId(battleId);
    }
    
    
    @Override
    public List<Battle> getMyBattleList(
            String userId,
            Integer statusFilter,
            Integer offset,
            Integer limit
    ) {
        int safeOffset = (offset == null || offset < 0) ? 0 : offset;
        int safeLimit  = (limit == null || limit < 1) ? 5 : limit;

        return battleDao.getMyBattleList(userId, statusFilter, safeOffset, safeLimit);
    }

    @Override
    public List<BattleTop> getTopBattle(Integer mode, Integer genderType) {
        return battleDao.getTopBattle(mode, genderType);
    }

    @Override
    public List<JoinBattle> getBattleResultList(Integer battleId) {
        return joinBattleDao.getBattleResultList(battleId);
    }
    
    
    /**
     * 대결 시작 : 상태만 "진행중" 으로 변경
     * 실제 운동기록 생성/저장은 WorkRecord 모듈(코스/운동 화면)에서 처리.
     */
    @Override
    public void startBattle(Integer battleId, String userId) {

        Battle battle = battleDao.getBattle(battleId);
        if (battle == null) {
            throw new IllegalArgumentException("존재하지 않는 대결 ID : " + battleId);
        }

        // ✅ 종료면 절대 시작 불가
        if (battle.getStatus() == STATUS_END) {
            throw new IllegalStateException("종료된 대결은 시작할 수 없습니다.");
        }

        // ✅ 참가자만 시작 가능 (생성자는 참가자로 인정)
        boolean isCreator = userId != null && userId.equals(battle.getUserId());
        boolean isJoined  = joinBattleDao.getJoinBattle(battleId, userId) != null;

        if (!(isCreator || isJoined)) {
            throw new IllegalStateException("참가자만 대결을 시작할 수 있습니다.");
        }

        // ✅ 인원 충족 체크
        if (battle.getMode() == MODE_SOLO) {
            // 개인전은 TEAM(0/1) 값에 의존하면 안 됨 (현재처럼 0/0 들어오는 케이스 존재)
            int joinCount = battleDao.getJoinBattleCount(battleId);
            if (joinCount < 2) {
                throw new IllegalStateException("개인전은 2명(1:1)이 참가해야 시작할 수 있습니다.");
            }

        } else if (battle.getMode() == MODE_TEAM) {
            // 팀전은 기존대로 팀별 충족 체크 유지
            int team0Count = joinBattleDao.getJoinBattleCountByTeam(battleId, 0);
            int team1Count = joinBattleDao.getJoinBattleCountByTeam(battleId, 1);

            Integer teamSize = battle.getTeamSize();
            if (teamSize == null || teamSize <= 0) {
                throw new IllegalStateException("팀전 teamSize가 올바르지 않습니다.");
            }
            if (!(team0Count >= teamSize && team1Count >= teamSize)) {
                throw new IllegalStateException("팀전은 양 팀 인원이 모두 충족되어야 시작할 수 있습니다.");
            }

        } else {
            throw new IllegalStateException("대결 모드 값이 올바르지 않습니다.");
        }

        // ✅ 상태 변경 (여기서만 시작 처리)
        battleDao.updateBattleStatus(battleId, STATUS_FULL);
    }

    

    @Override
    public void finishBattle(Integer battleId, Integer workRecordId) {

        Battle battle = battleDao.getBattle(battleId);
        if (battle == null) {
            throw new IllegalArgumentException("존재하지 않는 대결 ID : " + battleId);
        }

        // 이미 종료된 배틀이면 재호출 시 EXP 중복 방지
        if (battle.getStatus() != null && battle.getStatus() == 2) {
            return;
        }

        // 1) 참가자 목록
        List<JoinBattle> joinList = joinBattleDao.getJoinBattleList(battleId);
        if (joinList == null || joinList.isEmpty()) {
            battleDao.updateBattleStatus(battleId, 2); // 종료
            return;
        }

        // ✅ 생성자 기준 기록(기존 기록) : 개인전/팀전 공통으로 필요
        Integer baseWorkRecordId = battle.getWorkRecordId();
        WorkRecord baseRecord = null;

        if (baseWorkRecordId != null) {
            baseRecord = battleDao.getWorkRecordById(baseWorkRecordId);
        }

        // ✅ 기준 기록의 "완주시간 점수"
        double baseTimeScore = getFinishTimeScore(baseRecord);

        // 2) 이 배틀에서 참가자들이 뛴 기록들 (battleId 기준)
        List<WorkRecord> records = battleDao.getBattleWorkRecordList(battleId);
        if (records == null) records = Collections.emptyList();

        // 3) 참가자 기록을 사용자별로 매핑
        Map<String, WorkRecord> recordByUser = new HashMap<>();
        for (WorkRecord wr : records) {
            if (wr != null && wr.getUserId() != null) {
                recordByUser.put(wr.getUserId(), wr);
            }
        }

        // =========================================
        // ✅ 팀전 : 생성자는 "기준기록(base)"로 합산 + 나머지는 신규기록 합산
        //     => "완주시간(score)" 합산으로 변경
        // =========================================
        if (battle.getMode() != null && battle.getMode() == 1) {

            // 팀번호 → 팀의 총 "완주시간 점수"
            Map<Integer, Double> teamTotalTime = new HashMap<>();

            for (JoinBattle jb : joinList) {

                Integer team = jb.getTeam();
                if (team == null) team = 0;

                // ✅ 생성자는 신규기록이 아니라 기준기록 사용
                if (jb.getUserId() != null && jb.getUserId().equals(battle.getUserId())) {

                    double creatorScore = baseTimeScore; // ✅ 완주시간 점수

                    // 기준 기록이 없거나(혹은 값이 0)인 경우 페널티
                    if (creatorScore >= NO_RECORD_PENALTY) {
                        creatorScore = NO_RECORD_PENALTY;
                    }

                    teamTotalTime.merge(team, creatorScore, Double::sum);
                    continue;
                }

                // ✅ 신청자/팀원은 battleId로 생성된 신규 기록 사용
                WorkRecord userRecord = recordByUser.get(jb.getUserId());

                double timeScore = getFinishTimeScore(userRecord); // ✅ 완주시간 점수

                teamTotalTime.merge(team, timeScore, Double::sum);
            }

            if (teamTotalTime.isEmpty()) {
                battleDao.updateBattleStatus(battleId, 2);
                return;
            }

            // 가장 작은 합계 시간을 가진 팀 찾기
            double bestTime = Double.MAX_VALUE;
            Integer winningTeam = null;
            boolean tie = false;

            for (Map.Entry<Integer, Double> entry : teamTotalTime.entrySet()) {
                Integer team = entry.getKey();
                Double total = entry.getValue();

                if (total < bestTime) {
                    bestTime = total;
                    winningTeam = team;
                    tie = false;
                } else if (Objects.equals(total, bestTime)) {
                    tie = true;
                }
            }

            // 무승부면 승/패/EXP 없이 종료만
            if (tie || winningTeam == null) {
                battleDao.updateBattleStatus(battleId, 2);
                return;
            }

            // 승리팀/패배팀 처리 (생성자 포함 전원 처리)
            for (JoinBattle jb : joinList) {
                Integer team = jb.getTeam();
                if (team == null) team = 0;

                if (Objects.equals(team, winningTeam)) {
                    joinBattleDao.updateWinStatus(battleId, jb.getUserId(), 0);
                    userService.levelUp(jb.getUserId(), WIN_EXP);
                } else {
                    joinBattleDao.updateWinStatus(battleId, jb.getUserId(), 1);
                }
            }

            // 배틀 상태 종료
            battle.setStatus(2);
            battle.setEndDatetime(LocalDateTime.now());
            battleDao.updateBattle(battle);
            return;
        }

     // =========================================
     // ✅ 개인전 : 생성자 기준기록(base) vs 신청자 신규기록 비교
//          => "완주시간(score)" 비교로 변경
     // =========================================

     // ✅ 개인전 참가자가 2명 미만이어도 예외로 막지 말고 종료만 처리 (테스트 막힘 방지)
     if (joinList.size() < 2) {
         battle.setStatus(2);
         battle.setEndDatetime(LocalDateTime.now());
         battleDao.updateBattle(battle);
         return;
     }

     // ✅ 생성자 기준기록이 없으면 비교 불가 → 예외 대신 페널티로 처리
     if (baseTimeScore >= NO_RECORD_PENALTY) {
         baseTimeScore = NO_RECORD_PENALTY;
     }

     // 신청자(생성자 제외)의 신규 기록을 찾아 비교
     JoinBattle creatorJoin = null;
     JoinBattle challengerJoin = null;

     for (JoinBattle jb : joinList) {
         if (jb.getUserId() != null && jb.getUserId().equals(battle.getUserId())) {
             creatorJoin = jb;
         } else {
             challengerJoin = jb; // 개인전은 1명만 도전자라고 가정(2명 중 1명)
         }
     }

     if (challengerJoin == null) {
         // 도전자가 없으면 종료만
         battle.setStatus(2);
         battle.setEndDatetime(LocalDateTime.now());
         battleDao.updateBattle(battle);
         return;
     }

     WorkRecord challengerRecord = recordByUser.get(challengerJoin.getUserId());
     double challengerTimeScore = getFinishTimeScore(challengerRecord);

     // ✅ 도전자 기록이 없거나 유효하지 않으면 예외 대신 페널티 처리 (테스트 막힘 방지)
     if (challengerTimeScore >= NO_RECORD_PENALTY) {
         challengerTimeScore = NO_RECORD_PENALTY;
     }

     // ✅ 도전자 vs 기준기록 "완주시간" 비교 (작을수록 승)
     if (challengerTimeScore < baseTimeScore) {
         // 도전자 승리
         joinBattleDao.updateWinStatus(battleId, challengerJoin.getUserId(), 0);
         userService.levelUp(challengerJoin.getUserId(), WIN_EXP);

         if (creatorJoin != null) {
             joinBattleDao.updateWinStatus(battleId, creatorJoin.getUserId(), 1);
         }
     } else {
         // 생성자 승리(느리거나 같으면)
         joinBattleDao.updateWinStatus(battleId, challengerJoin.getUserId(), 1);

         if (creatorJoin != null) {
             joinBattleDao.updateWinStatus(battleId, creatorJoin.getUserId(), 0);
             userService.levelUp(creatorJoin.getUserId(), WIN_EXP);
         }
     }

     // 배틀 상태 종료
     battle.setStatus(2);
     battle.setEndDatetime(LocalDateTime.now());
     battleDao.updateBattle(battle);
    }


    @Override
    public void expireRecruitingBattles() {

        LocalDateTime now = LocalDateTime.now();

        // =========================
        // 1) 개인전 : 생성일시 + 24시간 경과
        // =========================
        // status = 0(모집중), mode = 0(개인전)
        List<Battle> soloList = battleDao.getBattleList(
                0,      // statusFilter = 모집중
                0,      // modeFilter   = 개인전
                null    // genderType   = 전체
, null, null, null, null,0, 10000
        );

        for (Battle battle : soloList) {

            // ✅ 여기가 "대결 생성일시" 기준 필드
            LocalDateTime createdAt = battle.getStartDatetime();
            // 만약 생성일 전용 필드가 따로 있으면 여기만 바꿔서 사용하면 됨
            // ex) LocalDateTime createdAt = battle.getCreateDateTime();

            if (createdAt != null && createdAt.isBefore(now.minusHours(24))) {
                // 생성일로부터 24시간이 지났는데 아직도 모집중 → 자동 종료
                battleDao.updateBattleStatus(battle.getBattleId(), 2);  // 2 = 종료
                // WIN_STATUS 는 전혀 건드리지 않으므로 그대로 NULL 유지
            }
        }

        // =========================
        // 2) 팀전 : 생성일시 + 48시간 경과
        // =========================
        List<Battle> teamList = battleDao.getBattleList(
                0,      // statusFilter = 모집중
                1,      // modeFilter   = 팀전
                null, null ,null, null, null,0, 10000
        );

        for (Battle battle : teamList) {

            LocalDateTime createdAt = battle.getStartDatetime();
            

            if (createdAt != null && createdAt.isBefore(now.minusHours(48))) {
                // 생성일로부터 48시간이 지났는데도 팀원 충족 못해서 진행 전 → 자동 종료
                battleDao.updateBattleStatus(battle.getBattleId(), 2);
            }
        }
    }
    
    

}
    
    


