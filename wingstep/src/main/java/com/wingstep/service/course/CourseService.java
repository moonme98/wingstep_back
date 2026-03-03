package com.wingstep.service.course;

import java.util.List;

import com.wingstep.common.Search;
import com.wingstep.service.domain.course.Course;
import com.wingstep.service.domain.course.CoursePathPoint;
import com.wingstep.service.domain.course.Review;
import com.wingstep.service.domain.course.TopRatedCourseResponse;

/**
 * 코스/후기 관련 비즈니스 로직을 정의하는 서비스 인터페이스
 * - 컨트롤러는 DAO 대신 이 인터페이스만 의존하도록 구성
 */
public interface CourseService {

    // ─────────────────────────────
    // 코스 생성 / 종료 / 사용 관련
    // ─────────────────────────────

    /**
     * 코스 생성
     * - distance >= 3km 검증
     * - 기본적으로 비공개 코스로 생성
     */
    Course addCourse(Course course);

    /**
     * 코스 상세 조회
     * - 필요 시 코스 경로/후기 등도 함께 반환하도록 구현 가능
     */
    Course getCourse(int courseId);

    /**
     * 코스 사용 준비
     * - 코스 경로 데이터 전달
     * - 비공개 코스인 경우, 소유자 외에는 사용 불가 검증 등
     * - 운동 컨트롤러에 기록 측정 요청하는 부분은 별도 컴포넌트와 연동 (TODO)
     */
    Course useCourse(int courseId, String userId);

    // ─────────────────────────────
    // 코스 삭제 / 리스트 / 공개 전환
    // ─────────────────────────────

    /**
     * 코스 삭제
     * - 자신의 코스이면서 비공개인 경우에만 삭제 가능
     */
    void deleteCourse(int courseId, String userId);

    /**
     * 코스 리스트 조회
     *
     * search 에 조건이 들어 있으면
     *  - 조건 기반 검색
     *
     * search 에 조건이 전혀 없으면
     *  - currentLocationWkt 와 START_POINT 의 거리를 기준으로 가까운 순 정렬
     */
    List<Course> listCourse(Search search,
                            String userId,
                            String currentLocationWkt);

    /**
     * 코스 공개 전환
     * - 자신의 코스만 공개로 전환 가능
     */
    void publicCourse(int courseId, String userId);

    /**
     * 현재 위치가 코스 경로로부터 100m 이내인지 여부
     * - ST_Distance_Sphere(ROUTE_LINE, POINT) <= 100 조건을 사용
     * - 프론트엔드에서 주기적으로 호출해 이탈 여부를 체크할 때 사용 가능
     */
    boolean isOnCourseRoute(int courseId, String currentLocationWkt, int radius);

    // ─────────────────────────────
    // 후기 관련
    // ─────────────────────────────

    /**
     * 코스 후기 리스트 조회
     */
    List<Review> listReview(int courseId);

    /**
     * 후기 생성
     * - 코스를 완주한 날짜 기준 7일 이내인지 검증 후 저장
     *   (실제 완주일 정보 조회는 WORKRECORD 와 연동 필요, TODO 로 남김)
     */
    Review addReview(Review review);

    /**
     * 후기 삭제
     * - 자신이 작성한 후기만 삭제
     */
    void deleteReview(int reviewId, String userId);

    /**
     * 후기 수정
     * - 자신이 작성한 후기이면서 코스 완주일로부터 7일 이내인 경우에만 수정
     */
    void updateReview(Review review);
    
    /**
     * 특정 운동기록 ID로 이미 작성된 후기가 존재하는지 여부
     */
    boolean existsReviewByWorkRecordId(int workRecordId);
    
    boolean isArrivedAtEndPoint(int courseId, String currentLocationWkt, int radius);

	List<CoursePathPoint> listCoursePathPoints(int courseId);
	
	List<TopRatedCourseResponse> listTopRatedCourseForHome();
}
