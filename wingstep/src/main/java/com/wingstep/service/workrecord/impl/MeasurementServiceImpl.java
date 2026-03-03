
package com.wingstep.service.workrecord.impl;

import com.wingstep.service.workrecord.MeasurementService;
import com.wingstep.service.domain.workrecord.Measurement;
import com.wingstep.service.workrecord.MeasurementDao;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class MeasurementServiceImpl implements MeasurementService {

    private final MeasurementDao dao;

    public MeasurementServiceImpl(MeasurementDao dao) {
        this.dao = dao;
    }

    @Override
    public void addMeasurement(Measurement measurement) {
        dao.addMeasurement(measurement);
    }

    @Override
    public List<Measurement> listMeasurement(int workRecordId) {
        return dao.listMeasurement(workRecordId);
    }

    @Override
    public Measurement getLastMeasurement(int workRecordId) {
        return dao.getLastMeasurement(workRecordId);
    }

    @Override
    public void deleteAllMeasurement(int workRecordId) {
        dao.deleteAllMeasurement(workRecordId);
    }
}

