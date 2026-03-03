package com.wingstep.service.workrecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.wingstep.service.domain.workrecord.WorkRecord;
import com.wingstep.service.domain.workrecord.WorkRecordView;

public interface WorkRecordService {

    /**
     * 운동 기록 시작
     *  - INSERT 수행
     *  - 생성된 PK(workRecordId) 반환
     */
    int startWorkRecord(WorkRecord workRecord);

    /**
     * 운동 기록 상세 조회
     */
    WorkRecord getWorkRecord(int workRecordId);
    
    /**
     * 운동 기록 상세 조회(View)
     */
    WorkRecordView getWorkRecordView(int workRecordId);
    
    /**
     * 운동 기록(측정), 측정중인 운동기록만 조회
     */
    WorkRecord getWorkRecordForMeasure(String userId);

    /**
     * 운동 기록 리스트(사용자별)
     */
    List<WorkRecordView> listWorkRecord(String userId, LocalDate startDate, LocalDate endDate, String type);
    
    /**
     * 운동 기록 리스트(코스 후기), 현재시간 기준 7일 이내 데이터만 조회
     */
    List<WorkRecord> listWorkRecordByCourse(String userId, int courseId);

    /**
     * 운동 기록 종료(코스 생성)
     *  - isDelete == true  : 운동기록 + 측정기록 삭제
     *  - isDelete == false : 측정기록 집계하여 WORKRECORD 요약값 UPDATE
     */
    void endWorkRecordForUse(int workRecordId, boolean isDelete);
    
    /**
     * 운동 기록 종료(코스 사용)
     *  - isDelete == true  : 운동기록 + 측정기록 삭제
     *  - isDelete == false : 측정기록 집계하여 WORKRECORD 요약값 UPDATE
     */
    void endWorkRecordForAdd(int workRecordId, int courseId, boolean isDelete);
    
    /**
     * 운동 기록 삭제
     */
    void deleteWorkRecord(int workRecordId);

    /**
     * 운동 기록 통계용 조회
     */
    List<WorkRecord> statsWorkRecord(String userId, String startDate, String endDate, String type, String battleType);

    /**
     * 운동 기록 통계용 기간 조회
     */
    Map<String, Object> getAvailablePeriods(String userId);
    
    List<WorkRecord> listAllForPeriod(String userId);
    WorkRecord getWorkRecordForMeasureByCourse(String userId, int courseId);
    Integer getAddingWorkRecordId(String userId);

}
