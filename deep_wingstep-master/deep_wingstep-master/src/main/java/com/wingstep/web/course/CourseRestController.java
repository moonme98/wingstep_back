package com.wingstep.web.course;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.wingstep.common.Search;
import com.wingstep.service.course.CourseService;
import com.wingstep.service.course.session.CourseUseSessionStore;
import com.wingstep.service.domain.course.Course;
import com.wingstep.service.domain.course.CourseCompleteResponse;
import com.wingstep.service.domain.course.CourseDetailResponse;
import com.wingstep.service.domain.course.CoursePath;
import com.wingstep.service.domain.course.CoursePathPoint;
import com.wingstep.service.domain.course.CourseUseResponse;
import com.wingstep.service.domain.course.CourseUseSession;
import com.wingstep.service.domain.course.Review;
import com.wingstep.service.domain.course.ReviewPermissionResponse;
import com.wingstep.service.domain.course.TopRatedCourseResponse;
import com.wingstep.service.domain.workrecord.Measurement;
import com.wingstep.service.domain.workrecord.WorkRecord;
import com.wingstep.service.user.UserService;
import com.wingstep.service.workrecord.MeasurementService;
import com.wingstep.service.workrecord.WorkRecordService;

import lombok.RequiredArgsConstructor;

/**
 * 코스/후기 REST 컨트롤러
 * - 리액트 프론트엔드와 JSON 으로 통신
 * - 화면과 서비스 사이에서 Request/Response 를 매핑하는 역할
 *
 * 기본 URL : /course
 */
@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class CourseRestController {

    // 코스/후기 비즈니스 로직을 담당하는 서비스
    private final CourseService courseService;
    
    // 운동(WorkRecord / Measurement) 비즈니스 로직
    private final WorkRecordService workRecordService;
    private final MeasurementService measurementService;
    private final CourseUseSessionStore courseUseSessionStore;
    private final UserService userService;
    
    @Value("${wingstep.course.monitor.leave.radius-meters}")
    private int radiusLeaveMeters; // 100 완주/이탈 판정
    
    @Value("${app.upload.review-dir}")
    private String reviewImageUploadDir;	// 파일 저장 경로 (/home/ubuntu/...)
    
    @Value("${app.review.base-url}")
    private String reviewBaseUrl;	// 외부 접속 URL (http://[퍼블릭IP]/api/upload/review)

    // ─────────────────────────────
    // 1. 코스 생성 (코스 생성 화면 / 코스 생성중 화면)
    // ─────────────────────────────

    /**
     * 코스 생성 시작 (코스 생성 화면 → 코스 생성중 화면 진입 시 호출)
     *
     * 역할
     * - 사용자 ID, 코스 유형, 코스명을 프론트에서 들고 있다가 endCourse 시점에 다시 보낸다.
     * - 여기서는 "운동 서비스에 기록 측정 시작"을 알리기 위해 WorkRecord를 하나 생성한다.
     * - 생성된 workRecordId 를 프론트에 돌려주고,
     *   프론트는 (type, courseName, workRecordId)를 기억했다가 /endCourse 호출 때 같이 전달한다.
     *
     * URL
     * - POST /course/addingCourse
     *
     * 반환
     * - 생성된 workRecordId (JSON 숫자)
     */
    @PostMapping("/addingCourse")
    public int addCourse(@RequestParam("userId") String userId,
                         @RequestParam("type") String type) {

        // 코스 생성용 운동기록 시작 (courseId / battleId 는 아직 없음)
        WorkRecord workRecord = new WorkRecord();
        workRecord.setUserId(userId);

        int workRecordId = workRecordService.startWorkRecord(workRecord);

        // 프론트는 type, courseName, workRecordId 를 저장해뒀다가 /endCourse 에서 다시 보냄
        return workRecordId;
    }

    /**
     * 코스 생성 완료 (코스 생성중 화면에서 "코스 종료" 클릭 시)
     *
     * 역할
     * - workRecordId 기반으로 운동 측정을 종료하고(WorkRecordService.endWorkRecord)
     * - 요약(WorkRecord) + 측정데이터(Measurement)를 읽어와서 Course/COURSE_PATH를 구성한다.
     * - 코스 거리가 3km 미만이면 코스를 생성하지 않는다.
     * - 띈 거리 * 100 만큼의 정수로 경험치를 획득한다
     *
     * URL
     * - POST /course/endCourse
     *
     * 파라미터
     * - userId 	  : 로그인한 사용자ID
     * - workRecordId : 운동 서비스에서 사용한 운동ID
     * - courseName   : 사용자가 입력한 코스명
     * - type         : 코스 유형(산책/러닝 등)
     *
     * 반환
     * - 생성된 Course 도메인 (JSON)
     * 
     * 코스 이미지 생성은 프로트로 이전, 저장은 구글 url 사용
     */
    @PostMapping("/endCourse")
    public Course endCourse(@RequestParam("userId") String userId,
                            @RequestParam("workRecordId") int workRecordId,
                            @RequestParam("courseName") String courseName,
                            @RequestParam("type") String type,
                            @RequestParam(value = "location", required = false) String location) {

        // 1. 측정데이터 기반으로 Course 도메인 구성
        Course course = buildCourseFromWorkRecord(workRecordId, courseName, type);

        // 1-1. 프론트에서 변환한 주소 문자열을 코스에 저장 (null/공백은 무시)
        if (course != null && location != null) {
            String trimmed = location.trim();
            if (!trimmed.isEmpty()) {
                course.setStartLocation(location);
            }
        }

        // 2. 코스 거리 3km 미만이면 코스는 생성하지 않고, 운동만 종료
        if (course == null
                || course.getDistance() == null
		/* || course.getDistance().compareTo(new BigDecimal("3.00")) < 0 */) { //********************임시 조건 삭제*****************************

            workRecordService.endWorkRecordForAdd(workRecordId, 0, true);
            throw new IllegalArgumentException("코스 거리가 3km 미만이라 코스를 생성할 수 없습니다.");
        }

        // 3. 코스 + 코스 경로 먼저 저장해서 courseId 확보
        Course saved = courseService.addCourse(course);

        // 4. 운동 측정 종료 + 요약 계산 + COURSE_ID 세팅
        workRecordService.endWorkRecordForAdd(workRecordId, saved.getCourseId(), false);

        // 5. 경험치 계산
        BigDecimal distance = saved.getDistance();
        int exp = distance
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.DOWN)
                .intValue();

        userService.levelUp(userId, exp);

        // 6. 화면에는 생성된 코스 정보 반환
        return saved;
    }
    
    @GetMapping("/listCoursePathPoints")
    public List<CoursePathPoint> listCoursePathPoints(@RequestParam("courseId") int courseId) {
        return courseService.listCoursePathPoints(courseId);
    }

    // ─────────────────────────────
    // 2. 코스 상세
    // ─────────────────────────────

    /**
     * 코스 상세 + 후기 리스트 조회
     *
     * 역할
     * - courseId 기반으로 코스 상세 정보 조회 (경로 포함)
     * - 같은 courseId 에 대한 후기 리스트도 함께 조회하여 한 번에 반환
     *
     * URL
     * - GET /course/getCourse?courseId=...
     *
     * 반환
     * - CourseDetailResponse { course, reviews }
     */
    @GetMapping("/getCourse")
    public CourseDetailResponse getCourse(@RequestParam("courseId") int courseId) {

        // 1. 코스 기본/경로 정보 조회
        Course course = courseService.getCourse(courseId);

        // 2. 코스에 달린 후기 리스트 조회
        List<Review> reviews = courseService.listReview(courseId);

        // 3. 하나의 DTO 로 묶어서 반환
        return CourseDetailResponse.builder()
                .course(course)
                .reviews(reviews)
                .build();
    }

    // ─────────────────────────────
    // 3. 코스 사용 / 코스 포기 / 경로 이탈 체크
    // ─────────────────────────────

    /**
     * 코스 사용 시작
     *
     * 역할
     * - courseId 기반으로 코스 정보를 조회 (경로 포함)
     * - 비공개 코스인 경우, 소유자가 아닌 사용자는 사용 불가 (서비스에서 체크)
     * - 프론트는 반환된 Course 의 경로 데이터를 이용해 지도에 경로 표시
     * - 운동 컨트롤러에 "기록 측정 시작"을 요청하는 트리거 역할
     * - 운동이 종료될 때 까지 스케쥴러가 반복적으로 완주/이탈 확인
     *
     * URL
     * - GET /course/useCourse?courseId=...&userId=...
     *
     * 반환
     * - CourseUseResponse { course, workRecordId }
     */
    @GetMapping("/useCourse")
    public CourseUseResponse useCourse(@RequestParam("courseId") int courseId,
                                       @RequestParam("userId") String userId,
                                       @RequestParam(value = "battleId", required = false) Integer battleId) {

        // 1. 코스 정보(경로 포함) 조회
        Course course = courseService.useCourse(courseId, userId);

        // 2. 코스 사용용 운동기록 시작
        WorkRecord workRecord = new WorkRecord();
        workRecord.setUserId(userId);
        workRecord.setCourseId(courseId);
        workRecord.setBattleId(battleId);

        int workRecordId = workRecordService.startWorkRecord(workRecord);

        // 3. "코스 사용 세션" 등록
        courseUseSessionStore.startSession(workRecordId, courseId, userId, battleId);

        // DTO에 담아서 리턴
        return CourseUseResponse.builder()
                .course(course)
                .workRecordId(workRecordId)
                .build();
    }
    
    /**
     * 자동 완주 결과 조회 API
     * 역할
     * - 스케줄러에 의해 세션이 종료된 후, 프론트엔드가 페이지 이동을 위해 호출
     * - 오직 이동할 페이지 타입(type)과 ID(targetId)만 반환
     */
    @GetMapping("/completeResult")
    public ResponseEntity<CourseCompleteResponse> getCompleteResult(@RequestParam("workRecordId") int workRecordId) {

        // 0) 아직 세션이 살아있으면 → 운동 중
        if (courseUseSessionStore.hasSession(workRecordId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409
        }

        // [추가] 세션이 끝났다면, 모니터가 저장해둔 결과가 있는지 먼저 확인
        CourseCompleteResponse cached = courseUseSessionStore.popCompleteResult(workRecordId);
        if (cached != null) {
            return ResponseEntity.ok(cached);
        }

        // 1) 운동기록 조회
        WorkRecord record = workRecordService.getWorkRecord(workRecordId);
        if (record == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404
        }

        // 2) IS_MEASURE 방어
        if (record.isMeasure()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409
        }

        // 3) 일반 완주/종료 결과
        return ResponseEntity.ok(buildCompleteResponse(workRecordId, record.getBattleId()));
    }

    // (내부 헬퍼 메소드) 응답 객체 생성 로직
    private CourseCompleteResponse buildCompleteResponse(int workRecordId, Integer battleId) {
        if (battleId != null && battleId > 0) {
            // 대결 모드 -> 대결 상세 페이지로 이동
            return CourseCompleteResponse.builder()
                    .type("BATTLE")
                    .targetId(battleId)
                    .build();
        } else {
            // 일반 모드 -> 운동 기록 상세 페이지로 이동
            return CourseCompleteResponse.builder()
                    .type("GENERAL")
                    .targetId(workRecordId)
                    .build();
        }
    }

    /**
     * 코스 포기
     *
     * 역할
     * - 코스 사용 중 사용자가 "코스 포기" 버튼을 눌렀을 때,
     * - 그외 여러 경우로 코스운동이 강제종료될 때 호출
     * - 운동 서비스에 기록 측정 강제 종료를 요청한다.
     *
     * URL
     * - POST /course/giveupCourse?workRecordId=...
     *
     * 반환
     * - 바디 없음 (void)
     */
    @PostMapping("/giveupCourse")
    public void giveupCourse(@RequestParam("workRecordId") int workRecordId) {

        // 포기 시 정책:
        //  - deleteMeasure = true 측정/운동기록 자체를 삭제
        boolean deleteMeasure = true;

        workRecordService.endWorkRecordForUse(workRecordId, deleteMeasure);
        courseUseSessionStore.removeSession(workRecordId);
    }

    /**
     * 현재 위치가 코스 경로로부터 이탈했는지 체크
     * * 역할
     * 1. 먼저 세션이 살아있는지 확인 (스케줄러가 종료시켰는지 체크)
     * 2. 살아있다면 경로 이탈 여부 계산 반환
     */
    @GetMapping("/onRoute")
    public boolean isOnCourseRoute(@RequestParam("workRecordId") int workRecordId, // [추가] 세션 확인용
                                   @RequestParam("courseId") int courseId,
                                   @RequestParam("currentLocationWkt") String currentLocationWkt) {

        // 1. 세션 생존 여부 체크
        // 스케줄러가 완주 처리하여 세션을 지웠다면 false가 됨
        boolean isAlive = courseUseSessionStore.hasSession(workRecordId);
        
        if (!isAlive) {
            // 세션이 없다 = 이미 종료되었다 (완주 or 강제종료)
            // 프론트엔드에게 "종료됨"을 알리기 위해 예외를 던지거나 특정 값을 리턴
            // 여기서는 400 Bad Request나 404 Not Found를 던져서 
            // 프론트의 catch 블록이나 에러 핸들러에서 /completeResult를 호출하게 유도
            throw new IllegalStateException("SESSION_CLOSED");
        }

        // 2. 세션이 있다면 경로 이탈 여부 체크 (기존 로직)
        return courseService.isOnCourseRoute(courseId, currentLocationWkt, radiusLeaveMeters);
    }

    // ─────────────────────────────
    // 4. 코스 / 리스트 / 공개 전환 / 안전정보 / 카카오 지도
    // ─────────────────────────────


    /**
     * 코스 리스트 조회
     *
     * 역할
     * - 검색조건에 맞는 코스 정보를 리스트 형태로 전달
     * - 검색 조건
     *   · search.keyword      : 코스명
     *   · search.minDistance  : 최소 거리
     *   · search.maxDistance  : 최대 거리
     *   · search.onlyMyCourse : 내 코스만
     *   · userId              : onlyMyCourse 와 함께 사용
     *   · currentLocationWkt  : 현재 위치 기준 정렬용 (출발지와 거리 계산)
     *
     * URL
     * - GET /course/listCourse (쿼리스트링으로 Search + userId + currentLocationWkt)
     *
     * 반환
     * - List<Course>
     */
    @GetMapping("/listCourse")
    public List<Course> listCourse(@ModelAttribute Search search,
                                   @RequestParam(value = "userId", required = false) String userId,
                                   @RequestParam(value = "currentLocationWkt", required = false) String currentLocationWkt) {

        return courseService.listCourse(search, userId, currentLocationWkt);
    }
    
    /**
     * 평균 별점 높은 순 코스 5개 조회
     * * 역할
     * - 메인 페이지 등에서 인기 코스를 보여주기 위함
     * - 공개된 코스 중에서 평균 별점이 높은 상위 5개를 반환
     * * URL
     * - GET /course/listTopRated
     */
    @GetMapping("/listTopRated")
    public List<TopRatedCourseResponse> listTopRatedCourse() {
        return courseService.listTopRatedCourseForHome();
    }

    /**
     * 코스 공개 전환
     *
     * 역할
     * - 선택한 코스의 상태를 "비공개" → "공개" 로 전환
     * - 자신이 생성한 코스만 전환 가능
     *
     * URL
     * - POST /course/publicCourse?courseId=...&userId=...
     *
     * 반환
     * - 바디 없음 (void)
     */
    @PostMapping("/publicCourse")
    public void publicCourse(@RequestParam("courseId") int courseId,
                             @RequestParam("userId") String userId) {

        courseService.publicCourse(courseId, userId);
    }

    // ─────────────────────────────
    // 5. 후기 관련
    // ─────────────────────────────
    
    /**
     * 후기 작성 사전 권한 체크
     *
     * 역할
     * - 코스 상세 화면에서 "후기 작성" 버튼을 누르기 전에 호출
     * - 최근 7일 이내 해당 코스를 완주한 기록이 있는지 확인
     * - 아직 후기가 달리지 않은 운동기록(workRecordId)이 있는지 확인
     * - 가능 여부와 메시지, 사용할 workRecordId를 반환
     * - UX용
     */
    @GetMapping("/checkAddReviewPermission")
    public ReviewPermissionResponse checkAddReviewPermission(
            @RequestParam("courseId") int courseId,
            @RequestParam("userId") String userId) {

        // 1) 최근 7일 이내, 해당 코스를 완주한 운동기록 리스트 조회
        List<WorkRecord> records = workRecordService.listWorkRecordByCourse(userId, courseId);

        if (records == null || records.isEmpty()) {
            return ReviewPermissionResponse.builder()
                    .allowed(false)
                    .mode("create")
                    .reasonCode("NOT_COMPLETED")
                    .message("후기는 해당 코스를 완주하고 7일 이내에만 작성할 수 있습니다.")
                    .build();
        }

        // 2) 아직 후기가 달리지 않은 운동기록 ID를 찾는다.
        for (WorkRecord record : records) {
            int workRecordId = record.getWorkRecordId();
            boolean exists = courseService.existsReviewByWorkRecordId(workRecordId);
            if (!exists) {
                // 사용 가능한 운동기록 발견 → 작성 가능
                return ReviewPermissionResponse.builder()
                        .allowed(true)
                        .mode("create")
                        .reasonCode("OK")
                        .message("후기를 작성할 수 있습니다.")
                        .workRecordId(workRecordId)
                        .build();
            }
        }

        // 3) 사용 가능한 운동기록이 하나도 없는 경우
        return ReviewPermissionResponse.builder()
                .allowed(false)
                .mode("create")
                .reasonCode("ALREADY_REVIEWED")
                .message("해당 코스에 대해 작성 가능한 운동기록이 없습니다. 이미 후기를 모두 작성하셨을 수 있습니다.")
                .build();
    }

    /**
     * 후기 생성
     *
     * 역할
     * - 후기 정보를 입력해 후기 생성
     * - 후기를 작성하는 코스를 완주하고 7일 이내만 작성 가능
     *   (비즈니스 로직은 서비스에서 체크)
     * - 사용자ID와 코스ID, 현재날짜에서 7일 이내의 기록으로 운동기록 리스트를 반환해주는 운동서비스 메소드를 호출하여 값을 받고 
     * - 운동기록 리스트의 운동ID값을 순차적으로 해당 ID로 후기가 작성된적이 있는지 검사
     *
     * URL
     * - POST /course/addReview?courseId=...&userId=...
     *
     * 입력
     * - 쿼리 파라미터 : courseId, userId
     * - RequestBody   : Review (내용, 별점, 이미지 등)
     *
     * 반환
     * - 생성된 Review
     */
    @PostMapping("/addReview")
    public Review addReview(@RequestParam("courseId") int courseId,
                            @RequestParam("userId") String userId,
                            @RequestBody Review review) {
        // (기존 로직 동일)
        List<WorkRecord> records = workRecordService.listWorkRecordByCourse(userId, courseId);
        if (records == null || records.isEmpty()) {
            throw new IllegalStateException("후기는 해당 코스를 완주하고 7일 이내에만 작성할 수 있습니다.");
        }

        Integer selectedWorkRecordId = null;
        for (WorkRecord record : records) {
            int workRecordId = record.getWorkRecordId();
            boolean exists = courseService.existsReviewByWorkRecordId(workRecordId);
            if (!exists) {
                selectedWorkRecordId = workRecordId;
                break;
            }
        }

        if (selectedWorkRecordId == null) {
            throw new IllegalStateException("해당 코스에 대해 작성 가능한 운동기록이 없습니다.");
        }

        review.setCourseId(courseId);
        review.setUserId(userId);
        review.setWorkRecordId(selectedWorkRecordId);

        return courseService.addReview(review);
    }
    
    /**
     * 후기 이미지 단일 업로드
     *
     * 역할
     * - multipart/form-data 로 전송된 이미지 파일을 서버에 저장
     * - 접근 가능한 이미지 URL 을 반환
     *
     * URL
     * - POST /course/uploadReviewImage?courseId=...
     *
     * 입력
     * - 쿼리 파라미터: courseId (필요 없다면 제거 가능)
     * - form-data: image (MultipartFile)
     *
     * 반환
     * - { "imageUrl": "/upload/review/파일명.png" } 형태의 JSON
     */
    @PostMapping("/uploadReviewImage")
    public Map<String, String> uploadReviewImage(
            @RequestParam("courseId") int courseId,
            @RequestParam("image") MultipartFile image) throws Exception {

        if (image.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        // 1) 파일명 생성
        String originalFilename = image.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        // 예: course_1_20251211123000.jpg
        String newFileName = "course_" + courseId + "_" + timestamp + ext;

        // 2) 리눅스 서버 디렉토리에 파일 저장
        // reviewImageUploadDir = /home/ubuntu/wingstep/upload/review
        Path uploadDirPath = Paths.get(reviewImageUploadDir); 
        if (!Files.exists(uploadDirPath)) {
            Files.createDirectories(uploadDirPath);
        }

        Path targetPath = uploadDirPath.resolve(newFileName);
        image.transferTo(targetPath.toFile());

        // 3) 외부 접속용 URL 생성 (수정된 부분)
        // reviewBaseUrl = http://[퍼블릭IP]/api/upload/review
        // 결과 imageUrl = http://[퍼블릭IP]/api/upload/review/course_1_xxxx.jpg
        
        // base-url 끝에 슬래시가 있는지 없는지 모를 수 있으니 안전하게 처리
        String finalBaseUrl = reviewBaseUrl;
        if (finalBaseUrl.endsWith("/")) {
            finalBaseUrl = finalBaseUrl.substring(0, finalBaseUrl.length() - 1);
        }
        
        String imageUrl = finalBaseUrl + "/" + newFileName;

        Map<String, String> result = new HashMap<>();
        result.put("imageUrl", imageUrl);
        return result;
    }

    /**
     * 후기 삭제
     *
     * 역할
     * - 선택한 후기를 삭제
     * - 자신이 작성한 후기만 삭제 가능
     *
     * URL
     * - DELETE /course/deleteReview?courseId=...&reviewId=...&userId=...
     *
     * 반환
     * - 바디 없음 (void)
     */
    @DeleteMapping("/deleteReview")
    public void deleteReview(@RequestParam("courseId") int courseId,
                             @RequestParam("reviewId") int reviewId,
                             @RequestParam("userId") String userId) {

        // courseId 는 필요 시 검증용으로 활용 가능 (현재 서비스에서는 reviewId, userId 로 처리)
        courseService.deleteReview(reviewId, userId);
    }
    
    /**
     * 후기 수정 사전 권한 체크
     *
     * 역할
     * - 후기 상세 화면에서 "수정" 버튼을 누르기 전에 호출
     * - 최근 7일 이내 해당 코스를 완주한 기록이 있는지 확인
     * - UX용
     */
    @GetMapping("/checkUpdateReviewPermission")
    public ReviewPermissionResponse checkUpdateReviewPermission(
            @RequestParam("courseId") int courseId,
            @RequestParam("reviewId") int reviewId,
            @RequestParam("userId") String userId) {

        // 1) 최근 7일 이내, 해당 코스를 완주한 운동기록 리스트 조회
        List<WorkRecord> records = workRecordService.listWorkRecordByCourse(userId, courseId);

        if (records == null || records.isEmpty()) {
            return ReviewPermissionResponse.builder()
                    .allowed(false)
                    .mode("update")
                    .reasonCode("EXPIRED")
                    .message("후기 수정 가능 기간(완주 후 7일 이내)을 초과했습니다.")
                    .build();
        }

        return ReviewPermissionResponse.builder()
                .allowed(true)
                .mode("update")
                .reasonCode("OK")
                .message("후기를 수정할 수 있습니다.")
                .build();
    }


    /**
     * 후기 수정
     *
     * 역할
     * - 선택한 후기를 수정
     * - 코스 완주일로부터 7일 이내 + 본인 후기만 수정 가능
     *
     * URL
     * - PUT /course/updateReview?courseId=...&reviewId=...&userId=...
     *
     * 반환
     * - 수정된 Review
     */
    @PutMapping("/updateReview")
    public void updateReview(@RequestParam("courseId") int courseId,
                               @RequestParam("reviewId") int reviewId,
                               @RequestParam("userId") String userId,
                               @RequestBody Review review) {

        // 1) 생성 때와 동일하게, 최근 7일 이내 완주 기록이 있는지 확인
        List<WorkRecord> records = workRecordService.listWorkRecordByCourse(userId, courseId);
        if (records == null || records.isEmpty()) {
            throw new IllegalStateException("후기 수정 가능 기간(완주 후 7일 이내)을 초과했습니다.");
        }

        // 2) 수정 대상 후기 기본 정보 세팅
        review.setCourseId(courseId);
        review.setReviewId(reviewId);
        review.setUserId(userId);

        // 실제 “본인 후기인지 여부” 체크와 DB 업데이트는 서비스에 위임
        courseService.updateReview(review);
    }
    
    /**
     * workRecordId 기반으로
     * - WorkRecord 요약 정보
     * - Measurement 리스트
     * 를 읽어서 Course + CoursePath 리스트를 구성하는 유틸리티 메서드.
     */
    private Course buildCourseFromWorkRecord(int workRecordId, String courseName, String type) {
        // 1. 운동 기록 요약 조회
        WorkRecord summary = workRecordService.getWorkRecord(workRecordId);
        if (summary == null) {
            throw new IllegalStateException("workRecordId=" + workRecordId + " 운동기록 없음");
        }

        // 2. 측정 데이터 리스트 조회
        List<Measurement> measurements = measurementService.listMeasurement(workRecordId);
        if (measurements == null || measurements.isEmpty()) {
//            throw new IllegalStateException("측정 데이터가 없어 코스를 생성할 수 없습니다.");
            System.out.println("측정 데이터가 없어 코스를 생성할 수 없습니다.");
            
            return null;
        }

        Measurement first = measurements.get(0);
        Measurement last  = measurements.get(measurements.size() - 1);

        System.out.println("=== [DEBUG] buildCourseFromWorkRecord ===");
        System.out.println("workRecordId = " + workRecordId);
        for (int i = 0; i < measurements.size(); i++) {
            Measurement m = measurements.get(i);
            System.out.println(
                    "  measurement[" + i + "] rawLocation = " + m.getMeasurementLocation()
                            + ", datetime = " + m.getMeasurementDatetime()
            );
        }

        // 3. CoursePath 리스트 변환
        //    -> DB에서 ST_GeomFromText(#{location}, 4326) 로 사용할 값이므로
        //       MEASUREMENT 에서 온 "POINT(37.5665 126.978)" 그대로 저장
        List<CoursePath> pathList = measurements.stream()
                .map(m -> CoursePath.builder()
                        .location(m.getMeasurementLocation())   // ★ 그대로 사용
                        .dateTime(m.getMeasurementDatetime())
                        .build())
                .collect(Collectors.toList());

        // 4. ROUTE_LINE WKT (LINESTRING)
        //    - POINT(37.5665 126.978) -> 괄호 안만 뽑아서 "37.5665 126.978"
        String lineStringWkt = "LINESTRING(" +
                measurements.stream()
                        .map(m -> {
                            String raw = m.getMeasurementLocation(); // 예: "POINT(37.5665 126.978)"
                            if (raw == null) return "0 0";

                            String loc = raw.trim();
                            if (loc.toUpperCase().startsWith("POINT")) {
                                int s = loc.indexOf('(');
                                int e = loc.indexOf(')');
                                if (s >= 0 && e > s) {
                                    loc = loc.substring(s + 1, e).trim(); // "37.5665 126.978"
                                }
                            }
                            // 혹시 쉼표가 있다면 공백으로만 통일
                            loc = loc.replace(",", " ");
                            return loc; // "37.5665 126.978"
                        })
                        .collect(Collectors.joining(", ")) +
                ")";

        // 5. START_POINT / END_POINT
        //    - 이미 "POINT(37.5665 126.978)" 형태이므로 그대로 넘기면 됨
        String startWkt = first.getMeasurementLocation();
        String endWkt   = last.getMeasurementLocation();

        System.out.println("  startWkt  = " + startWkt);
        System.out.println("  endWkt    = " + endWkt);
        System.out.println("  routeLine = " + lineStringWkt);
        System.out.println("  last distance = " + last.getDistance());
        System.out.println("=== [DEBUG] buildCourseFromWorkRecord END ===");

        // 6. 거리값은 마지막 측정값 distance 사용
        return Course.builder()
                .userId(summary.getUserId())
                .courseName(courseName)
                .type(type)
                .distance(BigDecimal.valueOf(last.getDistance()))
                .startPoint(startWkt)       // ST_GeomFromText(?, 4326)
                .endPoint(endWkt)
                .coursePathList(pathList)
                .routeLine(lineStringWkt)   // ST_GeomFromText(?, 4326)
                .build();
    }

}
