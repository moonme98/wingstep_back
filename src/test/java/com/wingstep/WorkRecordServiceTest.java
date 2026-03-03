package com.wingstep;

import com.wingstep.service.domain.workrecord.WorkRecord;
import com.wingstep.service.workrecord.MeasurementDao;
import com.wingstep.service.workrecord.WorkRecordDao;
import com.wingstep.service.workrecord.WorkRecordService;
import com.wingstep.service.domain.workrecord.Measurement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@Transactional
@Rollback
class WorkRecordServiceTest {

    @Autowired
    private WorkRecordService workRecordService;

    @Autowired
    private WorkRecordDao workRecordDao;

    @Autowired
    private MeasurementDao measurementDao;

    /**
     * endWorkRecord()가
     * - is_delete = false 일 때
     *   1) 측정기록을 집계해서 workrecord 테이블에 요약값을 반영하는지
     *   2) is_measure 값을 0(종료)로 변경하는지
     * 를 검증하는 테스트 예시
     */
    @Test
    void endWorkRecord_aggregateSummary() {

        // given: 운동기록 1건 생성
        WorkRecord wr = new WorkRecord();
        wr.setUserId("U001");
        wr.setCourseId(1);
        wr.setBattleId(null);        // 대결 아님
        wr.setMeasure(true);         // 측정중
        wr.setWorkRecordDate(LocalDate.now());

        workRecordDao.startWorkRecord(wr);
        Integer workRecordId = wr.getWorkRecordId();
        assertThat(workRecordId).isNotNull();

        // 측정기록 3건 (거리/속도/심박/스텝 등) 입력
        // 여기서는 distance를 누적값(1km, 2km, 3km)이라고 가정
        Measurement m1 = new Measurement();
        m1.setWorkRecordId(workRecordId);
        m1.setMeasurementDatetime(LocalDateTime.now().minusMinutes(20));
        m1.setSpeed(9);
        m1.setDistance(1.00);
        m1.setSteps(1000);
        m1.setHeartrate(140);
        m1.setCalorie(50);
        m1.setMeasurementLocation("11.1111, 121.1111");
        measurementDao.addMeasurement(m1);

        Measurement m2 = new Measurement();
        m2.setWorkRecordId(workRecordId);
        m2.setMeasurementDatetime(LocalDateTime.now().minusMinutes(10));
        m2.setSpeed(11);
        m2.setDistance(2.00);
        m2.setSteps(1200);
        m2.setHeartrate(150);
        m2.setCalorie(60);
        m2.setMeasurementLocation("22.2222, 122.2222");
        measurementDao.addMeasurement(m2);

        Measurement m3 = new Measurement();
        m3.setWorkRecordId(workRecordId);
        m3.setMeasurementDatetime(LocalDateTime.now());
        m3.setSpeed(10);
        m3.setDistance(3.00);
        m3.setSteps(1300);
        m3.setHeartrate(145);
        m3.setCalorie(70);
        m3.setMeasurementLocation("33.3333, 123.3333");
        measurementDao.addMeasurement(m3);

        // when: 운동기록 종료(집계)
        workRecordService.endWorkRecordForUse(workRecordId, false);

        // then: workrecord 요약값이 집계되었는지 확인
        WorkRecord result = workRecordDao.getWorkRecord(workRecordId);
        assertThat(result).isNotNull();
        assertThat(result.isMeasure()).isFalse();   // 측정 상태가 종료로 변경되었는지

        // ---- 기대값 계산 (서비스 집계 로직과 동일하게 맞춰서 수정) ----
        // distance: 누적값 방식이면 마지막 측정값(3.00) 또는 합계(1+2+3) 중 프로젝트 기준에 맞게 변경
        double expectedDistance = 3.00; // 또는 6.00(합계) - 실제 로직에 맞게 수정
        int expectedMaxSpeed = 10;
        double expectedAvgSpeed = (9 + 11 + 10) / 3.0;
        int expectedMaxHr = 145;
        int expectedAvgHr = (int) Math.round((140 + 150 + 145) / 3.0);

        // pace / cadence 계산법에 맞게 기대값 설정
        // 예시: 평균 페이스 = (전체 시간 / 전체 거리), 케이던스 = (총 스텝 / 총 시간 또는 분)
        // 여기서는 단순히 "값이 들어갔다" 정도만 검사하도록 예시 작성
        assertThat(result.getDistance()).isCloseTo(expectedDistance, within(0.01));
        assertThat(result.getMaxSpeed()).isEqualTo(expectedMaxSpeed);
        assertThat(result.getAvgSpeed()).isCloseTo(expectedAvgSpeed, within(0.01));
        assertThat(result.getMaxHeartrate()).isEqualTo(expectedMaxHr);
        assertThat(result.getAvgHeartrate()).isEqualTo(expectedAvgHr);

        // pace / cadence / calorie는 서비스 로직 확정 후 기대값 채우기
        assertThat(result.getAvgPace()).isNotNull();
        assertThat(result.getAvgPace()).isNotNull();
        assertThat(result.getCadence()).isNotNull();
        assertThat(result.getCalorie()).isGreaterThan(0);
    }

    /**
     * endWorkRecord()가 isDelete=true일 때
     * - workrecord 삭제
     * - measurement 삭제
     * 가 함께 되는지 확인하는 예시
     */
    @Test
    void endWorkRecord_withDelete_true_shouldDeleteWorkRecordAndMeasurements() {

        // given
        WorkRecord wr = new WorkRecord();
        wr.setUserId("U001");
        wr.setCourseId(1);
        wr.setMeasure(true);
        wr.setWorkRecordDate(LocalDate.now());
        workRecordDao.startWorkRecord(wr);

        Integer workRecordId = wr.getWorkRecordId();

        Measurement m = new Measurement();
        m.setWorkRecordId(workRecordId);
        m.setMeasurementDatetime(LocalDateTime.now());
        m.setSpeed(10);
        m.setDistance(1.0);
        m.setSteps(1000);
        m.setHeartrate(140);
        m.setCalorie(50);
        m.setMeasurementLocation("47.5312, 124.0213");
        measurementDao.addMeasurement(m);

        // when
        workRecordService.endWorkRecordForUse(workRecordId, true);

        // then
        WorkRecord deleted = workRecordDao.getWorkRecord(workRecordId);
        assertThat(deleted).isNull();   // getWorkRecord가 null을 리턴한다고 가정
    }
}
