package com.wingstep.service.domain.course;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REVIEW 테이블 도메인 객체
 * - 코스에 대한 후기 정보를 관리
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    // REVIEW_ID : 후기 ID (PK)
    private int reviewId;

    // COURSE_ID : 코스 ID (FK)
    private int courseId;

    // USER_ID : 작성자 ID (FK)
    private String userId;

    // WORKRECORD_ID : 운동기록 ID (FK)
    private int workRecordId;

    // NICKNAME : 작성자 닉네임 (REVIEW_VIEW)
    private String nickname;

    // AVATAR_IMG : 아바타 이미지 경로 (REVIEW_VIEW)
    private String avatarImg;

    // LEVEL_ICON : 레벨 아이콘 경로 (REVIEW_VIEW)
    private String levelIcon;

    // REVIEW_CONTENT : 후기 내용 (NULL 허용)
    private String reviewContent;

    // REVIEW_DATE : 후기 작성 날짜 (DATE, NOT NULL)
    private LocalDate reviewDate;

    // REVIEW_RATING : 별점 (1~5, INT, NOT NULL)
    private int reviewRating;

    // REVIEW_IMAGES : 후기 이미지들
    private List<ReviewImage> reviewImages;
}
