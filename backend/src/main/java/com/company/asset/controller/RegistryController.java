package com.company.asset.controller;

import com.company.asset.entity.ChannelAccount;
import com.company.asset.entity.DeviceAsset;
import com.company.asset.service.RegistryService;
import com.company.asset.service.RegistryService.AccountRequest;
import com.company.asset.service.RegistryService.DeviceRequest;
import com.company.asset.service.SummaryRows;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping("/api/registry")
public class RegistryController {
  private final RegistryService registryService;

  public RegistryController(RegistryService registryService) {
    this.registryService = registryService;
  }

  @PostMapping("/users/{employeeId}/devices")
  public DeviceAsset registerDevice(@PathVariable String employeeId, @Valid @RequestBody DeviceRequest request) {
    return registryService.registerDevice(employeeId, request);
  }

  @PostMapping("/users/{employeeId}/accounts")
  public ChannelAccount registerAccount(@PathVariable String employeeId, @Valid @RequestBody AccountRequest request) {
    return registryService.registerAccount(employeeId, request);
  }

  @GetMapping("/users/{employeeId}/summary")
  public SummaryRows summary(@PathVariable String employeeId, @RequestParam(defaultValue = "self") String scope) {
    return registryService.summary(employeeId, scope);
  }
}
