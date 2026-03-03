package com.wingstep.service.course.session;

import org.springframework.stereotype.Component;

import com.wingstep.service.domain.course.CourseCompleteResponse;
import com.wingstep.service.domain.course.CourseUseSession;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 현재 "코스 사용 중"인 세션들을 메모리에 들고 있는 저장소
 * - key : workRecordId
 */
@Component
public class CourseUseSessionStore {

    private final Map<Integer, CourseUseSession> sessions = new ConcurrentHashMap<>();
    private final Map<Integer, CourseCompleteResponse> completeResults = new ConcurrentHashMap<>();

    // 코스 사용 시작 시 세션 등록
    public void startSession(int workRecordId, int courseId, String userId, Integer battleId) {
        CourseUseSession session = CourseUseSession.builder()
                .workRecordId(workRecordId)
                .courseId(courseId)
                .userId(userId)
                .battleId(battleId)
                .offRouteSeconds(0)
                .build();

        sessions.put(workRecordId, session);
    }

    // 코스 사용 종료/포기 시 세션 제거
    public void removeSession(int workRecordId) {
        sessions.remove(workRecordId);
    }

    // 스케줄러가 전체 세션 돌면서 체크할 때 사용
    public Collection<CourseUseSession> getAllSessions() {
        return sessions.values();
    }
    
    /**
     * 해당 운동기록ID의 세션이 존재하는지 확인
     */
    public boolean hasSession(int workRecordId) {
        return sessions.containsKey(workRecordId);
    }
    
    // 결과 저장
    public void saveCompleteResult(int workRecordId, CourseCompleteResponse result) {
        if (result != null) {
            completeResults.put(workRecordId, result);
        }
    }

    // 결과 꺼내기(한 번 응답하면 제거)
    public CourseCompleteResponse popCompleteResult(int workRecordId) {
        return completeResults.remove(workRecordId);
    }
}
