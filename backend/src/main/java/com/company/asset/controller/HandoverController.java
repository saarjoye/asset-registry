package com.company.asset.controller;

import com.company.asset.entity.HandoverTask;
import com.company.asset.security.ApiTokenFilter;
import com.company.asset.service.RegistryService;
import com.company.asset.service.RegistryService.HandoverAllocationRequest;
import com.company.asset.service.RegistryService.HandoverApprovalRequest;
import com.company.asset.service.RegistryService.HandoverRejectRequest;
import com.company.asset.service.RegistryService.ResignationRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping("/api/handover")
public class HandoverController {
  private final RegistryService registryService;

  public HandoverController(RegistryService registryService) {
    this.registryService = registryService;
  }

  @GetMapping
  public List<HandoverTask> tasks(@RequestAttribute(ApiTokenFilter.EMPLOYEE_ID_ATTRIBUTE) String employeeId) {
    return registryService.handoverTasks(employeeId);
  }

  @PostMapping("/users/{employeeId}/resignation")
  public List<HandoverTask> submitResignation(
      @PathVariable String employeeId,
      @RequestAttribute(ApiTokenFilter.EMPLOYEE_ID_ATTRIBUTE) String currentEmployeeId,
      @RequestBody ResignationRequest request
  ) {
    return registryService.submitResignation(currentEmployeeId, request);
  }

  @PostMapping("/approve")
  public HandoverTask approve(
      @RequestParam String supervisorId,
      @RequestAttribute(ApiTokenFilter.EMPLOYEE_ID_ATTRIBUTE) String currentEmployeeId,
      @Valid @RequestBody HandoverApprovalRequest request
  ) {
    return registryService.approveHandover(currentEmployeeId, request);
  }

  @PostMapping("/allocate")
  public HandoverTask allocate(
      @RequestParam String allocatorId,
      @RequestAttribute(ApiTokenFilter.EMPLOYEE_ID_ATTRIBUTE) String currentEmployeeId,
      @Valid @RequestBody HandoverAllocationRequest request
  ) {
    return registryService.allocateDevice(currentEmployeeId, request);
  }

  @PostMapping("/{taskId}/confirm")
  public HandoverTask confirm(
      @PathVariable String taskId,
      @RequestParam String receiverId,
      @RequestAttribute(ApiTokenFilter.EMPLOYEE_ID_ATTRIBUTE) String currentEmployeeId
  ) {
    return registryService.confirmHandover(currentEmployeeId, taskId);
  }

  @PostMapping("/reject")
  public HandoverTask reject(
      @RequestParam String receiverId,
      @RequestAttribute(ApiTokenFilter.EMPLOYEE_ID_ATTRIBUTE) String currentEmployeeId,
      @Valid @RequestBody HandoverRejectRequest request
  ) {
    return registryService.rejectHandover(currentEmployeeId, request);
  }
}
