package com.wingstep.common;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 코스 검색 조건을 캡슐화하는 공용 VO
 * - 검색어, 거리 범위, 내 코스 여부 등을 한 번에 전달하기 위한 클래스
 */
@Data
public class Search {

    // 검색어 (코스명 등 키워드)
    private String keyword;

    // 최소 거리 (km 단위, null 이면 제한 없음)
    private BigDecimal minDistance;

    // 최대 거리 (km 단위, null 이면 제한 없음)
    private BigDecimal maxDistance;

    // 내 코스만 조회 여부 (true 이면 userId 기준으로 내 코스만 조회)
    private Boolean onlyMyCourse;

    /**
     * 검색 조건이 하나라도 설정되어 있는지 여부
     * - 컨트롤러/서비스에서 "조건 검색" vs "거리순 기본 정렬" 분기할 때 사용
     */
    public boolean hasCondition() {
        return (keyword != null && !keyword.isBlank())
                || minDistance != null
                || maxDistance != null
                || (onlyMyCourse != null && onlyMyCourse);
    }
}
