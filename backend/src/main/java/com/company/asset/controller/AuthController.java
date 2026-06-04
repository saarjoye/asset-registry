package com.company.asset.controller;

import com.company.asset.entity.Employee;
import com.company.asset.security.AuthTokenService;
import com.company.asset.service.RegistryService;
import com.company.asset.service.RegistryService.LoginRequest;
import com.company.asset.service.RegistryService.SetupRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@CrossOrigin
@RequestMapping("/api/auth")
public class AuthController {
  private final RegistryService registryService;
  private final AuthTokenService authTokenService;

  public AuthController(RegistryService registryService, AuthTokenService authTokenService) {
    this.registryService = registryService;
    this.authTokenService = authTokenService;
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
    return registryService.login(request)
        .<ResponseEntity<?>>map(employee -> ResponseEntity.ok(new LoginResponse(employee, authTokenService.issue(employee))))
        .orElseGet(() -> ResponseEntity.status(401).body(Map.of("message", "账号或密码错误")));
  }

  @GetMapping("/setup-required")
  public Map<String, Boolean> setupRequired() {
    return Map.of("required", registryService.setupRequired());
  }

  @PostMapping("/setup")
  public ResponseEntity<LoginResponse> setup(@Valid @RequestBody SetupRequest request) {
    Employee employee = registryService.initializeAdmin(request);
    return ResponseEntity.ok(new LoginResponse(employee, authTokenService.issue(employee)));
  }

  public record LoginResponse(Employee user, String token) {
  }
}
