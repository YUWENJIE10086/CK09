package com.barn.barn.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.barn.barn.entity.Farmer;
import com.barn.barn.mapper.FarmerMapper;
import com.barn.common.core.domain.R;
import com.barn.common.core.domain.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 烟农管理Controller
 */
@RestController
@RequestMapping("/farmer")
@PreAuthorize("hasRole('ADMIN')")
public class FarmerController {

    @Autowired
    private FarmerMapper farmerMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** 导出CSV表头 */
    private static final String CSV_HEADERS =
            "单位名称,种植者名称,年龄,手机号,行政区划,合同编号,合同类型,烟叶品种,种植面积,约定总收购量";

    /**
     * 烟农统计信息
     */
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        Long total = farmerMapper.selectCount(new LambdaQueryWrapper<>());
        Long active = farmerMapper.selectCount(new LambdaQueryWrapper<Farmer>().eq(Farmer::getStatus, 1));

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("active", active);
        result.put("creditA", 0);
        result.put("creditB", 0);
        result.put("creditC", 0);
        result.put("creditD", 0);

        List<Map<String, Object>> creditStats = jdbcTemplate.queryForList(
                "SELECT credit_level, COUNT(*) AS cnt FROM users GROUP BY credit_level");
        for (Map<String, Object> row : creditStats) {
            String level = (String) row.get("credit_level");
            Number count = (Number) row.get("cnt");
            if (level != null && count != null) {
                switch (level) {
                    case "A": result.put("creditA", count.intValue()); break;
                    case "B": result.put("creditB", count.intValue()); break;
                    case "C": result.put("creditC", count.intValue()); break;
                    case "D": result.put("creditD", count.intValue()); break;
                }
            }
        }

        // 烟农增长趋势 - 按月统计近6个月新增烟农数量
        List<Map<String, Object>> growthTrend = jdbcTemplate.queryForList(
                "SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, COUNT(*) AS cnt " +
                "FROM users WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH) " +
                "GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY month ASC");
        result.put("growthTrend", growthTrend);

        return R.ok(result);
    }

    /**
     * 烟农分页列表
     */
    @GetMapping("/list")
    public TableDataInfo<Farmer> list(@RequestParam(defaultValue = "1") int pageNum,
                                      @RequestParam(defaultValue = "10") int pageSize,
                                      @RequestParam(required = false) String name,
                                      @RequestParam(required = false) String phone,
                                      @RequestParam(required = false) String creditLevel,
                                      @RequestParam(required = false) Integer creditScore,
                                      @RequestParam(required = false) String area,
                                      @RequestParam(required = false) java.math.BigDecimal plantingArea,
                                      @RequestParam(required = false) Integer plantingYears,
                                      @RequestParam(required = false) Integer totalBakes,
                                      @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Farmer> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(name), Farmer::getName, name);
        wrapper.like(StringUtils.isNotBlank(phone), Farmer::getPhone, phone);
        wrapper.eq(StringUtils.isNotBlank(creditLevel), Farmer::getCreditLevel, creditLevel);
        wrapper.eq(creditScore != null, Farmer::getCreditScore, creditScore);
        wrapper.like(StringUtils.isNotBlank(area), Farmer::getArea, area);
        wrapper.eq(plantingArea != null, Farmer::getPlantingArea, plantingArea);
        wrapper.eq(plantingYears != null, Farmer::getPlantingYears, plantingYears);
        wrapper.eq(totalBakes != null, Farmer::getTotalBakes, totalBakes);
        wrapper.eq(status != null, Farmer::getStatus, status);
        wrapper.orderByDesc(Farmer::getCreatedAt);

        IPage<Farmer> page = farmerMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return TableDataInfo.build(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    /**
     * 新增烟农
     */
    @PostMapping
    public R<String> addFarmer(@RequestBody Farmer farmer) {
        try {
            // 设置默认值
            if (farmer.getCreditLevel() == null || farmer.getCreditLevel().isEmpty()) {
                farmer.setCreditLevel("B");
            }
            if (farmer.getCreditScore() == null) {
                farmer.setCreditScore(0);
            }
            if (farmer.getPlantingArea() == null) {
                farmer.setPlantingArea(java.math.BigDecimal.ZERO);
            }
            if (farmer.getPlantingYears() == null) {
                farmer.setPlantingYears(0);
            }
            if (farmer.getTotalBakes() == null) {
                farmer.setTotalBakes(0);
            }
            if (farmer.getStatus() == null) {
                farmer.setStatus(1);
            }

            // 生成唯一ID (最大20字符)
            long timestamp = System.currentTimeMillis();
            int random = (int)(Math.random() * 10000);
            farmer.setId("u" + timestamp + random);  // 格式: u时间戳4位随机数 (最多19字符)

            // 设置创建时间
            farmer.setCreatedAt(LocalDateTime.now());
            farmer.setUpdatedAt(LocalDateTime.now());

            farmerMapper.insert(farmer);

            // 同步插入到sys_admins表，用于小程序登录
            try {
                String encodedPassword = passwordEncoder.encode(UUID.randomUUID().toString());
                jdbcTemplate.update(
                    "INSERT INTO sys_admins (dept_id, user_name, nick_name, user_type, phone, password, status, del_flag, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                    100,  // 默认部门ID
                    farmer.getPhone(),  // user_name=手机号
                    farmer.getName(),  // nick_name=烟农姓名
                    "F",  // user_type=F表示烟农
                    farmer.getPhone(),  // phone=手机号
                    encodedPassword,
                    "0",  // status=0表示正常
                    "0"  // del_flag=0表示未删除
                );
            } catch (Exception e) {
                // sys_admins表可能已存在该用户，忽略错误
                System.out.println("同步到sys_admins表失败: " + e.getMessage());
            }

            return R.ok("添加成功");
        } catch (Exception e) {
            return R.fail("添加失败: " + e.getMessage());
        }
    }

    /**
     * 更新烟农
     */
    @PutMapping
    public R<String> updateFarmer(@RequestBody Farmer farmer) {
        try {
            Farmer existing = farmerMapper.selectById(farmer.getId());
            if (existing == null) {
                return R.fail("烟农不存在");
            }

            // 更新字段（除了id）
            farmerMapper.updateById(farmer);
            return R.ok("更新成功");
        } catch (Exception e) {
            return R.fail("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除烟农
     */
    @DeleteMapping("/{id}")
    public R<String> deleteFarmer(@PathVariable String id) {
        try {
            // 先查询烟农的手机号
            Farmer farmer = farmerMapper.selectById(id);
            if (farmer == null) {
                return R.fail("删除失败，烟农不存在");
            }
            String phone = farmer.getPhone();
            
            int rows = farmerMapper.deleteById(id);
            if (rows > 0) {
                // 同步删除sys_admins表的登录记录
                try {
                    jdbcTemplate.update("DELETE FROM sys_admins WHERE user_name = ?", phone);
                } catch (Exception e) {
                    System.out.println("删除sys_admins表记录失败: " + e.getMessage());
                }
                return R.ok("删除成功");
            } else {
                return R.fail("删除失败，烟农不存在");
            }
        } catch (Exception e) {
            return R.fail("删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除烟农
     */
    @DeleteMapping("/batch")
    public R<String> batchDeleteFarmer(@RequestBody Map<String, List<String>> request) {
        try {
            List<String> ids = request.get("ids");
            if (ids == null || ids.isEmpty()) {
                return R.fail("请选择要删除的记录");
            }
            int count = 0;
            for (String id : ids) {
                Farmer farmer = farmerMapper.selectById(id);
                if (farmer != null) {
                    String phone = farmer.getPhone();
                    if (farmerMapper.deleteById(id) > 0) {
                        // 同步删除sys_admins表的登录记录
                        try {
                            jdbcTemplate.update("DELETE FROM sys_admins WHERE user_name = ?", phone);
                        } catch (Exception ex) {
                            System.out.println("删除sys_admins表记录失败: " + ex.getMessage());
                        }
                        count++;
                    }
                }
            }
            return R.ok("成功删除" + count + "条记录");
        } catch (Exception e) {
            return R.fail("批量删除失败: " + e.getMessage());
        }
    }

    // ==================== 导入/导出/模板 ====================

    /**
     * 导入Excel（已废弃，请使用JSON导入方式 /import-json）
     */
    @PostMapping("/import")
    public R<Map<String, Object>> importFarmer(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", 0);
        result.put("skipped", 0);
        result.put("message", "请使用JSON导入方式");
        return R.ok(result);
    }

    /**
     * 导入JSON数据
     * 接收JSON数组，每条记录包含字段：unitName, name, age, phone, area, contractNo,
     * contractType, tobaccoVariety, plantingArea, agreedQuantity
     *
     * @param dataList 烟农数据列表
     * @return {success: N, skipped: N}
     */
    @PostMapping("/import-json")
    public R<Map<String, Object>> importFarmerJson(@RequestBody List<Map<String, String>> dataList) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int skipCount = 0;

        if (dataList == null || dataList.isEmpty()) {
            result.put("success", 0);
            result.put("skipped", 0);
            return R.ok(result);
        }

        for (Map<String, String> data : dataList) {
            String unitName         = data.get("unitName");
            String name             = data.get("name");
            String ageStr           = data.get("age");
            String phone            = data.get("phone");
            String area             = data.get("area");
            String contractNo       = data.get("contractNo");
            String contractType     = data.get("contractType");
            String tobaccoVariety   = data.get("tobaccoVariety");
            String plantingAreaStr  = data.get("plantingArea");
            String agreedQuantityStr = data.get("agreedQuantity");

            // 检查name和phone不为空
            if (name == null || name.isEmpty() || phone == null || phone.isEmpty()) {
                skipCount++;
                continue;
            }

            // 检查phone不重复（查users表）
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE phone = ?", Integer.class, phone);
            if (count != null && count > 0) {
                skipCount++;
                continue;
            }

            // 生成唯一ID: "u" + System.currentTimeMillis() + random
            String id = "u" + System.currentTimeMillis() + (int) (Math.random() * 10000);

            // 解析数值字段
            Integer age = parseIntSafe(ageStr);
            BigDecimal plantingArea = parseDecimalSafe(plantingAreaStr);
            BigDecimal agreedQuantity = parseDecimalSafe(agreedQuantityStr);

            // 插入users表
            try {
                jdbcTemplate.update(
                        "INSERT INTO users (id, unit_name, name, age, phone, area, contract_no, contract_type, " +
                                "tobacco_variety, planting_area, agreed_quantity, credit_level, credit_score, status, created_at, updated_at) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                        id, unitName, name, age, phone, area, contractNo, contractType,
                        tobaccoVariety, plantingArea, agreedQuantity, "B", 0, 1
                );
            } catch (Exception e) {
                e.printStackTrace();
                skipCount++;
                continue;
            }

            // 同步插入sys_admins表，随机密码仅用于禁止固定密码登录。
            try {
                String encodedPassword = passwordEncoder.encode(UUID.randomUUID().toString());
                jdbcTemplate.update(
                        "INSERT INTO sys_admins (dept_id, user_name, nick_name, user_type, phone, password, status, del_flag, created_at, updated_at) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                        100, phone, name, "F", phone, encodedPassword, "0", "0"
                );
            } catch (Exception e) {
                // sys_admins表可能已存在该用户，忽略错误
                System.out.println("同步到sys_admins表失败: " + e.getMessage());
            }

            successCount++;
        }

        result.put("success", successCount);
        result.put("skipped", skipCount);
        return R.ok(result);
    }

    /**
     * 导出CSV
     * 查询所有烟农数据，生成CSV文件（UTF-8 BOM）返回
     * Content-Type: text/csv，文件名: farmers_export.csv
     */
    @GetMapping("/export")
    public void exportFarmer(HttpServletResponse response) throws IOException {
        // 查询所有烟农数据
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT unit_name, name, age, phone, area, contract_no, contract_type, " +
                        "tobacco_variety, planting_area, agreed_quantity " +
                        "FROM users ORDER BY created_at DESC");

        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM，确保Excel打开时中文不乱码
        sb.append('\ufeff');
        // 表头
        sb.append(CSV_HEADERS).append("\r\n");
        // 数据行
        for (Map<String, Object> data : list) {
            sb.append(csvValue(data.get("unit_name"))).append(",")
              .append(csvValue(data.get("name"))).append(",")
              .append(csvValue(data.get("age"))).append(",")
              .append(csvValue(data.get("phone"))).append(",")
              .append(csvValue(data.get("area"))).append(",")
              .append(csvValue(data.get("contract_no"))).append(",")
              .append(csvValue(data.get("contract_type"))).append(",")
              .append(csvValue(data.get("tobacco_variety"))).append(",")
              .append(csvValue(data.get("planting_area"))).append(",")
              .append(csvValue(data.get("agreed_quantity"))).append("\r\n");
        }

        // 设置响应头
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=farmers_export.csv");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        response.setCharacterEncoding("UTF-8");

        response.getOutputStream().write(sb.toString().getBytes(StandardCharsets.UTF_8));
        response.getOutputStream().flush();
    }

    /**
     * 下载导入模板
     * 返回静态文件 classpath:/static/farmer_import_template.xlsx，不使用POI
     */
    @GetMapping("/import-template")
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        ClassPathResource resource = new ClassPathResource("static/farmer_import_template.xlsx");
        if (!resource.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().write("模板文件不存在: static/farmer_import_template.xlsx");
            response.getWriter().flush();
            return;
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=farmer_import_template.xlsx");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        try (InputStream is = resource.getInputStream()) {
            StreamUtils.copy(is, response.getOutputStream());
        }
        response.getOutputStream().flush();
    }

    // ==================== 辅助方法 ====================

    /**
     * 安全解析整数
     */
    private Integer parseIntSafe(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            try {
                return (int) Double.parseDouble(str.trim());
            } catch (NumberFormatException e2) {
                return 0;
            }
        }
    }

    /**
     * 安全解析BigDecimal
     */
    private BigDecimal parseDecimalSafe(String str) {
        if (str == null || str.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(str.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * CSV字段值处理：将值转为字符串并用双引号包裹，内部双引号转义为两个双引号
     */
    private String csvValue(Object value) {
        if (value == null) {
            return "";
        }
        String str = value.toString();
        return "\"" + str.replace("\"", "\"\"") + "\"";
    }
}
