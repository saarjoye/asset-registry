package com.company.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.company.asset.entity.ChannelAccount;
import com.company.asset.entity.Department;
import com.company.asset.entity.DeviceAsset;
import com.company.asset.entity.Employee;
import com.company.asset.entity.HandoverTask;
import com.company.asset.entity.PhoneNumber;
import com.company.asset.entity.Position;
import com.company.asset.entity.RecycleRecord;
import com.company.asset.mapper.ChannelAccountMapper;
import com.company.asset.mapper.DepartmentMapper;
import com.company.asset.mapper.DeviceAssetMapper;
import com.company.asset.mapper.EmployeeMapper;
import com.company.asset.mapper.HandoverTaskMapper;
import com.company.asset.mapper.PhoneNumberMapper;
import com.company.asset.mapper.PositionMapper;
import com.company.asset.mapper.RecycleRecordMapper;
import com.company.asset.service.RegistryService;
import com.company.asset.service.SummaryRows;
import com.company.asset.service.SummaryRows.AccountSummaryRow;
import com.company.asset.service.SummaryRows.DeviceSummaryRow;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistryServiceImpl implements RegistryService {

  private final DepartmentMapper departmentMapper;
  private final PositionMapper positionMapper;
  private final EmployeeMapper employeeMapper;
  private final PhoneNumberMapper phoneNumberMapper;
  private final DeviceAssetMapper deviceAssetMapper;
  private final ChannelAccountMapper channelAccountMapper;
  private final HandoverTaskMapper handoverTaskMapper;
  private final RecycleRecordMapper recycleRecordMapper;

  public RegistryServiceImpl(
      DepartmentMapper departmentMapper,
      PositionMapper positionMapper,
      EmployeeMapper employeeMapper,
      PhoneNumberMapper phoneNumberMapper,
      DeviceAssetMapper deviceAssetMapper,
      ChannelAccountMapper channelAccountMapper,
      HandoverTaskMapper handoverTaskMapper,
      RecycleRecordMapper recycleRecordMapper
  ) {
    this.departmentMapper = departmentMapper;
    this.positionMapper = positionMapper;
    this.employeeMapper = employeeMapper;
    this.phoneNumberMapper = phoneNumberMapper;
    this.deviceAssetMapper = deviceAssetMapper;
    this.channelAccountMapper = channelAccountMapper;
    this.handoverTaskMapper = handoverTaskMapper;
    this.recycleRecordMapper = recycleRecordMapper;
  }

  private String nextId(String prefix) {
    return prefix + "_" + System.nanoTime();
  }

  private LocalDateTime now() {
    return LocalDateTime.now();
  }

  private String nextEmployeeNo() {
    String prefix = "RY" + LocalDate.now().toString().replace("-", "");
    QueryWrapper<Employee> qw = new QueryWrapper<>();
    qw.likeRight("employee_no", prefix);
    long count = employeeMapper.selectCount(qw);
    int index = (int) count + 1;
    String employeeNo = prefix + String.format("%04d", index);
    while (employeeMapper.selectCount(new QueryWrapper<Employee>().eq("employee_no", employeeNo)) > 0) {
      index += 1;
      employeeNo = prefix + String.format("%04d", index);
    }
    return employeeNo;
  }

  private Employee requireEmployee(String id) {
    Employee e = employeeMapper.selectById(id);
    if (e == null) {
      throw new IllegalArgumentException("employee not found: " + id);
    }
    return e;
  }

  private DeviceAsset requireDevice(String id) {
    DeviceAsset d = deviceAssetMapper.selectById(id);
    if (d == null) {
      throw new IllegalArgumentException("device not found: " + id);
    }
    return d;
  }

  private HandoverTask requireHandover(String id) {
    HandoverTask t = handoverTaskMapper.selectById(id);
    if (t == null) {
      throw new IllegalArgumentException("handover task not found: " + id);
    }
    return t;
  }

  @Override
  @Transactional
  public Employee initializeAdmin(SetupRequest request) {
    if (employeeMapper.selectCount(null) > 0) {
      throw new IllegalStateException("system already initialized");
    }
    Department department = new Department(nextId("dept"), request.departmentName());
    departmentMapper.insert(department);
    Position position = new Position(nextId("pos"), request.positionName());
    positionMapper.insert(position);

    Employee admin = new Employee();
    admin.setId(nextId("emp"));
    admin.setEmployeeNo(nextEmployeeNo());
    admin.setName(request.name());
    admin.setGender("男");
    admin.setAge(0);
    admin.setDepartmentId(department.getId());
    admin.setPositionId(position.getId());
    admin.setHireDate(LocalDate.now());
    admin.setStatus("在职");
    admin.setLoginAccount(request.account());
    admin.setLoginPasswordHash("{noop}" + request.password());
    admin.setRoleCode("admin");
    admin.setCreatedAt(now());
    admin.setUpdatedAt(now());
    employeeMapper.insert(admin);
    return admin;
  }

  @Override
  public Optional<Employee> login(LoginRequest request) {
    QueryWrapper<Employee> qw = new QueryWrapper<>();
    qw.eq("login_account", request.account());
    Employee e = employeeMapper.selectOne(qw);
    if (e == null) {
      return Optional.empty();
    }
    String expected = "{noop}" + request.password();
    if (!expected.equals(e.getLoginPasswordHash())) {
      return Optional.empty();
    }
    if ("离职".equals(e.getStatus())) {
      return Optional.empty();
    }
    return Optional.of(e);
  }

  @Override
  public List<Department> departments() {
    return departmentMapper.selectList(null);
  }

  @Override
  public List<Position> positions() {
    return positionMapper.selectList(null);
  }

  @Override
  public List<Employee> employees() {
    return employeeMapper.selectList(null);
  }

  @Override
  public List<PhoneNumber> phones() {
    return phoneNumberMapper.selectList(null);
  }

  @Override
  public List<DeviceAsset> devices() {
    return deviceAssetMapper.selectList(null);
  }

  @Override
  public List<ChannelAccount> accounts() {
    return channelAccountMapper.selectList(null);
  }

  @Override
  public List<HandoverTask> handoverTasks() {
    return handoverTaskMapper.selectList(null);
  }

  @Override
  @Transactional
  public DeviceAsset registerDevice(String employeeId, DeviceRequest request) {
    Employee employee = requireEmployee(employeeId);
    PhoneNumber phone = ensurePhone(employeeId, request.phoneNumber(), request.operator());
    DeviceAsset device = new DeviceAsset();
    device.setId(nextId("dev"));
    device.setEmployeeId(employee.getId());
    device.setDepartmentId(employee.getDepartmentId());
    device.setPhoneId(phone.getId());
    device.setDeviceType(request.type());
    device.setBrand(request.brand());
    device.setModel(request.model());
    device.setStatus("在用");
    device.setRegisteredAt(LocalDate.now());
    device.setCreatedAt(now());
    device.setUpdatedAt(now());
    deviceAssetMapper.insert(device);
    return device;
  }

  @Override
  @Transactional
  public ChannelAccount registerAccount(String employeeId, AccountRequest request) {
    PhoneNumber phone = ensurePhone(employeeId, request.phoneNumber(), request.operator());
    ChannelAccount account = new ChannelAccount();
    account.setId(nextId("acct"));
    account.setEmployeeId(employeeId);
    account.setPhoneId(phone.getId());
    account.setChannel(request.channel());
    account.setAccountName(request.account());
    account.setAccountPasswordCipher("encrypted:" + request.password());
    account.setRealNameStatus(request.realNameStatus());
    account.setRealName(request.realName());
    account.setIdCardCipher(request.idCardNumber());
    account.setStatus("在用");
    account.setRegisteredAt(LocalDate.now());
    account.setCreatedAt(now());
    account.setUpdatedAt(now());
    channelAccountMapper.insert(account);
    return account;
  }

  @Override
  @Transactional
  public Employee saveEmployee(EmployeeRequest request, boolean includeAccount) {
    int normalizedAge = request.age() == null ? 0 : request.age();
    LocalDateTime ts = now();

    if (request.id() == null || request.id().isBlank()) {
      Employee created = new Employee();
      created.setId(nextId("emp"));
      created.setEmployeeNo(nextEmployeeNo());
      created.setName(request.name());
      created.setGender(request.gender());
      created.setAge(normalizedAge);
      created.setDepartmentId(request.departmentId());
      created.setPositionId(request.positionId());
      created.setHireDate(request.hireDate());
      created.setStatus(request.status());
      if (includeAccount) {
        created.setLoginAccount(request.account());
        created.setLoginPasswordHash("{noop}" + (request.password() == null || request.password().isBlank() ? "123456" : request.password()));
        created.setRoleCode(request.role());
      } else {
        created.setLoginAccount("");
        created.setLoginPasswordHash("{noop}123456");
        created.setRoleCode("employee");
      }
      created.setCreatedAt(ts);
      created.setUpdatedAt(ts);
      employeeMapper.insert(created);
      return created;
    }

    Employee current = requireEmployee(request.id());
    current.setName(request.name());
    current.setGender(request.gender());
    current.setAge(normalizedAge);
    current.setDepartmentId(request.departmentId());
    current.setPositionId(request.positionId());
    current.setHireDate(request.hireDate());
    current.setStatus(request.status());
    if (includeAccount) {
      current.setLoginAccount(request.account());
      if (request.password() != null && !request.password().isBlank()) {
        current.setLoginPasswordHash("{noop}" + request.password());
      }
      current.setRoleCode(request.role());
    }
    current.setUpdatedAt(ts);
    employeeMapper.updateById(current);

    if ("离职".equals(request.status())) {
      markDepartedDevices(current, request.operatorEmployeeId());
    }
    return current;
  }

  @Override
  @Transactional
  public Department saveDepartment(ArchiveRequest request) {
    LocalDateTime ts = now();
    if (request.id() == null || request.id().isBlank()) {
      Department created = new Department(nextId("dept"), request.name());
      departmentMapper.insert(created);
      return created;
    }
    Department d = departmentMapper.selectById(request.id());
    if (d == null) {
      throw new IllegalArgumentException("department not found");
    }
    d.setName(request.name());
    departmentMapper.updateById(d);
    return d;
  }

  @Override
  @Transactional
  public Position savePosition(ArchiveRequest request) {
    if (request.id() == null || request.id().isBlank()) {
      Position created = new Position(nextId("pos"), request.name());
      positionMapper.insert(created);
      return created;
    }
    Position p = positionMapper.selectById(request.id());
    if (p == null) {
      throw new IllegalArgumentException("position not found");
    }
    p.setName(request.name());
    positionMapper.updateById(p);
    return p;
  }

  @Override
  @Transactional
  public Employee openAccount(OpenAccountRequest request) {
    Employee e = requireEmployee(request.employeeId());
    e.setLoginAccount(request.account());
    e.setLoginPasswordHash("{noop}" + request.password());
    e.setRoleCode(request.role());
    e.setUpdatedAt(now());
    employeeMapper.updateById(e);
    return e;
  }

  @Override
  @Transactional
  public DeviceAsset confirmRecycle(String supervisorId, String deviceId) {
    Employee supervisor = requireEmployee(supervisorId);
    DeviceAsset d = requireDevice(deviceId);
    if (!Objects.equals(d.getDepartmentId(), supervisor.getDepartmentId()) || !"待回收".equals(d.getStatus())) {
      throw new IllegalArgumentException("device cannot recycle");
    }
    d.setStatus("已回收");
    d.setUpdatedAt(now());
    deviceAssetMapper.updateById(d);
    insertRecycle(d, "CONFIRM", supervisorId);
    return d;
  }

  @Override
  @Transactional
  public DeviceAsset transferDevice(String supervisorId, TransferRequest request) {
    Employee supervisor = requireEmployee(supervisorId);
    Employee target = requireEmployee(request.employeeId());
    if (!Objects.equals(target.getDepartmentId(), supervisor.getDepartmentId())) {
      throw new IllegalArgumentException("target not in department");
    }
    DeviceAsset d = requireDevice(request.deviceId());
    if (!Objects.equals(d.getDepartmentId(), supervisor.getDepartmentId()) || "待回收".equals(d.getStatus())) {
      throw new IllegalArgumentException("device cannot transfer");
    }
    d.setEmployeeId(target.getId());
    d.setDepartmentId(target.getDepartmentId());
    d.setStatus("已移交");
    d.setUpdatedAt(now());
    deviceAssetMapper.updateById(d);
    insertRecycle(d, "TRANSFER", supervisorId);
    return d;
  }

  @Override
  @Transactional
  public DeviceAsset stockIn(String supervisorId, String deviceId) {
    Employee supervisor = requireEmployee(supervisorId);
    DeviceAsset d = requireDevice(deviceId);
    if (!Objects.equals(d.getDepartmentId(), supervisor.getDepartmentId()) || !"已回收".equals(d.getStatus())) {
      throw new IllegalArgumentException("device cannot stock in");
    }
    d.setEmployeeId("");
    d.setStatus("旧机入库");
    d.setUpdatedAt(now());
    deviceAssetMapper.updateById(d);
    insertRecycle(d, "STOCK_IN", supervisorId);
    return d;
  }

  @Override
  @Transactional
  public List<HandoverTask> submitResignation(String employeeId, ResignationRequest request) {
    Employee applicant = requireEmployee(employeeId);
    List<HandoverTask> submitted = new ArrayList<>();
    String note = request.applicantNote() == null ? "" : request.applicantNote();

    for (DeviceAsset d : deviceAssetMapper.selectList(new QueryWrapper<DeviceAsset>().eq("employee_id", applicant.getId()))) {
      submitOne(applicant, "设备", d.getId(), note).ifPresent(submitted::add);
    }
    for (PhoneNumber p : phoneNumberMapper.selectList(new QueryWrapper<PhoneNumber>().eq("employee_id", applicant.getId()))) {
      submitOne(applicant, "手机号", p.getId(), note).ifPresent(submitted::add);
    }
    for (ChannelAccount a : channelAccountMapper.selectList(new QueryWrapper<ChannelAccount>().eq("employee_id", applicant.getId()))) {
      submitOne(applicant, "账号", a.getId(), note).ifPresent(submitted::add);
    }

    if (!submitted.isEmpty()) {
      applicant.setStatus("离职申请中");
      applicant.setUpdatedAt(now());
      employeeMapper.updateById(applicant);
    }
    return submitted;
  }

  @Override
  @Transactional
  public HandoverTask approveHandover(String supervisorId, HandoverApprovalRequest request) {
    Employee supervisor = requireEmployee(supervisorId);
    HandoverTask task = requireHandover(request.taskId());
    Employee receiver = requireEmployee(request.receiverEmployeeId());

    if (!"supervisor".equals(supervisor.getRoleCode())
        || !Objects.equals(task.getSourceDepartmentId(), supervisor.getDepartmentId())
        || !"待主管审批".equals(task.getStatus())
        || Objects.equals(receiver.getId(), task.getApplicantId())
        || !"在职".equals(receiver.getStatus())) {
      throw new IllegalArgumentException("handover cannot approve");
    }
    if ("本部门员工".equals(request.targetType())
        && !Objects.equals(receiver.getDepartmentId(), supervisor.getDepartmentId())) {
      throw new IllegalArgumentException("receiver not in department");
    }
    if ("其它部门员工".equals(request.targetType())
        && Objects.equals(receiver.getDepartmentId(), supervisor.getDepartmentId())) {
      throw new IllegalArgumentException("receiver must be another department");
    }

    task.setTargetType(request.targetType());
    task.setReceiverEmployeeId(receiver.getId());
    task.setReceiverDepartmentId(receiver.getDepartmentId());
    task.setApprovedById(supervisor.getId());
    task.setApprovedByName(supervisor.getName());
    task.setStatus("待接收确认");
    task.setUpdatedAt(now());
    handoverTaskMapper.updateById(task);
    setAssetStatus(task.getAssetType(), task.getAssetId(), "接收待确认");
    return task;
  }

  @Override
  @Transactional
  public HandoverTask allocateDevice(String allocatorId, HandoverAllocationRequest request) {
    Employee allocator = requireEmployee(allocatorId);
    Employee receiver = requireEmployee(request.receiverEmployeeId());
    String assetType = normalizeAssetType(request.assetType());
    String assetId = normalizeAssetId(request.assetId(), request.deviceId());

    if (!List.of("admin", "supervisor").contains(allocator.getRoleCode())
        || !assetOwnedBy(assetType, assetId, allocator.getId())
        || Objects.equals(receiver.getId(), allocator.getId())
        || !"在职".equals(receiver.getStatus())
        || hasOpenAssetHandoverTask(assetType, assetId)) {
      throw new IllegalArgumentException("asset cannot allocate");
    }
    if ("supervisor".equals(allocator.getRoleCode())
        && !Objects.equals(receiver.getDepartmentId(), allocator.getDepartmentId())) {
      throw new IllegalArgumentException("receiver not in department");
    }

    HandoverTask created = new HandoverTask();
    created.setId(nextId("handover"));
    created.setApplicantId(allocator.getId());
    created.setSourceDepartmentId(allocator.getDepartmentId());
    created.setDeviceId("设备".equals(assetType) ? assetId : "");
    created.setAssetType(assetType);
    created.setAssetId(assetId);
    created.setTargetType("资产分配");
    created.setReceiverEmployeeId(receiver.getId());
    created.setReceiverDepartmentId(receiver.getDepartmentId());
    created.setApprovedById(allocator.getId());
    created.setApprovedByName(allocator.getName());
    created.setStatus("待接收确认");
    created.setApplicantNote("");
    created.setRejectReason("");
    created.setCreatedAt(now());
    created.setUpdatedAt(now());
    handoverTaskMapper.insert(created);
    setAssetStatus(assetType, assetId, "接收待确认");
    return created;
  }

  @Override
  @Transactional
  public HandoverTask confirmHandover(String receiverId, String taskId) {
    Employee receiver = requireEmployee(receiverId);
    HandoverTask task = requireHandover(taskId);

    if (!Objects.equals(task.getReceiverEmployeeId(), receiver.getId()) || !"待接收确认".equals(task.getStatus())) {
      throw new IllegalArgumentException("handover cannot confirm");
    }
    task.setStatus("已完成");
    task.setRejectReason("");
    task.setUpdatedAt(now());
    handoverTaskMapper.updateById(task);

    DeviceAsset device = "设备".equals(task.getAssetType()) ? requireDevice(task.getAssetId()) : null;
    if (device != null) {
      device.setEmployeeId(receiver.getId());
      device.setDepartmentId(receiver.getDepartmentId());
      device.setStatus("回收入库".equals(task.getTargetType()) ? "旧机入库" : "在用");
      device.setUpdatedAt(now());
      deviceAssetMapper.updateById(device);
    } else {
      setAssetStatus(task.getAssetType(), task.getAssetId(), "在用");
      reassignAsset(task.getAssetType(), task.getAssetId(), receiver.getId());
    }

    if (!"资产分配".equals(task.getTargetType()) && !hasOpenApplicantHandoverTask(task.getApplicantId())) {
      Employee applicant = requireEmployee(task.getApplicantId());
      applicant.setStatus("离职");
      applicant.setUpdatedAt(now());
      employeeMapper.updateById(applicant);
    }
    return task;
  }

  @Override
  @Transactional
  public HandoverTask rejectHandover(String receiverId, HandoverRejectRequest request) {
    Employee receiver = requireEmployee(receiverId);
    HandoverTask task = requireHandover(request.taskId());
    if (!Objects.equals(task.getReceiverEmployeeId(), receiver.getId()) || !"待接收确认".equals(task.getStatus())) {
      throw new IllegalArgumentException("handover cannot reject");
    }
    task.setStatus("已回退");
    task.setRejectReason(request.rejectReason());
    task.setUpdatedAt(now());
    handoverTaskMapper.updateById(task);

    DeviceAsset device = "设备".equals(task.getAssetType()) ? requireDevice(task.getAssetId()) : null;
    if (device != null) {
      device.setStatus("在用");
      device.setUpdatedAt(now());
      deviceAssetMapper.updateById(device);
    } else {
      setAssetStatus(task.getAssetType(), task.getAssetId(), "在用");
    }
    if (!"资产分配".equals(task.getTargetType())) {
      Employee applicant = requireEmployee(task.getApplicantId());
      applicant.setStatus("离职申请中");
      applicant.setUpdatedAt(now());
      employeeMapper.updateById(applicant);
    }
    return task;
  }

  @Override
  public SummaryRows summary(String userId, String scope) {
    Employee user = requireEmployee(userId);
    List<Employee> scopedEmployees = employeeMapper.selectList(null).stream()
        .filter(e -> switch (scope) {
          case "all" -> true;
          case "department" -> Objects.equals(e.getDepartmentId(), user.getDepartmentId());
          default -> Objects.equals(e.getId(), user.getId());
        })
        .toList();
    List<String> scopedEmployeeIds = scopedEmployees.stream().map(Employee::getId).toList();
    List<Employee> allEmployees = employeeMapper.selectList(null);

    List<DeviceSummaryRow> deviceRows = deviceAssetMapper.selectList(null).stream()
        .filter(d -> {
          if ("all".equals(scope)) return true;
          if ("department".equals(scope)) return Objects.equals(d.getDepartmentId(), user.getDepartmentId());
          return scopedEmployeeIds.contains(d.getEmployeeId());
        })
        .map(d -> {
          Employee owner = d.getEmployeeId() == null ? null : employeeMapper.selectById(d.getEmployeeId());
          PhoneNumber phone = d.getPhoneId() == null ? null : phoneNumberMapper.selectById(d.getPhoneId());
          HandoverTask sourceTask = latestCompletedHandoverTask(d.getId());
          Employee sourceEmployee = sourceTask == null ? null : employeeMapper.selectById(sourceTask.getApplicantId());
          HandoverTask receiveTask = latestCompletedHandoverTaskExceptType(d.getId(), "设备分配");
          HandoverTask allocationTask = latestCompletedHandoverTaskByType(d.getId(), "设备分配");
          String linkedAccounts = channelAccountMapper.selectList(
              new QueryWrapper<ChannelAccount>()
                  .eq("employee_id", d.getEmployeeId())
                  .eq("phone_id", d.getPhoneId())
          ).stream().map(a -> a.getChannel() + ":" + a.getAccountName()).collect(Collectors.joining(" / "));
          return SummaryRows.toDeviceRow(
              d,
              owner == null ? "" : owner.getEmployeeNo(),
              owner == null ? "库房" : owner.getName(),
              sourceTask == null ? "" : departmentName(sourceTask.getSourceDepartmentId()),
              sourceEmployee == null ? "" : sourceEmployee.getName() + " / " + sourceEmployee.getEmployeeNo(),
              acquisitionType(sourceTask),
              receiveTask == null ? "" : receiveTask.getUpdatedAt() == null ? "" : receiveTask.getUpdatedAt().toString().substring(0, 10),
              allocationTask == null ? "" : allocationTask.getUpdatedAt() == null ? "" : allocationTask.getUpdatedAt().toString().substring(0, 10),
              phone == null ? "" : phone.getPhoneNumber(),
              phone == null ? "" : phone.getOperator(),
              linkedAccounts
          );
        })
        .toList();

    List<AccountSummaryRow> accountRows = channelAccountMapper.selectList(null).stream()
        .filter(a -> scopedEmployeeIds.contains(a.getEmployeeId()))
        .map(a -> {
          Employee owner = employeeMapper.selectById(a.getEmployeeId());
          PhoneNumber phone = phoneNumberMapper.selectById(a.getPhoneId());
          return SummaryRows.toAccountRow(
              a,
              owner == null ? "" : owner.getEmployeeNo(),
              owner == null ? "" : owner.getName(),
              phone == null ? "" : phone.getPhoneNumber(),
              phone == null ? "" : phone.getOperator()
          );
        })
        .toList();

    return new SummaryRows(deviceRows, accountRows);
  }

  private PhoneNumber ensurePhone(String employeeId, String number, String operator) {
    PhoneNumber existing = phoneNumberMapper.selectOne(
        new QueryWrapper<PhoneNumber>().eq("employee_id", employeeId).eq("phone_number", number)
    );
    if (existing != null) {
      existing.setOperator(operator);
      existing.setUpdatedAt(now());
      phoneNumberMapper.updateById(existing);
      return existing;
    }
    PhoneNumber created = new PhoneNumber();
    created.setId(nextId("phone"));
    created.setEmployeeId(employeeId);
    created.setPhoneNumber(number);
    created.setOperator(operator);
    created.setStatus("在用");
    created.setRegisteredAt(LocalDate.now());
    created.setCreatedAt(now());
    created.setUpdatedAt(now());
    phoneNumberMapper.insert(created);
    return created;
  }

  private Optional<HandoverTask> submitOne(Employee applicant, String assetType, String assetId, String note) {
    if (hasOpenAssetHandoverTask(assetType, assetId)) {
      return Optional.empty();
    }
    QueryWrapper<HandoverTask> qw = new QueryWrapper<>();
    qw.eq("applicant_id", applicant.getId())
        .eq("asset_type", assetType)
        .eq("asset_id", assetId)
        .eq("status", "已回退")
        .orderByDesc("updated_at");
    HandoverTask rejected = handoverTaskMapper.selectList(qw).stream().findFirst().orElse(null);
    if (rejected != null) {
      rejected.setStatus("待主管审批");
      rejected.setApplicantNote(note);
      rejected.setRejectReason("");
      rejected.setDeviceId("设备".equals(assetType) ? assetId : "");
      rejected.setTargetType("本部门员工");
      rejected.setReceiverEmployeeId("");
      rejected.setReceiverDepartmentId("");
      rejected.setApprovedById("");
      rejected.setApprovedByName("");
      rejected.setUpdatedAt(now());
      handoverTaskMapper.updateById(rejected);
      return Optional.of(rejected);
    }
    HandoverTask created = new HandoverTask();
    created.setId(nextId("handover"));
    created.setApplicantId(applicant.getId());
    created.setSourceDepartmentId(applicant.getDepartmentId());
    created.setDeviceId("设备".equals(assetType) ? assetId : "");
    created.setAssetType(assetType);
    created.setAssetId(assetId);
    created.setTargetType("本部门员工");
    created.setReceiverEmployeeId("");
    created.setReceiverDepartmentId("");
    created.setApprovedById("");
    created.setApprovedByName("");
    created.setStatus("待主管审批");
    created.setApplicantNote(note);
    created.setRejectReason("");
    created.setCreatedAt(now());
    created.setUpdatedAt(now());
    handoverTaskMapper.insert(created);
    return Optional.of(created);
  }

  private void markDepartedDevices(Employee employee, String operatorEmployeeId) {
    if (!"离职".equals(employee.getStatus())) {
      return;
    }
    Employee operator = operatorEmployeeId == null ? null : employeeMapper.selectById(operatorEmployeeId);
    String operatorId = operator == null ? "" : operator.getId();
    String operatorName = operator == null ? "系统" : operator.getName();

    List<DeviceAsset> owned = deviceAssetMapper.selectList(
        new QueryWrapper<DeviceAsset>().eq("employee_id", employee.getId()).eq("status", "在用")
    );
    for (DeviceAsset d : owned) {
      d.setStatus("待回收");
      d.setRecycleInitiatorEmployeeId(operatorId);
      d.setRecycleInitiatorName(operatorName);
      d.setRecycleSource("人员档案");
      d.setRecycleReason(employee.getName() + "状态变更为离职");
      d.setRecycleCreatedAt(now());
      d.setUpdatedAt(now());
      deviceAssetMapper.updateById(d);
    }
  }

  private boolean hasOpenAssetHandoverTask(String assetType, String assetId) {
    if (assetType == null || assetId == null) {
      return false;
    }
    Long count = handoverTaskMapper.selectCount(
        new QueryWrapper<HandoverTask>()
            .eq("asset_type", assetType)
            .eq("asset_id", assetId)
            .in("status", List.of("待主管审批", "待接收确认"))
    );
    return count != null && count > 0;
  }

  private boolean hasOpenApplicantHandoverTask(String applicantId) {
    Long count = handoverTaskMapper.selectCount(
        new QueryWrapper<HandoverTask>()
            .eq("applicant_id", applicantId)
            .in("status", List.of("待主管审批", "待接收确认", "已回退"))
    );
    return count != null && count > 0;
  }

  private void setAssetStatus(String assetType, String assetId, String status) {
    if (assetType == null || assetId == null) {
      return;
    }
    if ("设备".equals(assetType)) {
      DeviceAsset d = deviceAssetMapper.selectById(assetId);
      if (d != null) {
        d.setStatus(status);
        d.setUpdatedAt(now());
        deviceAssetMapper.updateById(d);
      }
      return;
    }
    if ("手机号".equals(assetType)) {
      PhoneNumber p = phoneNumberMapper.selectById(assetId);
      if (p != null) {
        p.setStatus(status);
        phoneNumberMapper.updateById(p);
      }
      return;
    }
    if ("账号".equals(assetType)) {
      ChannelAccount a = channelAccountMapper.selectById(assetId);
      if (a != null) {
        a.setStatus(status);
        channelAccountMapper.updateById(a);
      }
    }
  }

  private void reassignAsset(String assetType, String assetId, String newEmployeeId) {
    if (assetType == null || assetId == null) {
      return;
    }
    if ("手机号".equals(assetType)) {
      PhoneNumber p = phoneNumberMapper.selectById(assetId);
      if (p != null) {
        p.setEmployeeId(newEmployeeId);
        phoneNumberMapper.updateById(p);
      }
      return;
    }
    if ("账号".equals(assetType)) {
      ChannelAccount a = channelAccountMapper.selectById(assetId);
      if (a != null) {
        a.setEmployeeId(newEmployeeId);
        channelAccountMapper.updateById(a);
      }
    }
  }

  private String normalizeAssetType(String assetType) {
    if (assetType == null || assetType.isBlank()) {
      return "设备";
    }
    return assetType;
  }

  private String normalizeAssetId(String assetId, String deviceId) {
    if (assetId != null && !assetId.isBlank()) {
      return assetId;
    }
    return deviceId == null ? "" : deviceId;
  }

  private boolean assetOwnedBy(String assetType, String assetId, String employeeId) {
    if (assetType == null || assetId == null) {
      return false;
    }
    if ("设备".equals(assetType)) {
      DeviceAsset d = deviceAssetMapper.selectById(assetId);
      return d != null && Objects.equals(d.getEmployeeId(), employeeId);
    }
    if ("手机号".equals(assetType)) {
      PhoneNumber p = phoneNumberMapper.selectById(assetId);
      return p != null && Objects.equals(p.getEmployeeId(), employeeId);
    }
    if ("账号".equals(assetType)) {
      ChannelAccount a = channelAccountMapper.selectById(assetId);
      return a != null && Objects.equals(a.getEmployeeId(), employeeId);
    }
    return false;
  }

  private HandoverTask latestCompletedHandoverTask(String deviceId) {
    List<HandoverTask> list = handoverTaskMapper.selectList(
        new QueryWrapper<HandoverTask>()
            .eq("device_id", deviceId)
            .eq("status", "已完成")
            .orderByDesc("updated_at")
    );
    return list.isEmpty() ? null : list.get(0);
  }

  private HandoverTask latestCompletedHandoverTaskByType(String deviceId, String targetType) {
    List<HandoverTask> list = handoverTaskMapper.selectList(
        new QueryWrapper<HandoverTask>()
            .eq("device_id", deviceId)
            .eq("status", "已完成")
            .eq("target_type", targetType)
            .orderByDesc("updated_at")
    );
    return list.isEmpty() ? null : list.get(0);
  }

  private HandoverTask latestCompletedHandoverTaskExceptType(String deviceId, String targetType) {
    List<HandoverTask> list = handoverTaskMapper.selectList(
        new QueryWrapper<HandoverTask>()
            .eq("device_id", deviceId)
            .eq("status", "已完成")
            .ne("target_type", targetType)
            .orderByDesc("updated_at")
    );
    return list.isEmpty() ? null : list.get(0);
  }

  private String acquisitionType(HandoverTask task) {
    if (task == null) return "登记";
    return "设备分配".equals(task.getTargetType()) ? "分配" : "接收";
  }

  private String departmentName(String departmentId) {
    if (departmentId == null) return "";
    Department d = departmentMapper.selectById(departmentId);
    return d == null ? "" : d.getName();
  }

  private void insertRecycle(DeviceAsset d, String actionType, String operatorId) {
    RecycleRecord r = new RecycleRecord();
    r.setId(nextId("recycle"));
    r.setDeviceId(d.getId());
    r.setDepartmentId(d.getDepartmentId());
    r.setActionType(actionType);
    r.setActionTime(now());
    r.setOperatorEmployeeId(operatorId);
    recycleRecordMapper.insert(r);
  }
}
