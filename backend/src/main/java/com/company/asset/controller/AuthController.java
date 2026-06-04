package com.company.asset.controller;

import com.company.asset.entity.Employee;
import com.company.asset.service.RegistryService;
import com.company.asset.service.RegistryService.LoginRequest;
import com.company.asset.service.RegistryService.SetupRequest;
import jakarta.validation.Valid;
import java.util.Map;
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

  public AuthController(RegistryService registryService) {
    this.registryService = registryService;
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
    return registryService.login(request)
        .<ResponseEntity<?>>map(employee -> ResponseEntity.ok(new LoginResponse(employee)))
        .orElseGet(() -> ResponseEntity.status(401).body(Map.of("message", "账号或密码错误")));
  }

  @PostMapping("/setup")
  public ResponseEntity<LoginResponse> setup(@Valid @RequestBody SetupRequest request) {
    return ResponseEntity.ok(new LoginResponse(registryService.initializeAdmin(request)));
  }

  public record LoginResponse(Employee user) {
  }
}
