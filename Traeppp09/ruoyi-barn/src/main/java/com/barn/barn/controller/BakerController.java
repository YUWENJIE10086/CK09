package com.barn.barn.controller;

import com.barn.common.core.domain.R;
import com.barn.common.core.domain.TableDataInfo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 烘烤师管理Controller
 * 直接读取已有bakers表
 */
@RestController
@RequestMapping("/baker")
@PreAuthorize("hasRole('ADMIN')")
public class BakerController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 烘烤师统计信息
     */
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bakers", Integer.class);
            Integer active = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bakers WHERE status = 1", Integer.class);
            Integer senior = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bakers WHERE experience >= 10", Integer.class);
            Integer mid = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bakers WHERE experience >= 5 AND experience < 10", Integer.class);
            Integer junior = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bakers WHERE experience < 5", Integer.class);

            result.put("total", total != null ? total : 0);
            result.put("active", active != null ? active : 0);
            result.put("senior", senior != null ? senior : 0);
            result.put("mid", mid != null ? mid : 0);
            result.put("junior", junior != null ? junior : 0);
        } catch (Exception e) {
            result.put("total", 0);
            result.put("active", 0);
            result.put("senior", 0);
            result.put("mid", 0);
            result.put("junior", 0);
        }
        return R.ok(result);
    }

    /**
     * 烘烤师分页列表（JOIN pound_groups表显示磅组名称）
     */
    @GetMapping("/list")
    public TableDataInfo<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer status) {

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (name != null && !name.trim().isEmpty()) {
            where.append(" AND b.name LIKE ? ");
            params.add("%" + name + "%");
        }
        if (phone != null && !phone.trim().isEmpty()) {
            where.append(" AND b.phone LIKE ? ");
            params.add("%" + phone + "%");
        }
        if (status != null) {
            where.append(" AND b.status = ? ");
            params.add(status);
        }

        // 查询总数
        String countSql = "SELECT COUNT(*) FROM bakers b" + where;
        Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());
        if (total == null) total = 0;

        // 分页查询 - JOIN pound_groups 获取磅组名称
        int offset = (pageNum - 1) * pageSize;
        String listSql = "SELECT b.*, pg.name AS pound_group_name " +
                "FROM bakers b " +
                "LEFT JOIN pound_groups pg ON b.pound_group_id = pg.id" +
                where + " ORDER BY b.created_at DESC LIMIT ? OFFSET ?";
        params.add(pageSize);
        params.add(offset);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(listSql, params.toArray());

        // 转换字段名为驼峰
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", row.get("id"));
            m.put("name", row.get("name"));
            m.put("phone", row.get("phone"));
            m.put("experience", row.get("experience"));
            m.put("rating", row.get("rating"));
            m.put("totalOrders", row.get("total_orders"));
            m.put("poundGroupId", row.get("pound_group_id"));
            m.put("poundGroupName", row.get("pound_group_name"));
            m.put("status", row.get("status"));
            m.put("createdAt", row.get("created_at"));
            m.put("updatedAt", row.get("updated_at"));
            list.add(m);
        }

        return TableDataInfo.build(list, total, pageNum, pageSize);
    }

    /**
     * 获取所有在岗烘烤师（下拉选项用）
     */
    @GetMapping("/options")
    public R<List<Map<String, Object>>> options() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, name, phone FROM bakers WHERE status = 1 ORDER BY name");
        return R.ok(rows);
    }

    /**
     * 获取所有磅组（下拉选项用）
     */
    @GetMapping("/poundGroups")
    public R<List<Map<String, Object>>> poundGroups() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, name FROM pound_groups WHERE status = 1 ORDER BY sort_order");
        return R.ok(rows);
    }

    /**
     * 新增烘烤师
     */
    @PostMapping
    public R<String> add(@RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.getOrDefault("name", "");
            if (name == null || name.trim().isEmpty()) {
                return R.fail("姓名不能为空");
            }

            // 生成ID（如果未提供）
            String id = (String) body.get("id");
            if (id == null || id.trim().isEmpty()) {
                // 查找最大ID并+1
                Integer maxNum = 0;
                try {
                    String maxId = jdbcTemplate.queryForObject(
                        "SELECT id FROM bakers ORDER BY CAST(SUBSTRING(id, 3) AS UNSIGNED) DESC LIMIT 1", String.class);
                    if (maxId != null && maxId.length() > 2) {
                        maxNum = Integer.parseInt(maxId.substring(2));
                    }
                } catch (Exception e) {
                    // 忽略
                }
                id = "BK" + String.format("%03d", maxNum + 1);
            }

            jdbcTemplate.update(
                "INSERT INTO bakers (id, name, phone, experience, rating, total_orders, pound_group_id, status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                id,
                name,
                body.getOrDefault("phone", ""),
                body.getOrDefault("experience", 0),
                body.getOrDefault("rating", 5.0),
                body.getOrDefault("totalOrders", 0),
                body.get("poundGroupId"),
                body.getOrDefault("status", 1)
            );
            return R.ok("添加成功");
        } catch (Exception e) {
            return R.fail("添加失败: " + e.getMessage());
        }
    }

    /**
     * 更新烘烤师
     */
    @PutMapping
    public R<String> update(@RequestBody Map<String, Object> body) {
        try {
            Object id = body.get("id");
            if (id == null) {
                return R.fail("ID不能为空");
            }

            jdbcTemplate.update(
                "UPDATE bakers SET name=?, phone=?, experience=?, rating=?, total_orders=?, pound_group_id=?, status=?, updated_at=NOW() " +
                "WHERE id=?",
                body.getOrDefault("name", ""),
                body.getOrDefault("phone", ""),
                body.getOrDefault("experience", 0),
                body.getOrDefault("rating", 5.0),
                body.getOrDefault("totalOrders", 0),
                body.get("poundGroupId"),
                body.getOrDefault("status", 1),
                id
            );
            return R.ok("更新成功");
        } catch (Exception e) {
            return R.fail("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除烘烤师
     */
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable String id) {
        try {
            int rows = jdbcTemplate.update("DELETE FROM bakers WHERE id=?", id);
            if (rows > 0) {
                return R.ok("删除成功");
            } else {
                return R.fail("删除失败，烘烤师不存在");
            }
        } catch (Exception e) {
            return R.fail("删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除烘烤师
     */
    @DeleteMapping("/batch")
    public R<String> batchDelete(@RequestBody Map<String, List<String>> request) {
        try {
            List<String> ids = request.get("ids");
            if (ids == null || ids.isEmpty()) {
                return R.fail("请选择要删除的记录");
            }
            int count = 0;
            for (String id : ids) {
                count += jdbcTemplate.update("DELETE FROM bakers WHERE id=?", id);
            }
            return R.ok("成功删除" + count + "条记录");
        } catch (Exception e) {
            return R.fail("批量删除失败: " + e.getMessage());
        }
    }
}
