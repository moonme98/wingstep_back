
package com.wingstep.web.workrecord;

import com.wingstep.service.domain.workrecord.Measurement;
import com.wingstep.service.workrecord.MeasurementService;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/measurement")
public class MeasurementRestController {

    private final MeasurementService service;

    public MeasurementRestController(MeasurementService service){
        this.service = service;
    }

    @PostMapping
    public void addMeasurement(@RequestBody Measurement m){ service.addMeasurement(m); }

    @GetMapping("/list/{workRecordId}")
    public List<Measurement> listMeasurement(@PathVariable int workRecordId){
        return service.listMeasurement(workRecordId);
    }

    // 마지막 측정값 조회 (workRecord 기준)
    @GetMapping("/last/{workRecordId}")
    public Measurement getLastMeasurement(@PathVariable int workRecordId){
        return service.getLastMeasurement(workRecordId);
    }

    // 해당 workRecord 의 측정값 전체 삭제
    @DeleteMapping("/delete/{workRecordId}")
    public void deleteAllMeasurement(@PathVariable int workRecordId){
        service.deleteAllMeasurement(workRecordId);
    }
}
