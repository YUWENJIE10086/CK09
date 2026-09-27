package com.barn.barn.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.barn.barn.entity.BarnAssignment;
import com.barn.barn.entity.Farmer;
import com.barn.barn.mapper.BarnAssignmentMapper;
import com.barn.barn.mapper.FarmerMapper;
import com.barn.common.core.domain.R;
import com.barn.common.core.domain.TableDataInfo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 烤房分配管理
 */
@RestController
@RequestMapping("/barn/assignment")
@PreAuthorize("hasRole('ADMIN')")
public class BarnAssignmentController {

    @Autowired
    private BarnAssignmentMapper barnAssignmentMapper;

    @Autowired
    private FarmerMapper farmerMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 分页列表，支持 ovenId/farmerName/assignStatus 筛选
     * 返回时把数据库enum状态映射成中文，前端直接显示
     */
    @GetMapping("/list")
    public TableDataInfo<Map<String, Object>> list(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @RequestParam(required = false) String ovenId,
                                              @RequestParam(required = false) String farmerName,
                                              @RequestParam(required = false) String assignStatus) {
        // 查询前先自动归还到期的分配（和手动归还完全一致）
        autoReleaseExpired();
        
        Page<BarnAssignment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BarnAssignment> wrapper = new LambdaQueryWrapper<>();
        if (ovenId != null) {
            wrapper.eq(BarnAssignment::getOvenId, ovenId);
        }
        if (farmerName != null && !farmerName.isEmpty()) {
            wrapper.like(BarnAssignment::getFarmerName, farmerName);
        }
        if (assignStatus != null && !assignStatus.isEmpty()) {
            // 已归还→completed，已撤销→cancelled（不再混用cancelled+return_date区分）
            String dbStatus = mapChineseToDbStatus(assignStatus);
            wrapper.eq(BarnAssignment::getStatus, dbStatus);
        }
        wrapper.orderByDesc(BarnAssignment::getAssignedAt)
               .orderByDesc(BarnAssignment::getCreatedAt);
        IPage<BarnAssignment> result = barnAssignmentMapper.selectPage(page, wrapper);
        
        // 转换成前端需要的格式（状态中文、字段对齐）
        // 批量查询已评价的分配ID，避免N+1查询
        List<String> assignmentIds = new ArrayList<>();
        for (BarnAssignment a : result.getRecords()) {
            assignmentIds.add(a.getId());
        }
        Map<String, Boolean> evaluatedMap = new HashMap<>();
        if (!assignmentIds.isEmpty()) {
            String placeholders = String.join(",", assignmentIds.stream().map(id -> "?").toArray(String[]::new));
            List<Map<String, Object>> evalRows = jdbcTemplate.queryForList(
                "SELECT DISTINCT assignment_id FROM evaluations WHERE assignment_id IN (" + placeholders + ") AND assignment_id IS NOT NULL",
                assignmentIds.toArray()
            );
            for (Map<String, Object> r : evalRows) {
                evaluatedMap.put(String.valueOf(r.get("assignment_id")), true);
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (BarnAssignment a : result.getRecords()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("ovenId", a.getOvenId());
            m.put("barnName", a.getOvenName());
            m.put("farmerId", a.getUserId());
            m.put("farmerName", a.getFarmerName());
            m.put("farmerPhone", a.getFarmerPhone());
            m.put("status", mapDbStatusToChinese(a));
            m.put("assignDate", formatDateTime(a.getAssignedAt()));
            m.put("startTime", formatDateTime(a.getStartTime()));
            m.put("endTime", formatDateTime(a.getEndTime()));
            m.put("returnDate", a.getReturnDate());
            m.put("remark", a.getRemark());
            m.put("seasonYear", a.getSeasonYear());
            m.put("seasonName", a.getSeasonName());
            m.put("createdAt", formatDateTime(a.getCreatedAt()));
            // 是否已评价
            m.put("evaluated", evaluatedMap.containsKey(a.getId()));
            rows.add(m);
        }
        return TableDataInfo.build(rows, result.getTotal(), pageNum, pageSize);
    }

    /** 安全格式化日期时间 */
    private String formatDateTime(java.time.LocalDateTime dt) {
        if (dt == null) return null;
        String s = dt.toString().replace("T", " ");
        return s.length() >= 19 ? s.substring(0, 19) : s;
    }
    
    /** 数据库enum状态 -> 中文
     * completed=已归还（小程序可见、可评价）
     * cancelled=已撤销（小程序不显示）
     * 兼容历史数据：cancelled+return_date 视为已归还
     */
    private String mapDbStatusToChinese(BarnAssignment a) {
        if (a == null || a.getStatus() == null) return "未知";
        switch (a.getStatus()) {
            case "assigned": return "已分配";
            case "active": return "使用中";
            case "completed": return "已归还";
            case "cancelled":
                // 兼容历史数据：有归还日期的旧记录也显示为"已归还"
                return (a.getReturnDate() != null && !a.getReturnDate().isEmpty()) ? "已归还" : "已撤销";
            default: return a.getStatus();
        }
    }
    
    /** 中文状态 -> 数据库enum */
    private String mapChineseToDbStatus(String chinese) {
        if (chinese == null) return "assigned";
        switch (chinese) {
            case "已分配": return "assigned";
            case "使用中": return "active";
            case "已归还": return "completed";
            case "已撤销": return "cancelled";
            default: return "assigned";
        }
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public R<BarnAssignment> getInfo(@PathVariable String id) {
        return R.ok(barnAssignmentMapper.selectById(id));
    }

    /**
     * 新增分配（分配烤房给烟农）
     * 如果同一烟农+烤房+季度已有记录（含已归还/已撤销的），直接更新为"已分配"
     * 避免唯一键冲突 uk_user_oven_season (user_id, oven_id, season_year)
     */
    @PostMapping
    public R<Map<String, Object>> add(@RequestBody Map<String, Object> body) {
        String farmerId = String.valueOf(body.get("farmerId"));
        String ovenId = String.valueOf(body.get("ovenId"));
        int year = java.time.LocalDate.now().getYear();
        String seasonName = year + "年度烘烤季";

        // 解析时间段参数（格式如 "2026-07-23 08:00:00"）
        LocalDateTime startTime = parseDateTime(String.valueOf(body.get("startTime")));
        LocalDateTime endTime = parseDateTime(String.valueOf(body.get("endTime")));

        // 根据当前时间判断状态：在时间段内为active（使用中），未到开始时间为assigned（已分配）
        LocalDateTime now = LocalDateTime.now();
        String dbStatus;
        if (startTime != null && now.isBefore(startTime)) {
            dbStatus = "assigned"; // 还没到开始时间
        } else {
            dbStatus = "active"; // 已在时间段内
        }

        // 查找是否已有同一烟农+烤房+季度的记录
        LambdaQueryWrapper<BarnAssignment> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(BarnAssignment::getUserId, farmerId)
                    .eq(BarnAssignment::getOvenId, ovenId)
                    .eq(BarnAssignment::getSeasonYear, year);
        BarnAssignment existing = barnAssignmentMapper.selectOne(existWrapper);

        String assignedBy = body.get("assignedBy") != null ? String.valueOf(body.get("assignedBy")) : "Web端";
        String assignmentId;
        if (existing != null) {
            // 已有记录，直接更新状态
            assignmentId = existing.getId();
            existing.setStatus(dbStatus);
            existing.setFarmerName(String.valueOf(body.get("farmerName")));
            existing.setFarmerPhone(String.valueOf(body.get("farmerPhone")));
            existing.setAssignedBy(assignedBy);
            existing.setAssignedAt(LocalDateTime.now());
            existing.setStartTime(startTime);
            existing.setEndTime(endTime);
            existing.setReturnDate(null);  // 清空归还日期
            existing.setRemark(String.valueOf(body.getOrDefault("remark", "Web端重新分配")));
            existing.setUpdatedAt(LocalDateTime.now());
            barnAssignmentMapper.updateById(existing);

            // 更新ovens表状态：已在时间段内设为"在用"，否则保持闲置等待到期开始
            if ("active".equals(dbStatus)) {
                jdbcTemplate.update("UPDATE ovens SET status='baking', baker_id=?, updated_at=NOW() WHERE id=?", farmerId, ovenId);
            } else {
                jdbcTemplate.update("UPDATE ovens SET status='idle', baker_id=?, updated_at=NOW() WHERE id=?", farmerId, ovenId);
            }
        } else {
            // 新建记录
            assignmentId = "AS" + System.currentTimeMillis();
            BarnAssignment assignment = new BarnAssignment();
            assignment.setId(assignmentId);
            assignment.setOvenId(ovenId);
            assignment.setOvenName(String.valueOf(body.get("barnName")));
            assignment.setUserId(farmerId);
            assignment.setFarmerName(String.valueOf(body.get("farmerName")));
            assignment.setFarmerPhone(String.valueOf(body.get("farmerPhone")));
            assignment.setStatus(dbStatus);
            assignment.setAssignedBy(assignedBy);
            assignment.setSeasonYear(year);
            assignment.setSeasonName(seasonName);
            assignment.setAssignedAt(LocalDateTime.now());
            assignment.setStartTime(startTime);
            assignment.setEndTime(endTime);
            assignment.setCreatedAt(LocalDateTime.now());
            assignment.setRemark(String.valueOf(body.getOrDefault("remark", "Web端分配")));
            barnAssignmentMapper.insert(assignment);

            // 更新ovens表状态：已在时间段内设为"在用"，否则保持闲置等待到期开始
            if ("active".equals(dbStatus)) {
                jdbcTemplate.update("UPDATE ovens SET status='baking', baker_id=?, updated_at=NOW() WHERE id=?", farmerId, ovenId);
            } else {
                jdbcTemplate.update("UPDATE ovens SET status='idle', baker_id=?, updated_at=NOW() WHERE id=?", farmerId, ovenId);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", assignmentId);
        result.put("status", "active".equals(dbStatus) ? "使用中" : "已分配");
        result.put("msg", "分配成功");
        return R.ok(result, "烤房分配成功");
    }

    /** 解析日期时间字符串（格式如 "2026-07-23 08:00:00" 或 "2026-07-23T08:00:00"） */
    private LocalDateTime parseDateTime(String str) {
        if (str == null || "null".equals(str) || str.isEmpty()) return null;
        try {
            String s = str.replace("T", " ");
            return LocalDateTime.parse(s, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(str.replace("T", " ").substring(0, 19));
            } catch (Exception e2) {
                return null;
            }
        }
    }

    /** 自动释放到期分配（到点自动归还）
     * 归还使用 status='completed' + return_date + ovens.baker_id=NULL
     * 小程序查询用 a.status <> 'cancelled' 过滤，completed 状态小程序仍然可见（显示为"可评价"）
     * 真正的"已撤销"才使用 status='cancelled'，小程序不显示
     */
    @PostMapping("/auto-release")
    public R<String> autoRelease() {
        int count = autoReleaseExpired();
        return R.ok("自动归还了" + count + "条到期分配");
    }

    /** 内部方法：自动归还到期的分配记录
     * 到期的分配标记为 completed（已归还），小程序端仍可见且可评价
     */
    private int autoReleaseExpired() {
        try {
            // 1. 将所有end_time已过期且status为active/assigned的记录，更新为completed+return_date
            int updated = jdbcTemplate.update(
                "UPDATE oven_assignments SET status='completed', return_date=DATE_FORMAT(NOW(),'%Y-%m-%d %H:%i:%s'), updated_at=NOW() " +
                "WHERE end_time IS NOT NULL AND end_time < NOW() AND status IN ('assigned','active')"
            );
            // 2. 将对应烤房状态恢复为idle，清空baker_id
            if (updated > 0) {
                jdbcTemplate.update(
                    "UPDATE ovens o INNER JOIN oven_assignments a ON o.id = a.oven_id " +
                    "SET o.status='idle', o.baker_id=NULL, o.updated_at=NOW() " +
                    "WHERE a.status='completed' AND a.end_time < NOW() AND a.return_date IS NOT NULL"
                );
            }
            return updated;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 修改分配（更新状态：使用中/已归还/已撤销）
     * 前端传中文状态，转成enum存库
     * 同时更新ovens表的使用状态（在用/闲置）
     */
    @PutMapping
    public R<Void> edit(@RequestBody Map<String, Object> body) {
        BarnAssignment assignment = new BarnAssignment();
        assignment.setId(String.valueOf(body.get("id")));
        String ovenId = "";
        if (body.containsKey("status")) {
            String chineseStatus = String.valueOf(body.get("status"));
            String dbStatus;
            // 已归还→completed（小程序可见、可评价）；已撤销→cancelled（小程序不显示）
            switch (chineseStatus) {
                case "使用中":
                    dbStatus = "active";
                    // 更新ovens表状态为"在用"
                    ovenId = getOvenIdFromAssignment(String.valueOf(body.get("id")));
                    if (ovenId != null && !ovenId.isEmpty()) {
                        jdbcTemplate.update("UPDATE ovens SET status='baking', updated_at=NOW() WHERE id=?", ovenId);
                    }
                    break;
                case "已归还":
                    dbStatus = "completed";
                    // 更新ovens表状态为"闲置"，清空baker_id
                    ovenId = getOvenIdFromAssignment(String.valueOf(body.get("id")));
                    if (ovenId != null && !ovenId.isEmpty()) {
                        jdbcTemplate.update("UPDATE ovens SET status='idle', baker_id=NULL, updated_at=NOW() WHERE id=?", ovenId);
                    }
                    // 已归还时自动设置return_date（如果前端没传）
                    if (!body.containsKey("returnDate")) {
                        assignment.setReturnDate(java.time.LocalDate.now().toString());
                    }
                    break;
                case "已撤销":
                    dbStatus = "cancelled";
                    // 更新ovens表状态为"闲置"，清空baker_id
                    ovenId = getOvenIdFromAssignment(String.valueOf(body.get("id")));
                    if (ovenId != null && !ovenId.isEmpty()) {
                        jdbcTemplate.update("UPDATE ovens SET status='idle', baker_id=NULL, updated_at=NOW() WHERE id=?", ovenId);
                    }
                    // 清空 return_date，避免cancelled+return_date被误判为已归还
                    jdbcTemplate.update("UPDATE oven_assignments SET return_date=NULL WHERE id=?", String.valueOf(body.get("id")));
                    break;
                default: dbStatus = "assigned";
            }
            assignment.setStatus(dbStatus);
        }
        if (body.containsKey("returnDate")) {
            assignment.setReturnDate(String.valueOf(body.get("returnDate")));
        }
        if (body.containsKey("remark")) {
            assignment.setRemark(String.valueOf(body.get("remark")));
        }
        assignment.setUpdatedAt(LocalDateTime.now());
        barnAssignmentMapper.updateById(assignment);
        return R.ok();
    }

    /**
     * 从分配记录获取oven_id
     */
    private String getOvenIdFromAssignment(String assignmentId) {
        try {
            BarnAssignment assignment = barnAssignmentMapper.selectById(assignmentId);
            return assignment != null ? assignment.getOvenId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 删除分配记录
     */
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable String id) {
        barnAssignmentMapper.deleteById(id);
        return R.ok();
    }

    /**
     * 查询某烤房的所有分配记录
     */
    @GetMapping("/by-oven/{ovenId}")
    public R<List<BarnAssignment>> listByBarn(@PathVariable String ovenId) {
        LambdaQueryWrapper<BarnAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BarnAssignment::getOvenId, ovenId)
               .orderByDesc(BarnAssignment::getAssignedAt)
               .orderByDesc(BarnAssignment::getCreatedAt);
        return R.ok(barnAssignmentMapper.selectList(wrapper));
    }

    /**
     * 搜索烟农（根据姓名/手机号模糊查询 users 表，即烟农管理里的数据）
     * 返回 farmerId, farmerName, farmerPhone, creditLevel, area
     */
    @GetMapping("/search-farmer")
    public R<List<Map<String, Object>>> searchFarmer(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Farmer> wrapper = new LambdaQueryWrapper<>();
        // 仅查启用状态的烟农
        wrapper.eq(Farmer::getStatus, 1);
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Farmer::getName, kw)
                    .or().like(Farmer::getPhone, kw));
        }
        wrapper.orderByDesc(Farmer::getCreatedAt);
        wrapper.last("LIMIT 20");
        List<Farmer> farmers = farmerMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Farmer f : farmers) {
            Map<String, Object> m = new HashMap<>();
            m.put("farmerId", f.getId());
            m.put("farmerName", f.getName());
            m.put("farmerPhone", f.getPhone());
            m.put("creditLevel", f.getCreditLevel());
            m.put("creditScore", f.getCreditScore());
            m.put("area", f.getArea());
            m.put("plantingArea", f.getPlantingArea());
            m.put("plantingYears", f.getPlantingYears());
            m.put("totalBakes", f.getTotalBakes());
            result.add(m);
        }
        return R.ok(result);
    }

    /**
     * 统计（总分配数/使用中/已归还/已撤销）
     */
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        List<BarnAssignment> all = barnAssignmentMapper.selectList(null);
        Map<String, Object> result = new HashMap<>();
        long inUse = 0;
        long returned = 0;
        long revoked = 0;
        for (BarnAssignment a : all) {
            String s = a.getStatus();
            if ("active".equals(s)) {
                inUse++;
            } else if ("assigned".equals(s)) {
                inUse++; // 已分配也算在使用中
            } else if ("completed".equals(s)) {
                returned++;
            } else if ("cancelled".equals(s)) {
                // 兼容历史数据：有归还日期的旧cancelled记录也算已归还
                if (a.getReturnDate() != null && !a.getReturnDate().isEmpty()) {
                    returned++;
                } else {
                    revoked++;
                }
            }
        }
        result.put("total", all.size());
        result.put("inUse", inUse);
        result.put("returned", returned);
        result.put("revoked", revoked);
        return R.ok(result);
    }
}
