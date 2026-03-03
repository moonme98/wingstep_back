package com.wingstep.db;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.wingstep.service.course.CourseDao;

@SpringBootTest
@Transactional // 테스트가 끝나면 롤백 (데이터 조회만 하므로 실제 DB에는 영향 없음)
class CourseDaoTest {

    @Autowired
    private CourseDao courseDao;

    @Test
    @DisplayName("실제 DB 데이터(ID=1)를 이용한 코스 경로/도착 판별 테스트")
    void testIsOnCourseRoute_WithRealData() {
        // ==========================================
        // 1. Given: DB에 이미 들어있는 데이터 사용
        // ==========================================
        // SQL 스크립트에서 넣은 1번 코스 ("한강 5km 러닝코스")
        int existingCourseId = 1; 

        // ==========================================
        // 2. When & Then: 로직 검증
        // ==========================================
        
        // [테스트 1] 코스 시작점 위/경로 위 (성공 케이스)
        // SQL 데이터 기준 시작점: 위도 37.5312, 경도 127.0213
        // MySQL 8.0 함수용 포맷: POINT(위도 경도)
        String onRoutePoint = "POINT(37.5312 127.0213)"; 
        int radius = 100; // 100m 이내

        boolean isNear = courseDao.isOnCourseRoute(existingCourseId, onRoutePoint, radius);
        
        System.out.println(">>> [결과] 경로 위 판정: " + isNear);
        assertTrue(isNear, "1번 코스의 시작점 좌표이므로 true가 나와야 합니다.");


        // [테스트 2] 도착 지점 도착 여부 (성공 케이스)
        // SQL 데이터 기준 도착점: 위도 37.5360, 경도 127.0282
        String endPoint = "POINT(37.5360 127.0282)";
        
        boolean isArrived = courseDao.isArrivedAtEndPoint(existingCourseId, endPoint, radius);

        System.out.println(">>> [결과] 도착 판정: " + isArrived);
        assertTrue(isArrived, "도착점 좌표이므로 true가 나와야 합니다.");


        // [테스트 3] 엉뚱한 곳 (실패 케이스)
        String farPoint = "POINT(35.0 127.0)"; // 아주 먼 곳
        
        boolean isFar = courseDao.isOnCourseRoute(existingCourseId, farPoint, radius);
        
        System.out.println(">>> [결과] 경로 밖 판정: " + isFar);
        assertFalse(isFar, "경로에서 먼 좌표이므로 false가 나와야 합니다.");
    }
}