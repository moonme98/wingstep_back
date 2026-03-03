package com.wingstep.service.course;

import com.wingstep.service.course.session.CourseUseSessionStore;
import com.wingstep.service.domain.course.Course;
import com.wingstep.service.domain.course.CourseUseSession;
import com.wingstep.service.domain.workrecord.Measurement;
import com.wingstep.service.user.UserService;
import com.wingstep.service.workrecord.MeasurementService;
import com.wingstep.service.workrecord.WorkRecordService;
import com.wingstep.service.domain.course.CourseCompleteResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CourseUseMonitor {

    private final CourseUseSessionStore sessionStore;
    private final WorkRecordService workRecordService;
    private final MeasurementService measurementService;
    private final CourseService courseService;
    private final UserService userService;

    // ▼▼▼ [수정] 프로퍼티 값 주입 ▼▼▼
    
    @Value("${wingstep.course.monitor.off-route-limit-seconds}")
    private int offRouteLimitSeconds; // 10

    @Value("${wingstep.course.exp-multiplier}")
    private int expMultiplier; // 100 경험치 배율
    
    @Value("${wingstep.course.monitor.complete.radius-meters}")
    private int radiusCompleteMeters; // 완주판정
    
    @Value("${wingstep.course.monitor.leave.radius-meters}")
    private int radiusLeaveMeters; // 이탈판정
    
    @Scheduled(fixedRateString = "${wingstep.course.monitor.interval-ms}")
    public void checkCourseUseSessions() {

//        System.out.println(">>> [Monitor] 세션 수 = " + sessionStore.getAllSessions().size());

        for (CourseUseSession session : sessionStore.getAllSessions()) {

            int workRecordId = session.getWorkRecordId();
            int courseId = session.getCourseId();
            String userId = session.getUserId();

            List<Measurement> measurements = measurementService.listMeasurement(workRecordId);
            if (measurements == null || measurements.isEmpty()) {
                System.out.println(">>> [Monitor] 측정값 없음. workRecordId=" + workRecordId);
                continue;
            }

            Measurement last = measurements.get(measurements.size() - 1);
            double distanceKm = last.getDistance();
            String currentLocation = last.getMeasurementLocation();

            System.out.println(">>> [Monitor] workRecordId=" + workRecordId
                    + ", userId=" + userId
                    + ", courseId=" + courseId);
            System.out.println(">>> [Monitor]   rawLocation = " + currentLocation
                    + ", lastDistance=" + distanceKm);

            String currentLocationWkt = last.getMeasurementLocation();

            if (currentLocationWkt == null || !currentLocationWkt.toUpperCase().startsWith("POINT(")) {
                System.out.println(">>> [Monitor] 측정 위치 형식 이상. rawLocation=" + currentLocationWkt);
                continue;
            }
            
			// 위도/경도 순서 교체 "POINT(" 와 ")" 제거
			String content = currentLocationWkt.replace("POINT(", "").replace(")", "").trim();
			String[] coords = content.split(" "); // 공백으로 분리

			// DB에 맞는 포맷인 POINT(경도 위도)로 재조립
			String swappedWkt = String.format("POINT(%s %s)", coords[1], coords[0]);

			System.out.println(">>> [Monitor]   Swapped WKT = " + swappedWkt);

            System.out.println(">>> [Monitor]   WKT = " + currentLocationWkt);

            boolean onRoute = courseService.isOnCourseRoute(courseId, currentLocationWkt, radiusLeaveMeters);
            if (!onRoute) {
                session.setOffRouteSeconds(session.getOffRouteSeconds() + 1);
            } else {
                session.setOffRouteSeconds(0);
            }

            System.out.println(">>> [Monitor]   onRoute=" + onRoute
                    + ", offRouteSeconds=" + session.getOffRouteSeconds());

            if (session.getOffRouteSeconds() >= offRouteLimitSeconds) {
                System.out.println(">>> [Monitor]   이탈 한도 초과 -> 강제 종료. workRecordId=" + workRecordId);

                // [추가] 프론트가 completeResult로 받을 수 있게 결과 저장
                sessionStore.saveCompleteResult(
                    workRecordId,
                    CourseCompleteResponse.builder()
                        .type("OFF_ROUTE")
                        .targetId(courseId)   // 코스 상세로 보내기 위해 courseId 전달
                        .build()
                );

                workRecordService.endWorkRecordForUse(workRecordId, true); // 기록 삭제 정책 유지
                sessionStore.removeSession(workRecordId);
                continue;
            }

            Course course = courseService.getCourse(courseId);
            if (course == null || course.getDistance() == null) {
                System.out.println(">>> [Monitor]   코스 정보 없음 또는 거리 없음. courseId=" + courseId);
                continue;
            }

            BigDecimal courseDistance = course.getDistance();
            BigDecimal currentDistance = BigDecimal.valueOf(distanceKm);
            boolean enoughDistance = currentDistance.compareTo(courseDistance) >= 0;

            boolean nearEndPoint = courseService.isArrivedAtEndPoint(courseId, swappedWkt, radiusCompleteMeters);

            System.out.println(">>> [Monitor]   courseDistance=" + courseDistance
                    + ", currentDistance=" + currentDistance
                    + ", enoughDistance=" + enoughDistance
                    + ", nearEndPoint=" + nearEndPoint
                    + ", swappedWkt=" + swappedWkt);

            if (enoughDistance && nearEndPoint) {
                System.out.println(">>> [Monitor]  완주 조건 만족! workRecordId=" + workRecordId);

                workRecordService.endWorkRecordForUse(workRecordId, false);

                int exp = courseDistance
                        .multiply(BigDecimal.valueOf(expMultiplier))
                        .setScale(0, RoundingMode.DOWN)
                        .intValue();

                userService.levelUp(userId, exp);

                // [추가] 완주 결과 저장 (대결이면 BATTLE, 아니면 GENERAL)
                Integer battleId = session.getBattleId();
                CourseCompleteResponse result =
                    (battleId != null && battleId > 0)
                        ? CourseCompleteResponse.builder().type("BATTLE").targetId(battleId).build()
                        : CourseCompleteResponse.builder().type("GENERAL").targetId(workRecordId).build();

                sessionStore.saveCompleteResult(workRecordId, result);

                sessionStore.removeSession(workRecordId);
                System.out.println(">>> 코스 완주 확인! 세션 종료 : " + userId);
            }
        }
    }
}