package com.barn.system.controller;

import com.barn.common.core.domain.R;
import com.barn.common.security.JwtUtil;
import com.barn.system.entity.SysUser;
import com.barn.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录Controller
 */
@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> param) {
        String username = param.get("username");
        String password = param.get("password");
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            return R.fail(401, "用户名或密码错误");
        }

        SysUser user = sysUserService.getByUserName(username);
        if (user == null || !"0".equals(user.getStatus())) {
            return R.fail(401, "用户名或密码错误");
        }
        try {
            if (!passwordEncoder.matches(password, user.getPassword())) {
                return R.fail(401, "用户名或密码错误");
            }
        } catch (Exception ex) {
            return R.fail(401, "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUserName());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("userName", user.getUserName());
        userInfo.put("nickName", user.getNickName());
        userInfo.put("userType", user.getUserType());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone());
        data.put("userInfo", userInfo);
        return R.ok(data, "登录成功");
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok();
    }

    @PostMapping("/getInfo")
    public R<Map<String, Object>> getInfo() {
        SysUser user = (SysUser) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("userName", user.getUserName());
        userInfo.put("nickName", user.getNickName());
        userInfo.put("userType", user.getUserType());
        data.put("user", userInfo);
        return R.ok(data);
    }
}
