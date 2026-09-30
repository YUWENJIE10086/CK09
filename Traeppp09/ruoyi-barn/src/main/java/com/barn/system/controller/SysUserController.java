package com.barn.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.barn.common.core.domain.R;
import com.barn.common.core.domain.TableDataInfo;
import com.barn.system.dto.SysUserRequest;
import com.barn.system.entity.SysUser;
import com.barn.system.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
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
    public R<Void> add(@RequestBody SysUserRequest request) {
        // 检查用户名是否重复
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUserName, request.getUserName());
        if (sysUserMapper.selectCount(wrapper) > 0) {
            return R.fail("用户名已存在");
        }
        if (!isValidPassword(request.getPassword())) {
            return R.fail("密码长度必须为8-64位");
        }

        SysUser user = new SysUser();
        copyAllowedUserFields(request, user, false);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDelFlag("0");
        if (user.getStatus() == null || user.getStatus().isEmpty()) {
            user.setStatus("0");
        }
        sysUserMapper.insert(user);
        return R.ok();
    }

    /**
     * 修改用户
     */
    @PutMapping
    public R<Void> edit(@RequestBody SysUserRequest request) {
        if (request.getId() == null) {
            return R.fail("用户ID不能为空");
        }
        SysUser existing = sysUserMapper.selectById(request.getId());
        if (existing == null) {
            return R.fail("用户不存在");
        }

        SysUser user = new SysUser();
        user.setId(existing.getId());
        copyAllowedUserFields(request, user, true);
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return R.ok();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
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
    public R<Void> updatePwd(@RequestBody Map<String, String> params, Authentication authentication) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        if (oldPassword == null || !isValidPassword(newPassword)) {
            return R.fail("参数不完整");
        }

        // 只能修改当前已认证用户自己的密码，禁止信任请求体中的 userId。
        if (authentication == null || !(authentication.getPrincipal() instanceof SysUser)) {
            return R.fail(401, "认证失败，请重新登录");
        }
        SysUser principal = (SysUser) authentication.getPrincipal();
        SysUser user = sysUserMapper.selectById(principal.getId());
        if (user == null || !"0".equals(user.getStatus())) {
            return R.fail(401, "认证失败，请重新登录");
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

    /**
     * 只复制允许由用户管理接口写入的业务字段。
     * delFlag/loginIp/loginAt/createdBy/createdAt/updatedBy/password 等敏感字段不在此白名单中。
     */
    private void copyAllowedUserFields(SysUserRequest request, SysUser user, boolean editing) {
        user.setDeptId(request.getDeptId());
        user.setUserName(request.getUserName());
        user.setNickName(request.getNickName());
        user.setUserType(request.getUserType());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setSex(request.getSex());
        user.setAvatar(request.getAvatar());
        user.setStatus(request.getStatus());
        user.setRemark(request.getRemark());
    }

    private boolean isValidPassword(String password) {
        return password != null && !password.isBlank()
                && password.length() >= 8 && password.length() <= 64;
    }
}
