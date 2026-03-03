package com.wingstep.service.domain.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 후기 작성/수정 권한 체크 결과 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewPermissionResponse {

    // 권한 여부 (true = 가능, false = 불가능)
    private boolean allowed;

    // 모드 : "create" or "update"
    private String mode;

    // 사유 코드 (프론트에서 분기문에 사용 가능)
    // 예: OK, NOT_COMPLETED, EXPIRED, ALREADY_REVIEWED, NOT_OWNER
    private String reasonCode;

    // 사용자에게 보여줄 메시지
    private String message;

    // 작성 시에 사용할 수 있는 workRecordId (create 전용, 선택적으로 사용)
    // - 작성 가능할 때만 값 세팅
    private Integer workRecordId;
}
