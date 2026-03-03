package com.wingstep.service.domain.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REVIEW_IMAGE 테이블 도메인 객체
 * - 후기 이미지 정보를 관리
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewImage {

    // REVIEW_IMAGE_ID : 후기 이미지 ID (PK)
    private int reviewImageId;

    // REVIEW_ID : 후기 ID (FK)
    private int reviewId;

    // IMAGE : 이미지 경로/파일명 (NOT NULL)
    private String image;
}
