package com.wingstep.service.course.impl;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.wingstep.common.Search;
import com.wingstep.service.course.CourseDao;
import com.wingstep.service.course.CourseService;
import com.wingstep.service.domain.course.Course;
import com.wingstep.service.domain.course.CoursePath;
import com.wingstep.service.domain.course.CoursePathPoint;
import com.wingstep.service.domain.course.Review;
import com.wingstep.service.domain.course.ReviewImage;
import com.wingstep.service.domain.course.TopRatedCourseResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 코스/후기 관련 비즈니스 로직 구현체
 * - 컨트롤러와 DAO 사이에서 도메인 규칙을 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CourseServiceImpl implements CourseService {

    // 코스/후기 DB 접근을 담당하는 DAO 의존성 주입
    private final CourseDao courseDao;
    
    // 외부 HTTP 호출용 RestTemplate (별도 Bean 등록 필요)
    private final RestTemplate restTemplate;

    // ─────────────────────────────
    // 코스 생성 / 종료 / 사용 관련
    // ─────────────────────────────

    //로직상 측정 데이터가 나중에 생겨서 생성도 나중에 입력끝나고임 컨트롤러에서 조정 필요
    @Override
    public Course addCourse(Course course) {
        // distance 가 없으면 예외 처리 (코스 생성 시 필수라고 가정)
        if (course.getDistance() == null) {
            throw new IllegalArgumentException("코스 거리 정보가 없습니다.");
        }

//        // 3km 미만이면 코스 생성 불가
//        if (course.getDistance().compareTo(BigDecimal.valueOf(3.0)) < 0) {
//            throw new IllegalArgumentException("코스 거리가 3km 미만이어서 코스를 생성할 수 없습니다.");
//        }

        // 기본값으로 비공개 코스로 생성 (클라이언트에서 넘어온 값은 무시)
        course.setPublic(false);

        // 1) COURSE INSERT
        courseDao.addCourse(course);
        // 이 시점에서 useGeneratedKeys 설정으로 courseId 가 세팅되어 있다고 가정

        // 2) COURSE_PATH 리스트가 있으면 일괄 INSERT
        if (course.getCoursePathList() != null
                && !course.getCoursePathList().isEmpty()) {

            courseDao.addCoursePathList(
                    course.getCourseId(),
                    course.getCoursePathList()
            );
        }

        // 최종 Course (courseId 포함) 반환
        return course;
    }
    
    @Override
    public List<CoursePathPoint> listCoursePathPoints(int courseId) {
        List<CoursePath> pathList = courseDao.listCoursePath(courseId);

        if (pathList == null || pathList.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<CoursePathPoint> points = new java.util.ArrayList<>();

        for (CoursePath cp : pathList) {
            double[] latLng = parsePointWktToLatLng(cp.getLocation());
            if (latLng == null) {
                continue;
            }
            points.add(new CoursePathPoint(latLng[0], latLng[1]));
        }

        return points;
    }


    @Override
    @Transactional(readOnly = true)
    public Course getCourse(int courseId) {
        // 1. 코스 기본 정보 조회
        Course course = courseDao.getCourse(courseId);
        if (course == null) {
            return null;
        }

        // 2. 코스 경로 리스트 조회
        //    - COURSE_PATH 테이블에서 courseId 기준으로 좌표/시간 목록 가져오기
        course.setCoursePathList(courseDao.listCoursePath(courseId));

        // 3. 코스 기본 정보 + 경로 정보를 모두 포함한 도메인 반환
        return course;
    }

    @Override
    @Transactional(readOnly = true)
    public Course useCourse(int courseId, String userId) {
        // 코스 정보를 조회
        Course course = courseDao.getCourse(courseId);
        if (course == null) {
            throw new IllegalArgumentException("존재하지 않는 코스입니다. courseId=" + courseId);
        }

        // 비공개 코스인데 소유자가 아니면 사용 불가
        if (!course.isPublic() && !Objects.equals(course.getUserId(), userId)) {
            throw new IllegalStateException("비공개 코스는 생성한 사용자만 사용할 수 있습니다.");
        }

        // 이 시점에서
        // - 프론트엔드는 course 의 경로 데이터를 이용해 지도를 그려주고
        // - 운동 컨트롤러(별도 서비스)를 호출해 기록 측정을 시작하도록 연동
        return course;
    }

    // ─────────────────────────────
    // 코스 삭제 / 리스트 / 공개 전환
    // ─────────────────────────────

    @Override
    public void deleteCourse(int courseId, String userId) {
        // 자신의 코스이면서 비공개인 경우에만 삭제
        int deleted = courseDao.deleteCourse(courseId, userId);
        if (deleted == 0) {
            throw new IllegalStateException("코스를 삭제할 수 없습니다. 자신의 비공개 코스인지 확인이 필요합니다.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> listCourse(Search search,
                                   String userId,
                                   String currentLocationWkt) {

        // search 가 null 로 들어오는 상황 방어
        if (search == null) {
            search = new Search();
        }

        // "내 코스만" 인데 userId 가 없는 경우는 잘못된 요청
        if (Boolean.TRUE.equals(search.getOnlyMyCourse()) && (userId == null || userId.isBlank())) {
            throw new IllegalArgumentException("내 코스만 조회하려면 사용자 ID 가 필요합니다.");
        }

        // 실제 필터링/정렬 로직은 MyBatis 매퍼의 SQL 에서 구현
        // - search.hasCondition() == true 이면 조건 검색
        // - false 이면 currentLocationWkt 기준 거리 순 정렬
        return courseDao.listCourse(search, userId, currentLocationWkt);
    }

    @Override
    public void publicCourse(int courseId, String userId) {
        // 자신의 비공개 코스만 공개로 전환
        int updated = courseDao.publicCourse(courseId, userId);
        if (updated == 0) {
            throw new IllegalStateException("코스를 공개로 전환할 수 없습니다. 자신의 비공개 코스인지 확인이 필요합니다.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOnCourseRoute(int courseId, String currentLocationWkt, int radius) {
        // DAO로 radius 전달
        return courseDao.isOnCourseRoute(courseId, currentLocationWkt, radius);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isArrivedAtEndPoint(int courseId, String currentLocationWkt, int radius) {
        // DAO로 radius 전달
        return courseDao.isArrivedAtEndPoint(courseId, currentLocationWkt, radius);
    }

    // ─────────────────────────────
    // 후기 관련
    // ─────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Review> listReview(int courseId) {
        return courseDao.listReview(courseId);
    }

    @Override
    @Transactional
    public Review addReview(Review review) {

        // 1) REVIEW_DATE 가 비어있으면 오늘 날짜로 세팅 (필요 시)
        if (review.getReviewDate() == null) {
            review.setReviewDate(LocalDate.now());
        }

        // 2) REVIEW 테이블에 저장 (reviewId 생성)
        courseDao.addReview(review);

        // 3) 후기 이미지 리스트가 있으면 REVIEW_IMAGE 테이블에 저장
        if (review.getReviewImages() != null && !review.getReviewImages().isEmpty()) {
            for (ReviewImage image : review.getReviewImages()) {
                // 이미지 경로가 비어있으면 스킵
                if (image == null || image.getImage() == null || image.getImage().isBlank()) {
                    continue;
                }

                // 방금 생성된 reviewId 세팅
                image.setReviewId(review.getReviewId());

                // REVIEW_IMAGE INSERT
                courseDao.addReviewImage(image);
            }
        }

        return review;
    }

    @Override
    public void deleteReview(int reviewId, String userId) {
        // 자신이 작성한 후기만 삭제되도록 userId 를 함께 전달
        int deleted = courseDao.deleteReview(reviewId, userId);
        if (deleted == 0) {
            throw new IllegalStateException("후기를 삭제할 수 없습니다. 본인이 작성한 후기인지 확인이 필요합니다.");
        }
    }

    @Override
    @Transactional
    public void updateReview(Review review) {

        // 1) REVIEW 내용/별점 수정
        courseDao.updateReview(review);

        // 2) 기존 이미지 전부 삭제
        courseDao.deleteReviewImagesByReviewId(review.getReviewId());

        // 3) 새로 넘어온 리뷰 이미지 리스트 저장
        if (review.getReviewImages() != null && !review.getReviewImages().isEmpty()) {
            for (ReviewImage image : review.getReviewImages()) {
                if (image == null || image.getImage() == null || image.getImage().isBlank()) {
                    continue;
                }
                image.setReviewId(review.getReviewId());
                courseDao.addReviewImage(image);
            }
        }
    }

    
    @Override
    @Transactional(readOnly = true)
    public boolean existsReviewByWorkRecordId(int workRecordId) {
        return courseDao.countReviewByWorkRecordId(workRecordId) > 0;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TopRatedCourseResponse> listTopRatedCourseForHome() {

        List<Course> courses = courseDao.getTopRatedCourses(3); // 기존 로직 그대로
        if (courses == null || courses.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return courses.stream().map(course -> {

            int reviewCount = courseDao.countReviewByCourseId(course.getCourseId());

            Review representative = null;
            if (reviewCount > 0) {
                Integer repReviewId = courseDao.getRepresentativeReviewIdByCourseId(course.getCourseId());
                if (repReviewId != null) {
                    representative = courseDao.getReviewByReviewId(repReviewId);
                }
            }

            return TopRatedCourseResponse.builder()
                    .course(course)
                    .representativeReview(representative)
                    .reviewCount(reviewCount)
                    .build();
        }).toList();
    }

    
    /**
     * POINT WKT를 위도/경도 배열로 변환
     * - "POINT(lat lng)" 또는 "POINT(lng lat)" 모두 대응
     * - 한국 좌표 범위를 기준으로 어떤 값이 위도/경도인지 판별
     */
    private double[] parsePointWktToLatLng(String wkt) {
        if (wkt == null) {
            return null;
        }

        String trimmed = wkt.trim();
        if (!trimmed.toUpperCase().startsWith("POINT")) {
            return null;
        }

        int start = trimmed.indexOf('(');
        int end = trimmed.indexOf(')');
        if (start < 0 || end < 0 || end <= start) {
            return null;
        }

        String body = trimmed.substring(start + 1, end).trim();
        if (body.isEmpty()) {
            return null;
        }

        String[] parts = body.split("\\s+");
        if (parts.length < 2) {
            return null;
        }

        try {
            double a = Double.parseDouble(parts[0]);
            double b = Double.parseDouble(parts[1]);

            // 한국 기준 범위
            java.util.function.DoublePredicate isLat = v -> v >= 30 && v <= 45;
            java.util.function.DoublePredicate isLng = v -> v >= 120 && v <= 135;

            double lat;
            double lng;

            if (isLat.test(a) && isLng.test(b)) {
                lat = a;
                lng = b;
            } else if (isLng.test(a) && isLat.test(b)) {
                lat = b;
                lng = a;
            } else {
                // 애매하면 a=lat, b=lng 로 취급
                lat = a;
                lng = b;
            }

            if (!isLat.test(lat) || !isLng.test(lng)) {
                // 로그만 찍고 null 처리
                // log.warn("비정상 좌표 WKT: {}", wkt);
                return null;
            }

            return new double[] { lat, lng };
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
