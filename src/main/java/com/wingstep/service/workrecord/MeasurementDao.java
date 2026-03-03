package com.wingstep.service.workrecord;

import org.apache.ibatis.annotations.Mapper;

import com.wingstep.service.domain.workrecord.Measurement;

import java.util.List;

@Mapper
public interface MeasurementDao {

    void addMeasurement(Measurement measurement);

    List<Measurement> listMeasurement(int workRecordId);

    Measurement getLastMeasurement(int workRecordId);

    void deleteAllMeasurement(int workRecordId);
}

