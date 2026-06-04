package com.company.asset.controller;

import com.company.asset.entity.SupervisorDataScope;
import com.company.asset.security.ApiTokenFilter;
import com.company.asset.service.RegistryService;
import com.company.asset.service.RegistryService.SupervisorDataScopeRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping("/api/permissions")
public class PermissionController {
  private final RegistryService registryService;

  public PermissionController(RegistryService registryService) {
    this.registryService = registryService;
  }

  @GetMapping("/supervisors/scopes")
  public List<SupervisorDataScope> supervisorDataScopes(
      @RequestAttribute(ApiTokenFilter.EMPLOYEE_ID_ATTRIBUTE) String operatorId
  ) {
    return registryService.supervisorDataScopesForAdmin(operatorId);
  }

  @GetMapping("/supervisors/{supervisorId}/scopes")
  public List<SupervisorDataScope> supervisorDataScopes(
      @PathVariable String supervisorId,
      @RequestAttribute(ApiTokenFilter.EMPLOYEE_ID_ATTRIBUTE) String operatorId
  ) {
    return registryService.supervisorDataScopesForAdmin(operatorId).stream()
        .filter(scope -> scope.getSupervisorId().equals(supervisorId))
        .toList();
  }

  @PostMapping("/supervisors/{supervisorId}/scopes")
  public List<SupervisorDataScope> saveSupervisorDataScopes(
      @PathVariable String supervisorId,
      @RequestAttribute(ApiTokenFilter.EMPLOYEE_ID_ATTRIBUTE) String operatorId,
      @Valid @RequestBody SupervisorDataScopeRequest request
  ) {
    return registryService.saveSupervisorDataScopes(operatorId, supervisorId, request);
  }
}
