package com.wingstep.service.workrecord.impl;

import com.wingstep.service.domain.workrecord.Measurement;
import com.wingstep.service.domain.workrecord.WorkRecord;
import com.wingstep.service.domain.workrecord.WorkRecordView;
import com.wingstep.service.workrecord.MeasurementDao;
import com.wingstep.service.workrecord.WorkRecordDao;
import com.wingstep.service.workrecord.WorkRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class WorkRecordServiceImpl implements WorkRecordService {

    private final WorkRecordDao workRecordDao;
    private final MeasurementDao measurementDao;

    public WorkRecordServiceImpl(WorkRecordDao workRecordDao,
                                 MeasurementDao measurementDao){
        this.workRecordDao = workRecordDao;
        this.measurementDao = measurementDao;
    }

    @Override
    public int startWorkRecord(WorkRecord wr) {
        // INSERT
        workRecordDao.startWorkRecord(wr);
        // useGeneratedKeys 로 세팅된 PK 반환
        return wr.getWorkRecordId();
    }

    @Override
    public WorkRecord getWorkRecord(int id){
        return workRecordDao.getWorkRecord(id);
    }
    
    @Override
    public WorkRecordView getWorkRecordView(int id){
        return workRecordDao.getWorkRecordView(id);
    }
    
    @Override
    public WorkRecord getWorkRecordForMeasure(String userId) {
        return workRecordDao.getWorkRecordForMeasure(userId);
    }

    @Override
    public List<WorkRecordView> listWorkRecord(String userId, LocalDate startDate, LocalDate endDate, String type){
        return workRecordDao.listWorkRecord(userId, startDate, endDate, type);
    }
    
    @Override
    public List<WorkRecord> listWorkRecordByCourse(String userId, int courseId){
        return workRecordDao.listWorkRecordByCourse(userId, courseId);
    }
    
    private void applyLastMeasurementSummary(int workRecordId, WorkRecord wr) {
        // 마지막 측정 1건 조회
        Measurement last = measurementDao.getLastMeasurement(workRecordId);

        if (last == null) {
            // 측정값이 하나도 없는 경우: 측정 종료만 표시
            wr.setMeasure(false);
            workRecordDao.updateWorkRecordSummary(wr);
            return;
        }

        // 측정 종료 상태
        wr.setMeasure(false);

        // 누적 값(마지막 측정)이 곧 최종값이라는 전제
        wr.setDistance(last.getDistance());
        wr.setMaxSpeed(last.getSpeed());
        wr.setAvgSpeed(last.getSpeed());
        wr.setMaxHeartrate(last.getHeartrate());
        wr.setAvgHeartrate(last.getHeartrate());
        wr.setCalorie(last.getCalorie());

        double speed = last.getSpeed();
        double distance = last.getDistance();
        int steps = last.getSteps();

        // 현재 페이스 (분/km)
        // speed == 0 인 경우 0으로 보호
        double currentPace = 0.0;
        if (speed > 0) {
            currentPace = 60.0 / speed;  // 분/킬로
        }

        wr.setAvgPace(currentPace); 

        // 현재 Cadence (steps/min) -> 정수 변환
        int cadence = 0;
        if (distance > 0 && speed > 0 && steps > 0) {
            double cadenceValue = (steps * speed) / (60.0 * distance);
            cadence = (int) Math.round(cadenceValue);   // 가장 정확한 정수 변환
        }

        wr.setCadence(cadence);

        workRecordDao.updateWorkRecordSummary(wr);
    }

    // 운동기록 종료(코스 생성)
    @Override
    public void endWorkRecordForAdd(int workRecordId, int courseId, boolean isDelete) {
        WorkRecord wr = getWorkRecord(workRecordId);

        if (isDelete) {
            // 1) 측정기록 삭제
            measurementDao.deleteAllMeasurement(workRecordId);
            // 2) 운동기록 삭제
            workRecordDao.deleteWorkRecord(workRecordId);
        } else {
            // 코스 생성 시에는 courseId를 새로 세팅
            wr.setCourseId(courseId);

            // 마지막 측정 1건 기준으로 요약 반영
            applyLastMeasurementSummary(workRecordId, wr);
        }
    }
    
    // 운동기록 종료(코스 사용)
    @Override
    public void endWorkRecordForUse(int workRecordId, boolean isDelete) {
        WorkRecord wr = getWorkRecord(workRecordId);

        if (isDelete) {
            // 1) 측정기록 삭제
            measurementDao.deleteAllMeasurement(workRecordId);
            // 2) 운동기록 삭제
            workRecordDao.deleteWorkRecord(workRecordId);
        } else {
            // 마지막 측정 1건 기준으로 요약 반영
            applyLastMeasurementSummary(workRecordId, wr);
        }
    }

    @Override
    public void deleteWorkRecord(int id){
        workRecordDao.deleteWorkRecord(id);
    }

    @Override
    public List<WorkRecord> statsWorkRecord(String userId,
                                            String startDate,
                                            String endDate,
                                            String type,
                                            String battleType) {
        return workRecordDao.statsWorkRecord(userId, startDate, endDate, type, battleType);
    }
    
    @Override
    public Map<String, Object> getAvailablePeriods(String userId) {

        List<WorkRecord> list = workRecordDao.listAllForPeriod(userId);

        Set<String> weekKeys = new HashSet<>();
        Set<String> monthKeys = new HashSet<>();
        Set<String> yearKeys = new HashSet<>();

        List<Map<String, String>> weeks = new ArrayList<>();
        List<Map<String, String>> months = new ArrayList<>();
        List<Map<String, String>> years = new ArrayList<>();

        for (WorkRecord wr : list) {

            LocalDate date = wr.getWorkRecordDate();

            // ===== 주간 계산 =====
            WeekFields wf = WeekFields.ISO;
            int week = date.get(wf.weekOfWeekBasedYear());
            int year = date.get(wf.weekBasedYear());

            String weekKey = year + "-" + String.format("%02d", week);

            if (!weekKeys.contains(weekKey)) {
                weekKeys.add(weekKey);

                LocalDate monday = date.with(wf.dayOfWeek(), 1);
                LocalDate sunday = monday.plusDays(6);

                Map<String, String> item = new HashMap<>();
                item.put("key", weekKey);
                item.put("label", monday.getMonthValue() + "/" + monday.getDayOfMonth()
                        + "~" + sunday.getMonthValue() + "/" + sunday.getDayOfMonth());

                weeks.add(item);
            }

            // ===== 월간 계산 =====
            String monthKey = date.getYear() + "-" + String.format("%02d", date.getMonthValue());
            if (!monthKeys.contains(monthKey)) {
                monthKeys.add(monthKey);

                Map<String, String> item = new HashMap<>();
                item.put("key", monthKey);
                item.put("label", date.getYear() + "." + date.getMonthValue());
                months.add(item);
            }

            // ===== 연간 계산 =====
            String yearKey = String.valueOf(date.getYear());
            if (!yearKeys.contains(yearKey)) {
                yearKeys.add(yearKey);

                Map<String, String> item = new HashMap<>();
                item.put("key", yearKey);
                item.put("label", yearKey + "년");
                years.add(item);
            }
        }

        // 최신순 정렬
        weeks.sort((a, b) -> b.get("key").compareTo(a.get("key")));
        months.sort((a, b) -> b.get("key").compareTo(a.get("key")));
        years.sort((a, b) -> b.get("key").compareTo(a.get("key")));

        Map<String, Object> result = new HashMap<>();
        result.put("weeks", weeks);
        result.put("months", months);
        result.put("years", years);

        return result;
    }

    @Override
    public List<WorkRecord> listAllForPeriod(String userId) {
        return workRecordDao.listAllForPeriod(userId);
    }
    
    @Override
    public WorkRecord getWorkRecordForMeasureByCourse(String userId, int courseId) {
        return workRecordDao.getWorkRecordForMeasureByCourse(userId, courseId);
    }
    
    @Override
    public Integer getAddingWorkRecordId(String userId) {
        return workRecordDao.getAddingWorkRecordId(userId);
    }
    
} // end of class
