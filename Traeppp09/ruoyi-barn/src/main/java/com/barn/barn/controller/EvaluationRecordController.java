package com.barn.barn.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.barn.barn.entity.EvaluationRecord;
import com.barn.barn.mapper.EvaluationRecordMapper;
import com.barn.common.core.domain.R;
import com.barn.common.core.domain.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/barn/evaluation")
public class EvaluationRecordController {

    @Autowired
    private EvaluationRecordMapper evaluationRecordMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 分页列表 - JOIN users + ovens 表，返回包含烟农姓名/手机号、烤房名称/县区/乡镇的完整数据
     */
    @GetMapping("/list")
    public TableDataInfo<Map<String, Object>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize,
                                                @RequestParam(required = false) String ovenId,
                                                @RequestParam(required = false) String userId,
                                                @RequestParam(required = false) String evaluatorPhone,
                                                @RequestParam(required = false) String county,
                                                @RequestParam(required = false) Integer minRating,
                                                @RequestParam(required = false) Integer maxRating,
                                                @RequestParam(required = false) String startDate,
                                                @RequestParam(required = false) String endDate) {
        StringBuilder where = new StringBuilder("WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (ovenId != null && !ovenId.isEmpty()) {
            where.append(" AND e.oven_id LIKE ?");
            params.add("%" + ovenId + "%");
        }
        if (userId != null && !userId.isEmpty()) {
            where.append(" AND e.user_id = ?");
            params.add(userId);
        }
        if (evaluatorPhone != null && !evaluatorPhone.isEmpty()) {
            where.append(" AND u.phone LIKE ?");
            params.add("%" + evaluatorPhone + "%");
        }
        if (county != null && !county.isEmpty()) {
            where.append(" AND o.county = ?");
            params.add(county);
        }
        if (minRating != null) {
            where.append(" AND e.overall_rating >= ?");
            params.add(minRating);
        }
        if (maxRating != null) {
            where.append(" AND e.overall_rating <= ?");
            params.add(maxRating);
        }
        if (startDate != null && !startDate.isEmpty()) {
            where.append(" AND e.evaluate_time >= ?");
            params.add(startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            where.append(" AND e.evaluate_time <= ?");
            params.add(endDate + " 23:59:59");
        }

        // 查询总数 - 需要JOIN用户和烤房表以支持按手机号/烤房编号搜索
        String countSql = "SELECT COUNT(*) as total FROM evaluations e " +
                "LEFT JOIN users u ON e.user_id = u.id " +
                "LEFT JOIN ovens o ON e.oven_id = o.id " + where.toString();
        Map<String, Object> countResult = jdbcTemplate.queryForMap(countSql, params.toArray());
        long total = ((Number) countResult.get("total")).longValue();

        // 分页查询 - LEFT JOIN users（烟农）+ ovens（烤房）
        // 增加 assignment_id 字段用于判断评价来源（非空=小程序提交）
        int offset = (pageNum - 1) * pageSize;
        String listSql = "SELECT e.id, e.user_id, e.oven_id, e.overall_rating, e.baker_rating, " +
                "e.temp_control_rating, e.equipment_rating, e.oven_tags, e.comment, e.baker_comment, " +
                "e.images, e.evaluate_time, e.evaluator_name, e.baker_name, e.oven_name, " +
                "e.assignment_id, e.reservation_id, e.baking_record_id, " +
                "u.name as user_name, u.phone as evaluator_phone, " +
                "o.barn_name as oven_barn_name, o.county, o.township " +
                "FROM evaluations e " +
                "LEFT JOIN users u ON e.user_id = u.id " +
                "LEFT JOIN ovens o ON e.oven_id = o.id " +
                where.toString() +
                " ORDER BY e.evaluate_time DESC, e.id DESC LIMIT ? OFFSET ?";
        params.add(pageSize);
        params.add(offset);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(listSql, params.toArray());

        // 格式化返回数据，关联烟农与烤房信息
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", row.get("id"));
            m.put("userId", row.get("user_id"));
            m.put("ovenId", row.get("oven_id"));
            m.put("overallRating", row.get("overall_rating"));
            m.put("bakerRating", row.get("baker_rating"));
            m.put("tempControlRating", row.get("temp_control_rating"));
            m.put("equipmentRating", row.get("equipment_rating"));
            m.put("ovenTags", row.get("oven_tags"));
            m.put("comment", row.get("comment"));
            m.put("bakerComment", row.get("baker_comment"));
            m.put("images", row.get("images"));
            m.put("evaluateTime", row.get("evaluate_time"));
            // 评价人姓名：优先取 evaluations.evaluator_name，为空时回退 users.name
            m.put("evaluatorName", row.get("evaluator_name") != null ? row.get("evaluator_name") : row.get("user_name"));
            m.put("bakerName", row.get("baker_name"));
            // 烤房名称：优先取 evaluations.oven_name，为空时回退 ovens.barn_name
            m.put("ovenName", row.get("oven_name") != null ? row.get("oven_name") : row.get("oven_barn_name"));
            m.put("evaluatorPhone", row.get("evaluator_phone"));
            m.put("county", row.get("county"));
            m.put("township", row.get("township"));
            // 评价来源：assignment_id非空=小程序评价，否则=Web端评价
            boolean fromMiniApp = row.get("assignment_id") != null && !String.valueOf(row.get("assignment_id")).isEmpty();
            m.put("source", fromMiniApp ? "小程序" : "Web端");
            result.add(m);
        }
        return TableDataInfo.build(result, total, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    public R<EvaluationRecord> getInfo(@PathVariable Long id) {
        return R.ok(evaluationRecordMapper.selectById(id));
    }

    @PostMapping
    public R<Void> add(@RequestBody EvaluationRecord record) {
        evaluationRecordMapper.insert(record);
        return R.ok();
    }

    @PutMapping
    public R<Void> edit(@RequestBody EvaluationRecord record) {
        evaluationRecordMapper.updateById(record);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        evaluationRecordMapper.deleteById(id);
        return R.ok();
    }

    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        List<EvaluationRecord> all = evaluationRecordMapper.selectList(null);
        Map<String, Object> result = new HashMap<>();
        double totalScore = 0;
        Map<Integer, Integer> distribution = new HashMap<>();
        for (EvaluationRecord e : all) {
            if (e.getEquipmentRating() != null) {
                totalScore += e.getEquipmentRating();
                int star = e.getEquipmentRating();
                distribution.merge(star, 1, Integer::sum);
            }
        }
        result.put("total", all.size());
        result.put("avgScore", all.isEmpty() ? 0 : totalScore / all.size());
        result.put("distribution", distribution);
        return R.ok(result);
    }
}
