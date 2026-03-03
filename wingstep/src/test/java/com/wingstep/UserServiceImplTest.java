package com.wingstep;

import com.wingstep.service.domain.user.User;
import com.wingstep.service.user.UserDao;
import com.wingstep.service.user.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    private UserDao userDao;
    private UserServiceImpl userService;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userDao = mock(UserDao.class);
        userService = new UserServiceImpl(userDao);
        passwordEncoder = new BCryptPasswordEncoder();
    }

    //@Test
    void addUser() {
        User user = new User();
        user.setUserId("test@example.com");
        user.setNickname("tester");
        user.setPassword("plainPass");
        user.setLevelId(1);
        user.setAvatarId(1);
        user.setExp(0);
        user.setDelete(true);
        user.setGender('1');

        when(userDao.addUser(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            System.out.println("=== addUser 호출 ===");
            System.out.println("ID: " + u.getUserId());
            System.out.println("Nickname: " + u.getNickname());
            System.out.println("암호화된 PW: " + u.getPassword());
            return 1;
        });

        int result = userService.addUser(user);
        assertEquals(1, result);
    }

    //@Test
    void login() {
        User user = new User();
        user.setUserId("test@example.com");
        user.setPassword(passwordEncoder.encode("secret"));

        when(userDao.login("test@example.com")).thenReturn(user);

        User result = userService.login("test@example.com", "secret");
        System.out.println("=== login 호출 ===");
        System.out.println("로그인 ID: " + result.getUserId());
        assertNotNull(result);
    }

    //@Test
    void loginKakao() {
        User user = new User();
        when(userDao.login("kakao")).thenReturn(user);

        User result = userService.login("kakaoexample", null);
        System.out.println("=== loginKakao 호출 ===");
        assertNotNull(result);
    }

    //@Test
    void loginGoogle() {
        User user = new User();
        when(userDao.login("google")).thenReturn(user);

        User result = userService.loginGoogle("google", null);
        System.out.println("=== loginGoogle 호출 ===");
        assertNotNull(result);
    }

    //@Test
    void logout() {
        doNothing().when(userDao).logout("test@example.com");
        userService.logout("test@example.com");
        System.out.println("=== logout 호출 ===");
        verify(userDao, times(1)).logout("test@example.com");
    }

    //@Test
    void checkIdDuplicate() {
        when(userDao.checkIdDuplicate("test@example.com")).thenReturn(1);
        boolean result = userService.checkIdDuplicate("test@example.com");
        System.out.println("=== checkIdDuplicate 호출 === Result: " + result);
        assertTrue(result);
    }

    //@Test
    void checkNicknameDuplicate() {
        when(userDao.checkNicknameDuplicate("tester")).thenReturn(1);
        boolean result = userService.checkNicknameDuplicate("tester");
        System.out.println("=== checkNicknameDuplicate 호출 === Result: " + result);
        assertTrue(result);
    }

    //@Test
    void verifyPassword() {
        String plain = "mypassword";
        String encoded = passwordEncoder.encode(plain);
        when(userDao.verifyPassword("test@example.com")).thenReturn(encoded);

        boolean result = userService.verifyPassword("test@example.com", plain);
        System.out.println("=== verifyPassword 호출 === Result: " + result);
        assertTrue(result);
    }

    //@Test
    void encryptPassword() {
        String enc = userService.encryptPassword("mypassword");
        System.out.println("=== encryptPassword 호출 === 암호화된 PW: " + enc);
        assertTrue(passwordEncoder.matches("mypassword", enc));
    }

    //@Test
    void getUser() {
        User user = new User();
        when(userDao.getUser("test@example.com")).thenReturn(user);

        User result = userService.getUser("test@example.com");
        System.out.println("=== getUser 호출 ===" + user);
        assertNotNull(result);
    }

    //@Test
    void updateUser() {
        User user = new User();
        user.setUserId("update@example.com");
        user.setPassword("updatePass");
        when(userDao.updateUser(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            System.out.println("=== updateUser 호출 ===");
            System.out.println("ID: " + u.getUserId());
            System.out.println("암호화된 PW: " + u.getPassword());
            return 1;
        });

        int result = userService.updateUser(user);
        assertEquals(1, result);
    }

    @Test
    void levelUp() {
        when(userDao.levelUp("test@example.com", 0, 0)).thenReturn(2);
        int result = userService.levelUp("test@example.com", 2);
        System.out.println("=== levelUp 호출 === Result: " + result);
        assertEquals(2, result);
    }

    //@Test
    void changePassword() {
        when(userDao.changePassword(eq("test@example.com"), anyString())).thenAnswer(invocation -> {
            String encPw = invocation.getArgument(1);
            System.out.println("=== changePassword 호출 === 암호화된 PW: " + encPw);
            return 1;
        });

        int result = userService.changePassword("test@example.com", "newPass");
        assertEquals(1, result);
    }

    //@Test
    void deleteUser() {
        when(userDao.deleteUser("test@example.com")).thenReturn(1);
        int result = userService.deleteUser("test@example.com");
        System.out.println("=== deleteUser 호출 === Result: " + result);
        assertEquals(1, result);
    }
}
