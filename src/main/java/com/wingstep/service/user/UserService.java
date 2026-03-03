package com.wingstep.service.user;

import java.util.List;

import com.wingstep.service.domain.user.Avatar;
import com.wingstep.service.domain.user.LevelIcon;
import com.wingstep.service.domain.user.User;

public interface UserService {

    int addUser(User user);

    User login(String userId, String password);

    User loginKakao(String kakaoId, String nickname);

    User loginGoogle(String googleId, String nickname);

    void logout(String userId);

    boolean checkIdDuplicate(String userId);

    boolean checkNicknameDuplicate(String nickname);

    boolean verifyPassword(String userId, String password);

    String encryptPassword(String password);

    User getUser(String userId);

    int updateUser(User user);
    
    User updateGender(String userId, String gender);

    int levelUp(String userId, int exp);
    
    LevelIcon getLevelIcon(int levelId);
    
    Avatar getAvatar(int avatarId);
    
    List<Avatar> getAvatarList(); 

    int changePassword(String userId, String newPassword);

    int deleteUser(String userId);
}
