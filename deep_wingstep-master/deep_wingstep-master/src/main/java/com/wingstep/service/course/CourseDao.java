package com.wingstep.service.course;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wingstep.common.Search;
import com.wingstep.service.domain.course.Course;
import com.wingstep.service.domain.course.CoursePath;
import com.wingstep.service.domain.course.Review;
import com.wingstep.service.domain.course.ReviewImage;

/**
 * 코스/후기 관련 DB 접근을 담당하는 MyBatis DAO 인터페이스
 * - XML 매퍼에서 실제 SQL 을 구현
 */
@Mapper
public interface CourseDao {

    // ─────────────────────────────
    // 코스 생성 / 종료 / 사용 관련
    // ─────────────────────────────

    /**
     * 코스 생성
     * - COURSE 및 필요시 COURSE_PATH, ROUTE_LINE 등을 INSERT 하는 SQL 을 매퍼에서 구현
     */
    int addCourse(Course course);

    /**
     * 코스 상세 조회
     * - COURSE 1건을 조회하고 필요하다면 코스 경로(COURSE_PATH) 목록도 함께 조회하도록 매퍼에서 구현
     */
    Course getCourse(@Param("courseId") int courseId);
    
    /**
     * 코스 경로 좌표 리스트 조회 (COURSE_PATH)
     * - 지도에 polyline 을 그리기 위한 용도
     */
    List<CoursePath> listCoursePath(@Param("courseId") int courseId);

    /**
     * 코스 경로 좌표 일괄 등록 (COURSE_PATH)
     * - A안 : courseId 를 파라미터로 받고, CoursePath 안의 courseId 는 사용하지 않음
     * - COURSE_ID, DATE_TIME, LOCATION 만 INSERT
     */
    int addCoursePathList(@Param("courseId") int courseId,
                          @Param("pathList") List<CoursePath> pathList);

    // ─────────────────────────────
    // 코스 삭제 / 리스트 / 공개 전환
    // ─────────────────────────────

    /**
     * 코스 삭제
     * - 자신의 코스이면서 비공개(IS_PUBLIC = 0)인 경우에만 삭제되도록
     *   WHERE 절에 USER_ID, IS_PUBLIC 조건을 함께 넣어 구현
     */
    int deleteCourse(@Param("courseId") int courseId,
                     @Param("userId") String userId);

    /**
     * 코스 리스트 조회
     *
     * 조건이 있을 때
     *  - Search 에 들어온 검색어/거리/내 코스 여부를 이용해 필터링
     *
     * 조건이 없을 때
     *  - 사용자의 위치(currentLocationWkt)와 코스 출발 위치(START_POINT) 사이 거리를 이용해
     *    ST_Distance_Sphere(START_POINT, ST_GeomFromText(#{currentLocationWkt}, 4326))
     *    ASC 순으로 정렬
     *
     * @param search             검색 조건
     * @param userId             로그인 사용자 ID (onlyMyCourse=true 인 경우 사용)
     * @param currentLocationWkt 현재 위치를 나타내는 "POINT(lon lat)" 형태 WKT 문자열
     */
    List<Course> listCourse(@Param("search") Search search,
                            @Param("userId") String userId,
                            @Param("currentLocationWkt") String currentLocationWkt);
    
    /**
     * [추가] 별점 높은 순 코스 조회
     * @param limit 조회할 개수 (예: 5)
     */
    List<Course> getTopRatedCourses(@Param("limit") int limit);

    /**
     * 코스 공개 전환
     * - 자신의 코스인 경우에만 IS_PUBLIC 을 0 -> 1 로 변경
     * - WHERE 절에 COURSE_ID, USER_ID, IS_PUBLIC = 0 조건 포함
     */
    int publicCourse(@Param("courseId") int courseId,
                     @Param("userId") String userId);

    /**
     * 현재 위치가 코스의 경로(ROUTE_LINE)로부터 100m 이내인지 판단
     *
     * 매퍼에서 예시처럼 구현
     * SELECT
     *   CASE
     *     WHEN ST_Distance_Sphere(ROUTE_LINE, ST_GeomFromText(#{currentLocationWkt}, 4326)) <= 100
     *     THEN 1 ELSE 0
     *   END
     * FROM COURSE
     * WHERE COURSE_ID = #{courseId}
     */
    boolean isOnCourseRoute(@Param("courseId") int courseId,
            @Param("currentLocationWkt") String currentLocationWkt,
            @Param("radius") int radius);
    
    /**
     * 현재 위치가 코스 도착점(END_POINT)으로부터 100m 이내인지 판단
     * @return true(도착함), false(아직)
     */
    boolean isArrivedAtEndPoint(@Param("courseId") int courseId,
            @Param("currentLocationWkt") String currentLocationWkt,
            @Param("radius") int radius);

    // ─────────────────────────────
    // 후기 관련
    // ─────────────────────────────

    /**
     * 특정 코스의 후기 리스트 조회
     * - 필요하다면 REVIEW_VIEW 를 활용해 닉네임/아바타까지 한번에 가져오는 쿼리를 매퍼에서 작성
     */
    List<Review> listReview(@Param("courseId") int courseId);

    /**
     * 후기 생성
     * - REVIEW, REVIEW_IMAGE 테이블에 INSERT
     * - 7일 이내 작성 제한 등 비즈니스 로직은 서비스에서 먼저 검증
     */
    int addReview(Review review);

    /**
     * 후기 삭제
     * - 자신이 작성한 후기만 삭제되도록 WHERE 절에 REVIEW_ID, USER_ID 조건 포함
     */
    int deleteReview(@Param("reviewId") int reviewId,
                     @Param("userId") String userId);

    /**
     * 후기 수정
     * - 자신이 작성한 후기 + 수정 가능 기간(예: 7일 이내)을 만족하는 경우에만 업데이트
     * - 기간 제한은 DB 또는 서비스에서 처리 가능
     */
    int updateReview(Review review);
    
    // 후기 이미지 등록
    void addReviewImage(ReviewImage reviewImage);

    // 후기 이미지 전체 삭제
    void deleteReviewImagesByReviewId(int reviewId);

    
    /**
     * 특정 운동기록 ID로 작성된 후기 개수
     * - 0 이면 아직 후기 없음
     * - 1 이상이면 이미 후기 존재
     */
    int countReviewByWorkRecordId(@Param("workRecordId") int workRecordId);

    int countReviewByCourseId(int courseId);
    Integer getRepresentativeReviewIdByCourseId(int courseId);
    Review getReviewByReviewId(int reviewId);
    
}
