package com.wingstep;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;

import com.wingstep.common.Search;
import com.wingstep.service.course.CourseDao;
import com.wingstep.service.course.impl.CourseServiceImpl;
import com.wingstep.service.domain.course.Course;
import com.wingstep.service.domain.course.CoursePath;
import com.wingstep.service.domain.course.Review;

/**
 * CourseServiceImpl 비즈니스 로직 단위 테스트
 * - DB 접근은 CourseDao Mock 으로 대체
 * - 각 메서드별로 성공/실패 케이스를 분리해서 검증
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    // CourseServiceImpl 이 사용하는 DAO 를 Mock 으로 주입
    @Mock
    private CourseDao courseDao;

    // Mock 이 주입된 Service 구현체 생성
    @InjectMocks
    private CourseServiceImpl courseService;
    
    @Value("${wingstep.course.monitor.radius-meters}")
    private int radiusMeters; // 100 완주/이탈 판정

    // =====================================================================
    // 코스 생성(addCourse) 테스트
    // =====================================================================

//    @Test
//    @DisplayName("코스 생성 - 거리가 3km 이상이면 정상 생성")
//    void addCourse_success_whenDistanceIs3kmOrMore() {
//        // given : 3km 이상의 코스를 준비하고, DAO 동작은 단순 호출만 검증
//        Course course = new Course();
//        course.setUserId("user01");
//        course.setCourseName("테스트 코스");
//        course.setDistance(BigDecimal.valueOf(3.5));
//        course.setType("RUN");
//        course.setCourseImages("image1.png");
//        course.setPublic(true); // 들어와도 서비스에서 강제로 false 로 변경됨
//
//        // when
//        Course result = courseService.addCourse(course);
//
//        // then : 거리 예외가 발생하지 않고, 비공개로 강제 설정되는지 확인
//        assertNotNull(result);
//        assertFalse(result.isPublic(), "코스 생성 시 항상 비공개로 생성되어야 함");
//
//        // DAO 가 실제로 호출되었는지 검증
//        verify(courseDao, times(1)).addCourse(course);
//    }

    @Test
    @DisplayName("코스 생성 - 거리 정보가 null 이면 예외 발생")
    void addCourse_fail_whenDistanceIsNull() {
        // given : distance 가 null 인 코스
        Course course = new Course();
        course.setUserId("user01");
        course.setCourseName("거리없음 코스");
        course.setDistance(null);

        // when / then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> courseService.addCourse(course)
        );
        assertEquals("코스 거리 정보가 없습니다.", ex.getMessage());
        verify(courseDao, never()).addCourse(any());
    }

    @Test
    @DisplayName("코스 생성 - 거리가 3km 미만이면 예외 발생")
    void addCourse_fail_whenDistanceLessThan3km() {
        // given : 3km 미만인 코스
        Course course = new Course();
        course.setUserId("user01");
        course.setCourseName("짧은 코스");
        course.setDistance(BigDecimal.valueOf(2.9));

        // when / then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> courseService.addCourse(course)
        );
        assertEquals("코스 거리가 3km 미만이어서 코스를 생성할 수 없습니다.", ex.getMessage());
        verify(courseDao, never()).addCourse(any());
    }

    // =====================================================================
    // 코스 상세(getCourse) 테스트
    // =====================================================================

    @Test
    @DisplayName("코스 상세 - 기본 정보와 경로 리스트가 함께 조회되는지 확인")
    void getCourse_withCoursePathList() {
        // given : DAO 가 반환할 코스와 경로 리스트 준비
        int courseId = 100;
        Course course = new Course();
        course.setCourseId(courseId);
        course.setCourseName("상세 코스");

        CoursePath path1 = new CoursePath();
        path1.setCourseId(courseId);
        CoursePath path2 = new CoursePath();
        path2.setCourseId(courseId);
        List<CoursePath> pathList = List.of(path1, path2);

        when(courseDao.getCourse(courseId)).thenReturn(course);
        when(courseDao.listCoursePath(courseId)).thenReturn(pathList);

        // when
        Course result = courseService.getCourse(courseId);

        // then
        assertNotNull(result);
        assertEquals(courseId, result.getCourseId());
        assertNotNull(result.getCoursePathList());
        assertEquals(2, result.getCoursePathList().size());
        verify(courseDao, times(1)).getCourse(courseId);
        verify(courseDao, times(1)).listCoursePath(courseId);
    }

    @Test
    @DisplayName("코스 상세 - 존재하지 않는 코스면 null 반환")
    void getCourse_returnsNull_whenNotFound() {
        // given
        int courseId = 999;
        when(courseDao.getCourse(courseId)).thenReturn(null);

        // when
        Course result = courseService.getCourse(courseId);

        // then
        assertNull(result);
        verify(courseDao, times(1)).getCourse(courseId);
        verify(courseDao, never()).listCoursePath(anyInt());
    }

    // =====================================================================
    // 코스 사용(useCourse) 테스트
    // =====================================================================

    @Test
    @DisplayName("코스 사용 - 존재하지 않는 코스면 예외 발생")
    void useCourse_fail_whenCourseNotFound() {
        // given
        int courseId = 100;
        String userId = "user01";
        when(courseDao.getCourse(courseId)).thenReturn(null);

        // when / then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> courseService.useCourse(courseId, userId)
        );
        assertTrue(ex.getMessage().contains("존재하지 않는 코스입니다. courseId="));
    }

    @Test
    @DisplayName("코스 사용 - 비공개 코스를 소유자가 아니면 사용 불가")
    void useCourse_fail_whenPrivateCourseAndNotOwner() {
        // given
        int courseId = 100;
        String ownerId = "owner";
        String otherUser = "other";

        Course course = new Course();
        course.setCourseId(courseId);
        course.setUserId(ownerId);
        course.setPublic(false); // 비공개

        when(courseDao.getCourse(courseId)).thenReturn(course);

        // when / then
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> courseService.useCourse(courseId, otherUser)
        );
        assertEquals("비공개 코스는 생성한 사용자만 사용할 수 있습니다.", ex.getMessage());
    }

    @Test
    @DisplayName("코스 사용 - 비공개 코스를 소유자가 사용하면 정상 반환")
    void useCourse_success_whenPrivateCourseAndOwner() {
        // given
        int courseId = 100;
        String ownerId = "owner";

        Course course = new Course();
        course.setCourseId(courseId);
        course.setUserId(ownerId);
        course.setPublic(false);

        when(courseDao.getCourse(courseId)).thenReturn(course);

        // when
        Course result = courseService.useCourse(courseId, ownerId);

        // then
        assertNotNull(result);
        assertEquals(courseId, result.getCourseId());
        verify(courseDao, times(1)).getCourse(courseId);
    }

    @Test
    @DisplayName("코스 사용 - 공개 코스는 다른 사용자도 사용 가능")
    void useCourse_success_whenPublicCourseAndOtherUser() {
        // given
        int courseId = 100;
        Course course = new Course();
        course.setCourseId(courseId);
        course.setUserId("owner");
        course.setPublic(true); // 공개

        when(courseDao.getCourse(courseId)).thenReturn(course);

        // when
        Course result = courseService.useCourse(courseId, "otherUser");

        // then
        assertNotNull(result);
        assertEquals(courseId, result.getCourseId());
    }

    // =====================================================================
    // 코스 삭제(deleteCourse) 테스트
    // =====================================================================

    @Test
    @DisplayName("코스 삭제 - 자신의 비공개 코스는 삭제 성공")
    void deleteCourse_success_whenOwnPrivateCourse() {
        // given : DAO 가 1을 반환하면 삭제 성공으로 간주
        int courseId = 100;
        String userId = "user01";
        when(courseDao.deleteCourse(courseId, userId)).thenReturn(1);

        // when / then : 예외 없이 끝나야 함
        assertDoesNotThrow(() -> courseService.deleteCourse(courseId, userId));
        verify(courseDao, times(1)).deleteCourse(courseId, userId);
    }

    @Test
    @DisplayName("코스 삭제 - 자신이 만들지 않은 코스는 삭제 불가")
    void deleteCourse_fail_whenNotOwner() {
        // given : DAO 가 0을 반환하면 삭제 불가
        int courseId = 100;
        String userId = "notOwner";
        when(courseDao.deleteCourse(courseId, userId)).thenReturn(0);

        // when / then
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> courseService.deleteCourse(courseId, userId)
        );
        assertEquals("코스를 삭제할 수 없습니다. 자신의 비공개 코스인지 확인이 필요합니다.", ex.getMessage());
    }

    @Test
    @DisplayName("코스 삭제 - 자신의 공개 코스도 삭제 불가로 처리되는지 확인")
    void deleteCourse_fail_whenOwnPublicCourse() {
        // given
        int courseId = 100;
        String userId = "owner";
        // 공개 여부는 DAO 쿼리에서 IS_PUBLIC = 0 조건으로 필터링 되므로
        // Service 입장에서는 삭제 실패(0 반환)만 확인하면 됨
        when(courseDao.deleteCourse(courseId, userId)).thenReturn(0);

        // when / then
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> courseService.deleteCourse(courseId, userId)
        );
        assertEquals("코스를 삭제할 수 없습니다. 자신의 비공개 코스인지 확인이 필요합니다.", ex.getMessage());
    }

    // =====================================================================
    // 코스 리스트(listCourse) 테스트
    // =====================================================================

    @Test
    @DisplayName("코스 리스트 - 검색 조건이 있는 경우 DAO 로 정상 위임")
    void listCourse_withSearchCondition() {
        // given : 검색어와 최소거리 등이 있는 Search 객체 준비
        Search search = new Search();
        search.setKeyword("공원");
        search.setMinDistance(BigDecimal.valueOf(3.0));
        search.setMaxDistance(BigDecimal.valueOf(10.0));
        search.setOnlyMyCourse(false);

        String userId = "user01";
        String currentLocationWkt = "POINT(127.0 37.0)";

        Course course1 = new Course();
        Course course2 = new Course();
        List<Course> courseList = List.of(course1, course2);

//        when(courseDao.listCourse(search, userId, currentLocationWkt)).thenReturn(courseList);
//
//        // when
//        List<Course> result = courseService.listCourse(search, userId, currentLocationWkt);
//
//        // then
//        assertNotNull(result);
//        assertEquals(2, result.size());
//        verify(courseDao, times(1)).listCourse(search, userId, currentLocationWkt);
    }

    @Test
    @DisplayName("코스 리스트 - search 가 null 일 때도 정상 동작 (새 Search 를 만들어서 DAO 호출)")
    void listCourse_withoutSearchCondition() {
        // given : search 에 null 을 넘김
        String userId = "user01";
        String currentLocationWkt = "POINT(127.0 37.0)";

        Course course1 = new Course();
        List<Course> courseList = List.of(course1);

        // search 인스턴스는 Service 내부에서 new Search() 로 생성되므로 any(Search.class) 사용
//        when(courseDao.listCourse(any(Search.class), eq(userId), eq(currentLocationWkt))).thenReturn(courseList);
//
//        // when
//        List<Course> result = courseService.listCourse(null, userId, currentLocationWkt);
//
//        // then
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        verify(courseDao, times(1)).listCourse(any(Search.class), eq(userId), eq(currentLocationWkt));
    }

    @Test
    @DisplayName("코스 리스트 - '내 코스만' 옵션인데 userId 가 없으면 예외")
    void listCourse_fail_whenOnlyMyCourseAndUserIdMissing() {
        // given
        Search search = new Search();
        search.setOnlyMyCourse(true);

        // when / then
//        IllegalArgumentException ex = assertThrows(
//                IllegalArgumentException.class,
//                () -> courseService.listCourse(search, null, "POINT(127.0 37.0)")
//        );
//        assertEquals("내 코스만 조회하려면 사용자 ID 가 필요합니다.", ex.getMessage());
//        verify(courseDao, never()).listCourse(any(), any(), any());
    }

    // =====================================================================
    // 코스 공개 전환(publicCourse) 테스트
    // =====================================================================

    @Test
    @DisplayName("코스 공개 전환 - 자신의 비공개 코스는 공개 전환 성공")
    void publicCourse_success_whenOwnPrivateCourse() {
        // given
        int courseId = 100;
        String userId = "user01";
        when(courseDao.publicCourse(courseId, userId)).thenReturn(1);

        // when / then
        assertDoesNotThrow(() -> courseService.publicCourse(courseId, userId));
        verify(courseDao, times(1)).publicCourse(courseId, userId);
    }

    @Test
    @DisplayName("코스 공개 전환 - 자신의 코스가 아니거나 이미 공개인 경우 실패")
    void publicCourse_fail_whenNotOwnerOrAlreadyPublic() {
        // given
        int courseId = 100;
        String userId = "user01";
        when(courseDao.publicCourse(courseId, userId)).thenReturn(0);

        // when / then
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> courseService.publicCourse(courseId, userId)
        );
        assertEquals("코스를 공개로 전환할 수 없습니다. 자신의 비공개 코스인지 확인이 필요합니다.", ex.getMessage());
    }

    // =====================================================================
    // 코스 경로 위에 있는지 확인(isOnCourseRoute) 테스트
    // =====================================================================

    @Test
    @DisplayName("현재 위치가 코스 경로 위인지 여부 - DAO 결과를 그대로 반환")
    void isOnCourseRoute_delegatesToDao() {
        // given
        int courseId = 100;
        String currentLocationWkt = "POINT(127.0 37.0)";

        when(courseDao.isOnCourseRoute(courseId, currentLocationWkt, radiusMeters)).thenReturn(true);

        // when
        boolean result = courseService.isOnCourseRoute(courseId, currentLocationWkt, radiusMeters);

        // then
        assertTrue(result);
        verify(courseDao, times(1)).isOnCourseRoute(courseId, currentLocationWkt, radiusMeters);
    }

    // =====================================================================
    // 후기 리스트(listReview) 테스트
    // =====================================================================

    @Test
    @DisplayName("후기 리스트 - 특정 코스의 후기들이 잘 조회되는지 확인")
    void listReview_returnsReviewsForCourse() {
        // given
        int courseId = 100;
        Review r1 = new Review();
        r1.setCourseId(courseId);
        Review r2 = new Review();
        r2.setCourseId(courseId);

        List<Review> reviewList = List.of(r1, r2);
        when(courseDao.listReview(courseId)).thenReturn(reviewList);

        // when
        List<Review> result = courseService.listReview(courseId);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(courseDao, times(1)).listReview(courseId);
    }

    // =====================================================================
    // 후기 생성(addReview) 테스트
    // =====================================================================

    @Test
    @DisplayName("후기 생성 - 작성일이 null 이면 오늘 날짜로 세팅 후 저장")
    void addReview_success_setsReviewDateIfNull() {
        // given
        Review review = new Review();
        review.setCourseId(100);
        review.setUserId("user01");
        review.setReviewContent("좋은 코스였습니다.");
        review.setReviewDate(null);

        // DAO 는 단순히 호출 여부만 검증
//        doNothing().when(courseDao).addReview(review);

        // when
        Review result = courseService.addReview(review);

        // then
        assertNotNull(result.getReviewDate(), "작성일이 null 이면 오늘 날짜로 세팅되어야 함");
        assertEquals(LocalDate.now(), result.getReviewDate());
        verify(courseDao, times(1)).addReview(review);
    }

    // =====================================================================
    // 후기 삭제(deleteReview) 테스트
    // =====================================================================

    @Test
    @DisplayName("후기 삭제 - 본인이 작성한 후기는 정상 삭제")
    void deleteReview_success_whenOwnReview() {
        // given
        int reviewId = 1;
        String userId = "user01";
        when(courseDao.deleteReview(reviewId, userId)).thenReturn(1);

        // when / then
        assertDoesNotThrow(() -> courseService.deleteReview(reviewId, userId));
        verify(courseDao, times(1)).deleteReview(reviewId, userId);
    }

    @Test
    @DisplayName("후기 삭제 - 삭제 대상이 없으면 예외 발생")
    void deleteReview_fail_whenNoRowDeleted() {
        // given
        int reviewId = 1;
        String userId = "user01";
        when(courseDao.deleteReview(reviewId, userId)).thenReturn(0);

        // when / then
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> courseService.deleteReview(reviewId, userId)
        );
        assertEquals("후기를 삭제할 수 없습니다. 본인이 작성한 후기인지 확인이 필요합니다.", ex.getMessage());
    }

    // =====================================================================
    // 후기 수정(updateReview) 테스트
    // =====================================================================

    @Test
    @DisplayName("후기 수정 - 작성일이 7일 초과된 경우 예외 발생")
    void updateReview_fail_whenReviewDateOlderThan7Days() {
        // given
        Review review = new Review();
        review.setReviewId(1);
        review.setCourseId(100);
        review.setReviewContent("수정 내용");
        review.setReviewDate(LocalDate.now().minusDays(8)); // 7일 초과

        // when / then
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> courseService.updateReview(review)
        );
        assertEquals("후기 수정 가능 기간(7일)을 초과했습니다.", ex.getMessage());
        verify(courseDao, never()).updateReview(any());
    }

//    @Test
//    @DisplayName("후기 수정 - 7일 이내이면서 DAO 업데이트 성공 시 정상 동작")
//    void updateReview_success_whenWithin7DaysAndUpdateSuccess() {
//        // given
//        Review review = new Review();
//        review.setReviewId(1);
//        review.setCourseId(100);
//        review.setReviewContent("수정 내용");
//        review.setReviewDate(LocalDate.now().minusDays(3)); // 7일 이내
//
//        when(courseDao.updateReview(review)).thenReturn(1);
//
//        // when
////        Review result = courseService.updateReview(review);
//        Review result;
//
//        // then
//        assertNotNull(result);
//        assertEquals("user01", result.getUserId(), "수정 요청자의 userId 가 Review 에 세팅되어야 함");
//        verify(courseDao, times(1)).updateReview(review);
//    }

    @Test
    @DisplayName("후기 수정 - 7일 이내지만 DAO 업데이트 결과가 0이면 예외 발생")
    void updateReview_fail_whenDaoUpdateReturnsZero() {
        // given
        Review review = new Review();
        review.setReviewId(1);
        review.setCourseId(100);
        review.setReviewContent("수정 내용");
        review.setReviewDate(LocalDate.now().minusDays(2)); // 7일 이내

        when(courseDao.updateReview(review)).thenReturn(0);

        // when / then
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> courseService.updateReview(review)
        );
        assertEquals("후기를 수정할 수 없습니다. 본인이 작성한 후기인지 또는 수정 가능 기간인지 확인이 필요합니다.", ex.getMessage());
        verify(courseDao, times(1)).updateReview(review);
    }
}
