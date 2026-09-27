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
                    String expectedRole = "00".equals(user.getUserType()) ? "ROLE_ADMIN" : "ROLE_USER";
                    String tokenRole = claims.get("role", String.class);
                    if (tokenRole != null && !expectedRole.equals(tokenRole)) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            user, null, Collections.singletonList(new SimpleGrantedAuthority(expectedRole))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
