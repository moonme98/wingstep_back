package com.wingstep.common;

import lombok.Data;

@Data
public class PageRequest {

    // 0부터 시작
    private int offset = 0;

    // 한 번에 가져올 개수
    private int limit;

    public PageRequest(int limit) {
        this.limit = limit;
    }

    public PageRequest(int offset, int limit) {
        this.offset = Math.max(0, offset);
        this.limit = limit;
    }
}
