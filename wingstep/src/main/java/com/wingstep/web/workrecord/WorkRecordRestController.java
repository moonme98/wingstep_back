package com.wingstep.web.workrecord;

import com.wingstep.service.domain.workrecord.WorkRecord;
import com.wingstep.service.domain.workrecord.WorkRecordView;
import com.wingstep.service.workrecord.WorkRecordService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workrecord")
public class WorkRecordRestController {

    private final WorkRecordService service;

    public WorkRecordRestController(WorkRecordService service){
        this.service = service;
    }

    /**
     * 운동 기록 시작
     * - WORKRECORD INSERT
     * - 생성된 workRecordId 반환
     */
    @PostMapping
    public int startWorkRecord(@RequestBody WorkRecord wr){
        return service.startWorkRecord(wr);
    }

    /**
     * 운동 기록 상세
     */
    // import org.springframework.http.ResponseEntity;

    @GetMapping("/get/{workRecordId}")
    public ResponseEntity<?> getWorkRecord(@PathVariable int workRecordId) {

        WorkRecord workRecord = service.getWorkRecord(workRecordId);

        if (workRecord == null) {
            // ✅ 운동기록 없으면 404 + 빈 바디
            return ResponseEntity.notFound().build();
        }

        // ✅ 운동기록 있으면 200 + JSON(body)
        System.out.println("운동기록: (" + workRecord + ")");
        return ResponseEntity.ok(workRecord);
    }
    
    /**
     * 운동 기록 상세(View)
     */
    @GetMapping("/view/{workRecordId}")
    public WorkRecordView getWorkRecordView(@PathVariable int workRecordId) {
        return service.getWorkRecordView(workRecordId);
    }
    
    /**
     * 운동 기록(측정중인 기록 조회)
     * - 조건: user_id = ? AND is_measure = true
     */
    @GetMapping("/measure/{userId}")
    public ResponseEntity<WorkRecord> getWorkRecordForMeasure(
            @PathVariable String userId) {
        WorkRecord record = service.getWorkRecordForMeasure(userId);

        if (record == null) {
            return ResponseEntity.noContent().build(); // 204
        }

        return ResponseEntity.ok(record);
    }

    /**
     * 운동 기록 리스트 (사용자별)
     */
    @GetMapping("/list/{userId}")
    public List<WorkRecordView> listWorkRecord(
            @PathVariable String userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String type
    ){
        LocalDate sDate = (startDate == null || startDate.isBlank()) ? null : LocalDate.parse(startDate);
        LocalDate eDate = (endDate == null || endDate.isBlank()) ? null : LocalDate.parse(endDate);
        String searchType = (type == null || type.isBlank()) ? null : type;

        return service.listWorkRecord(userId, sDate, eDate, searchType);
    }
    
    /**
     * 운동 기록 리스트 (코스 후기)
     */
    @GetMapping("/list/{userId}/{courseId}")
    public List<WorkRecord> listWorkRecordByCourse(@PathVariable String userId, @PathVariable int courseId){
        return service.listWorkRecordByCourse(userId, courseId);
    }
    
    /**
     * 운동 기록 삭제 (단독 호출이 필요할 때 사용)
     */
    @DeleteMapping("/delete/{workRecordId}")
    public void deleteWorkRecord(@PathVariable int workRecordId){
        service.deleteWorkRecord(workRecordId);
    }

    /**
     * 운동 기록 통계
     *  - /workrecord/stats/U001?period=week&type=run&battleType=
     *  type: 러닝 / 산책
     *  battleType: 대결 / 개인
     */
    @GetMapping("/stats/{userId}")
    public List<WorkRecord> statsWorkRecord(
            @PathVariable String userId,
            @RequestParam(required=false) String startDate,
            @RequestParam(required=false) String endDate,
            @RequestParam(required=false) String type,
            @RequestParam(required=false) String battleType
    ) {
        return service.statsWorkRecord(userId, startDate, endDate, type, battleType);
    }
    
    /**
     * 운동 기록 통계용 기간 조회
     */
    @GetMapping("/periods/{userId}")
    public Map<String, Object> getPeriods(@PathVariable String userId) {
        return service.getAvailablePeriods(userId);
    }
    
    @GetMapping("/getMeasureWorkRecord")
    public ResponseEntity<Map<String, Object>> getMeasureWorkRecord(
            @RequestParam("courseId") int courseId,
            @RequestParam("userId") String userId) {

        WorkRecord wr = service.getWorkRecordForMeasureByCourse(userId, courseId);

        Map<String, Object> result = new HashMap<>();
        result.put("workRecordId", (wr == null) ? null : wr.getWorkRecordId());

        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/getAddingWorkRecord")
    public ResponseEntity<Map<String, Object>> getAddingWorkRecord(
            @RequestParam("userId") String userId) {

        Integer workRecordId = service.getAddingWorkRecordId(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("workRecordId", workRecordId); // null이면 null로 내려감

        return ResponseEntity.ok(result);
    }

} // end of class
