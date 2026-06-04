package com.company.asset.security;

import com.company.asset.entity.Employee;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AuthTokenService {
  private final SecureRandom secureRandom = new SecureRandom();
  private final Map<String, String> tokenEmployeeIds = new ConcurrentHashMap<>();

  public String issue(Employee employee) {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    tokenEmployeeIds.put(token, employee.getId());
    return token;
  }

  public String requireEmployeeId(String authorization) {
    String token = parseBearerToken(authorization);
    String employeeId = token == null ? null : tokenEmployeeIds.get(token);
    if (employeeId == null || employeeId.isBlank()) {
      throw new IllegalArgumentException("unauthorized");
    }
    return employeeId;
  }

  private String parseBearerToken(String authorization) {
    if (authorization == null || authorization.isBlank()) {
      return null;
    }
    String prefix = "Bearer ";
    return authorization.startsWith(prefix) ? authorization.substring(prefix.length()).trim() : authorization.trim();
  }
}
