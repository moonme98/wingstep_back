package com.wingstep.service.domain.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Avatar {

    private int avatarId;      // 아바타 ID
    private String avatarImg;  // 아바타 이미지 URL or 경로
    private int reqLevel;      // 충족 레벨
}

