package org.example.hrmanagement.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.util.JwtUtil;
import org.example.hrmanagement.module.auth.entity.User;
import org.example.hrmanagement.module.auth.mapper.PermissionMapper;
import org.example.hrmanagement.module.auth.mapper.UserMapper;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final PermissionMapper permissionMapper;
    private final EmployeeMapper employeeMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response); // 没 Token，交给 Security 判断要不要拦
            return;
        }

        try {
            Long userId = jwtUtil.getUserId(token);
            String username = jwtUtil.getUsername(token);
            String roleCode = jwtUtil.getRoleCode(token);

            User user = userMapper.selectById(userId);
            if (user == null || user.getStatus() == 0) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // Token 必须携带登录角色，且用户仍拥有该角色
            if (roleCode == null || roleCode.isBlank()
                    || permissionMapper.countUserRole(userId, roleCode) <= 0) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            List<String> roles = List.of(roleCode);
            List<String> permissions = permissionMapper.selectPermCodesByUserIdAndRole(userId, roleCode);

            Long employeeId = user.getEmployeeId();
            Long deptId = null;
            if (employeeId != null) {
                Employee emp = employeeMapper.selectById(employeeId);
                deptId = emp != null ? emp.getDeptId() : null;
            }

            LoginUser loginUser = new LoginUser(userId, username);
            loginUser.setRoles(roles);
            loginUser.setPermissions(permissions);
            loginUser.setEmployeeId(employeeId);
            loginUser.setDeptId(deptId);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request,response);
    }

    /**
     * 优先 Authorization: Bearer；SSE（EventSource）无法自定义 Header，兼容 query ?token=
     */
    private String resolveToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        String queryToken = request.getParameter("token");
        if (queryToken != null && !queryToken.isBlank()) {
            return queryToken.trim();
        }
        return null;
    }
}
