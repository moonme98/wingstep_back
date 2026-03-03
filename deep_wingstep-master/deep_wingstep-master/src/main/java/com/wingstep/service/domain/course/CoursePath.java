package com.wingstep.service.domain.course;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * COURSE_PATH 테이블 도메인 객체
 * - 코스의 이동 경로(위치 + 시각)를 관리
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoursePath {

    // COURSE_ID : 코스 ID (FK)
    private int courseId;

    // LOCATION : 측정 위치 (MySQL POINT)
    private String location;

    // DATE_TIME : 측정 일시 (DATETIME, NOT NULL)
    private LocalDateTime dateTime;
}
