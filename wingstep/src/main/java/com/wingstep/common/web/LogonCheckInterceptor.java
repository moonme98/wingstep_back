package com.wingstep.common.web;

import com.wingstep.common.jwt.JwtUtil;
import com.wingstep.service.domain.user.User;
import com.wingstep.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/*
 * 로그인 체크 Interceptor
 *  - 세션(User) 있으면 통과
 *  - 세션 없으면 JWT 토큰 검사 후 통과/차단
 *  - 웹(JSP)/API 둘 다 고려
 */

@Component
@RequiredArgsConstructor
public class LogonCheckInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
    	if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
    	
        System.out.println("\n[ LogonCheckInterceptor start........]");
        System.out.println("Authorization = " + request.getHeader("Authorization"));

        String uri = request.getRequestURI();
        System.out.println("[URI] " + uri);

        // 1. 로그인/회원가입/중복체크/소셜로그인 등은 항상 통과시키기
        if (isLoginOrJoinRequest(uri)) {
            System.out.println("[로그인/회원가입 관련 URI, 그대로 통과]");
            System.out.println("[ LogonCheckInterceptor end........]\n");
            return true;
        }

        // 2. 세션 기반 로그인 체크
        HttpSession session = request.getSession(false);
        User sessionUser = null;
        if (session != null) {
            Object obj = session.getAttribute("user");
            if (obj instanceof User) {
                sessionUser = (User) obj;
            }
        }

        if (sessionUser != null) {
            System.out.println("[세션 로그인 상태 ... userId=" + sessionUser.getUserId() + "]");
            System.out.println("[ LogonCheckInterceptor end........]\n");
            return true;
        }

        // 3. 세션이 없으면 JWT 토큰 검사 (측정앱 / REST 용)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.validateToken(token)) {
                String userId = jwtUtil.getUserId(token);
                User user = userService.getUser(userId);

                if (user != null) {
                    // 필요하다면 세션에 user를 다시 넣어도 되고, request attribute만 써도 됨
                    // 세션 연동을 원하면 아래 주석 해제
                    // HttpSession newSession = request.getSession(true);
                    // newSession.setAttribute("user", user);

                    // 컨트롤러에서 필요하면 request에서 userId 꺼내서 쓰도록
                    request.setAttribute("loginUserId", userId);

                    System.out.println("[JWT 인증 성공 ... userId=" + userId + "]");
                    System.out.println("[ LogonCheckInterceptor end........]\n");
                    return true;
                }
            }

            System.out.println("[JWT 인증 실패 또는 user 없음]");
        }

        // 4. 세션도 없고 JWT도 없거나 실패 → 차단
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }

    // ==================== 헬퍼 메서드 ====================

    // 로그인/회원가입/중복체크/소셜로그인 URI 예외 처리
    private boolean isLoginOrJoinRequest(String uri) {
        return uri.contains("/user/addUser")
                || uri.contains("/user/login")
                || uri.contains("/user/login/jwt")
                || uri.contains("/user/login/kakao")
                || uri.contains("/user/login/google")
                || uri.contains("/user/check")
                || uri.contains("checkDuplication")  // 예전 패턴 유지
        		|| uri.contains("/user/me");

    }

    // API 요청인지 여부 판단 (필요에 따라 규칙 조정)
    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // REST API 경로는 무조건 API로 취급
        return uri.startsWith("/user")
            || uri.startsWith("/workrecord")
            || uri.startsWith("/battle")
            || uri.startsWith("/course")
            || uri.startsWith("/api");
    }
}
