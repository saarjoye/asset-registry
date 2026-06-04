package com.company.asset.controller;

import com.company.asset.entity.ChannelAccount;
import com.company.asset.entity.Department;
import com.company.asset.entity.DeviceAsset;
import com.company.asset.entity.Employee;
import com.company.asset.entity.PhoneNumber;
import com.company.asset.entity.Position;
import com.company.asset.service.RegistryService;
import com.company.asset.service.RegistryService.ArchiveRequest;
import com.company.asset.service.RegistryService.EmployeeRequest;
import com.company.asset.service.RegistryService.ImportResult;
import com.company.asset.service.RegistryService.OpenAccountRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
  public List<Employee> employees() {
    return registryService.employees();
  }

  @PostMapping("/employees")
  public Employee saveEmployee(
      @RequestParam(defaultValue = "false") boolean includeAccount,
      @Valid @RequestBody EmployeeRequest request
  ) {
    return registryService.saveEmployee(request, includeAccount);
  }

  @GetMapping("/departments")
  public List<Department> departments() {
    return registryService.departments();
  }

  @PostMapping("/departments")
  public Department saveDepartment(@Valid @RequestBody ArchiveRequest request) {
    return registryService.saveDepartment(request);
  }

  @GetMapping("/positions")
  public List<Position> positions() {
    return registryService.positions();
  }

  @PostMapping("/positions")
  public Position savePosition(@Valid @RequestBody ArchiveRequest request) {
    return registryService.savePosition(request);
  }

  @PostMapping(value = "/import/departments-positions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ImportResult importDepartmentsAndPositions(@RequestParam("file") MultipartFile file) {
    return registryService.importDepartmentsAndPositions(file);
  }

  @PostMapping(value = "/import/employees", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ImportResult importEmployees(@RequestParam("file") MultipartFile file) {
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
  public List<PhoneNumber> phones() {
    return registryService.phones();
  }

  @GetMapping("/devices")
  public List<DeviceAsset> devices() {
    return registryService.devices();
  }

  @GetMapping("/accounts")
  public List<ChannelAccount> accounts() {
    return registryService.accounts();
  }

  @PostMapping("/accounts/open")
  public Employee openAccount(@Valid @RequestBody OpenAccountRequest request) {
    return registryService.openAccount(request);
  }

  private ResponseEntity<byte[]> templateResponse(String filename, byte[] content) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
        .body(content);
  }
}
