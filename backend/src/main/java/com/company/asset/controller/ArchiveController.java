package com.company.asset.controller;

import com.company.asset.entity.ChannelAccount;
import com.company.asset.entity.Department;
import com.company.asset.entity.DeviceAsset;
import com.company.asset.entity.Employee;
import com.company.asset.entity.PhoneNumber;
import com.company.asset.entity.Position;
import com.company.asset.security.ApiTokenFilter;
import com.company.asset.service.RegistryService;
import com.company.asset.service.RegistryService.ArchiveRequest;
import com.company.asset.service.RegistryService.EmployeeRequest;
import com.company.asset.service.RegistryService.ImportResult;
import com.company.asset.service.RegistryService.OpenAccountRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin
@RequestMapping("/api/archive")
public class ArchiveController {
  private final RegistryService registryService;

  public ArchiveController(RegistryService registryService) {
    this.registryService = registryService;
  }

  @GetMapping("/employees")
  public List<Employee> employees(@RequestAttribute(ApiTokenFilter.EMPLOYEE_ID_ATTRIBUTE) String employeeId) {
    return registryService.employees(employeeId);
  }

  @PostMapping("/employees")
  public Employee saveEmployee(
      @RequestParam(defaultValue = "false") boolean includeAccount,
      @RequestAttribute(ApiTokenFilter.EMPLOYEE_ID_ATTRIBUTE) String employeeId,
      @Valid @RequestBody EmployeeRequest request
  ) {
    requireRole(employeeId, "admin", "hr");
    return registryService.saveEmployee(new EmployeeRequest(
        request.id(),
        request.name(),
        request.gender(),
        request.age(),
        request.departmentId(),
        request.positionId(),
        request.hireDate(),
        request.status(),
        request.account(),
        request.password(),
        request.role(),
        request.recycleReceiver(),
        employeeId
    ), includeAccount);
  }

  @GetMapping("/departments")
  public List<Department> departments() {
    return registryService.departments();
  }

  @PostMapping("/departments")
  public Department saveDepartment(
      @RequestAttribute(ApiTokenFilter.EMPLOYEE_ID_ATTRIBUTE) String employeeId,
      @Valid @RequestBody ArchiveRequest request
  ) {
    requireRole(employeeId, "admin", "hr");
    return registryService.saveDepartment(request);
  }

  @GetMapping("/positions")
  public List<Position> positions() {
    return registryService.positions();
  }

  @PostMapping("/positions")
  public Position savePosition(
      @RequestAttribute(ApiTokenFilter.EMPLOYEE_ID_ATTRIBUTE) String employeeId,
      @Valid @RequestBody ArchiveRequest request
  ) {
    requireRole(employeeId, "admin", "hr");
    return registryService.savePosition(request);
  }

  @PostMapping(value = "/import/departments-positions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ImportResult importDepartmentsAndPositions(
      @RequestAttribute(ApiTokenFilter.EMPLOYEE_ID_ATTRIBUTE) String employeeId,
      @RequestParam("file") MultipartFile file
  ) {
    requireRole(employeeId, "admin", "hr");
    return registryService.importDepartmentsAndPositions(file);
  }

  @PostMapping(value = "/import/employees", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ImportResult importEmployees(
      @RequestAttribute(ApiTokenFilter.EMPLOYEE_ID_ATTRIBUTE) String employeeId,
      @RequestParam("file") MultipartFile file
  ) {
    requireRole(employeeId, "admin", "hr");
    return registryService.importEmployees(file);
  }

  @GetMapping("/import/templates/departments-positions")
  public ResponseEntity<byte[]> departmentPositionImportTemplate() {
    return templateResponse("部门岗位导入模板.xlsx", registryService.departmentPositionImportTemplate());
  }

  @GetMapping("/import/templates/employees")
  public ResponseEntity<byte[]> employeeImportTemplate() {
    return templateResponse("人员档案导入模板.xlsx", registryService.employeeImportTemplate());
  }

  @GetMapping("/phones")
  public List<PhoneNumber> phones(@RequestAttribute(ApiTokenFilter.EMPLOYEE_ID_ATTRIBUTE) String employeeId) {
    return registryService.phones(employeeId);
  }

  @GetMapping("/devices")
  public List<DeviceAsset> devices(@RequestAttribute(ApiTokenFilter.EMPLOYEE_ID_ATTRIBUTE) String employeeId) {
    return registryService.devices(employeeId);
  }

  @GetMapping("/accounts")
  public List<ChannelAccount> accounts(@RequestAttribute(ApiTokenFilter.EMPLOYEE_ID_ATTRIBUTE) String employeeId) {
    return registryService.accounts(employeeId);
  }

  @PostMapping("/accounts/open")
  public Employee openAccount(
      @RequestAttribute(ApiTokenFilter.EMPLOYEE_ID_ATTRIBUTE) String employeeId,
      @Valid @RequestBody OpenAccountRequest request
  ) {
    requireRole(employeeId, "admin", "hr");
    return registryService.openAccount(request);
  }

  private ResponseEntity<byte[]> templateResponse(String filename, byte[] content) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
        .body(content);
  }

  private void requireRole(String employeeId, String... roles) {
    String role = registryService.employee(employeeId).getRoleCode();
    if (!Set.of(roles).contains(role)) {
      throw new IllegalArgumentException("permission denied");
    }
  }
}
