package com.barn.barn.controller;

import com.barn.common.core.domain.R;
import com.barn.common.core.domain.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部件报价Controller
 * 数据来源：component_quotes 表
 * 使用 JdbcTemplate 直接查询，不依赖 MyBatis
 */
@RestController
@RequestMapping("/barn/component-quote")
public class ComponentQuoteController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 分页查询部件报价列表，支持按项目名称模糊搜索
     */
    @GetMapping("/list")
    public TableDataInfo<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String itemName) {

        StringBuilder where = new StringBuilder("WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (itemName != null && !itemName.trim().isEmpty()) {
            where.append(" AND item_name LIKE ?");
            params.add("%" + itemName.trim() + "%");
        }

        // 查询总数
        String countSql = "SELECT COUNT(*) as total FROM component_quotes " + where;
        Map<String, Object> countResult = jdbcTemplate.queryForMap(countSql, params.toArray());
        long total = ((Number) countResult.get("total")).longValue();

        // 分页查询
        int offset = (pageNum - 1) * pageSize;
        String listSql = "SELECT * FROM component_quotes " + where +
                " ORDER BY id ASC LIMIT ? OFFSET ?";
        params.add(pageSize);
        params.add(offset);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(listSql, params.toArray());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(convertToCamelCase(row));
        }

        return TableDataInfo.build(result, total, pageNum, pageSize);
    }

    /**
     * 获取部件报价详情
     */
    @GetMapping("/{id}")
    public R<Map<String, Object>> getInfo(@PathVariable Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM component_quotes WHERE id = ?", id);
        if (rows.isEmpty()) {
            return R.fail("报价记录不存在: " + id);
        }
        return R.ok(convertToCamelCase(rows.get(0)));
    }

    /**
     * 新增部件报价
     */
    @PostMapping
    public R<Void> add(@RequestBody Map<String, Object> body) {
        String itemName = getStr(body, "itemName");
        String specModel = getStr(body, "specModel");
        String unit = getStr(body, "unit");
        BigDecimal unitPrice = getDecimal(body, "unitPrice");
        String remark = getStr(body, "remark");

        jdbcTemplate.update(
                "INSERT INTO component_quotes (item_name, spec_model, unit, unit_price, remark, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                itemName, specModel, unit, unitPrice, remark);
        return R.ok();
    }

    /**
     * 修改部件报价
     */
    @PutMapping
    public R<Void> edit(@RequestBody Map<String, Object> body) {
        Object idObj = body.get("id");
        if (idObj == null) {
            return R.fail("修改时必须提供id");
        }
        Long id = Long.parseLong(idObj.toString());

        String itemName = getStr(body, "itemName");
        String specModel = getStr(body, "specModel");
        String unit = getStr(body, "unit");
        BigDecimal unitPrice = getDecimal(body, "unitPrice");
        String remark = getStr(body, "remark");

        jdbcTemplate.update(
                "UPDATE component_quotes SET item_name=?, spec_model=?, unit=?, unit_price=?, remark=?, updated_at=NOW() WHERE id=?",
                itemName, specModel, unit, unitPrice, remark, id);
        return R.ok();
    }

    /**
     * 删除部件报价
     */
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        jdbcTemplate.update("DELETE FROM component_quotes WHERE id = ?", id);
        return R.ok();
    }

    // ======================== 私有辅助方法 ========================

    /**
     * 将数据库下划线命名转换为驼峰命名的 Map
     */
    private Map<String, Object> convertToCamelCase(Map<String, Object> row) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", row.get("id"));
        m.put("itemName", row.get("item_name"));
        m.put("specModel", row.get("spec_model"));
        m.put("unit", row.get("unit"));
        m.put("unitPrice", row.get("unit_price"));
        m.put("remark", row.get("remark"));
        m.put("createdAt", row.get("created_at"));
        m.put("updatedAt", row.get("updated_at"));
        return m;
    }

    /** 从请求体安全取字符串 */
    private String getStr(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : v.toString();
    }

    /** 从请求体安全取 BigDecimal */
    private BigDecimal getDecimal(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null || v.toString().isEmpty()) {
            return null;
        }
        return new BigDecimal(v.toString());
    }
}
