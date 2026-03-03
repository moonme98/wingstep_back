package com.wingstep.service.domain.course;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

import com.wingstep.service.domain.course.CoursePath;

/**
 * COURSE 테이블 도메인 객체
 * - 코스 기본 정보를 관리
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    // COURSE_ID : 코스 ID (PK)
    private int courseId;

    // USER_ID : 사용자 ID (FK)
    private String userId;

    // COURSE_NAME : 코스명 (NOT NULL)
    private String courseName;

    // DISTANCE : 거리 (DECIMAL(6,2), NULL 허용)
    private BigDecimal distance;

    // TYPE : 유형 (산책/러닝 등, NOT NULL)
    private String type;

    // IS_PUBLIC : 공개 여부 (TINYINT(1) 0/1, NOT NULL, DEFAULT 0)
    // 0 = 비공개, 1 = 공개
    private boolean isPublic;
    
    private double avgRating;
    
    //출발 위치의 대략적인 주소
    private String startLocation;

    // START_POINT : 출발 위치 (MySQL POINT)
    // 예) "POINT(126.9784 37.5667)" 형태 문자열로 저장/조회한다고 가정
    private String startPoint;

    // END_POINT : 도착 위치 (MySQL POINT)
    private String endPoint;

    //코스의 경로
    private List<CoursePath> coursePathList;
    
    // [추가됨] DB의 ROUTE_LINE 컬럼에 매핑될 WKT 문자열
    // 예: "LINESTRING(127.1 37.1, 127.2 37.2, ...)"
    private String routeLine;
}
