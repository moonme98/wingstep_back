package com.wingstep.service.user.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.wingstep.service.domain.user.Avatar;
import com.wingstep.service.domain.user.LevelIcon;
import com.wingstep.service.domain.user.User;
import com.wingstep.service.user.UserDao;
import com.wingstep.service.user.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;


@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class UserServiceImpl implements UserService {

	private final UserDao userDao;
	private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private RestTemplate restTemplate = new RestTemplate();

	@Override
	public int addUser(User user) {
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		return userDao.addUser(user);
	}

	@Override
	public User login(String userId, String password) {
		User user = userDao.login(userId); // SQL: 아이디만 조회
		if (user != null && passwordEncoder.matches(password, user.getPassword())) {
			return user; // 로그인 성공
		}
		return null; // 로그인 실패
	}
    
	@Override
	@Transactional(rollbackFor = Exception.class)
	public User loginKakao(String kakaoId, String nickname) {

	    String userId = "kakao_" + kakaoId;

	    // 기존 회원 조회
	    User user = userDao.getUser(userId);

	    // 신규 회원인 경우
	    if (user == null) {

	        if (nickname == null || nickname.isBlank()) {
	            nickname = "카카오사용자";
	        }

	        String baseName = nickname;
	        String finalName = nickname;
	        int suffix = 1;

	        while (userDao.checkNicknameDuplicate(finalName) > 0) {
	            finalName = baseName + "_" + suffix++;
	        }

	        // 신규 User 생성
	        user = new User();
	        user.setUserId(userId);
	        user.setNickname(finalName);
	        user.setPassword(encryptPassword(userId));
	        user.setLevelId(1);
	        user.setAvatarId(1);
	        user.setExp(0);
	        user.setDelete(false);
	        user.setGender(null);

	        int result = userDao.addUser(user);
            if (result <= 0) {
                throw new RuntimeException("카카오 자동 회원가입 실패");
            }
            
            user = userDao.getUser(userId);
	    }
	    // 기존 회원인 경우 → 카카오 닉네임 변경되면 업데이트
	    else {

	        if (nickname != null && !nickname.isBlank()) {

	            String baseName = nickname;
	            String finalName = nickname;

	            if (!finalName.equals(user.getNickname())) {

	                int suffix = 1;

	                while (userDao.checkNicknameDuplicate(finalName) > 0) {
	                    finalName = baseName + "_" + suffix++;
	                }

	                // 변경이 있는 경우 DB 업데이트
	                if (!finalName.equals(user.getNickname())) {
	                    user.setNickname(finalName);
	                    userDao.updateUser(user);
	                }
	            }
	        }
	    }

	    return user;
	}

	@Value("${google.client-id}")
    private String googleClientId;

	@Override
	public User loginGoogle(String googleId, String nickname) {

	    String userId = "google_" + googleId;

	    User user = userDao.getUser(userId);

	    if (user == null) {
	        // 신규 회원 자동 가입
	        if (nickname == null || nickname.isBlank()) {
	            nickname = "구글사용자";
	        }

	        String baseName = nickname;
	        String finalName = nickname;
	        int suffix = 1;

	        while (userDao.checkNicknameDuplicate(finalName) > 0) {
	            finalName = baseName + "_" + suffix++;
	        }

	        user = new User();
	        user.setUserId(userId);
	        user.setNickname(finalName);
	        user.setPassword(encryptPassword(userId));
	        user.setLevelId(1);
	        user.setAvatarId(1);
	        user.setExp(0);
	        user.setDelete(false);
	        user.setGender(null);

	        userDao.addUser(user);
	    } else {
	        // 기존 회원인데 닉네임 비어있고, 새 nickname 있으면 업데이트
	        if ((user.getNickname() == null || user.getNickname().isBlank())
	                && nickname != null && !nickname.isBlank()) {

	            String baseName = nickname;
	            String finalName = nickname;
	            int suffix = 1;

	            while (userDao.checkNicknameDuplicate(finalName) > 0) {
	                finalName = baseName + "_" + suffix++;
	            }

	            user.setNickname(finalName);
	            userDao.updateUser(user);
	        }
	    }

	    return user;
	}

	@SuppressWarnings("unchecked")
    private Map<String, Object> getGoogleUserInfo(String googleAccessToken) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(googleAccessToken); // Authorization: Bearer {accessToken}
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
        	    "https://www.googleapis.com/oauth2/v3/userinfo",
                HttpMethod.GET,
                entity,
                Map.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("구글 사용자 정보 조회 실패");
        }

        return response.getBody();
    }

	@Override
	public void logout(String userId) {
		userDao.logout(userId);
	}

	@Override
	public boolean checkIdDuplicate(String userId) {
		return userDao.checkIdDuplicate(userId) > 0;
	}

	@Override
	public boolean checkNicknameDuplicate(String nickname) {
		return userDao.checkNicknameDuplicate(nickname) > 0;
	}

	@Override
	public boolean verifyPassword(String userId, String password) {
		String encPassword = userDao.verifyPassword(userId);
		
		if (encPassword == null) {
            return false;
        }
		
		return passwordEncoder.matches(password, encPassword);
	}

	@Override
	public String encryptPassword(String password) {
		return passwordEncoder.encode(password);
	}

	@Override
	public User getUser(String userId) {
		return userDao.getUser(userId);
	}

	@Override
	public int updateUser(User user) {
		// 비밀번호가 변경되면 암호화
		if (user.getPassword() != null && !user.getPassword().isEmpty()) {
			user.setPassword(passwordEncoder.encode(user.getPassword()));
		}
		return userDao.updateUser(user);
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public User updateGender(String userId, String gender) {

	    // 1) null / 공백 체크
	    if (gender == null || gender.isBlank()) {
	        throw new IllegalArgumentException("성별 값이 비었습니다.");
	    }

	    gender = gender.trim();

	    // 2) 허용값 체크: 남자=0, 여자=1
	    if (!gender.equals("0") && !gender.equals("1")) {
	        throw new IllegalArgumentException("허용되지 않은 gender 값: " + gender);
	    }

	    // 3) DB 업데이트
	    int result = userDao.updateGender(userId, gender);
	    if (result <= 0) {
	        throw new RuntimeException("성별 업데이트 실패");
	    }

	    // 4) 변경된 유저 정보 다시 조회해서 반환
	    return userDao.getUser(userId);
	}

	@Override
	public int levelUp(String userId, int exp) {
		User user = userDao.getUser(userId);
		if (user == null)
			return 0;

		int totalExp = user.getExp() + exp;
		int levelId = user.getLevelId();

		// 누적 경험치 기준으로 레벨업 반복
		LevelIcon nextInfo = userDao.getLevelIcon(levelId + 1);
		
		System.out.println("[LEVELUP] start userId=" + userId + ", level=" + levelId + ", totalExp=" + totalExp
			    + ", nextReq=" + (nextInfo == null ? "null" : nextInfo.getReqExp()));

		while (nextInfo != null && totalExp >= nextInfo.getReqExp()) {
		    levelId++;
		    nextInfo = userDao.getLevelIcon(levelId + 1);
		}
		
		System.out.println("[LEVELUP] end   userId=" + userId + ", level=" + levelId + ", totalExp=" + totalExp);

		// 최종 레벨 + 남은 경험치 DB에 반영
		user.setExp(totalExp);
		user.setLevelId(levelId);

		return userDao.levelUp(user.getUserId(), user.getLevelId(), user.getExp());
	}

	@Override
	public LevelIcon getLevelIcon(int levelId) {
		return userDao.getLevelIcon(levelId);
	}
	
	@Override
    public Avatar getAvatar(int avatarId) {
        return userDao.getAvatar(avatarId);
    }
	
	@Override
	public List<Avatar> getAvatarList() {
	    return userDao.getAvatarList();
	}

	@Override
	public int changePassword(String userId, String newPassword) {
		String encPw = passwordEncoder.encode(newPassword);
		return userDao.changePassword(userId, encPw);
	}

	@Override
	public int deleteUser(String userId) {
		return userDao.deleteUser(userId);
	}
}
