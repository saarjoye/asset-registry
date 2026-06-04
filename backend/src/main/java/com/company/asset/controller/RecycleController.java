package com.company.asset.controller;

import com.company.asset.entity.DeviceAsset;
import com.company.asset.service.RegistryService;
import com.company.asset.service.RegistryService.TransferRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping("/api/recycle")
public class RecycleController {
  private final RegistryService registryService;

  public RecycleController(RegistryService registryService) {
    this.registryService = registryService;
  }

  @PostMapping("/{deviceId}/confirm")
  public DeviceAsset confirm(@PathVariable String deviceId, @RequestParam String supervisorId) {
    return registryService.confirmRecycle(supervisorId, deviceId);
  }

  @PostMapping("/transfer")
  public DeviceAsset transfer(@RequestParam String supervisorId, @Valid @RequestBody TransferRequest request) {
    return registryService.transferDevice(supervisorId, request);
  }

  @PostMapping("/{deviceId}/stock-in")
  public DeviceAsset stockIn(@PathVariable String deviceId, @RequestParam String supervisorId) {
    return registryService.stockIn(supervisorId, deviceId);
  }
}
