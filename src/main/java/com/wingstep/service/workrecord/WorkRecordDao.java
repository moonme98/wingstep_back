package com.wingstep.service.workrecord;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wingstep.service.domain.workrecord.WorkRecord;
import com.wingstep.service.domain.workrecord.WorkRecordView;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface WorkRecordDao {
    // 운동기록 시작(INSERT)
    int startWorkRecord(WorkRecord workRecord);

    // 운동기록 상세
    WorkRecord getWorkRecord(int workRecordId);
    
    // 운동기록 상세(View)
    WorkRecordView getWorkRecordView(int workRecordId);

    // 측정중인 운동기록
    WorkRecord getWorkRecordForMeasure(String userId);

    // 운동기록 리스트(사용자)
    List<WorkRecordView> listWorkRecord(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("type") String type
    );
    
    // 운동기록 리스트(코스 후기), 현재시간 기준 7일 이내 데이터만 조회
    List<WorkRecord> listWorkRecordByCourse(
            @Param("userId") String userId,
            @Param("courseId") int courseId);

    // 운동기록 요약 값 업데이트
    void updateWorkRecordSummary(WorkRecord workRecord);

    // 운동기록 삭제
    void deleteWorkRecord(int workRecordId);

    // 운동기록 통계 조회
    List<WorkRecord> statsWorkRecord(@Param("userId") String userId, // 필수
    		@Param("startDate") String startDate,
    		@Param("endDate") String endDate,
    		@Param("type") String type,      // "산책", "러닝" 또는 null/빈값
    		@Param("battleType") String battleType // "개인", "대결" 또는 null/빈값
    );
    
    // 측정중인 운동기록(유저 + 코스)
    WorkRecord getWorkRecordForMeasureByCourse(
            @Param("userId") String userId,
            @Param("courseId") int courseId
    );
    
    Integer getAddingWorkRecordId(@Param("userId") String userId);


    
    // --------------------------
    // 운동 기록 통계용 기간 조회
    // --------------------------

    /** 실제 운동기록이 존재하는 주 단위 목록 */
    List<Map<String, Object>> getAvailableWeeks(String userId);

    /** 실제 운동기록이 존재하는 월 단위 목록 */
    List<String> getAvailableMonths(String userId);

    /** 실제 운동기록이 존재하는 연 단위 목록 */
    List<Integer> getAvailableYears(String userId);
    
    List<WorkRecord> listAllForPeriod(String userId);
}
