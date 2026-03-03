// package는 네 프로젝트 구조에 맞게 조정
// 예: com.wingstep.service.course.session
package com.wingstep.service.domain.course;

import lombok.Builder;
import lombok.Data;

/**
 * 코스 사용 중인 1개의 세션 정보
 * - 어떤 사용자가
 * - 어떤 코스(courseId)를
 * - 어떤 운동기록(workRecordId)으로
 * 사용 중인지 저장
 */
@Data
@Builder
public class CourseUseSession {

    // WORKRECORD_ID
    private int workRecordId;

    // COURSE_ID
    private int courseId;

    // USER_ID
    private String userId;

    // (선택) BATTLE_ID - 대결 사용 시
    private Integer battleId;

    // 100m 밖에 나가 있는 누적 초 (스케줄러가 1초마다 +1)
    private int offRouteSeconds;
}
