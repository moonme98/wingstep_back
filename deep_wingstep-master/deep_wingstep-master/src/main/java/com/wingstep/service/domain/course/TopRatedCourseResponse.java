package com.wingstep.service.domain.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopRatedCourseResponse {

    // 코스 기본 정보(평균별점 포함)
    private Course course;

    // 대표 후기(없으면 null)
    private Review representativeReview;

    // 해당 코스 후기 총 개수
    private int reviewCount;
}
