package com.wingstep.service.workrecord;

import java.util.List;

import com.wingstep.service.domain.workrecord.Measurement;

public interface MeasurementService {

    void addMeasurement(Measurement measurement);

    List<Measurement> listMeasurement(int workRecordId);

    Measurement getLastMeasurement(int workRecordId);

    void deleteAllMeasurement(int workRecordId);
}
