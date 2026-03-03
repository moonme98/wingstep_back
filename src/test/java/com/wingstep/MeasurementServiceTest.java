package com.wingstep;

import com.wingstep.service.domain.workrecord.Measurement;
import com.wingstep.service.workrecord.MeasurementDao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback
class MeasurementDaoTest {

    @Autowired
    private MeasurementDao measurementDao;

    @Test
    void addAndListByWorkRecordId() {
        int workRecordId = 9999; // 테스트용 workrecord_id (테스트 DB에서만 사용)

        Measurement m1 = new Measurement();
        m1.setWorkRecordId(workRecordId);
        m1.setMeasurementDatetime(LocalDateTime.now().minusMinutes(5));
        m1.setSpeed(9);
        m1.setDistance(1.0);
        m1.setSteps(900);
        m1.setHeartrate(140);
        m1.setCalorie(40);
        m1.setMeasurementLocation("11.1111, 121.1111"); // 위도, 경도
        measurementDao.addMeasurement(m1);

        Measurement m2 = new Measurement();
        m2.setWorkRecordId(workRecordId);
        m2.setMeasurementDatetime(LocalDateTime.now());
        m2.setSpeed(11);
        m2.setDistance(2.0);
        m2.setSteps(1100);
        m2.setHeartrate(150);
        m2.setCalorie(60);
        m2.setMeasurementLocation("22.2222, 122.2222");
        measurementDao.addMeasurement(m2);

        List<Measurement> list = measurementDao.listMeasurement(workRecordId);
        assertThat(list).hasSize(2);
        assertThat(list)
                .extracting(Measurement::getSpeed)
                .containsExactly(9d, 11d);
    }

    @Test
    void deleteByWorkRecordId() {
        int workRecordId = 8888;

        Measurement m = new Measurement();
        m.setWorkRecordId(workRecordId);
        m.setMeasurementDatetime(LocalDateTime.now());
        m.setSpeed(10);
        m.setDistance(1.5);
        m.setSteps(1000);
        m.setHeartrate(145);
        m.setCalorie(50);
        m.setMeasurementLocation("47.5312, 124.0213");
        measurementDao.addMeasurement(m);

        List<Measurement> list = measurementDao.listMeasurement(workRecordId);
        assertThat(list).isEmpty();
    }
}
