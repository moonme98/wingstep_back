package com.wingstep.web.user;

import com.wingstep.common.jwt.JwtUtil;
import com.wingstep.service.domain.user.Avatar;
import com.wingstep.service.domain.user.User;
import com.wingstep.service.user.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserRestController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    
    @Autowired
    private HttpSession session;

    // --------------------------
    // 회원가입
    // --------------------------
    @PostMapping("/addUser")
    public ResponseEntity<?> addUser(@RequestBody User user) {
        int result = userService.addUser(user);
        if (result <= 0) {
            return ResponseEntity.badRequest().body("회원가입 실패");
        }
        return ResponseEntity.ok("회원가입 성공");
    }

    // --------------------------
    // 일반 로그인
    // --------------------------
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpSession session) {
        String userId = body.get("userId");
        String password = body.get("password");
        
        System.out.println("_ userid : " + userId +  " _ password : " + password ); // 확인
        
        User user = userService.login(userId, password);
        if (user == null) {
            return ResponseEntity.badRequest().body("로그인 실패: 아이디 또는 비밀번호 확인");
        }
        session.setAttribute("user", user);
        return ResponseEntity.ok(user);
    }
    
    @Value("${kakao.rest-api-key}")
    private String kakaoClientId;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;
    
	// --------------------------
	// 카카오 로그인
	// --------------------------
    @PostMapping("/login/kakao")
    public ResponseEntity<?> loginKakao(@RequestBody Map<String, String> body,
                                        HttpSession session) {

        String kakaoId = body.get("kakaoId");
        String nickname = body.get("nickname");

        if (kakaoId == null || kakaoId.isBlank()) {
            return ResponseEntity.badRequest().body("kakaoId 누락");
        }

        User user = userService.loginKakao(kakaoId, nickname);

        session.setAttribute("user", user);

        boolean showGenderPopup = (user.getGender() == null);
        String jwt = jwtUtil.generateToken(user.getUserId());

        return ResponseEntity.ok(Map.of(
                "user", user,
                "accessToken", jwt,
                "showGenderPopup", showGenderPopup
        ));
    }
    
    // --------------------------
    // 구글 로그인
    // --------------------------
    @PostMapping("/login/google")
    public ResponseEntity<?> loginGoogle(@RequestBody Map<String, String> body,
                                         HttpSession session) {

        String googleId = body.get("googleId");
        String nickname = body.get("nickname");

        if (googleId == null || googleId.isBlank()) {
            return ResponseEntity.badRequest().body("googleId 누락");
        }

        User user = userService.loginGoogle(googleId, nickname);

        session.setAttribute("user", user);

        boolean showGenderPopup = (user.getGender() == null);
        String jwt = jwtUtil.generateToken(user.getUserId());

        return ResponseEntity.ok(Map.of(
                "user", user,
                "accessToken", jwt,
                "showGenderPopup", showGenderPopup
        ));
    }
    
    // 성별 업데이트
    @PostMapping("/gender")
    public ResponseEntity<?> updateGender(@RequestBody Map<String, String> body,
                                          HttpSession session) {

        String gender = body.get("gender");
        if (gender == null || gender.isBlank()) {
            return ResponseEntity.badRequest().body("성별 값이 비어 있습니다.");
        }

        // 1) 세션에서 현재 로그인 유저 가져오기
        User loginUser = (User) session.getAttribute("user");
        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("로그인 정보가 없습니다.");
        }

        String userId = loginUser.getUserId();

        // 2) 서비스 호출 → gender 업데이트
        User updated = userService.updateGender(userId, gender);

        // 3) 세션도 최신 유저로 덮어쓰기
        session.setAttribute("user", updated);

        // 4) 프론트에 바뀐 유저 반환
        return ResponseEntity.ok(Map.of(
                "user", updated,
                "gender", updated.getGender()
        ));
    }
    
	 // --------------------------
	 // JWT 로그인
	 // --------------------------
	 @PostMapping("/login/jwt")
	 public ResponseEntity<?> loginJwt(@RequestBody Map<String, String> body) {
	     String userId = body.get("userId");
	     String password = body.get("password");
	
	     User user = userService.login(userId, password);
	     if (user == null) {
	         return ResponseEntity.badRequest().body("로그인 실패: 아이디 또는 비밀번호 확인");
	     }
	
	     // JWT 토큰 생성 (한 달 유효)
	     String accessToken = jwtUtil.generateToken(user.getUserId());
	
	     // 세션은 사용 안 해도 됨 (원하면 같이 써도 됨)
	     return ResponseEntity.ok(Map.of(
	             "user", user,
	             "accessToken", accessToken
	     ));
	 }

    // --------------------------
    // 로그아웃
    // --------------------------
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("로그아웃 완료");
    }

    // --------------------------
    // 아이디 중복확인
    // --------------------------
    @GetMapping("/check/userId")
    public ResponseEntity<?> checkIdDuplicate(@RequestParam String userId) {
    	if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.ok(false);
        }

        boolean result = userService.checkIdDuplicate(userId.trim());
        return ResponseEntity.ok(result);
    }

    // --------------------------
    // 닉네임 중복확인
    // --------------------------
    @GetMapping("/check/nickname")
    public ResponseEntity<?> checkNicknameDuplicate(@RequestParam String nickname) {
    	if (nickname == null || nickname.trim().isEmpty()) {
            return ResponseEntity.ok(false);
        }

        boolean result = userService.checkNicknameDuplicate(nickname.trim());
        return ResponseEntity.ok(result);
    }

    // --------------------------
    // 내정보 조회
    // --------------------------
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable String userId) {
        User user = userService.getUser(userId);
        if (user == null) {
            return ResponseEntity.badRequest().body("사용자를 찾을 수 없음");
        }
        return ResponseEntity.ok(user);
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(HttpServletRequest request) {

        // 인터셉터에서 넣어준 로그인 유저 ID
        String userId = (String) request.getAttribute("loginUserId");

        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.getUser(userId);

        return ResponseEntity.ok(user); // nickname 포함
    }

    // --------------------------
    // 내정보 수정
    // --------------------------
    @PostMapping("/update")
    public ResponseEntity<?> updateUser(
            @RequestBody User user,
            HttpServletRequest request) {

        // JWT 기준 로그인 사용자
        String loginUserId = (String) request.getAttribute("loginUserId");
        if (loginUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("로그인이 필요합니다.");
        }

        // 프론트에서 넘어온 userId 무시
        user.setUserId(loginUserId);

        int result = userService.updateUser(user);
        if (result <= 0) {
            return ResponseEntity.badRequest().body("회원정보 수정 실패");
        }

        return ResponseEntity.ok("회원정보 수정 완료");
    }
    
    // 아바타 리스트
    @GetMapping("/avatar/list")
    public ResponseEntity<List<Avatar>> getAvatarList() {
        return ResponseEntity.ok(userService.getAvatarList());
    }

    // --------------------------
    // 비밀번호 변경
    // --------------------------
    @PostMapping("/password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> map,
            HttpServletRequest request) {

        String loginUserId = (String) request.getAttribute("loginUserId");
        if (loginUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        String currentPassword = map.get("currentPassword");
        String newPassword = map.get("newPassword");

        boolean isValid = userService.verifyPassword(loginUserId, currentPassword);
        if (!isValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "현재 비밀번호가 일치하지 않습니다."));
        }

        userService.changePassword(loginUserId, newPassword);
        return ResponseEntity.ok(Map.of("message", "비밀번호 변경 성공"));
    }

    // --------------------------
    // 회원탈퇴
    // --------------------------
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(HttpServletRequest request) {

        String userId = (String) request.getAttribute("loginUserId");
        
        if (userId == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Object obj = session.getAttribute("user");
                if (obj instanceof User user) {
                    userId = user.getUserId();
                }
            }
        }
        
        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        int result = userService.deleteUser(userId);

        if (result <= 0) {
            return ResponseEntity.badRequest().body("이미 탈퇴했거나 존재하지 않는 회원입니다.");
        }

        return ResponseEntity.ok("회원탈퇴가 정상 처리되었습니다.");
    }
}
