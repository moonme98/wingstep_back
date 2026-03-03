// src/main/java/com/wingstep/service/domain/course/CourseDetailResponse.java
package com.wingstep.service.domain.course;

import java.util.List;

// Lombok 사용 가정 (프로젝트 다른 DTO와 스타일 맞춤)
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 코스 상세 응답 DTO
 * - 단일 코스 정보 + 해당 코스의 후기 리스트를 한 번에 내려주기 위한 용도
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDetailResponse {

    /** 코스 기본 정보 + 경로 정보 등 */
    private Course course;

    /** 코스에 달린 후기 리스트 */
    private List<Review> reviews;
}
