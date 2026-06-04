package com.company.asset.service;

import com.company.asset.entity.ChannelAccount;
import com.company.asset.entity.Department;
import com.company.asset.entity.DeviceAsset;
import com.company.asset.entity.Employee;
import com.company.asset.entity.HandoverTask;
import com.company.asset.entity.PhoneNumber;
import com.company.asset.entity.Position;
import com.company.asset.entity.SupervisorDataScope;
import com.company.asset.service.SummaryRows;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

public interface RegistryService {

  Employee initializeAdmin(SetupRequest request);

  boolean setupRequired();

  Optional<Employee> login(LoginRequest request);

  Employee employee(String employeeId);

  List<Department> departments();

  List<Position> positions();

  List<Employee> employees(String viewerId);

  List<PhoneNumber> phones(String viewerId);

  List<DeviceAsset> devices(String viewerId);

  List<ChannelAccount> accounts(String viewerId);

  List<HandoverTask> handoverTasks(String viewerId);

  List<SupervisorDataScope> supervisorDataScopesForAdmin(String operatorId);

  List<SupervisorDataScope> supervisorDataScopes(String supervisorId);

  List<SupervisorDataScope> saveSupervisorDataScopes(
      String operatorId,
      String supervisorId,
      SupervisorDataScopeRequest request
  );

  DeviceAsset registerDevice(String employeeId, DeviceRequest request);

  ChannelAccount registerAccount(String employeeId, AccountRequest request);

  Employee saveEmployee(EmployeeRequest request, boolean includeAccount);

  Department saveDepartment(ArchiveRequest request);

  Position savePosition(ArchiveRequest request);

  ImportResult importDepartmentsAndPositions(MultipartFile file);

  ImportResult importEmployees(MultipartFile file);

  byte[] departmentPositionImportTemplate();

  byte[] employeeImportTemplate();

  Employee openAccount(OpenAccountRequest request);

  DeviceAsset confirmRecycle(String supervisorId, String deviceId);

  DeviceAsset transferDevice(String supervisorId, TransferRequest request);

  DeviceAsset stockIn(String supervisorId, String deviceId);

  List<HandoverTask> submitResignation(String employeeId, ResignationRequest request);

  HandoverTask approveHandover(String supervisorId, HandoverApprovalRequest request);

  HandoverTask allocateDevice(String allocatorId, HandoverAllocationRequest request);

  HandoverTask confirmHandover(String receiverId, String taskId);

  HandoverTask rejectHandover(String receiverId, HandoverRejectRequest request);

  SummaryRows summary(String userId, String scope);

  record LoginRequest(@NotBlank String account, @NotBlank String password) {
  }

  record SetupRequest(
      @NotBlank String name,
      @NotBlank String account,
      @NotBlank String password,
      @NotBlank String departmentName,
      @NotBlank String positionName
  ) {
  }

  record DeviceRequest(
      @NotBlank String type,
      @NotBlank String brand,
      @NotBlank String model,
      @NotBlank String phoneNumber,
      @NotBlank String operator
  ) {
  }

  record AccountRequest(
      @NotBlank String channel,
      @NotBlank String account,
      @NotBlank String password,
      @NotBlank String realNameStatus,
      @NotBlank String realName,
      @NotBlank String idCardNumber,
      @NotBlank String phoneNumber,
      @NotBlank String operator
  ) {
  }

  record EmployeeRequest(
      String id,
      @NotBlank String name,
      @NotBlank String gender,
      @PositiveOrZero Integer age,
      @NotBlank String departmentId,
      @NotBlank String positionId,
      @NotNull LocalDate hireDate,
      @NotBlank String status,
      String account,
      String password,
      String role,
      Boolean recycleReceiver,
      String operatorEmployeeId
  ) {
  }

  record ArchiveRequest(String id, @NotBlank String name, String departmentId) {
  }

  record ImportResult(
      int totalRows,
      int successRows,
      int createdRows,
      int skippedRows,
      List<String> errors
  ) {
  }

  record OpenAccountRequest(
      @NotBlank String employeeId,
      @NotBlank String account,
      @NotBlank String password,
      @NotBlank String role
  ) {
  }

  record TransferRequest(@NotBlank String deviceId, @NotBlank String employeeId) {
  }

  record ResignationRequest(String applicantNote) {
  }

  record HandoverApprovalRequest(
      @NotBlank String taskId,
      @NotBlank String targetType,
      @NotBlank String receiverEmployeeId
  ) {
  }

  record HandoverAllocationRequest(
      String assetType,
      String assetId,
      String deviceId,
      @NotBlank String receiverEmployeeId
  ) {
  }

  record HandoverRejectRequest(
      @NotBlank String taskId,
      @NotBlank String rejectReason
  ) {
  }

  record SupervisorDataScopeRequest(List<SupervisorDataScopeItem> scopes) {
  }

  record SupervisorDataScopeItem(
      @NotBlank String departmentId,
      Boolean allPositions,
      List<String> positionIds
  ) {
  }
}
