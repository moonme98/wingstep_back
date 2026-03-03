package com.wingstep.web.battle;

import com.wingstep.common.jwt.JwtUtil; 

import com.wingstep.service.battle.BattleService;
import com.wingstep.service.domain.battle.Battle;
import com.wingstep.service.domain.battle.BattleDetail;
import com.wingstep.service.domain.battle.BattleTop;
import com.wingstep.service.domain.battle.JoinBattle;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/battle")  // ✅ /api 제거, 요구서 경로와 맞춤
@RequiredArgsConstructor
public class BattleRestController {

    private final BattleService battleService;
    private final JwtUtil jwtUtil;    
    @Value("${wingstep.page.size.default:5}")
    private int defaultPageSize;

    @Value("${wingstep.page.size.max:20}")
    private int maxPageSize;

    
    
    
    private int clampLimit(Integer limit) {
        int size = (limit == null) ? defaultPageSize : limit;
        if (size < 1) size = defaultPageSize;
        if (size > maxPageSize) size = maxPageSize;
        return size;
    }
    
    
    
    
    
    // ==============================
    // 내부 헬퍼 : JWT → 로그인 유저 ID
    // ==============================
    private String getLoginUserId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        String token = auth.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return null;
        }
        return jwtUtil.getUserId(token);
    }
    

    // ==============================
    // 1. 대결 리스트 (battlelist 화면)
    // ==============================
    // 화면정의서 : Path : /battle/listBattle : GET
    @GetMapping("/listBattle")
    public ResponseEntity<List<Battle>> listBattle(
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) String searchStartDate,
            @RequestParam(required = false) Integer statusFilter,
            @RequestParam(required = false) Integer modeFilter,
            @RequestParam(required = false) Integer genderType,
            @RequestParam(required = false) Double userLat,
            @RequestParam(required = false) Double userLng,
            
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) Integer limit
            
    ) {
    	int pageSize = clampLimit(limit);
    	int safeOffset = Math.max(0, offset);

        List<Battle> list = battleService.getBattleList(
                searchKeyword,
                searchStartDate,
                statusFilter,
                modeFilter,
                genderType,
                userLat,
                userLng,
                offset,
                pageSize
        );

        System.out.println(">>> listBattle 호출됨");
        System.out.println(">>> searchStartDate = " + searchStartDate);
        System.out.println(">>> modeFilter = " + modeFilter);
        System.out.println(">>> userLat/userLng = " + userLat + " / " + userLng);

        return ResponseEntity.ok(list);
    }


    
 // ==================
 // 대결 상세 (getBattle 화면)
 // ==================
    @GetMapping("/getBattle")
    public ResponseEntity<BattleDetail> getBattle(
            @RequestParam Integer battleId,
            HttpServletRequest request
    ) {
        String loginUserId = getLoginUserId(request);

        BattleDetail detail = battleService.getBattleDetail(battleId, loginUserId);

        if (detail == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(detail);
    }


    
    
    
    @PostMapping("/addBattle")
    public ResponseEntity<?> addBattle(@RequestBody Battle battle) {

        try {
            // 🔥 서비스의 모든 IllegalArgumentException 여기서 잡힘
            battleService.addBattle(battle);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", true,
                            "message", e.getMessage()
                    ));
        }

        // 🔥 성공 시 기존 코드 100% 그대로 유지됨
        Integer battleId = battle.getBattleId();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("battleId", battleId));
    }



    // ==============================
    // 4. 대결 참가 (getBattle 화면의 "신청" 버튼)
    // ==============================
    // 화면정의서 : Path : /battle/joinBattle : POST
    @PostMapping("/joinBattle")
    public ResponseEntity<Void> joinBattle(
            @RequestBody JoinBattle joinBattle,
            HttpServletRequest request
    ) {
        // 🔹 JWT 에서 로그인 유저 ID가 있으면 그걸로 userId 강제 세팅
        String loginUserId = getLoginUserId(request);
        if (loginUserId != null) {
            joinBattle.setUserId(loginUserId);
        }

        battleService.addJoinBattle(joinBattle);  // 이미 참가한 경우 IllegalStateException 발생
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // ==============================
    // 5. 특정 대결의 참가자 리스트 (내대결/관리 등에서 필요할 수 있음)
    // ==============================
    // 요구서에 명시된 건 아니지만, 이미 Service/Dao가 있으니 보조용으로 둠
    @GetMapping("/joinBattle/list")
    public ResponseEntity<List<JoinBattle>> getJoinBattleList(@RequestParam Integer battleId) {
        return ResponseEntity.ok(battleService.getJoinBattleList(battleId));
    }

    
 // com.wingstep.web.battle.BattleRestController

 // 6. 내 대결 리스트 (내가 참가한 대결들)
    @GetMapping("/listMyBattle")
    public ResponseEntity<List<Battle>> listMyBattle(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Integer statusFilter,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request
    ) {
        String loginUserId = getLoginUserId(request);
        String effectiveUserId = (loginUserId != null) ? loginUserId : userId;

        if (effectiveUserId == null || effectiveUserId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        int pageSize = clampLimit(limit);
        int safeOffset = Math.max(0, offset);

        List<Battle> list =
                battleService.getMyBattleList(effectiveUserId, statusFilter, safeOffset, pageSize);

        return ResponseEntity.ok(list);
    }


 // 7. 대결의 전당 TOP10
 @GetMapping("/topBattle")
 public ResponseEntity<List<BattleTop>> topBattle(
         @RequestParam(required = false) Integer mode,
         @RequestParam(required = false) Integer genderType
 ) {
     List<BattleTop> list = battleService.getTopBattle(mode, genderType);
     return ResponseEntity.ok(list);
 }

    
 @PostMapping("/{battleId}/finish")
 public ResponseEntity<Void> finishBattle(
         @PathVariable Integer battleId,
         @RequestParam Integer workRecordId // ✅ 쿼리스트링으로 받기
 ) {
     battleService.finishBattle(battleId, workRecordId);
     return ResponseEntity.ok().build();
 }


 
    
 @PostMapping("/expire")
 public ResponseEntity<Void> expireBattles() {
     battleService.expireRecruitingBattles();
     return ResponseEntity.ok().build();
 }

//7-1. 대결 결과 조회 (JoinBattle 재사용 버전)
//URL 예시 : /battle/result?battleId=3
@GetMapping("/result")
public ResponseEntity<List<JoinBattle>> getBattleResult(@RequestParam Integer battleId) {

  // 👉 BattleService에 구현해둔 메서드 호출
  List<JoinBattle> resultList = battleService.getBattleResultList(battleId);

  return ResponseEntity.ok(resultList);
}

//==============================
//8. 대결 시작 (getBattle 화면의 "시작" 버튼)
//==============================
//Path : /battle/startBattle : POST
@PostMapping("/startBattle")
public ResponseEntity<?> startBattle(
     @RequestBody Map<String, Integer> body,
     HttpServletRequest request
) {
 String loginUserId = getLoginUserId(request);
 if (loginUserId == null || loginUserId.isBlank()) {
     return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
             .body(Map.of("error", true, "message", "로그인 필요"));
 }

 Integer battleId = body.get("battleId");
 if (battleId == null) {
     return ResponseEntity.badRequest()
             .body(Map.of("error", true, "message", "battleId 누락"));
 }

 try {
     battleService.startBattle(battleId, loginUserId);
     return ResponseEntity.ok(Map.of("ok", true));
 } catch (IllegalArgumentException | IllegalStateException e) {
     return ResponseEntity.status(HttpStatus.BAD_REQUEST)
             .body(Map.of("error", true, "message", e.getMessage()));
 }
}

 
 
    
    
    // ==============================
    // (참고) 아래처럼 "상태 변경 / 삭제" API는
    //       요구서에 없으니까 현재는 노출 안 함
    // ==============================

    /*
    @PostMapping("/updateBattleStatus")
    public ResponseEntity<Void> updateBattleStatus(
            @RequestParam Integer battleId,
            @RequestParam Integer status
    ) {
        battleService.updateBattleStatus(battleId, status);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/deleteBattle")
    public ResponseEntity<Void> deleteBattle(@RequestParam Integer battleId) {
        battleService.deleteBattle(battleId);
        return ResponseEntity.ok().build();
    }
    */

}
