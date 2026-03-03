package com.wingstep.service.user;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wingstep.service.domain.user.Avatar;
import com.wingstep.service.domain.user.LevelIcon;
import com.wingstep.service.domain.user.User;

@Mapper
public interface UserDao {

    // 회원가입
    int addUser(User user);

    // 로그인
    User login(String userId);

    // 로그아웃
    int logout(String userId);

    // 아이디 중복 확인
    int checkIdDuplicate(String userId);

    // 닉네임 중복 확인
    int checkNicknameDuplicate(String nickname);

    // 현재 암호화된 비밀번호 확인
    String verifyPassword(String userId);

    // 비밀번호 암호화 (DB가 아닌 서비스 단에서 암호화하므로 DB 저장용)
    int encryptPassword(String userId, String encPassword);

    // 내정보 조회
    User getUser(String userId);

    // 내정보 수정
    int updateUser(User user);
    
    // 성별만 업데이트
    int updateGender(@Param("userId") String userId,
                     @Param("gender") String gender);

    // 레벨 + 경험치 업데이트
    int levelUp(@Param("userId") String userId,
                @Param("levelId") int levelId,
                @Param("exp") int exp);
    
    // 레벨 정보 조회
    LevelIcon getLevelIcon(int levelId);
    
    // 아바타 조회
    Avatar getAvatar(int avatarId);
    
    // 아바타 리스트
    List<Avatar> getAvatarList();

    // 새 비밀번호로 변경
    int changePassword(@Param("userId") String userId, @Param("newPassword") String newPassword);

    // 회원탈퇴
    int deleteUser(String userId);
}
