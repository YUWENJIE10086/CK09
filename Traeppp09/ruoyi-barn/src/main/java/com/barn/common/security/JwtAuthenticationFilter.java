package com.barn.common.security;

import com.barn.system.entity.SysUser;
import com.barn.system.service.SysUserService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Locale;

/**
 * JWT认证过滤器
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserService sysUserService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            Claims claims = jwtUtil.parseToken(token);
            if (claims != null) {
                String userName = (String) claims.get("userName");
                SysUser user = sysUserService.getByUserName(userName);
                if (user != null && "0".equals(user.getStatus())) {
                    String role = resolveRole(user.getUserType());
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            user, null, Collections.singletonList(new SimpleGrantedAuthority(role))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 将数据库 userType 映射为 Spring Security Authority。
     * 角色编码：00=管理员、01=合作社、02=技术员、03=烟农。
     * 其他非空类型仍映射为独立 ROLE_<USERTYPE>，不再统一降维为 ROLE_USER。
     */
    private String resolveRole(String userType) {
        if (userType == null || userType.trim().isEmpty()) {
            return "ROLE_USER";
        }
        String normalized = userType.trim().toUpperCase(Locale.ROOT);
        if ("00".equals(normalized)) {
            return "ROLE_ADMIN";
        }
        if ("01".equals(normalized)) {
            return "ROLE_COOP";
        }
        if ("02".equals(normalized)) {
            return "ROLE_TECH";
        }
        if ("03".equals(normalized)) {
            return "ROLE_FARMER";
        }
        normalized = normalized.replaceAll("[^A-Z0-9_]", "_");
        return "ROLE_" + normalized;
    }
}
