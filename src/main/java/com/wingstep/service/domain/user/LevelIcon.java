package com.wingstep.service.domain.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LevelIcon {

    private int levelId;       // 레벨 ID
    private int reqExp;        // 해당 레벨 목표 경험치
    private String levelIcon;  // 아이콘 이미지 경로
}
