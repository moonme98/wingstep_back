package com.wingstep.unit;

// ▼▼▼ 매처 임포트 추가/확인 ▼▼▼
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils; // [추가] private 필드(@Value) 주입용

import com.wingstep.service.course.CourseService;
import com.wingstep.service.course.CourseUseMonitor;
import com.wingstep.service.course.session.CourseUseSessionStore;
import com.wingstep.service.domain.course.CourseUseSession;
import com.wingstep.service.domain.workrecord.Measurement;
import com.wingstep.service.user.UserService; // [추가]
import com.wingstep.service.workrecord.MeasurementService;
import com.wingstep.service.workrecord.WorkRecordService;

@ExtendWith(MockitoExtension.class)
class CourseUseMonitorTest {

    @InjectMocks
    private CourseUseMonitor monitor; // 테스트 대상

    @Mock private CourseUseSessionStore sessionStore;
    @Mock private WorkRecordService workRecordService;
    @Mock private MeasurementService measurementService;
    @Mock private CourseService courseService;
    @Mock private UserService userService; // [추가]

    @Test
    @DisplayName("경로 이탈 5초 이상 시 강제 종료 테스트")
    void testForceQuit() {
        // [중요] @Value로 주입받는 private 필드들에 값을 강제로 넣어줍니다.
        // 테스트 환경에서는 application.properties를 읽지 않으므로 직접 세팅해야 합니다.
        ReflectionTestUtils.setField(monitor, "offRouteLimitSeconds", 5);
        ReflectionTestUtils.setField(monitor, "radiusMeters", 100);

        // Given: 세션이 하나 존재함
        CourseUseSession mockSession = CourseUseSession.builder()
                .workRecordId(100)
                .courseId(1)
                .userId("testUser")
                .offRouteSeconds(0)
                .build();
        
        when(sessionStore.getAllSessions()).thenReturn(List.of(mockSession));
        
        // 가짜 측정 데이터 (현재 위치) 반환 설정
        Measurement mockMeasurement = new Measurement();
        mockMeasurement.setMeasurementLocation("127.0, 37.0");
        when(measurementService.listMeasurement(100)).thenReturn(List.of(mockMeasurement));

        // [수정 포인트] isOnCourseRoute 메서드 시그니처 변경 반영
        // 기존: isOnCourseRoute(eq(1), anyString())
        // 변경: isOnCourseRoute(eq(1), anyString(), anyInt())  <-- radius 파라미터 대응
        when(courseService.isOnCourseRoute(eq(1), anyString(), anyInt())).thenReturn(false);

        // When: 스케줄러를 5번 실행 (5초 경과 시뮬레이션)
        for (int i = 0; i < 5; i++) {
            monitor.checkCourseUseSessions();
        }

        // Then:
        // 1. 5번째 실행 후 세션 제거가 호출되었는지 확인
        verify(sessionStore).removeSession(100);
        // 2. 운동 기록 종료(실패 처리)가 호출되었는지 확인
        verify(workRecordService).endWorkRecordForUse(100, false);
    }
}