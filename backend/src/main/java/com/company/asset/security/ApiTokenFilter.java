package com.company.asset.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiTokenFilter extends OncePerRequestFilter {
  public static final String EMPLOYEE_ID_ATTRIBUTE = "authenticatedEmployeeId";

  private final AuthTokenService authTokenService;

  public ApiTokenFilter(AuthTokenService authTokenService) {
    this.authTokenService = authTokenService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    if (!requiresAuth(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      String employeeId = authTokenService.requireEmployeeId(request.getHeader("Authorization"));
      request.setAttribute(EMPLOYEE_ID_ATTRIBUTE, employeeId);
      filterChain.doFilter(request, response);
    } catch (IllegalArgumentException ex) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write("{\"message\":\"未登录或登录已失效\"}");
    }
  }

  private boolean requiresAuth(HttpServletRequest request) {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return false;
    }
    String path = request.getRequestURI();
    if (!path.startsWith("/api/")) {
      return false;
    }
    return !path.equals("/api/auth/login")
        && !path.equals("/api/auth/setup")
        && !path.equals("/api/auth/setup-required");
  }
}
