package com.barn.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.barn.common.core.domain.R;
import com.barn.common.core.domain.TableDataInfo;
import com.barn.system.dto.SysUserDTO;
import com.barn.system.entity.SysUser;
import com.barn.system.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户管理Controller
 */
@RestController
@RequestMapping("/system/user")
public class SysUserController {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * 用户列表（分页）
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public TableDataInfo<SysUser> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userType) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (userName != null && !userName.isEmpty()) {
            wrapper.like(SysUser::getUserName, userName);
        }
        if (phone != null && !phone.isEmpty()) {
            wrapper.like(SysUser::getPhone, phone);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SysUser::getStatus, status);
        }
        if (userType != null && !userType.isEmpty()) {
            wrapper.eq(SysUser::getUserType, userType);
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);
        IPage<SysUser> result = sysUserMapper.selectPage(page, wrapper);
        // 清除密码字段不返回
        result.getRecords().forEach(u -> u.setPassword(null));
        return TableDataInfo.build(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<SysUser> getInfo(@PathVariable Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return R.ok(user);
    }

    /**
     * 新增用户
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> add(@RequestBody SysUserDTO dto) {
        SysUser user = new SysUser();
        user.setUserName(dto.getUserName());
        user.setNickName(dto.getNickName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setSex(dto.getSex());
        user.setAvatar(dto.getAvatar());
        user.setRemark(dto.getRemark());
        user.setPassword(dto.getPassword());
        // 检查用户名是否重复
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUserName, user.getUserName());
        if (sysUserMapper.selectCount(wrapper) > 0) {
            return R.fail("用户名已存在");
        }
        if (!isValidPassword(user.getPassword())) {
            return R.fail("密码长度必须为8-64位");
        }
        user.setUserType("01");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        if (user.getStatus() == null || user.getStatus().isEmpty()) {
            user.setStatus("0");
        }
        if (user.getDelFlag() == null || user.getDelFlag().isEmpty()) {
            user.setDelFlag("0");
        }
        sysUserMapper.insert(user);
        return R.ok();
    }

    /**
     * 修改用户
     */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> edit(@RequestBody SysUserDTO dto) {
        if (dto.getId() == null) {
            return R.fail("用户ID不能为空");
        }
        SysUser user = sysUserMapper.selectById(dto.getId());
        if (user == null) {
            return R.fail("用户不存在");
        }
        user.setUserName(dto.getUserName());
        user.setNickName(dto.getNickName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setSex(dto.getSex());
        user.setAvatar(dto.getAvatar());
        user.setRemark(dto.getRemark());
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return R.ok();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> remove(@PathVariable Long id) {
        if (id == 1L) {
            return R.fail("不允许删除超级管理员");
        }
        sysUserMapper.deleteById(id);
        return R.ok();
    }

    /**
     * 重置密码（管理员操作）
     */
    @PutMapping("/resetPwd/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> resetPwd(@PathVariable Long id, @RequestBody Map<String, String> params) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            return R.fail("用户不存在");
        }
        String newPassword = params.get("newPassword");
        if (!isValidPassword(newPassword)) {
            return R.fail("密码长度必须为8-64位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return R.ok();
    }

    /**
     * 修改密码（用户自己修改）
     */
    @PutMapping("/updatePwd")
    public R<Void> updatePwd(@RequestBody Map<String, String> params) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        if (oldPassword == null || !isValidPassword(newPassword)) {
            return R.fail("参数不完整");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SysUser)) {
            return R.fail(401, "认证失败，请重新登录");
        }
        SysUser principal = (SysUser) authentication.getPrincipal();
        Long id = principal.getId();
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            return R.fail("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return R.fail("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return R.ok();
    }

    /**
     * 修改用户状态
     */
    @PutMapping("/changeStatus")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> changeStatus(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("userId").toString());
        String status = params.get("status").toString();
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return R.ok();
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.isBlank() || password.length() < 8 || password.length() > 64) {
            return false;
        }
        String normalized = password.toLowerCase();
        return !java.util.Set.of("admin123", "password", "12345678", "123456789", "qwerty123", "abc123456")
                .contains(normalized);
    }
}
