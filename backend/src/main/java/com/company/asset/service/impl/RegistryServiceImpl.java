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
import com.company.asset.entity.SupervisorDataScope;
import com.company.asset.mapper.ChannelAccountMapper;
import com.company.asset.mapper.DepartmentMapper;
import com.company.asset.mapper.DeviceAssetMapper;
import com.company.asset.mapper.EmployeeMapper;
import com.company.asset.mapper.HandoverTaskMapper;
import com.company.asset.mapper.PhoneNumberMapper;
import com.company.asset.mapper.PositionMapper;
import com.company.asset.mapper.RecycleRecordMapper;
import com.company.asset.mapper.SupervisorDataScopeMapper;
import com.company.asset.service.RegistryService;
import com.company.asset.service.SummaryRows;
import com.company.asset.service.SummaryRows.AccountSummaryRow;
import com.company.asset.service.SummaryRows.DeviceSummaryRow;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RegistryServiceImpl implements RegistryService {
  private record EnsureResult<T>(T value, boolean created) {
  }

  private static class ImportAccumulator {
    private int totalRows;
    private int successRows;
    private int createdRows;
    private final List<String> errors = new ArrayList<>();

    private ImportResult toResult() {
      return new ImportResult(totalRows, successRows, createdRows, errors.size(), errors);
    }
  }

  private final DepartmentMapper departmentMapper;
  private final PositionMapper positionMapper;
  private final EmployeeMapper employeeMapper;
  private final PhoneNumberMapper phoneNumberMapper;
  private final DeviceAssetMapper deviceAssetMapper;
  private final ChannelAccountMapper channelAccountMapper;
  private final HandoverTaskMapper handoverTaskMapper;
  private final RecycleRecordMapper recycleRecordMapper;
  private final SupervisorDataScopeMapper supervisorDataScopeMapper;

  public RegistryServiceImpl(
      DepartmentMapper departmentMapper,
      PositionMapper positionMapper,
      EmployeeMapper employeeMapper,
      PhoneNumberMapper phoneNumberMapper,
      DeviceAssetMapper deviceAssetMapper,
      ChannelAccountMapper channelAccountMapper,
      HandoverTaskMapper handoverTaskMapper,
      RecycleRecordMapper recycleRecordMapper,
      SupervisorDataScopeMapper supervisorDataScopeMapper
  ) {
    this.departmentMapper = departmentMapper;
    this.positionMapper = positionMapper;
    this.employeeMapper = employeeMapper;
    this.phoneNumberMapper = phoneNumberMapper;
    this.deviceAssetMapper = deviceAssetMapper;
    this.channelAccountMapper = channelAccountMapper;
    this.handoverTaskMapper = handoverTaskMapper;
    this.recycleRecordMapper = recycleRecordMapper;
    this.supervisorDataScopeMapper = supervisorDataScopeMapper;
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
    Position position = new Position(nextId("pos"), department.getId(), request.positionName());
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
    admin.setRecycleReceiver(true);
    admin.setCreatedAt(now());
    admin.setUpdatedAt(now());
    employeeMapper.insert(admin);
    return admin;
  }

  @Override
  public boolean setupRequired() {
    return employeeMapper.selectCount(null) == 0;
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
  public Employee employee(String employeeId) {
    return requireEmployee(employeeId);
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
  public List<Employee> employees(String viewerId) {
    if (viewerId == null || viewerId.isBlank()) {
      return List.of();
    }
    Employee viewer = requireEmployee(viewerId);
    return visibleEmployeesForArchive(viewer);
  }

  @Override
  public List<PhoneNumber> phones(String viewerId) {
    if (viewerId == null || viewerId.isBlank()) {
      return List.of();
    }
    Set<String> visibleEmployeeIds = visibleEmployeesForData(requireEmployee(viewerId)).stream()
        .map(Employee::getId)
        .collect(Collectors.toSet());
    return phoneNumberMapper.selectList(null).stream()
        .filter(p -> visibleEmployeeIds.contains(p.getEmployeeId()))
        .toList();
  }

  @Override
  public List<DeviceAsset> devices(String viewerId) {
    if (viewerId == null || viewerId.isBlank()) {
      return List.of();
    }
    Employee viewer = requireEmployee(viewerId);
    Set<String> visibleEmployeeIds = visibleEmployeesForData(viewer).stream()
        .map(Employee::getId)
        .collect(Collectors.toSet());
    return deviceAssetMapper.selectList(null).stream()
        .filter(d -> visibleDevice(viewer, visibleEmployeeIds, d))
        .toList();
  }

  @Override
  public List<ChannelAccount> accounts(String viewerId) {
    if (viewerId == null || viewerId.isBlank()) {
      return List.of();
    }
    Set<String> visibleEmployeeIds = visibleEmployeesForData(requireEmployee(viewerId)).stream()
        .map(Employee::getId)
        .collect(Collectors.toSet());
    return channelAccountMapper.selectList(null).stream()
        .filter(a -> visibleEmployeeIds.contains(a.getEmployeeId()))
        .toList();
  }

  @Override
  public List<SupervisorDataScope> supervisorDataScopesForAdmin(String operatorId) {
    Employee operator = requireEmployee(operatorId);
    if (!"admin".equals(operator.getRoleCode())) {
      throw new IllegalArgumentException("only admin can view data scope");
    }
    return supervisorDataScopeMapper.selectList(null);
  }

  @Override
  public List<SupervisorDataScope> supervisorDataScopes(String supervisorId) {
    return supervisorDataScopeMapper.selectList(
        new QueryWrapper<SupervisorDataScope>().eq("supervisor_id", supervisorId)
    );
  }

  @Override
  @Transactional
  public List<SupervisorDataScope> saveSupervisorDataScopes(
      String operatorId,
      String supervisorId,
      SupervisorDataScopeRequest request
  ) {
    Employee operator = requireEmployee(operatorId);
    if (!"admin".equals(operator.getRoleCode())) {
      throw new IllegalArgumentException("only admin can configure data scope");
    }
    Employee supervisor = requireEmployee(supervisorId);
    if (!"supervisor".equals(supervisor.getRoleCode())) {
      throw new IllegalArgumentException("target employee is not supervisor");
    }
    supervisorDataScopeMapper.delete(new QueryWrapper<SupervisorDataScope>().eq("supervisor_id", supervisorId));
    LocalDateTime time = now();
    if (request != null && request.scopes() != null) {
      for (SupervisorDataScopeItem item : request.scopes()) {
        String departmentId = normalizeText(item.departmentId());
        if (departmentId == null || departmentMapper.selectById(departmentId) == null) {
          throw new IllegalArgumentException("department not found");
        }
        if (Boolean.TRUE.equals(item.allPositions())) {
          insertSupervisorDataScope(supervisorId, departmentId, null, true, time);
          continue;
        }
        List<String> positionIds = item.positionIds() == null ? List.of() : item.positionIds();
        for (String rawPositionId : positionIds.stream().filter(Objects::nonNull).distinct().toList()) {
          String positionId = normalizeText(rawPositionId);
          Position position = positionId == null ? null : positionMapper.selectById(positionId);
          if (position == null || !Objects.equals(position.getDepartmentId(), departmentId)) {
            throw new IllegalArgumentException("position not found in department");
          }
          insertSupervisorDataScope(supervisorId, departmentId, positionId, false, time);
        }
      }
    }
    return supervisorDataScopes(supervisorId);
  }

  private void insertSupervisorDataScope(
      String supervisorId,
      String departmentId,
      String positionId,
      boolean allPositions,
      LocalDateTime time
  ) {
    SupervisorDataScope scope = new SupervisorDataScope();
    scope.setId(nextId("scope"));
    scope.setSupervisorId(supervisorId);
    scope.setDepartmentId(departmentId);
    scope.setPositionId(positionId);
    scope.setAllPositions(allPositions);
    scope.setCreatedAt(time);
    scope.setUpdatedAt(time);
    supervisorDataScopeMapper.insert(scope);
  }

  private List<Employee> visibleEmployeesForArchive(Employee viewer) {
    if ("admin".equals(viewer.getRoleCode()) || "hr".equals(viewer.getRoleCode())) {
      return employeeMapper.selectList(null);
    }
    if ("supervisor".equals(viewer.getRoleCode())) {
      return employeeMapper.selectList(null).stream()
          .filter(e -> Objects.equals(e.getId(), viewer.getId())
              || matchesSupervisorScope(viewer.getId(), e)
              || Boolean.TRUE.equals(e.getRecycleReceiver()))
          .toList();
    }
    return employeeMapper.selectList(null).stream()
        .filter(e -> Objects.equals(e.getId(), viewer.getId()))
        .toList();
  }

  private List<Employee> visibleEmployeesForData(Employee viewer) {
    if ("admin".equals(viewer.getRoleCode())) {
      return employeeMapper.selectList(null);
    }
    if ("supervisor".equals(viewer.getRoleCode())) {
      return employeeMapper.selectList(null).stream()
          .filter(e -> Objects.equals(e.getId(), viewer.getId()) || matchesSupervisorScope(viewer.getId(), e))
          .toList();
    }
    return employeeMapper.selectList(null).stream()
        .filter(e -> Objects.equals(e.getId(), viewer.getId()))
        .toList();
  }

  private boolean matchesSupervisorScope(String supervisorId, Employee target) {
    if (target == null) {
      return false;
    }
    Employee supervisor = employeeMapper.selectById(supervisorId);
    if (supervisor != null && Objects.equals(supervisor.getDepartmentId(), target.getDepartmentId())) {
      return true;
    }
    return supervisorDataScopes(supervisorId).stream().anyMatch(scope ->
        Objects.equals(scope.getDepartmentId(), target.getDepartmentId())
            && (Boolean.TRUE.equals(scope.getAllPositions())
            || Objects.equals(scope.getPositionId(), target.getPositionId()))
    );
  }

  private boolean departmentAllowedForSupervisor(String supervisorId, String departmentId) {
    Employee supervisor = employeeMapper.selectById(supervisorId);
    if (supervisor != null && Objects.equals(supervisor.getDepartmentId(), departmentId)) {
      return true;
    }
    return supervisorDataScopes(supervisorId).stream()
        .anyMatch(scope -> Objects.equals(scope.getDepartmentId(), departmentId));
  }

  private boolean visibleDevice(Employee viewer, Set<String> visibleEmployeeIds, DeviceAsset device) {
    if ("admin".equals(viewer.getRoleCode())) {
      return true;
    }
    if (device.getEmployeeId() != null && visibleEmployeeIds.contains(device.getEmployeeId())) {
      return true;
    }
    return "supervisor".equals(viewer.getRoleCode())
        && device.getEmployeeId() == null
        && departmentAllowedForSupervisor(viewer.getId(), device.getDepartmentId());
  }

  @Override
  public List<HandoverTask> handoverTasks(String viewerId) {
    if (viewerId == null || viewerId.isBlank()) {
      return List.of();
    }
    Employee viewer = requireEmployee(viewerId);
    if ("admin".equals(viewer.getRoleCode())) {
      return handoverTaskMapper.selectList(null);
    }
    Set<String> visibleEmployeeIds = visibleEmployeesForData(viewer).stream()
        .map(Employee::getId)
        .collect(Collectors.toSet());
    return handoverTaskMapper.selectList(null).stream()
        .filter(task -> visibleHandoverTask(viewer, visibleEmployeeIds, task))
        .toList();
  }

  private boolean visibleHandoverTask(Employee viewer, Set<String> visibleEmployeeIds, HandoverTask task) {
    if (Objects.equals(task.getApplicantId(), viewer.getId())
        || Objects.equals(task.getReceiverEmployeeId(), viewer.getId())
        || Objects.equals(task.getApprovedById(), viewer.getId())) {
      return true;
    }
    if ("supervisor".equals(viewer.getRoleCode())) {
      return visibleEmployeeIds.contains(task.getApplicantId())
          || visibleEmployeeIds.contains(task.getReceiverEmployeeId())
          || departmentAllowedForSupervisor(viewer.getId(), task.getSourceDepartmentId())
          || departmentAllowedForSupervisor(viewer.getId(), task.getReceiverDepartmentId());
    }
    return false;
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
      created.setRecycleReceiver(Boolean.TRUE.equals(request.recycleReceiver()));
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
    current.setRecycleReceiver(Boolean.TRUE.equals(request.recycleReceiver()));
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
    String departmentId = normalizeText(request.departmentId());
    if (departmentId == null) {
      throw new IllegalArgumentException("departmentId is required");
    }
    if (departmentMapper.selectById(departmentId) == null) {
      throw new IllegalArgumentException("department not found");
    }
    if (request.id() == null || request.id().isBlank()) {
      Position created = new Position(nextId("pos"), departmentId, request.name());
      positionMapper.insert(created);
      return created;
    }
    Position p = positionMapper.selectById(request.id());
    if (p == null) {
      throw new IllegalArgumentException("position not found");
    }
    p.setDepartmentId(departmentId);
    p.setName(request.name());
    positionMapper.updateById(p);
    return p;
  }

  @Override
  @Transactional
  public ImportResult importDepartmentsAndPositions(MultipartFile file) {
    validateImportFile(file);
    ImportAccumulator acc = new ImportAccumulator();
    try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
      Sheet departmentSheet = findSheet(workbook, "部门");
      if (departmentSheet != null) {
        importDepartmentSheet(departmentSheet, acc);
      }
      Sheet positionSheet = findSheet(workbook, "岗位");
      if (positionSheet == null) {
        positionSheet = firstSheetWithHeaders(workbook, "职位名称", "岗位名称");
      }
      if (positionSheet == null) {
        acc.errors.add("未找到“岗位”Sheet，或首行缺少“职位名称/岗位名称”表头");
      } else {
        importPositionSheet(positionSheet, acc);
      }
      return acc.toResult();
    } catch (Exception e) {
      throw new IllegalArgumentException("导入失败：" + e.getMessage(), e);
    }
  }

  @Override
  @Transactional
  public ImportResult importEmployees(MultipartFile file) {
    validateImportFile(file);
    ImportAccumulator acc = new ImportAccumulator();
    try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
      Sheet sheet = findSheet(workbook, "人员");
      if (sheet == null) {
        sheet = firstSheetWithHeaders(workbook, "姓名", "用户名");
      }
      if (sheet == null) {
        acc.errors.add("未找到“人员”Sheet，或首行缺少“姓名/用户名”表头");
        return acc.toResult();
      }
      importEmployeeSheet(sheet, acc);
      return acc.toResult();
    } catch (Exception e) {
      throw new IllegalArgumentException("导入失败：" + e.getMessage(), e);
    }
  }

  @Override
  public byte[] departmentPositionImportTemplate() {
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet departments = workbook.createSheet("部门");
      writeHeader(departments, "部门名称");
      Sheet positions = workbook.createSheet("岗位");
      writeHeader(positions, "职位名称", "所属部门");
      Sheet instructions = workbook.createSheet("填写说明");
      writeRows(
          instructions,
          List.of(
              List.of("模板用途", "部门岗位导入"),
              List.of("部门Sheet", "只填写部门名称，一行一个部门"),
              List.of("岗位Sheet", "职位名称和所属部门都必填；所属部门不存在时系统会自动创建"),
              List.of("注意", "请不要修改Sheet名称和表头名称")
          )
      );
      return workbookBytes(workbook);
    } catch (Exception e) {
      throw new IllegalArgumentException("生成模板失败：" + e.getMessage(), e);
    }
  }

  @Override
  public byte[] employeeImportTemplate() {
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet employees = workbook.createSheet("人员");
      writeHeader(employees, "姓名", "性别", "入职时间", "用户名", "职位", "部门");
      Sheet instructions = workbook.createSheet("填写说明");
      writeRows(
          instructions,
          List.of(
              List.of("模板用途", "人员档案导入"),
              List.of("必填字段", "姓名、性别、入职时间、用户名、职位、部门"),
              List.of("性别", "只能填写 男 或 女"),
              List.of("入职时间", "建议格式 yyyy-MM-dd，例如 2026-06-04"),
              List.of("用户名", "作为登录账号，重复用户名会跳过"),
              List.of("默认值", "年龄=0，状态=在职，角色=员工，初始密码=123456"),
              List.of("注意", "请不要修改Sheet名称和表头名称")
          )
      );
      return workbookBytes(workbook);
    } catch (Exception e) {
      throw new IllegalArgumentException("生成模板失败：" + e.getMessage(), e);
    }
  }

  @Override
  @Transactional
  public Employee openAccount(OpenAccountRequest request) {
    Employee e = requireEmployee(request.employeeId());
    e.setLoginAccount(request.account());
    e.setLoginPasswordHash("{noop}" + request.password());
    e.setRoleCode(request.role());
    if ("admin".equals(request.role()) && e.getRecycleReceiver() == null) {
      e.setRecycleReceiver(true);
    }
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
    if ("回收入库".equals(request.targetType()) && !Boolean.TRUE.equals(receiver.getRecycleReceiver())) {
      throw new IllegalArgumentException("receiver is not recycle receiver");
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
    List<Employee> scopedEmployees = visibleEmployeesForData(user);
    List<String> scopedEmployeeIds = scopedEmployees.stream().map(Employee::getId).toList();
    Set<String> scopedEmployeeIdSet = Set.copyOf(scopedEmployeeIds);
    List<Employee> allEmployees = employeeMapper.selectList(null);

    List<DeviceSummaryRow> deviceRows = deviceAssetMapper.selectList(null).stream()
        .filter(d -> visibleDevice(user, scopedEmployeeIdSet, d))
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
              sourceTask == null ? registeredSourceDepartment(d, owner) : departmentName(sourceTask.getSourceDepartmentId()),
              sourceTask == null ? registeredSourceEmployee(owner) : sourceEmployee == null ? "" : sourceEmployee.getName() + " / " + sourceEmployee.getEmployeeNo(),
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

  private String registeredSourceDepartment(DeviceAsset device, Employee owner) {
    if (owner != null) {
      return departmentName(owner.getDepartmentId());
    }
    return device == null ? "" : departmentName(device.getDepartmentId());
  }

  private String registeredSourceEmployee(Employee owner) {
    return owner == null ? "" : owner.getName() + " / " + owner.getEmployeeNo();
  }

  private void writeHeader(Sheet sheet, String... headers) {
    Row row = sheet.createRow(0);
    for (int i = 0; i < headers.length; i++) {
      row.createCell(i).setCellValue(headers[i]);
      sheet.autoSizeColumn(i);
    }
    sheet.createFreezePane(0, 1);
  }

  private void writeRows(Sheet sheet, List<List<String>> rows) {
    for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
      Row row = sheet.createRow(rowIndex);
      List<String> values = rows.get(rowIndex);
      for (int columnIndex = 0; columnIndex < values.size(); columnIndex++) {
        row.createCell(columnIndex).setCellValue(values.get(columnIndex));
        sheet.autoSizeColumn(columnIndex);
      }
    }
  }

  private byte[] workbookBytes(Workbook workbook) {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      workbook.write(output);
      return output.toByteArray();
    } catch (Exception e) {
      throw new IllegalArgumentException("写入 Excel 失败：" + e.getMessage(), e);
    }
  }

  private void validateImportFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("请选择 Excel 文件");
    }
    String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
    if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
      throw new IllegalArgumentException("仅支持 .xlsx 或 .xls 文件");
    }
  }

  private void importDepartmentSheet(Sheet sheet, ImportAccumulator acc) {
    Map<String, Integer> headers = readHeaders(sheet);
    Integer departmentIndex = headerIndex(headers, "部门名称", "部门", "科室");
    if (departmentIndex == null) {
      acc.errors.add("部门Sheet缺少“部门名称”表头");
      return;
    }
    for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      Row row = sheet.getRow(rowIndex);
      if (isBlankRow(row)) continue;
      acc.totalRows += 1;
      String departmentName = cellText(row, departmentIndex);
      if (departmentName == null) {
        acc.errors.add(rowError(rowIndex, "部门名称不能为空"));
        continue;
      }
      EnsureResult<Department> department = ensureDepartment(departmentName);
      acc.successRows += 1;
      if (department.created()) acc.createdRows += 1;
    }
  }

  private void importPositionSheet(Sheet sheet, ImportAccumulator acc) {
    Map<String, Integer> headers = readHeaders(sheet);
    Integer positionIndex = headerIndex(headers, "职位名称", "岗位名称", "职位", "岗位");
    Integer departmentIndex = headerIndex(headers, "所属部门", "部门名称", "部门", "科室");
    if (positionIndex == null || departmentIndex == null) {
      acc.errors.add("岗位Sheet必须包含“职位名称”和“所属部门”表头");
      return;
    }
    for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      Row row = sheet.getRow(rowIndex);
      if (isBlankRow(row)) continue;
      acc.totalRows += 1;
      String positionName = cellText(row, positionIndex);
      String departmentName = cellText(row, departmentIndex);
      if (positionName == null || departmentName == null) {
        acc.errors.add(rowError(rowIndex, "职位名称和所属部门不能为空"));
        continue;
      }
      EnsureResult<Department> department = ensureDepartment(departmentName);
      EnsureResult<Position> position = ensurePosition(department.value().getId(), positionName);
      acc.successRows += 1;
      if (department.created()) acc.createdRows += 1;
      if (position.created()) acc.createdRows += 1;
    }
  }

  private void importEmployeeSheet(Sheet sheet, ImportAccumulator acc) {
    Map<String, Integer> headers = readHeaders(sheet);
    Integer nameIndex = headerIndex(headers, "姓名");
    Integer genderIndex = headerIndex(headers, "性别");
    Integer hireDateIndex = headerIndex(headers, "入职时间", "入职日期");
    Integer accountIndex = headerIndex(headers, "用户名", "账号", "登录账号");
    Integer positionIndex = headerIndex(headers, "职位", "岗位", "职位名称", "岗位名称");
    Integer departmentIndex = headerIndex(headers, "部门", "部门名称", "科室");
    if (nameIndex == null || genderIndex == null || hireDateIndex == null || accountIndex == null || positionIndex == null || departmentIndex == null) {
      acc.errors.add("人员Sheet必须包含“姓名、性别、入职时间、用户名、职位、部门”表头");
      return;
    }
    for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      Row row = sheet.getRow(rowIndex);
      if (isBlankRow(row)) continue;
      acc.totalRows += 1;
      try {
        String name = requireCell(row, nameIndex, "姓名");
        String gender = normalizeGender(requireCell(row, genderIndex, "性别"));
        LocalDate hireDate = parseDate(row.getCell(hireDateIndex), rowIndex);
        String account = requireCell(row, accountIndex, "用户名");
        String positionName = requireCell(row, positionIndex, "职位");
        String departmentName = requireCell(row, departmentIndex, "部门");
        if (employeeMapper.selectCount(new QueryWrapper<Employee>().eq("login_account", account)) > 0) {
          acc.errors.add(rowError(rowIndex, "用户名已存在：" + account));
          continue;
        }
        EnsureResult<Department> department = ensureDepartment(departmentName);
        EnsureResult<Position> position = ensurePosition(department.value().getId(), positionName);
        Employee employee = new Employee();
        employee.setId(nextId("emp"));
        employee.setEmployeeNo(nextEmployeeNo());
        employee.setName(name);
        employee.setGender(gender);
        employee.setAge(0);
        employee.setDepartmentId(department.value().getId());
        employee.setPositionId(position.value().getId());
        employee.setHireDate(hireDate);
        employee.setStatus("在职");
        employee.setLoginAccount(account);
        employee.setLoginPasswordHash("{noop}123456");
        employee.setRoleCode("employee");
        employee.setRecycleReceiver(false);
        employee.setCreatedAt(now());
        employee.setUpdatedAt(now());
        employeeMapper.insert(employee);
        acc.successRows += 1;
        acc.createdRows += 1;
        if (department.created()) acc.createdRows += 1;
        if (position.created()) acc.createdRows += 1;
      } catch (IllegalArgumentException e) {
        acc.errors.add(rowError(rowIndex, e.getMessage()));
      }
    }
  }

  private EnsureResult<Department> ensureDepartment(String rawName) {
    String name = normalizeText(rawName);
    if (name == null) {
      throw new IllegalArgumentException("部门名称不能为空");
    }
    Department existing = departmentMapper.selectOne(new QueryWrapper<Department>().eq("name", name));
    if (existing != null) {
      return new EnsureResult<>(existing, false);
    }
    Department created = new Department(nextId("dept"), name);
    departmentMapper.insert(created);
    return new EnsureResult<>(created, true);
  }

  private EnsureResult<Position> ensurePosition(String departmentId, String rawName) {
    String name = normalizeText(rawName);
    if (name == null) {
      throw new IllegalArgumentException("职位名称不能为空");
    }
    Position existing = positionMapper.selectOne(
        new QueryWrapper<Position>()
            .eq("department_id", departmentId)
            .eq("name", name)
    );
    if (existing != null) {
      return new EnsureResult<>(existing, false);
    }
    Position created = new Position(nextId("pos"), departmentId, name);
    positionMapper.insert(created);
    return new EnsureResult<>(created, true);
  }

  private Map<String, Integer> readHeaders(Sheet sheet) {
    Map<String, Integer> headers = new HashMap<>();
    Row row = sheet.getRow(0);
    if (row == null) return headers;
    for (int i = 0; i < row.getLastCellNum(); i++) {
      String text = normalizeHeader(cellText(row, i));
      if (text != null) {
        headers.put(text, i);
      }
    }
    return headers;
  }

  private Integer headerIndex(Map<String, Integer> headers, String... names) {
    for (String name : names) {
      Integer index = headers.get(normalizeHeader(name));
      if (index != null) return index;
    }
    return null;
  }

  private Sheet findSheet(Workbook workbook, String name) {
    for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
      Sheet sheet = workbook.getSheetAt(i);
      if (name.equals(normalizeText(sheet.getSheetName()))) {
        return sheet;
      }
    }
    return null;
  }

  private Sheet firstSheetWithHeaders(Workbook workbook, String... headers) {
    for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
      Sheet sheet = workbook.getSheetAt(i);
      Map<String, Integer> actualHeaders = readHeaders(sheet);
      for (String header : headers) {
        if (actualHeaders.containsKey(normalizeHeader(header))) {
          return sheet;
        }
      }
    }
    return null;
  }

  private boolean isBlankRow(Row row) {
    if (row == null) return true;
    for (int i = 0; i < row.getLastCellNum(); i++) {
      if (cellText(row, i) != null) return false;
    }
    return true;
  }

  private String requireCell(Row row, int index, String label) {
    String value = cellText(row, index);
    if (value == null) {
      throw new IllegalArgumentException(label + "不能为空");
    }
    return value;
  }

  private String cellText(Row row, int index) {
    if (row == null || index < 0) return null;
    Cell cell = row.getCell(index);
    if (cell == null) return null;
    String text = new DataFormatter().formatCellValue(cell);
    return normalizeText(text);
  }

  private String normalizeText(String text) {
    if (text == null) return null;
    String normalized = text.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private String normalizeHeader(String text) {
    String normalized = normalizeText(text);
    return normalized == null ? null : normalized.replace(" ", "").replace("　", "");
  }

  private String normalizeGender(String gender) {
    if ("男".equals(gender) || "女".equals(gender)) {
      return gender;
    }
    throw new IllegalArgumentException("性别只能填写男或女");
  }

  private LocalDate parseDate(Cell cell, int rowIndex) {
    if (cell == null) {
      throw new IllegalArgumentException("入职时间不能为空");
    }
    if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
      return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
    String text = normalizeText(new DataFormatter().formatCellValue(cell));
    if (text == null) {
      throw new IllegalArgumentException("入职时间不能为空");
    }
    List<DateTimeFormatter> formatters = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyy/M/d"),
        DateTimeFormatter.ofPattern("yyyy.M.d"),
        DateTimeFormatter.ofPattern("yyyy年M月d日")
    );
    for (DateTimeFormatter formatter : formatters) {
      try {
        return LocalDate.parse(text, formatter);
      } catch (DateTimeParseException ignored) {
      }
    }
    throw new IllegalArgumentException("入职时间格式错误，请使用 yyyy-MM-dd：" + text);
  }

  private String rowError(int rowIndex, String message) {
    return "第 " + (rowIndex + 1) + " 行：" + message;
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
