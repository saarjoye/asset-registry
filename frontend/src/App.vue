<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import {
  api,
  adaptDevice,
  adaptEmployee,
  adaptPhone,
  adaptAccount,
  clearAuthToken,
  saveAuthToken,
  type Department,
  type Position,
  type Employee,
  type PhoneNumber,
  type Device,
  type ChannelAccount,
  type HandoverTask,
  type Role,
  type EmployeeStatus,
  type DeviceStatus,
  type HandoverStatus,
  type HandoverTargetType,
  type ImportResult,
  type SupervisorDataScope,
  type SupervisorDataScopeInput
} from "./api/adapter";

type RealNameStatus = "已实名" | "未实名";
type FilterKey = "employeeNo" | "name" | "phone" | "type" | "brand" | "model";
type AssetType = "设备" | "手机号" | "账号";

interface MenuItem {
  key: string;
  label: string;
}

interface AppState {
  departments: Department[];
  positions: Position[];
  employees: Employee[];
  phones: PhoneNumber[];
  devices: Device[];
  accounts: ChannelAccount[];
  handoverTasks: HandoverTask[];
  recycleReceivers: Employee[];
  supervisorScopes: SupervisorDataScope[];
}

const SESSION_KEY = "work-device-registry-user";

const operators = ["移动", "电信", "联通", "广电", "其他"];
const deviceTypes = ["手机", "平板", "笔记本", "台式机", "其他"];
const realNameStatuses: RealNameStatus[] = ["已实名", "未实名"];
const employeeStatuses: EmployeeStatus[] = ["在职", "离职申请中", "离职"];
const handoverTargetTypes: HandoverTargetType[] = ["本部门员工", "其它部门员工", "回收入库"];
const genders: Array<Employee["gender"]> = ["男", "女"];

const menuMap: Record<Role, MenuItem[]> = {
  employee: [
    { key: "device", label: "登记设备" },
    { key: "account", label: "登记账号" },
    { key: "summary", label: "名下汇总" },
    { key: "resignation", label: "离职申请" },
    { key: "receiveConfirm", label: "待确认" }
  ],
  supervisor: [
    { key: "device", label: "登记设备" },
    { key: "account", label: "登记账号" },
    { key: "deptSummary", label: "部门汇总" },
    { key: "handoverApprove", label: "离职审批" },
    { key: "deviceAllocation", label: "资产分配" },
    { key: "receiveConfirm", label: "待确认" }
  ],
  hr: [
    { key: "device", label: "登记设备" },
    { key: "account", label: "登记账号" },
    { key: "people", label: "人员档案" },
    { key: "departments", label: "部门档案" },
    { key: "positions", label: "岗位档案" },
    { key: "archiveImport", label: "批量导入" },
    { key: "summary", label: "名下汇总" },
    { key: "openAccount", label: "开通账号" },
    { key: "receiveConfirm", label: "待确认" }
  ],
  admin: [
    { key: "device", label: "登记设备" },
    { key: "account", label: "登记账号" },
    { key: "peopleAdmin", label: "人员档案" },
    { key: "deptPosition", label: "部门岗位档案" },
    { key: "dataPermission", label: "数据权限" },
    { key: "archiveImport", label: "批量导入" },
    { key: "allSummary", label: "所有汇总" },
    { key: "deviceAllocation", label: "资产分配" },
    { key: "receiveConfirm", label: "待确认" }
  ]
};

const filterFields: Array<{ key: FilterKey; label: string }> = [
  { key: "employeeNo", label: "人员编号" },
  { key: "name", label: "姓名" },
  { key: "phone", label: "手机号" },
  { key: "type", label: "设备类型" },
  { key: "brand", label: "品牌" },
  { key: "model", label: "型号" }
];

function emptyState(): AppState {
  return { departments: [], positions: [], employees: [], phones: [], devices: [], accounts: [], handoverTasks: [], recycleReceivers: [], supervisorScopes: [] };
}

const state = reactive<AppState>(emptyState());
const dataLoaded = ref(false);
const dataError = ref("");
const needsInitialSetup = ref(false);
const currentUserId = ref(localStorage.getItem(SESSION_KEY) ?? "");
const activePage = ref("");
const loginForm = reactive({ account: "", password: "" });
const loginError = ref("");
const setupForm = reactive({
  name: "",
  account: "",
  password: "",
  departmentName: "",
  positionName: ""
});

const deviceForm = reactive({
  type: "手机",
  brand: "",
  model: "",
  phoneNumber: "",
  operator: "移动"
});

const accountForm = reactive({
  channel: "",
  account: "",
  password: "",
  realNameStatus: "已实名" as RealNameStatus,
  realName: "",
  idCardNumber: "",
  phoneNumber: "",
  operator: "移动"
});

const summaryFilters = ref<FilterKey[]>([]);
const summaryValues = reactive<Record<FilterKey, string>>({
  employeeNo: "",
  name: "",
  phone: "",
  type: "",
  brand: "",
  model: ""
});
const preciseQuery = ref("");
const personSearchQuery = ref("");

const personForm = reactive({
  id: "",
  employeeNo: "",
  name: "",
  gender: "男" as Employee["gender"],
  age: 0,
  departmentId: "",
  positionId: "",
  hireDate: new Date().toISOString().slice(0, 10),
  status: "在职" as EmployeeStatus,
  account: "",
  loginPassword: "",
  role: "employee" as Role,
  recycleReceiver: false
});

const deptForm = reactive({ id: "", name: "" });
const positionForm = reactive({ id: "", departmentId: "", name: "" });
const deptPositionImportInput = ref<HTMLInputElement | null>(null);
const employeeImportInput = ref<HTMLInputElement | null>(null);
const importBusy = ref(false);
const importResult = ref<ImportResult | null>(null);
const importMessage = ref("");
const openAccountForm = reactive({
  employeeId: "",
  account: "",
  loginPassword: "",
  role: "employee" as Role
});
const resignationForm = reactive({ applicantNote: "" });
const approvalForm = reactive({
  taskId: "",
  targetType: "本部门员工" as HandoverTargetType,
  receiverEmployeeId: ""
});
const allocationForm = reactive({ assetType: "设备" as AssetType, assetId: "", receiverEmployeeId: "" });
const returnReasons = reactive<Record<string, string>>({});
const transferForm = reactive({ deviceId: "", employeeId: "" });
const stockForm = reactive({ deviceId: "" });
const dataPermissionForm = reactive({
  supervisorId: "",
  departmentIds: [] as string[],
  allPositionDepartments: [] as string[],
  positionIdsByDepartment: {} as Record<string, string[]>
});
const dataPermissionMessage = ref("");

async function refresh(viewerId = currentUserId.value) {
  try {
    const normalizedViewerId = viewerId || undefined;
    const [employees, departments, positions, devices, accounts, phones, handoverTasks] = await Promise.all([
      api.archive.employees(),
      api.archive.departments(),
      api.archive.positions(),
      api.archive.devices(),
      api.archive.accounts(),
      api.archive.phones(),
      api.handover.tasks()
    ]);
    const viewer = normalizedViewerId ? employees.find((employee) => employee.id === normalizedViewerId) : null;
    const supervisorScopes = viewer?.roleCode === "admin" ? await api.permissions.supervisorScopes() : [];
    const recycleReceivers = viewer && ["admin", "supervisor", "hr"].includes(viewer.roleCode)
      ? await api.archive.recycleReceivers()
      : [];
    state.employees = employees.map(adaptEmployee);
    state.departments = departments;
    state.positions = positions;
    state.devices = devices.map(adaptDevice);
    state.accounts = accounts.map(adaptAccount);
    state.phones = phones.map(adaptPhone);
    state.handoverTasks = handoverTasks;
    state.recycleReceivers = recycleReceivers.map(adaptEmployee);
    state.supervisorScopes = supervisorScopes;
    dataError.value = "";
  } catch (e) {
    dataError.value = e instanceof Error ? e.message : String(e);
  } finally {
    dataLoaded.value = true;
  }
}

async function refreshScopedSummary(scope: "mine" | "department" | "all") {
  if (!currentUser.value) return;
  try {
    const summary = await api.registry.summary(currentUser.value.id, scope);
    state.devices = (await api.registry.summary(currentUser.value.id, scope)).devices.map(d => ({
      id: d.id,
      employeeId: "",
      departmentId: "",
      type: d.type,
      brand: d.brand,
      model: d.model,
      phoneId: "",
      status: d.status as DeviceStatus,
      registeredAt: d.registeredAt
    }));
    state.accounts = summary.accounts.map(a => ({
      id: a.id,
      employeeId: "",
      phoneId: "",
      channel: a.channel,
      account: a.account,
      password: a.password,
      realNameStatus: (a.realNameStatus as RealNameStatus) ?? "已实名",
      realName: a.realName,
      idCardNumber: a.idCardNumber
    }));
  } catch (e) {
    dataError.value = e instanceof Error ? e.message : String(e);
  }
}

onMounted(async () => {
  const setupStatus = await api.auth.setupRequired();
  needsInitialSetup.value = setupStatus.required;
  if (currentUserId.value) {
    await refresh(currentUserId.value);
    const u = state.employees.find(e => e.id === currentUserId.value);
    if (u) {
      activePage.value = menuMap[u.role][0].key;
    } else {
      currentUserId.value = "";
      clearAuthToken();
    }
  } else {
    dataLoaded.value = true;
  }
});

const currentUser = computed(() => {
  return state.employees.find((employee) => employee.id === currentUserId.value) ?? null;
});

const currentMenu = computed(() => {
  if (!currentUser.value) {
    return [];
  }

  return menuMap[currentUser.value.role];
});

const activeMenuItem = computed(() => {
  return currentMenu.value.find((item) => item.key === activePage.value) ?? currentMenu.value[0];
});

const pageTitle = computed(() => {
  if (!currentUser.value || !activeMenuItem.value) {
    return "";
  }

  return `${currentUser.value.name} - ${activeMenuItem.value.label}`;
});

const currentDepartmentName = computed(() => {
  if (!currentUser.value) {
    return "";
  }

  return getDepartmentName(currentUser.value.departmentId);
});

const currentPositionName = computed(() => {
  if (!currentUser.value) {
    return "";
  }

  return getPositionName(currentUser.value.positionId);
});

const personDepartmentPositions = computed(() => {
  return state.positions.filter((position) => position.departmentId === personForm.departmentId);
});

const currentPhones = computed(() => {
  if (!currentUser.value) {
    return [];
  }

  return state.phones.filter((phone) => phone.employeeId === currentUser.value?.id);
});

const currentDevices = computed(() => {
  if (!currentUser.value) {
    return [];
  }

  return state.devices.filter((device) => device.employeeId === currentUser.value?.id);
});

const currentAccounts = computed(() => {
  if (!currentUser.value) {
    return [];
  }

  return state.accounts.filter((account) => account.employeeId === currentUser.value?.id);
});

const supervisorDepartmentEmployees = computed(() => {
  if (!currentUser.value) {
    return [];
  }

  return state.employees.filter((employee) => employee.departmentId === currentUser.value?.departmentId);
});

const selectableTransferEmployees = computed(() => {
  return supervisorDepartmentEmployees.value.filter((employee) => employee.status === "在职");
});

const pendingRecycleDevices = computed(() => {
  if (!currentUser.value || currentUser.value.role !== "supervisor") {
    return [];
  }

  return state.devices.filter((device) => {
    const owner = getEmployee(device.employeeId);
    return (
      device.departmentId === currentUser.value?.departmentId &&
      owner?.status === "离职" &&
      device.status === "待回收"
    );
  });
});

const confirmedRecycleDevices = computed(() => {
  if (!currentUser.value || currentUser.value.role !== "supervisor") {
    return [];
  }

  return state.devices.filter((device) => {
    return device.departmentId === currentUser.value?.departmentId && device.status === "已回收";
  });
});

const transferableDevices = computed(() => {
  if (!currentUser.value || currentUser.value.role !== "supervisor") {
    return [];
  }

  return state.devices.filter((device) => {
    return device.departmentId === currentUser.value?.departmentId && device.status !== "待回收";
  });
});

const stockInDevices = computed(() => {
  if (!currentUser.value) {
    return [];
  }

  return state.devices.filter((device) => {
    return device.status === "旧机入库" && device.departmentId === currentUser.value?.departmentId;
  });
});

const employeeHandoverRows = computed(() => {
  if (!currentUser.value) {
    return [];
  }

  return [
    ...currentDevices.value.map((device) => ({
      assetType: "设备" as AssetType,
      assetId: device.id,
      assetText: assetText("设备", device.id),
      phone: assetPhone("设备", device.id),
      status: device.status,
      task: getLatestAssetHandoverTask("设备", device.id, currentUser.value?.id)
    })),
    ...currentPhones.value.map((phone) => ({
      assetType: "手机号" as AssetType,
      assetId: phone.id,
      assetText: assetText("手机号", phone.id),
      phone: phone.number,
      status: phone.status ?? "在用",
      task: getLatestAssetHandoverTask("手机号", phone.id, currentUser.value?.id)
    })),
    ...currentAccounts.value.map((account) => ({
      assetType: "账号" as AssetType,
      assetId: account.id,
      assetText: assetText("账号", account.id),
      phone: assetPhone("账号", account.id),
      status: account.status ?? "在用",
      task: getLatestAssetHandoverTask("账号", account.id, currentUser.value?.id)
    }))
  ];
});

const employeeRejectedTasks = computed(() => {
  if (!currentUser.value) {
    return [];
  }

  return state.handoverTasks.filter((task) => {
    return task.applicantId === currentUser.value?.id && task.status === "已回退";
  });
});

const supervisorApprovalTasks = computed(() => {
  if (!currentUser.value || currentUser.value.role !== "supervisor") {
    return [];
  }

  return state.handoverTasks.filter((task) => {
    return task.sourceDepartmentId === currentUser.value?.departmentId && task.status === "待主管审批";
  });
});

const selectedApprovalTask = computed(() => {
  return state.handoverTasks.find((task) => task.id === approvalForm.taskId) ?? null;
});

const approvalReceiverEmployees = computed(() => {
  const task = selectedApprovalTask.value;
  if (!currentUser.value || !task) {
    return [];
  }

  return state.employees.filter((employee) => {
    if (employee.id === task.applicantId || employee.status !== "在职") {
      return false;
    }

    if (approvalForm.targetType === "本部门员工") {
      return employee.departmentId === currentUser.value?.departmentId;
    }

    if (approvalForm.targetType === "其它部门员工") {
      return employee.departmentId !== currentUser.value?.departmentId;
    }

    return true;
  });
});

const approvalRecycleReceivers = computed(() => {
  const task = selectedApprovalTask.value;
  if (!task) {
    return [];
  }
  return state.recycleReceivers.filter((employee) => {
    return employee.id !== task.applicantId && employee.status === "在职";
  });
});

const approvalReceiverOptions = computed(() => {
  return approvalForm.targetType === "回收入库"
    ? approvalRecycleReceivers.value
    : approvalReceiverEmployees.value;
});

const receiverPendingTasks = computed(() => {
  if (!currentUser.value) {
    return [];
  }

  return state.handoverTasks.filter((task) => {
    return task.receiverEmployeeId === currentUser.value?.id && task.status === "待接收确认";
  });
});

const allocationAssets = computed(() => {
  if (!currentUser.value || !["admin", "supervisor"].includes(currentUser.value.role)) {
    return [];
  }

  return [
    ...currentDevices.value.map((device) => ({
      assetType: "设备" as AssetType,
      assetId: device.id,
      assetText: assetText("设备", device.id),
      phone: getPhone(device.phoneId)?.number ?? "",
      status: device.status,
      registeredAt: device.registeredAt ?? ""
    })),
    ...currentPhones.value.map((phone) => ({
      assetType: "手机号" as AssetType,
      assetId: phone.id,
      assetText: assetText("手机号", phone.id),
      phone: phone.number,
      status: phone.status ?? "在用",
      registeredAt: phone.registeredAt ?? ""
    })),
    ...currentAccounts.value.map((account) => ({
      assetType: "账号" as AssetType,
      assetId: account.id,
      assetText: assetText("账号", account.id),
      phone: getPhone(account.phoneId)?.number ?? "",
      status: account.status ?? "在用",
      registeredAt: account.registeredAt ?? ""
    }))
  ].filter((asset) => {
    return asset.status !== "接收待确认" && !getActiveAssetHandoverTask(asset.assetType, asset.assetId);
  });
});

const allocationReceiverEmployees = computed(() => {
  if (!currentUser.value || !["admin", "supervisor"].includes(currentUser.value.role)) {
    return [];
  }

  return state.employees.filter((employee) => {
    if (employee.id === currentUser.value?.id || employee.status !== "在职") {
      return false;
    }

    if (currentUser.value?.role === "supervisor") {
      return employee.departmentId === currentUser.value.departmentId;
    }

    return true;
  });
});

const supervisorEmployees = computed(() => {
  return state.employees.filter((employee) => employee.role === "supervisor");
});

const selectedDataPermissionSupervisor = computed(() => {
  return supervisorEmployees.value.find((employee) => employee.id === dataPermissionForm.supervisorId) ?? null;
});

const supervisorScopesForSelected = computed(() => {
  if (!dataPermissionForm.supervisorId) {
    return [];
  }
  return state.supervisorScopes.filter((scope) => scope.supervisorId === dataPermissionForm.supervisorId);
});

const filteredEmployees = computed(() => {
  const query = personSearchQuery.value.trim().toLowerCase();
  if (!query) {
    return state.employees;
  }

  return state.employees.filter((employee) => {
    return [
      employee.employeeNo,
      employee.name,
      employee.account,
      getDepartmentName(employee.departmentId),
      getPositionName(employee.positionId)
    ].some((value) => value.toLowerCase().includes(query));
  });
});

const summaryScopeEmployees = computed(() => {
  if (!currentUser.value) {
    return [];
  }

  if (activePage.value === "allSummary") {
    return state.employees;
  }

  if (activePage.value === "deptSummary") {
    return state.employees;
  }

  return state.employees.filter((employee) => employee.id === currentUser.value?.id);
});

const summaryEmployeeIds = computed(() => new Set(summaryScopeEmployees.value.map((employee) => employee.id)));

const summaryDeviceRows = computed(() => {
  let rows = state.devices
    .filter((device) => {
      if (activePage.value === "deptSummary") {
        return true;
      }

      if (activePage.value === "allSummary") {
        return true;
      }

      return summaryEmployeeIds.value.has(device.employeeId);
    })
    .map((device) => {
      const owner = getEmployee(device.employeeId);
      const phone = getPhone(device.phoneId);
      const linkedAccounts = state.accounts
        .filter((account) => account.employeeId === device.employeeId && account.phoneId === device.phoneId)
        .map((account) => `${account.channel}:${account.account}`)
        .join(" / ");

      return {
        id: device.id,
        employeeNo: owner?.employeeNo ?? "",
        name: owner?.name ?? "库房",
        sourceDepartment: getDeviceSourceDepartmentName(device.id),
        sourceEmployee: getDeviceSourceEmployeeName(device.id),
        acquisitionType: getDeviceAcquisitionType(device.id),
        registeredAt: device.registeredAt ?? "",
        receiveAt: getDeviceReceiveTime(device.id),
        allocationAt: getDeviceAllocationTime(device.id),
        phone: phone?.number ?? "",
        operator: phone?.operator ?? "",
        type: device.type,
        brand: device.brand,
        model: device.model,
        status: device.status,
        department: owner ? getDepartmentName(owner.departmentId) : getDepartmentName(device.departmentId),
        accounts: linkedAccounts
      };
    });

  rows = rows.filter((row) => passSummaryFilter(row));
  return rows;
});

const summaryAccountRows = computed(() => {
  let rows = state.accounts
    .filter((account) => {
      if (activePage.value === "deptSummary") {
        return true;
      }

      if (activePage.value === "allSummary") {
        return true;
      }

      return summaryEmployeeIds.value.has(account.employeeId);
    })
    .map((account) => {
      const owner = getEmployee(account.employeeId);
      const phone = getPhone(account.phoneId);

      return {
        id: account.id,
        employeeNo: owner?.employeeNo ?? "",
        name: owner?.name ?? "",
        phone: phone?.number ?? "",
        operator: phone?.operator ?? "",
        channel: account.channel,
        account: account.account,
        password: "******",
        realNameStatus: account.realNameStatus,
        realName: account.realName ?? "",
        idCardNumber: account.idCardNumber ?? "",
        department: owner ? getDepartmentName(owner.departmentId) : ""
      };
    });

  rows = rows.filter((row) => passSummaryFilter(row));
  return rows;
});

watch(currentUserId, (value) => {
  if (value) {
    localStorage.setItem(SESSION_KEY, value);
  } else {
    localStorage.removeItem(SESSION_KEY);
  }
});

watch(
  currentUser,
  (user) => {
    if (!user) {
      activePage.value = "";
      return;
    }

    if (user.status === "离职") {
      currentUserId.value = "";
      activePage.value = "";
      loginError.value = "账号已离职，禁止登录";
      return;
    }

    const menu = menuMap[user.role];
    if (!menu.some((item) => item.key === activePage.value)) {
      activePage.value = menu[0].key;
    }

    resetDeviceForm();
    resetAccountForm();
  },
  { immediate: true }
);

watch(
  () => approvalForm.targetType,
  () => {
    approvalForm.receiverEmployeeId = approvalForm.targetType === "回收入库"
      ? approvalReceiverOptions.value[0]?.id ?? ""
      : "";
  }
);

watch(
  () => approvalForm.taskId,
  () => {
    if (approvalForm.targetType === "回收入库") {
      approvalForm.receiverEmployeeId = approvalReceiverOptions.value[0]?.id ?? "";
    }
  }
);

watch(
  approvalReceiverOptions,
  (options) => {
    if (approvalForm.targetType === "回收入库" && !options.some((employee) => employee.id === approvalForm.receiverEmployeeId)) {
      approvalForm.receiverEmployeeId = options[0]?.id ?? "";
    }
  }
);

watch(
  () => personForm.departmentId,
  () => {
    if (!personDepartmentPositions.value.some((position) => position.id === personForm.positionId)) {
      personForm.positionId = personDepartmentPositions.value[0]?.id ?? "";
    }
  }
);

watch(
  () => state.departments.length,
  () => {
    if (!positionForm.departmentId) {
      positionForm.departmentId = state.departments[0]?.id ?? "";
    }
    if (!personForm.departmentId) {
      personForm.departmentId = state.departments[0]?.id ?? "";
    }
  }
);

watch(
  () => allocationForm.assetType,
  () => {
    allocationForm.assetId = "";
  }
);

function getEmployee(employeeId: string) {
  return state.employees.find((employee) => employee.id === employeeId);
}

function getDepartmentName(departmentId: string) {
  return state.departments.find((department) => department.id === departmentId)?.name ?? "";
}

function getPositionName(positionId: string) {
  return state.positions.find((position) => position.id === positionId)?.name ?? "";
}

function getPhone(phoneId: string) {
  return state.phones.find((phone) => phone.id === phoneId);
}

function getDeviceOwnerName(device: Device) {
  const owner = getEmployee(device.employeeId);
  return owner ? employeeDisplay(owner) : "库房";
}

function taskAssetType(task: HandoverTask): AssetType {
  const t = task.assetType as AssetType;
  return t ?? "设备";
}

function taskAssetId(task: HandoverTask) {
  return task.assetId ?? task.deviceId;
}

function isTaskForAsset(task: HandoverTask, assetType: AssetType, assetId: string) {
  return taskAssetType(task) === assetType && taskAssetId(task) === assetId;
}

function isAllocationTask(task: HandoverTask) {
  return task.targetType === "资产分配";
}

function assetText(assetType: AssetType, assetId: string) {
  if (assetType === "设备") {
    const device = state.devices.find((item) => item.id === assetId);
    return device ? `${device.type} / ${device.brand} / ${device.model}` : "";
  }

  if (assetType === "手机号") {
    const phone = getPhone(assetId);
    return phone ? `${phone.number} / ${phone.operator}` : "";
  }

  const account = state.accounts.find((item) => item.id === assetId);
  return account ? `${account.channel} / ${account.account}` : "";
}

function assetPhone(assetType: AssetType, assetId: string) {
  if (assetType === "手机号") {
    return getPhone(assetId)?.number ?? "";
  }

  if (assetType === "账号") {
    const account = state.accounts.find((item) => item.id === assetId);
    return account ? getPhone(account.phoneId)?.number ?? "" : "";
  }

  const device = state.devices.find((item) => item.id === assetId);
  return device ? getPhone(device.phoneId)?.number ?? "" : "";
}

function setAssetStatus(assetType: AssetType, assetId: string, status: DeviceStatus) {
  if (assetType === "设备") {
    const device = state.devices.find((item) => item.id === assetId);
    if (device) {
      device.status = status;
    }
    return;
  }

  if (assetType === "手机号") {
    const phone = getPhone(assetId);
    if (phone) {
      phone.status = status;
    }
    return;
  }

  const account = state.accounts.find((item) => item.id === assetId);
  if (account) {
    account.status = status;
  }
}

function assignAssetToEmployee(assetType: AssetType, assetId: string, employee: Employee, status: DeviceStatus) {
  if (assetType === "设备") {
    const device = state.devices.find((item) => item.id === assetId);
    if (device) {
      device.employeeId = employee.id;
      device.departmentId = employee.departmentId;
      device.status = status;
    }
    return;
  }

  if (assetType === "手机号") {
    const phone = getPhone(assetId);
    if (phone) {
      phone.employeeId = employee.id;
      phone.status = status;
    }
    return;
  }

  const account = state.accounts.find((item) => item.id === assetId);
  if (account) {
    account.employeeId = employee.id;
    account.status = status;
  }
}

function getLatestHandoverTask(deviceId: string, applicantId = "") {
  return [...state.handoverTasks].reverse().find((task) => {
    return isTaskForAsset(task, "设备", deviceId) && (!applicantId || task.applicantId === applicantId);
  });
}

function getLatestAssetHandoverTask(assetType: AssetType, assetId: string, applicantId = "") {
  return [...state.handoverTasks].reverse().find((task) => {
    return isTaskForAsset(task, assetType, assetId) && (!applicantId || task.applicantId === applicantId);
  });
}

function getCompletedHandoverTask(deviceId: string) {
  return [...state.handoverTasks].reverse().find((task) => {
    return isTaskForAsset(task, "设备", deviceId) && task.status === "已完成";
  });
}

function getCompletedHandoverTaskByType(deviceId: string, targetType: HandoverTargetType) {
  return [...state.handoverTasks].reverse().find((task) => {
    return isTaskForAsset(task, "设备", deviceId) && task.status === "已完成" && task.targetType === targetType;
  });
}

function getCompletedHandoverTaskExceptType(deviceId: string, targetType: HandoverTargetType) {
  return [...state.handoverTasks].reverse().find((task) => {
    return isTaskForAsset(task, "设备", deviceId) && task.status === "已完成" && task.targetType !== targetType;
  });
}

function getDeviceAcquisitionType(deviceId: string) {
  const task = getCompletedHandoverTask(deviceId);
  if (!task) {
    return "登记";
  }

  return isAllocationTask(task) ? "分配" : "接收";
}

function getDeviceReceiveTime(deviceId: string) {
  return [...state.handoverTasks].reverse().find((task) => {
    return isTaskForAsset(task, "设备", deviceId) && task.status === "已完成" && !isAllocationTask(task);
  })?.updatedAt ?? "";
}

function getDeviceAllocationTime(deviceId: string) {
  return [...state.handoverTasks].reverse().find((task) => {
    return isTaskForAsset(task, "设备", deviceId) && task.status === "已完成" && isAllocationTask(task);
  })?.updatedAt ?? "";
}

function getDeviceSourceDepartmentName(deviceId: string) {
  const task = getCompletedHandoverTask(deviceId);
  if (task) {
    return getDepartmentName(task.sourceDepartmentId);
  }
  const device = state.devices.find((item) => item.id === deviceId);
  const owner = device ? getEmployee(device.employeeId) : undefined;
  return owner ? getDepartmentName(owner.departmentId) : device ? getDepartmentName(device.departmentId) : "";
}

function getDeviceSourceEmployeeName(deviceId: string) {
  const task = getCompletedHandoverTask(deviceId);
  const device = state.devices.find((item) => item.id === deviceId);
  const applicant = task ? getEmployee(task.applicantId) : device ? getEmployee(device.employeeId) : undefined;
  return applicant ? `${applicant.name} / ${applicant.employeeNo}` : "";
}

function getActiveHandoverTask(deviceId: string) {
  return state.handoverTasks.find((task) => {
    return isTaskForAsset(task, "设备", deviceId) && ["待主管审批", "待接收确认"].includes(task.status);
  });
}

function getActiveAssetHandoverTask(assetType: AssetType, assetId: string) {
  return state.handoverTasks.find((task) => {
    return isTaskForAsset(task, assetType, assetId) && ["待主管审批", "待接收确认"].includes(task.status);
  });
}

function getHandoverDevice(task: HandoverTask) {
  return state.devices.find((device) => device.id === task.deviceId);
}

function getHandoverApplicant(task: HandoverTask) {
  return getEmployee(task.applicantId);
}

function getHandoverReceiver(task: HandoverTask) {
  return getEmployee(task.receiverEmployeeId);
}

function getHandoverPhone(task: HandoverTask) {
  const phoneNumber = assetPhone(taskAssetType(task), taskAssetId(task));
  return state.phones.find((phone) => phone.number === phoneNumber);
}

function handoverDeviceText(task: HandoverTask) {
  return assetText(taskAssetType(task), taskAssetId(task));
}

function employeeDisplay(employee: Employee) {
  return `${employee.name} / ${employee.employeeNo} / ${getDepartmentName(employee.departmentId)} / ${getPositionName(employee.positionId)}`;
}

function statusTone(status?: string) {
  const value = status ?? "";
  if (["在职", "在用", "已完成", "已回收", "已实名"].includes(value)) {
    return "is-success";
  }
  if (["离职申请中", "待回收", "待主管审批", "待接收确认", "接收待确认", "待确认"].includes(value)) {
    return "is-warning";
  }
  if (["已回退", "未实名"].includes(value)) {
    return "is-danger";
  }
  return "is-neutral";
}

function eventChecked(event: Event) {
  return (event.target as HTMLInputElement).checked;
}

function positionsByDepartment(departmentId: string) {
  return state.positions.filter((position) => position.departmentId === departmentId);
}

function isPermissionDepartmentSelected(departmentId: string) {
  return dataPermissionForm.departmentIds.includes(departmentId);
}

function isAllPositionsSelected(departmentId: string) {
  return dataPermissionForm.allPositionDepartments.includes(departmentId);
}

function isPermissionPositionSelected(departmentId: string, positionId: string) {
  return (dataPermissionForm.positionIdsByDepartment[departmentId] ?? []).includes(positionId);
}

function resetDataPermissionForm() {
  dataPermissionForm.departmentIds = [];
  dataPermissionForm.allPositionDepartments = [];
  dataPermissionForm.positionIdsByDepartment = {};
  dataPermissionMessage.value = "";
}

function loadSupervisorDataPermission(supervisorId: string) {
  resetDataPermissionForm();
  dataPermissionForm.supervisorId = supervisorId;
  const scopes = state.supervisorScopes.filter((scope) => scope.supervisorId === supervisorId);
  const departmentIds = new Set<string>();
  const allPositionDepartments = new Set<string>();
  const positionIdsByDepartment: Record<string, string[]> = {};

  for (const scope of scopes) {
    departmentIds.add(scope.departmentId);
    if (scope.allPositions) {
      allPositionDepartments.add(scope.departmentId);
      continue;
    }
    if (scope.positionId) {
      positionIdsByDepartment[scope.departmentId] = [
        ...(positionIdsByDepartment[scope.departmentId] ?? []),
        scope.positionId
      ];
    }
  }

  dataPermissionForm.departmentIds = [...departmentIds];
  dataPermissionForm.allPositionDepartments = [...allPositionDepartments];
  dataPermissionForm.positionIdsByDepartment = positionIdsByDepartment;
}

function togglePermissionDepartment(departmentId: string, checked: boolean) {
  if (checked && !dataPermissionForm.departmentIds.includes(departmentId)) {
    dataPermissionForm.departmentIds.push(departmentId);
  }
  if (!checked) {
    dataPermissionForm.departmentIds = dataPermissionForm.departmentIds.filter((id) => id !== departmentId);
    dataPermissionForm.allPositionDepartments = dataPermissionForm.allPositionDepartments.filter((id) => id !== departmentId);
    delete dataPermissionForm.positionIdsByDepartment[departmentId];
  }
}

function toggleAllPositions(departmentId: string, checked: boolean) {
  if (!isPermissionDepartmentSelected(departmentId)) {
    togglePermissionDepartment(departmentId, true);
  }
  if (checked && !dataPermissionForm.allPositionDepartments.includes(departmentId)) {
    dataPermissionForm.allPositionDepartments.push(departmentId);
    dataPermissionForm.positionIdsByDepartment[departmentId] = [];
  }
  if (!checked) {
    dataPermissionForm.allPositionDepartments = dataPermissionForm.allPositionDepartments.filter((id) => id !== departmentId);
  }
}

function togglePermissionPosition(departmentId: string, positionId: string, checked: boolean) {
  if (!isPermissionDepartmentSelected(departmentId)) {
    togglePermissionDepartment(departmentId, true);
  }
  dataPermissionForm.allPositionDepartments = dataPermissionForm.allPositionDepartments.filter((id) => id !== departmentId);
  const currentPositionIds = dataPermissionForm.positionIdsByDepartment[departmentId] ?? [];
  dataPermissionForm.positionIdsByDepartment[departmentId] = checked
    ? [...new Set([...currentPositionIds, positionId])]
    : currentPositionIds.filter((id) => id !== positionId);
}

function buildSupervisorScopePayload() {
  return dataPermissionForm.departmentIds.map((departmentId): SupervisorDataScopeInput => {
    const allPositions = isAllPositionsSelected(departmentId);
    return {
      departmentId,
      allPositions,
      positionIds: allPositions ? [] : dataPermissionForm.positionIdsByDepartment[departmentId] ?? []
    };
  });
}

async function submitDataPermission() {
  const operator = currentUser.value;
  if (!operator || operator.role !== "admin") {
    dataPermissionMessage.value = "只有管理员可以配置数据权限";
    return;
  }
  if (!dataPermissionForm.supervisorId) {
    dataPermissionMessage.value = "请选择主管账号";
    return;
  }
  try {
    const savedScopes = await api.permissions.saveSupervisorScope(
      dataPermissionForm.supervisorId,
      buildSupervisorScopePayload()
    );
    state.supervisorScopes = [
      ...state.supervisorScopes.filter((scope) => scope.supervisorId !== dataPermissionForm.supervisorId),
      ...savedScopes
    ];
    loadSupervisorDataPermission(dataPermissionForm.supervisorId);
    dataPermissionMessage.value = "数据权限已保存";
  } catch (e) {
    dataPermissionMessage.value = e instanceof Error ? e.message : "保存失败";
  }
}

async function login() {
  try {
    const r = await api.auth.login({ account: loginForm.account.trim(), password: loginForm.password });
    saveAuthToken(r.token);
    currentUserId.value = r.user.id;
    await refresh(r.user.id);
    const u = state.employees.find(e => e.id === r.user.id);
    if (!u) {
      loginError.value = "账号或密码错误";
      currentUserId.value = "";
      clearAuthToken();
      return;
    }
    if (u.status === "离职") {
      loginError.value = "账号已离职，禁止登录";
      return;
    }
    loginError.value = "";
    activePage.value = menuMap[u.role][0].key;
    loginForm.account = "";
    loginForm.password = "";
  } catch (e) {
    loginError.value = e instanceof Error ? e.message : "账号或密码错误";
  }
}

async function initializeAdmin() {
  try {
    const r = await api.auth.setup({
      name: setupForm.name.trim(),
      account: setupForm.account.trim(),
      password: setupForm.password,
      departmentName: setupForm.departmentName.trim(),
      positionName: setupForm.positionName.trim()
    });
    saveAuthToken(r.token);
    currentUserId.value = r.user.id;
    needsInitialSetup.value = false;
    await refresh(r.user.id);
    activePage.value = "peopleAdmin";
    personForm.departmentId = r.user.departmentId;
    personForm.positionId = r.user.positionId;
    openAccountForm.employeeId = r.user.id;
    setupForm.name = "";
    setupForm.account = "";
    setupForm.password = "";
    setupForm.departmentName = "";
    setupForm.positionName = "";
  } catch (e) {
    loginError.value = e instanceof Error ? e.message : "初始化失败";
  }
}

function logout() {
  currentUserId.value = "";
  clearAuthToken();
}

function setPage(page: string) {
  activePage.value = page;
  if (page === "dataPermission" && !dataPermissionForm.supervisorId && supervisorEmployees.value.length) {
    loadSupervisorDataPermission(supervisorEmployees.value[0].id);
  }
}

function resetDeviceForm() {
  deviceForm.type = "手机";
  deviceForm.brand = "";
  deviceForm.model = "";
  deviceForm.phoneNumber = "";
  deviceForm.operator = "移动";
}

function resetAccountForm() {
  accountForm.channel = "";
  accountForm.account = "";
  accountForm.password = "";
  accountForm.realNameStatus = "已实名";
  accountForm.realName = currentUser.value?.name ?? "";
  accountForm.idCardNumber = "";
  accountForm.phoneNumber = "";
  accountForm.operator = "移动";
}

async function submitDevice() {
  if (!currentUser.value) return;
  try {
    await api.registry.registerDevice(currentUser.value.id, {
      type: deviceForm.type,
      brand: deviceForm.brand.trim(),
      model: deviceForm.model.trim(),
      phoneNumber: deviceForm.phoneNumber.trim(),
      operator: deviceForm.operator
    });
    await refresh();
    resetDeviceForm();
  } catch (e) {
    dataError.value = e instanceof Error ? e.message : "登记失败";
  }
}

async function submitAccount() {
  if (!currentUser.value) return;
  try {
    await api.registry.registerAccount(currentUser.value.id, {
      channel: accountForm.channel.trim(),
      account: accountForm.account.trim(),
      password: accountForm.password,
      realNameStatus: accountForm.realNameStatus,
      realName: accountForm.realName.trim(),
      idCardNumber: accountForm.idCardNumber.trim(),
      phoneNumber: accountForm.phoneNumber.trim(),
      operator: accountForm.operator
    });
    await refresh();
    resetAccountForm();
  } catch (e) {
    dataError.value = e instanceof Error ? e.message : "登记失败";
  }
}

async function submitResignation() {
  const applicant = currentUser.value;
  if (!applicant) return;
  try {
    await api.handover.submitResignation(applicant.id, resignationForm.applicantNote.trim());
    await refresh();
    resignationForm.applicantNote = "";
  } catch (e) {
    dataError.value = e instanceof Error ? e.message : "提交离职申请失败";
  }
}

function resetApprovalForm() {
  approvalForm.taskId = "";
  approvalForm.targetType = "本部门员工";
  approvalForm.receiverEmployeeId = "";
}

async function submitHandoverApproval() {
  const task = selectedApprovalTask.value;
  if (!currentUser.value || !task) return;
  try {
    await api.handover.approve(currentUser.value.id, {
      taskId: task.id,
      targetType: approvalForm.targetType,
      receiverEmployeeId: approvalForm.receiverEmployeeId
    });
    await refresh();
    resetApprovalForm();
  } catch (e) {
    dataError.value = e instanceof Error ? e.message : "审批失败";
  }
}

async function submitDeviceAllocation() {
  const allocator = currentUser.value;
  if (!allocator) return;
  try {
    await api.handover.allocate(allocator.id, {
      assetType: allocationForm.assetType,
      assetId: allocationForm.assetId,
      receiverEmployeeId: allocationForm.receiverEmployeeId
    });
    await refresh();
    allocationForm.assetId = "";
    allocationForm.receiverEmployeeId = "";
  } catch (e) {
    dataError.value = e instanceof Error ? e.message : "分配失败";
  }
}

async function confirmReceive(taskId: string) {
  const receiver = currentUser.value;
  if (!receiver) return;
  try {
    await api.handover.confirm(taskId, receiver.id);
    await refresh();
  } catch (e) {
    dataError.value = e instanceof Error ? e.message : "确认失败";
  }
}

async function rejectReceive(taskId: string) {
  const receiver = currentUser.value;
  const reason = returnReasons[taskId]?.trim();
  if (!receiver || !reason) return;
  try {
    await api.handover.reject(receiver.id, taskId, reason);
    returnReasons[taskId] = "";
    await refresh();
  } catch (e) {
    dataError.value = e instanceof Error ? e.message : "回退失败";
  }
}

function passSummaryFilter(row: unknown) {
  const record = row as Record<string, string | undefined>;
  const precise = preciseQuery.value.trim();

  if (precise) {
    const exactMatched = ["employeeNo", "name", "phone", "type", "brand", "model"].some((key) => {
      return (record[key] ?? "") === precise;
    });

    if (!exactMatched) {
      return false;
    }
  }

  return summaryFilters.value.every((field) => {
    const query = summaryValues[field].trim();
    if (!query) {
      return true;
    }

    if (record[field] === undefined) {
      return true;
    }

    return record[field].includes(query);
  });
}

function editPerson(employee: Employee) {
  personForm.id = employee.id;
  personForm.employeeNo = employee.employeeNo;
  personForm.name = employee.name;
  personForm.gender = employee.gender;
  personForm.age = employee.age;
  personForm.departmentId = employee.departmentId;
  personForm.positionId = employee.positionId;
  personForm.hireDate = employee.hireDate;
  personForm.status = employee.status;
  personForm.account = employee.account;
  personForm.loginPassword = "";
  personForm.role = employee.role;
  personForm.recycleReceiver = employee.recycleReceiver;
}

function resetPersonForm() {
  personForm.id = "";
  personForm.employeeNo = "";
  personForm.name = "";
  personForm.gender = "男";
  personForm.age = 0;
  personForm.departmentId = state.departments[0]?.id ?? "";
  personForm.positionId = state.positions.find((position) => position.departmentId === personForm.departmentId)?.id ?? "";
  personForm.hireDate = new Date().toISOString().slice(0, 10);
  personForm.status = "在职";
  personForm.account = "";
  personForm.loginPassword = "";
  personForm.role = "employee";
  personForm.recycleReceiver = false;
}

function cancelPersonEdit() {
  resetPersonForm();
}

function normalizeAge(age: number | "") {
  const numericAge = Number(age);
  return Number.isFinite(numericAge) && numericAge >= 0 ? numericAge : 0;
}

async function submitPerson(includeAccount: boolean) {
  try {
    await api.archive.saveEmployee(
      {
        id: personForm.id || null,
        name: personForm.name.trim(),
        gender: personForm.gender,
        age: normalizeAge(personForm.age),
        departmentId: personForm.departmentId,
        positionId: personForm.positionId,
        hireDate: personForm.hireDate,
        status: personForm.status,
        account: personForm.account.trim(),
        password: includeAccount && personForm.loginPassword ? personForm.loginPassword : null,
        role: includeAccount ? personForm.role : "employee",
        recycleReceiver: personForm.recycleReceiver,
        operatorEmployeeId: currentUser.value?.id ?? null
      },
      includeAccount
    );
    await refresh();
    resetPersonForm();
  } catch (e) {
    dataError.value = e instanceof Error ? e.message : "保存失败";
  }
}

async function submitDepartment() {
  try {
    await api.archive.saveDepartment({ id: deptForm.id || undefined, name: deptForm.name.trim() });
    await refresh();
    deptForm.id = "";
    deptForm.name = "";
  } catch (e) {
    dataError.value = e instanceof Error ? e.message : "保存失败";
  }
}

function editDepartment(department: Department) {
  deptForm.id = department.id;
  deptForm.name = department.name;
}

async function submitPosition() {
  try {
    await api.archive.savePosition({
      id: positionForm.id || undefined,
      departmentId: positionForm.departmentId,
      name: positionForm.name.trim()
    });
    await refresh();
    positionForm.id = "";
    positionForm.departmentId = state.departments[0]?.id ?? "";
    positionForm.name = "";
  } catch (e) {
    dataError.value = e instanceof Error ? e.message : "保存失败";
  }
}

function editPosition(position: Position) {
  positionForm.id = position.id;
  positionForm.departmentId = position.departmentId;
  positionForm.name = position.name;
}

function resetPositionForm() {
  positionForm.id = "";
  positionForm.departmentId = state.departments[0]?.id ?? "";
  positionForm.name = "";
}

function importSummary(result: ImportResult) {
  return `读取 ${result.totalRows} 行，成功 ${result.successRows} 行，新建 ${result.createdRows} 条，跳过 ${result.skippedRows} 行`;
}

function saveTemplate(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

async function downloadDepartmentsAndPositionsTemplate() {
  try {
    const blob = await api.archive.departmentPositionTemplate();
    saveTemplate(blob, "部门岗位导入模板.xlsx");
  } catch (e) {
    importMessage.value = e instanceof Error ? e.message : "模板下载失败";
  }
}

async function downloadEmployeesTemplate() {
  try {
    const blob = await api.archive.employeeTemplate();
    saveTemplate(blob, "人员档案导入模板.xlsx");
  } catch (e) {
    importMessage.value = e instanceof Error ? e.message : "模板下载失败";
  }
}

async function uploadDepartmentsAndPositions(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  importBusy.value = true;
  importMessage.value = "";
  importResult.value = null;
  try {
    const result = await api.archive.importDepartmentsAndPositions(file);
    importResult.value = result;
    importMessage.value = `部门岗位导入完成：${importSummary(result)}`;
    await refresh();
    resetPersonForm();
    resetPositionForm();
  } catch (e) {
    importMessage.value = e instanceof Error ? e.message : "部门岗位导入失败";
  } finally {
    importBusy.value = false;
    input.value = "";
  }
}

async function uploadEmployees(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  importBusy.value = true;
  importMessage.value = "";
  importResult.value = null;
  try {
    const result = await api.archive.importEmployees(file);
    importResult.value = result;
    importMessage.value = `人员档案导入完成：${importSummary(result)}`;
    await refresh();
    resetPersonForm();
  } catch (e) {
    importMessage.value = e instanceof Error ? e.message : "人员档案导入失败";
  } finally {
    importBusy.value = false;
    input.value = "";
  }
}

async function submitOpenAccount() {
  try {
    await api.archive.openAccount({
      employeeId: openAccountForm.employeeId,
      account: openAccountForm.account.trim(),
      password: openAccountForm.loginPassword,
      role: openAccountForm.role
    });
    await refresh();
    openAccountForm.account = "";
    openAccountForm.loginPassword = "";
    openAccountForm.role = "employee";
  } catch (e) {
    dataError.value = e instanceof Error ? e.message : "开通失败";
  }
}

async function confirmRecycle(deviceId: string) {
  const supervisor = currentUser.value;
  if (!supervisor) return;
  try {
    await api.recycle.confirm(deviceId, supervisor.id);
    await refresh();
  } catch (e) {
    dataError.value = e instanceof Error ? e.message : "回收失败";
  }
}

async function submitTransfer() {
  const supervisor = currentUser.value;
  if (!supervisor) return;
  try {
    await api.recycle.transfer(supervisor.id, transferForm.deviceId, transferForm.employeeId);
    await refresh();
    transferForm.deviceId = "";
    transferForm.employeeId = "";
  } catch (e) {
    dataError.value = e instanceof Error ? e.message : "移交失败";
  }
}

async function submitStockIn() {
  const supervisor = currentUser.value;
  if (!supervisor) return;
  try {
    await api.recycle.stockIn(stockForm.deviceId, supervisor.id);
    await refresh();
    stockForm.deviceId = "";
  } catch (e) {
    dataError.value = e instanceof Error ? e.message : "入库失败";
  }
}
</script>

<template>
  <main class="app-shell">
    <section v-if="!currentUser && needsInitialSetup" class="login-page">
      <form class="login-card" @submit.prevent="initializeAdmin">
        <h1>工作设备登记系统</h1>
        <label>
          <span>姓名</span>
          <input v-model="setupForm.name" required />
        </label>
        <label>
          <span>账号</span>
          <input v-model="setupForm.account" autocomplete="username" required />
        </label>
        <label>
          <span>密码</span>
          <input v-model="setupForm.password" autocomplete="new-password" type="password" required />
        </label>
        <label>
          <span>部门名称</span>
          <input v-model="setupForm.departmentName" required />
        </label>
        <label>
          <span>岗位名称</span>
          <input v-model="setupForm.positionName" required />
        </label>
        <button class="primary-btn" type="submit">保存</button>
      </form>
    </section>

    <section v-else-if="!currentUser" class="login-page">
      <form class="login-card" @submit.prevent="login">
        <h1>工作设备登记系统</h1>
        <label>
          <span>账号</span>
          <input v-model="loginForm.account" autocomplete="username" required />
        </label>
        <label>
          <span>密码</span>
          <input v-model="loginForm.password" autocomplete="current-password" type="password" required />
        </label>
        <button class="primary-btn" type="submit">登录</button>
        <p v-if="loginError" class="error-text">{{ loginError }}</p>
      </form>
    </section>

    <section v-else class="workspace">
      <aside class="sidebar">
        <div class="brand">工作设备登记系统</div>
        <nav>
          <button
            v-for="item in currentMenu"
            :key="item.key"
            :class="{ active: item.key === activePage }"
            type="button"
            @click="setPage(item.key)"
          >
            {{ item.label }}
          </button>
        </nav>
      </aside>

      <section class="main-panel">
        <header class="topbar">
          <h2>{{ pageTitle }}</h2>
          <div class="top-actions">
            <span class="name-pill">{{ currentUser.name }}</span>
            <button class="ghost-btn" type="button" @click="logout">退出</button>
          </div>
        </header>

        <section class="content-grid">
          <div v-if="dataError" class="error-banner wide-panel" role="alert">
            <span>{{ dataError }}</span>
            <button class="error-banner-close" type="button" aria-label="关闭" @click="dataError = ''">×</button>
          </div>

          <form v-if="activePage === 'device'" class="form-panel" @submit.prevent="submitDevice">
            <div class="form-title">登记设备</div>
            <label>
              <span>设备类型</span>
              <select v-model="deviceForm.type" required>
                <option v-for="type in deviceTypes" :key="type" :value="type">{{ type }}</option>
              </select>
            </label>
            <label>
              <span>设备品牌</span>
              <input v-model="deviceForm.brand" required />
            </label>
            <label>
              <span>设备型号</span>
              <input v-model="deviceForm.model" required />
            </label>
            <label>
              <span>手机号</span>
              <input v-model="deviceForm.phoneNumber" required />
            </label>
            <label>
              <span>运营商</span>
              <select v-model="deviceForm.operator" required>
                <option v-for="operator in operators" :key="operator" :value="operator">{{ operator }}</option>
              </select>
            </label>
            <button class="primary-btn" type="submit">保存</button>
          </form>

          <form v-if="activePage === 'account'" class="form-panel" @submit.prevent="submitAccount">
            <div class="form-title">登记账号</div>
            <label>
              <span>渠道</span>
              <input v-model="accountForm.channel" required />
            </label>
            <label>
              <span>账号</span>
              <input v-model="accountForm.account" required />
            </label>
            <label>
              <span>密码</span>
              <input v-model="accountForm.password" type="password" required />
            </label>
            <label>
              <span>实名认证</span>
              <select v-model="accountForm.realNameStatus" required>
                <option v-for="status in realNameStatuses" :key="status" :value="status">{{ status }}</option>
              </select>
            </label>
            <label>
              <span>实名姓名</span>
              <input v-model="accountForm.realName" required />
            </label>
            <label>
              <span>身份证号</span>
              <input v-model="accountForm.idCardNumber" required />
            </label>
            <label>
              <span>手机号</span>
              <input v-model="accountForm.phoneNumber" required />
            </label>
            <label>
              <span>运营商</span>
              <select v-model="accountForm.operator" required>
                <option v-for="operator in operators" :key="operator" :value="operator">{{ operator }}</option>
              </select>
            </label>
            <button class="primary-btn" type="submit">保存</button>
          </form>

          <section v-if="activePage === 'device'" class="table-panel">
            <div class="section-title">名下设备</div>
            <table>
              <thead>
                <tr>
                  <th>设备类型</th>
                  <th>品牌</th>
                  <th>型号</th>
                  <th>手机号</th>
                  <th>运营商</th>
                  <th>来源部门</th>
                  <th>来源人员</th>
                  <th>取得方式</th>
                  <th>登记时间</th>
                  <th>接收时间</th>
                  <th>分配时间</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="device in currentDevices" :key="device.id">
                  <td>{{ device.type }}</td>
                  <td>{{ device.brand }}</td>
                  <td>{{ device.model }}</td>
                  <td>{{ getPhone(device.phoneId)?.number }}</td>
                  <td>{{ getPhone(device.phoneId)?.operator }}</td>
                  <td>{{ getDeviceSourceDepartmentName(device.id) }}</td>
                  <td>{{ getDeviceSourceEmployeeName(device.id) }}</td>
                  <td>{{ getDeviceAcquisitionType(device.id) }}</td>
                  <td>{{ device.registeredAt }}</td>
                  <td>{{ getDeviceReceiveTime(device.id) }}</td>
                  <td>{{ getDeviceAllocationTime(device.id) }}</td>
                  <td>
                    <span class="status-tag" :class="statusTone(device.status)">{{ device.status }}</span>
                  </td>
                </tr>
                <tr v-if="!currentDevices.length">
                  <td colspan="12">暂无数据</td>
                </tr>
              </tbody>
            </table>
          </section>

          <section v-if="activePage === 'account'" class="table-panel">
            <div class="section-title">名下账号</div>
            <table>
              <thead>
                <tr>
                  <th>渠道</th>
                  <th>账号</th>
                  <th>密码</th>
                  <th>实名认证</th>
                  <th>实名姓名</th>
                  <th>身份证号</th>
                  <th>手机号</th>
                  <th>运营商</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="account in currentAccounts" :key="account.id">
                  <td>{{ account.channel }}</td>
                  <td>{{ account.account }}</td>
                  <td>******</td>
                  <td>{{ account.realNameStatus }}</td>
                  <td>{{ account.realName }}</td>
                  <td>{{ account.idCardNumber }}</td>
                  <td>{{ getPhone(account.phoneId)?.number }}</td>
                  <td>{{ getPhone(account.phoneId)?.operator }}</td>
                  <td><span class="status-tag" :class="statusTone(account.status ?? '在用')">{{ account.status ?? "在用" }}</span></td>
                </tr>
                <tr v-if="!currentAccounts.length">
                  <td colspan="9">暂无数据</td>
                </tr>
              </tbody>
            </table>
          </section>

          <section
            v-if="['summary', 'deptSummary', 'allSummary'].includes(activePage)"
            class="summary-layout wide-panel"
          >
            <div class="filter-panel">
              <div class="section-title">筛选查询</div>
              <div class="checkbox-row">
                <label v-for="field in filterFields" :key="field.key" class="check-item">
                  <input v-model="summaryFilters" :value="field.key" type="checkbox" />
                  <span>{{ field.label }}</span>
                </label>
              </div>
              <div class="filter-grid">
                <label v-for="field in filterFields" v-show="summaryFilters.includes(field.key)" :key="field.key">
                  <span>{{ field.label }}</span>
                  <input v-model="summaryValues[field.key]" />
                </label>
                <label>
                  <span>精准查询</span>
                  <input v-model="preciseQuery" />
                </label>
              </div>
            </div>

            <div class="table-panel">
              <div class="section-title">分类明细表</div>
              <table>
                <thead>
                  <tr>
                    <th>人员编号</th>
                    <th>姓名</th>
                    <th>来源部门</th>
                    <th>来源人员</th>
                    <th>取得方式</th>
                    <th>登记时间</th>
                    <th>接收时间</th>
                    <th>分配时间</th>
                    <th>手机号</th>
                    <th>运营商</th>
                    <th>设备类型</th>
                    <th>品牌</th>
                    <th>型号</th>
                    <th>账号</th>
                    <th>状态</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in summaryDeviceRows" :key="row.id">
                    <td>{{ row.employeeNo }}</td>
                    <td>{{ row.name }}</td>
                    <td>{{ row.sourceDepartment }}</td>
                    <td>{{ row.sourceEmployee }}</td>
                    <td>{{ row.acquisitionType }}</td>
                    <td>{{ row.registeredAt }}</td>
                    <td>{{ row.receiveAt }}</td>
                    <td>{{ row.allocationAt }}</td>
                    <td>{{ row.phone }}</td>
                    <td>{{ row.operator }}</td>
                    <td>{{ row.type }}</td>
                    <td>{{ row.brand }}</td>
                    <td>{{ row.model }}</td>
                    <td>{{ row.accounts }}</td>
                    <td>
                      <span class="status-tag" :class="statusTone(row.status)">{{ row.status }}</span>
                    </td>
                  </tr>
                  <tr v-if="!summaryDeviceRows.length">
                    <td colspan="15">暂无数据</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="table-panel">
              <div class="section-title">账号明细表</div>
              <table>
                <thead>
                  <tr>
                    <th>人员编号</th>
                    <th>姓名</th>
                    <th>手机号</th>
                    <th>运营商</th>
                    <th>渠道</th>
                    <th>账号</th>
                    <th>密码</th>
                    <th>实名认证</th>
                    <th>实名姓名</th>
                    <th>身份证号</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in summaryAccountRows" :key="row.id">
                    <td>{{ row.employeeNo }}</td>
                    <td>{{ row.name }}</td>
                    <td>{{ row.phone }}</td>
                    <td>{{ row.operator }}</td>
                    <td>{{ row.channel }}</td>
                    <td>{{ row.account }}</td>
                    <td>{{ row.password }}</td>
                    <td>{{ row.realNameStatus }}</td>
                    <td>{{ row.realName }}</td>
                    <td>{{ row.idCardNumber }}</td>
                  </tr>
                  <tr v-if="!summaryAccountRows.length">
                    <td colspan="10">暂无数据</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <section v-if="activePage === 'resignation'" class="archive-layout wide-panel">
            <form class="form-panel" @submit.prevent="submitResignation">
              <div class="form-title">离职申请</div>
              <label>
                <span>申请说明</span>
                <input v-model="resignationForm.applicantNote" />
              </label>
              <button class="primary-btn" type="submit">提交</button>
            </form>

            <section class="table-panel">
              <div class="section-title">已有资产</div>
              <table>
                <thead>
                  <tr>
                    <th>资产类型</th>
                    <th>资产</th>
                    <th>手机号</th>
                    <th>状态</th>
                    <th>流程状态</th>
                    <th>回退原因</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in employeeHandoverRows" :key="`${row.assetType}-${row.assetId}`">
                    <td>{{ row.assetType }}</td>
                    <td>{{ row.assetText }}</td>
                    <td>{{ row.phone }}</td>
                    <td><span class="status-tag" :class="statusTone(row.status)">{{ row.status }}</span></td>
                    <td><span class="status-tag" :class="statusTone(row.task?.status ?? '未发起')">{{ row.task?.status ?? "未发起" }}</span></td>
                    <td>{{ row.task?.rejectReason }}</td>
                  </tr>
                  <tr v-if="!employeeHandoverRows.length">
                    <td colspan="6">暂无数据</td>
                  </tr>
                </tbody>
              </table>
            </section>
          </section>

          <section v-if="activePage === 'handoverApprove'" class="archive-layout wide-panel">
            <form class="form-panel" @submit.prevent="submitHandoverApproval">
              <div class="form-title">离职审批</div>
              <label>
                <span>申请记录</span>
                <select v-model="approvalForm.taskId" required>
                  <option value="" disabled>请选择</option>
                  <option v-for="task in supervisorApprovalTasks" :key="task.id" :value="task.id">
                    {{ getHandoverApplicant(task)?.name }} / {{ getHandoverApplicant(task)?.employeeNo }} / {{ taskAssetType(task) }} / {{ handoverDeviceText(task) }}
                  </option>
                </select>
              </label>
              <label>
                <span>处置方式</span>
                <select v-model="approvalForm.targetType" required>
                  <option v-for="targetType in handoverTargetTypes" :key="targetType" :value="targetType">
                    {{ targetType }}
                  </option>
                </select>
              </label>
              <label>
                <span>接收人</span>
                <select v-model="approvalForm.receiverEmployeeId" required>
                  <option value="" disabled>请选择</option>
                  <option v-for="employee in approvalReceiverOptions" :key="employee.id" :value="employee.id">
                    {{ employeeDisplay(employee) }}
                  </option>
                </select>
              </label>
              <p v-if="approvalForm.targetType === '回收入库' && !approvalReceiverOptions.length" class="form-help">
                请先在人员档案中把接收人标记为“回收接收人”。
              </p>
              <button class="primary-btn" type="submit">同意</button>
            </form>

            <section class="table-panel">
              <div class="section-title">申请列表</div>
              <table>
                <thead>
                  <tr>
                    <th>人员编号</th>
                    <th>姓名</th>
                    <th>部门</th>
                    <th>资产类型</th>
                    <th>手机号</th>
                    <th>资产</th>
                    <th>申请说明</th>
                    <th>申请时间</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="task in supervisorApprovalTasks" :key="task.id">
                    <td>{{ getHandoverApplicant(task)?.employeeNo }}</td>
                    <td>{{ getHandoverApplicant(task)?.name }}</td>
                    <td>{{ getDepartmentName(task.sourceDepartmentId) }}</td>
                    <td>{{ taskAssetType(task) }}</td>
                    <td>{{ getHandoverPhone(task)?.number }}</td>
                    <td>{{ handoverDeviceText(task) }}</td>
                    <td>{{ task.applicantNote }}</td>
                    <td>{{ task.createdAt }}</td>
                    <td>
                      <button class="small-btn" type="button" @click="approvalForm.taskId = task.id">选择</button>
                    </td>
                  </tr>
                  <tr v-if="!supervisorApprovalTasks.length">
                    <td colspan="9">暂无数据</td>
                  </tr>
                </tbody>
              </table>
            </section>
          </section>

          <section v-if="activePage === 'deviceAllocation'" class="archive-layout wide-panel">
            <form class="form-panel" @submit.prevent="submitDeviceAllocation">
              <div class="form-title">资产分配</div>
              <label>
                <span>资产类型</span>
                <select v-model="allocationForm.assetType" required>
                  <option value="设备">设备</option>
                  <option value="手机号">手机号</option>
                  <option value="账号">账号</option>
                </select>
              </label>
              <label>
                <span>资产</span>
                <select v-model="allocationForm.assetId" required>
                  <option value="" disabled>请选择</option>
                  <option
                    v-for="asset in allocationAssets.filter((item) => item.assetType === allocationForm.assetType)"
                    :key="`${asset.assetType}-${asset.assetId}`"
                    :value="asset.assetId"
                  >
                    {{ asset.assetText }} / {{ asset.phone }} / {{ asset.status }}
                  </option>
                </select>
              </label>
              <label>
                <span>接收人</span>
                <select v-model="allocationForm.receiverEmployeeId" required>
                  <option value="" disabled>请选择</option>
                  <option v-for="employee in allocationReceiverEmployees" :key="employee.id" :value="employee.id">
                    {{ employeeDisplay(employee) }}
                  </option>
                </select>
              </label>
              <button class="primary-btn" type="submit">分配</button>
            </form>

            <section class="table-panel">
              <div class="section-title">可分配资产</div>
              <table>
                <thead>
                  <tr>
                    <th>资产类型</th>
                    <th>资产</th>
                    <th>手机号</th>
                    <th>登记时间</th>
                    <th>状态</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="asset in allocationAssets" :key="`${asset.assetType}-${asset.assetId}`">
                    <td>{{ asset.assetType }}</td>
                    <td>{{ asset.assetText }}</td>
                    <td>{{ asset.phone }}</td>
                    <td>{{ asset.registeredAt }}</td>
                    <td><span class="status-tag" :class="statusTone(asset.status)">{{ asset.status }}</span></td>
                  </tr>
                  <tr v-if="!allocationAssets.length">
                    <td colspan="5">暂无数据</td>
                  </tr>
                </tbody>
              </table>
            </section>
          </section>

          <section v-if="activePage === 'receiveConfirm'" class="table-panel wide-panel">
            <div class="section-title">待确认</div>
            <table>
              <thead>
                <tr>
                  <th>发起人</th>
                  <th>部门</th>
                  <th>资产类型</th>
                  <th>手机号</th>
                  <th>资产</th>
                  <th>处置方式</th>
                  <th>审批人</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="task in receiverPendingTasks" :key="task.id">
                  <td>{{ getHandoverApplicant(task)?.name }} / {{ getHandoverApplicant(task)?.employeeNo }}</td>
                  <td>{{ getDepartmentName(task.sourceDepartmentId) }}</td>
                  <td>{{ taskAssetType(task) }}</td>
                  <td>{{ getHandoverPhone(task)?.number }}</td>
                  <td>{{ handoverDeviceText(task) }}</td>
                  <td>{{ task.targetType }}</td>
                  <td>{{ task.approvedByName }}</td>
                  <td>
                    <div class="button-row table-action-row">
                      <button class="small-btn" type="button" @click="confirmReceive(task.id)">确认接收</button>
                      <input v-model="returnReasons[task.id]" />
                      <button class="small-btn" type="button" @click="rejectReceive(task.id)">回退</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="!receiverPendingTasks.length">
                  <td colspan="8">暂无数据</td>
                </tr>
              </tbody>
            </table>
          </section>

          <section v-if="['people', 'peopleAdmin'].includes(activePage)" class="archive-layout wide-panel">
            <form class="form-panel" @submit.prevent="submitPerson(activePage === 'peopleAdmin')">
              <div class="form-title">人员档案</div>
              <label v-if="personForm.id">
                <span>人员编号</span>
                <input :value="personForm.employeeNo" disabled />
              </label>
              <label>
                <span>姓名</span>
                <input v-model="personForm.name" required />
              </label>
              <label>
                <span>性别</span>
                <select v-model="personForm.gender" required>
                  <option v-for="gender in genders" :key="gender" :value="gender">{{ gender }}</option>
                </select>
              </label>
              <label>
                <span>年龄</span>
                <input v-model.number="personForm.age" min="0" step="1" type="number" />
              </label>
              <label>
                <span>科室</span>
                <select v-model="personForm.departmentId" required>
                  <option v-for="department in state.departments" :key="department.id" :value="department.id">
                    {{ department.name }}
                  </option>
                </select>
              </label>
              <label>
                <span>岗位</span>
                <select v-model="personForm.positionId" required>
                  <option v-for="position in personDepartmentPositions" :key="position.id" :value="position.id">
                    {{ position.name }}
                  </option>
                </select>
              </label>
              <label>
                <span>入职时间</span>
                <input v-model="personForm.hireDate" type="date" required />
              </label>
              <label>
                <span>状态</span>
                <select v-model="personForm.status" required>
                  <option v-for="status in employeeStatuses" :key="status" :value="status">{{ status }}</option>
                </select>
              </label>
              <label class="check-item">
                <input v-model="personForm.recycleReceiver" type="checkbox" />
                <span>回收接收人</span>
              </label>
              <p class="form-help">标记后，离职审批选择“回收入库”时可作为接收人。</p>
              <template v-if="activePage === 'peopleAdmin'">
                <label>
                  <span>账号</span>
                  <input v-model="personForm.account" required />
                </label>
                <label>
                  <span>密码</span>
                  <input v-model="personForm.loginPassword" :required="!personForm.id" type="password" />
                </label>
                <label>
                  <span>账号类型</span>
                  <select v-model="personForm.role" required>
                    <option value="employee">员工</option>
                    <option value="supervisor">主管</option>
                    <option value="hr">人事</option>
                    <option value="admin">管理员</option>
                  </select>
                </label>
              </template>
              <div class="button-row">
                <button class="primary-btn" type="submit">保存</button>
                <button class="ghost-btn" type="button" @click="cancelPersonEdit">取消</button>
                <button class="ghost-btn" type="button" @click="resetPersonForm">清空</button>
              </div>
            </form>

            <section class="table-panel compact-table-panel">
              <div class="table-toolbar">
                <div class="section-title">人员列表</div>
                <label class="inline-search">
                  <span>查询</span>
                  <input v-model="personSearchQuery" placeholder="输入姓名、人员编号、账号、科室或岗位" />
                </label>
                <button v-if="personSearchQuery" class="ghost-btn" type="button" @click="personSearchQuery = ''">清空</button>
                <span class="form-help">共 {{ filteredEmployees.length }} / {{ state.employees.length }} 人</span>
              </div>
              <table>
                <thead>
                  <tr>
                    <th>人员编号</th>
                    <th>姓名</th>
                    <th>性别</th>
                    <th>年龄</th>
                    <th>科室</th>
                    <th>岗位</th>
                    <th>入职时间</th>
                    <th>状态</th>
                    <th>回收接收</th>
                    <th v-if="activePage === 'peopleAdmin'">账号</th>
                    <th v-if="activePage === 'peopleAdmin'">密码</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="employee in filteredEmployees" :key="employee.id">
                    <td>{{ employee.employeeNo }}</td>
                    <td>{{ employee.name }}</td>
                    <td>{{ employee.gender }}</td>
                    <td>{{ employee.age }}</td>
                    <td>{{ getDepartmentName(employee.departmentId) }}</td>
                    <td>{{ getPositionName(employee.positionId) }}</td>
                    <td>{{ employee.hireDate }}</td>
                    <td>
                      <span class="status-tag" :class="statusTone(employee.status)">{{ employee.status }}</span>
                    </td>
                    <td>{{ employee.recycleReceiver ? "是" : "否" }}</td>
                    <td v-if="activePage === 'peopleAdmin'">{{ employee.account }}</td>
                    <td v-if="activePage === 'peopleAdmin'">******</td>
                    <td>
                      <button class="small-btn" type="button" @click="editPerson(employee)">修改</button>
                    </td>
                  </tr>
                  <tr v-if="!filteredEmployees.length">
                    <td :colspan="activePage === 'peopleAdmin' ? 12 : 10">未找到匹配人员</td>
                  </tr>
                </tbody>
              </table>
            </section>
          </section>

          <section v-if="activePage === 'departments'" class="archive-layout wide-panel">
            <form class="form-panel" @submit.prevent="submitDepartment">
              <div class="form-title">部门档案</div>
              <label>
                <span>部门名称</span>
                <input v-model="deptForm.name" required />
              </label>
              <div class="button-row">
                <button class="primary-btn" type="submit">{{ deptForm.id ? "修改" : "新建" }}</button>
                <button class="ghost-btn" type="button" @click="deptForm.id = ''; deptForm.name = ''">清空</button>
              </div>
            </form>
            <section class="table-panel compact-table-panel">
              <div class="section-title">部门列表</div>
              <table>
                <thead>
                  <tr>
                    <th>部门名称</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="department in state.departments" :key="department.id">
                    <td>{{ department.name }}</td>
                    <td>
                      <button class="small-btn" type="button" @click="editDepartment(department)">修改</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </section>
          </section>

          <section v-if="activePage === 'positions'" class="archive-layout wide-panel">
            <form class="form-panel" @submit.prevent="submitPosition">
              <div class="form-title">岗位档案</div>
              <label>
                <span>所属部门</span>
                <select v-model="positionForm.departmentId" required>
                  <option v-for="department in state.departments" :key="department.id" :value="department.id">
                    {{ department.name }}
                  </option>
                </select>
              </label>
              <label>
                <span>岗位名称</span>
                <input v-model="positionForm.name" required />
              </label>
              <div class="button-row">
                <button class="primary-btn" type="submit">{{ positionForm.id ? "修改" : "新建" }}</button>
                <button class="ghost-btn" type="button" @click="resetPositionForm">清空</button>
              </div>
            </form>
            <section class="table-panel">
              <div class="section-title">岗位列表</div>
              <table>
                <thead>
                  <tr>
                    <th>所属部门</th>
                    <th>岗位名称</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="position in state.positions" :key="position.id">
                    <td>{{ getDepartmentName(position.departmentId) }}</td>
                    <td>{{ position.name }}</td>
                    <td>
                      <button class="small-btn" type="button" @click="editPosition(position)">修改</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </section>
          </section>

          <section v-if="activePage === 'deptPosition'" class="dept-position-layout wide-panel">
            <form class="form-panel" @submit.prevent="submitDepartment">
              <div class="form-title">部门档案</div>
              <label>
                <span>部门名称</span>
                <input v-model="deptForm.name" required />
              </label>
              <button class="primary-btn" type="submit">{{ deptForm.id ? "修改" : "新建" }}</button>
            </form>
            <form class="form-panel" @submit.prevent="submitPosition">
              <div class="form-title">岗位档案</div>
              <label>
                <span>所属部门</span>
                <select v-model="positionForm.departmentId" required>
                  <option v-for="department in state.departments" :key="department.id" :value="department.id">
                    {{ department.name }}
                  </option>
                </select>
              </label>
              <label>
                <span>岗位名称</span>
                <input v-model="positionForm.name" required />
              </label>
              <button class="primary-btn" type="submit">{{ positionForm.id ? "修改" : "新建" }}</button>
            </form>
            <section class="table-panel compact-table-panel">
              <div class="section-title">部门列表</div>
              <table>
                <thead>
                  <tr>
                    <th>部门名称</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="department in state.departments" :key="department.id">
                    <td>{{ department.name }}</td>
                    <td><button class="small-btn" type="button" @click="editDepartment(department)">修改</button></td>
                  </tr>
                </tbody>
              </table>
            </section>
            <section class="table-panel compact-table-panel">
              <div class="section-title">岗位列表</div>
              <table>
                <thead>
                  <tr>
                    <th>所属部门</th>
                    <th>岗位名称</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="position in state.positions" :key="position.id">
                    <td>{{ getDepartmentName(position.departmentId) }}</td>
                    <td>{{ position.name }}</td>
                    <td><button class="small-btn" type="button" @click="editPosition(position)">修改</button></td>
                  </tr>
                </tbody>
              </table>
            </section>
          </section>

          <section v-if="activePage === 'dataPermission'" class="archive-layout wide-panel">
            <form class="form-panel" @submit.prevent="submitDataPermission">
              <div class="form-title">数据权限配置</div>
              <label>
                <span>主管账号</span>
                <select v-model="dataPermissionForm.supervisorId" required @change="loadSupervisorDataPermission(dataPermissionForm.supervisorId)">
                  <option value="" disabled>请选择主管</option>
                  <option v-for="employee in supervisorEmployees" :key="employee.id" :value="employee.id">
                    {{ employeeDisplay(employee) }}
                  </option>
                </select>
              </label>

              <section v-for="department in state.departments" :key="department.id" class="form-subsection">
                <label class="checkbox-row">
                  <input
                    :checked="isPermissionDepartmentSelected(department.id)"
                    type="checkbox"
                    @change="togglePermissionDepartment(department.id, eventChecked($event))"
                  />
                  <span>{{ department.name }}</span>
                </label>
                <div v-if="isPermissionDepartmentSelected(department.id)" class="permission-position-box">
                  <label class="checkbox-row">
                    <input
                      :checked="isAllPositionsSelected(department.id)"
                      type="checkbox"
                      @change="toggleAllPositions(department.id, eventChecked($event))"
                    />
                    <span>该部门全部岗位</span>
                  </label>
                  <label v-for="position in positionsByDepartment(department.id)" :key="position.id" class="checkbox-row">
                    <input
                      :checked="isPermissionPositionSelected(department.id, position.id)"
                      :disabled="isAllPositionsSelected(department.id)"
                      type="checkbox"
                      @change="togglePermissionPosition(department.id, position.id, eventChecked($event))"
                    />
                    <span>{{ position.name }}</span>
                  </label>
                  <p v-if="!positionsByDepartment(department.id).length" class="form-help">该部门暂无岗位。</p>
                </div>
              </section>

              <div class="button-row">
                <button class="primary-btn" type="submit">保存权限</button>
                <button class="ghost-btn" type="button" @click="resetDataPermissionForm">清空选择</button>
              </div>
              <p v-if="dataPermissionMessage" class="form-help">{{ dataPermissionMessage }}</p>
            </form>

            <section class="table-panel">
              <div class="section-title">已配置权限</div>
              <table>
                <thead>
                  <tr>
                    <th>主管</th>
                    <th>授权部门</th>
                    <th>岗位范围</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="scope in supervisorScopesForSelected" :key="scope.id">
                    <td>{{ selectedDataPermissionSupervisor ? employeeDisplay(selectedDataPermissionSupervisor) : "" }}</td>
                    <td>{{ getDepartmentName(scope.departmentId) }}</td>
                    <td>{{ scope.allPositions ? "全部岗位" : getPositionName(scope.positionId ?? "") }}</td>
                  </tr>
                  <tr v-if="!supervisorScopesForSelected.length">
                    <td colspan="3">暂无配置</td>
                  </tr>
                </tbody>
              </table>
              <p class="form-help">员工账号只看本人；HR 的人员档案看全部；管理员看全部；主管按这里配置的部门和岗位查看人员档案与资产数据。</p>
            </section>
          </section>

          <section v-if="activePage === 'archiveImport'" class="archive-layout wide-panel">
            <section class="form-panel">
              <div class="form-title">部门岗位导入</div>
              <p class="form-help">Excel 建议包含两个 Sheet：部门 Sheet 表头为“部门名称”；岗位 Sheet 表头为“职位名称、所属部门”。</p>
              <input
                ref="deptPositionImportInput"
                accept=".xlsx,.xls"
                class="hidden-file-input"
                type="file"
                @change="uploadDepartmentsAndPositions"
              />
              <div class="button-row">
                <button class="ghost-btn" type="button" @click="downloadDepartmentsAndPositionsTemplate">下载模板</button>
                <button class="primary-btn" :disabled="importBusy" type="button" @click="deptPositionImportInput?.click()">
                  {{ importBusy ? "导入中" : "选择Excel导入" }}
                </button>
              </div>
            </section>

            <section class="form-panel">
              <div class="form-title">人员档案导入</div>
              <p class="form-help">人员 Sheet 表头为“姓名、性别、入职时间、用户名、职位、部门”。导入后默认状态为在职，角色为员工，初始密码为 123456。</p>
              <input
                ref="employeeImportInput"
                accept=".xlsx,.xls"
                class="hidden-file-input"
                type="file"
                @change="uploadEmployees"
              />
              <div class="button-row">
                <button class="ghost-btn" type="button" @click="downloadEmployeesTemplate">下载模板</button>
                <button class="primary-btn" :disabled="importBusy" type="button" @click="employeeImportInput?.click()">
                  {{ importBusy ? "导入中" : "选择Excel导入" }}
                </button>
              </div>
            </section>

            <section class="table-panel compact-table-panel">
              <div class="section-title">导入结果</div>
              <p class="form-help">{{ importMessage || "请选择 Excel 文件开始导入。" }}</p>
              <table v-if="importResult?.errors.length">
                <thead>
                  <tr>
                    <th>未导入原因</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="error in importResult.errors" :key="error">
                    <td>{{ error }}</td>
                  </tr>
                </tbody>
              </table>
            </section>
          </section>

          <section v-if="activePage === 'openAccount'" class="archive-layout wide-panel">
            <form class="form-panel" @submit.prevent="submitOpenAccount">
              <div class="form-title">开通账号</div>
              <label>
                <span>姓名</span>
                <select v-model="openAccountForm.employeeId" required>
                  <option v-for="employee in state.employees" :key="employee.id" :value="employee.id">
                    {{ employeeDisplay(employee) }}
                  </option>
                </select>
              </label>
              <label>
                <span>账号</span>
                <input v-model="openAccountForm.account" required />
              </label>
              <label>
                <span>密码</span>
                <input v-model="openAccountForm.loginPassword" type="password" required />
              </label>
              <label>
                <span>账号类型</span>
                <select v-model="openAccountForm.role" required>
                  <option value="employee">员工</option>
                  <option value="supervisor">主管</option>
                </select>
              </label>
              <button class="primary-btn" type="submit">保存</button>
            </form>

            <section class="table-panel">
              <div class="section-title">账号列表</div>
              <table>
                <thead>
                  <tr>
                    <th>人员编号</th>
                    <th>姓名</th>
                    <th>账号</th>
                    <th>密码</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="employee in state.employees" :key="employee.id">
                    <td>{{ employee.employeeNo }}</td>
                    <td>{{ employee.name }}</td>
                    <td>{{ employee.account }}</td>
                    <td>******</td>
                  </tr>
                </tbody>
              </table>
            </section>
          </section>

          <section v-if="activePage === 'recyclePending'" class="table-panel wide-panel">
            <div class="section-title">待回收</div>
            <table>
              <thead>
                <tr>
                  <th>人员编号</th>
                  <th>姓名</th>
                  <th>手机号</th>
                  <th>设备类型</th>
                  <th>品牌</th>
                  <th>型号</th>
                  <th>来源</th>
                  <th>发起人</th>
                  <th>产生原因</th>
                  <th>发起时间</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="device in pendingRecycleDevices" :key="device.id">
                  <td>{{ getEmployee(device.employeeId)?.employeeNo }}</td>
                  <td>{{ getEmployee(device.employeeId)?.name }}</td>
                  <td>{{ getPhone(device.phoneId)?.number }}</td>
                  <td>{{ device.type }}</td>
                  <td>{{ device.brand }}</td>
                  <td>{{ device.model }}</td>
                  <td>{{ device.recycleSource }}</td>
                  <td>{{ device.recycleInitiatorName }}</td>
                  <td>{{ device.recycleReason }}</td>
                  <td>{{ device.recycleCreatedAt }}</td>
                  <td><span class="status-tag" :class="statusTone(device.status)">{{ device.status }}</span></td>
                </tr>
                <tr v-if="!pendingRecycleDevices.length">
                  <td colspan="11">暂无数据</td>
                </tr>
              </tbody>
            </table>
          </section>

          <section v-if="activePage === 'recycleConfirm'" class="table-panel wide-panel">
            <div class="section-title">回收确认</div>
            <table>
              <thead>
                <tr>
                  <th>人员编号</th>
                  <th>姓名</th>
                  <th>手机号</th>
                  <th>设备类型</th>
                  <th>品牌</th>
                  <th>型号</th>
                  <th>来源</th>
                  <th>发起人</th>
                  <th>产生原因</th>
                  <th>发起时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="device in pendingRecycleDevices" :key="device.id">
                  <td>{{ getEmployee(device.employeeId)?.employeeNo }}</td>
                  <td>{{ getEmployee(device.employeeId)?.name }}</td>
                  <td>{{ getPhone(device.phoneId)?.number }}</td>
                  <td>{{ device.type }}</td>
                  <td>{{ device.brand }}</td>
                  <td>{{ device.model }}</td>
                  <td>{{ device.recycleSource }}</td>
                  <td>{{ device.recycleInitiatorName }}</td>
                  <td>{{ device.recycleReason }}</td>
                  <td>{{ device.recycleCreatedAt }}</td>
                  <td>
                    <button class="small-btn" type="button" @click="confirmRecycle(device.id)">确认回收</button>
                  </td>
                </tr>
                <tr v-if="!pendingRecycleDevices.length">
                  <td colspan="11">暂无数据</td>
                </tr>
              </tbody>
            </table>
          </section>

          <section v-if="activePage === 'transfer'" class="archive-layout wide-panel">
            <form class="form-panel" @submit.prevent="submitTransfer">
              <div class="form-title">移交新员工</div>
              <label>
                <span>设备</span>
                <select v-model="transferForm.deviceId" required>
                  <option value="" disabled>请选择</option>
                  <option v-for="device in transferableDevices" :key="device.id" :value="device.id">
                    {{ getDeviceOwnerName(device) }} / {{ device.brand }} {{ device.model }} / {{ getPhone(device.phoneId)?.number }} / {{ device.status }}
                  </option>
                </select>
              </label>
              <label>
                <span>新员工</span>
                <select v-model="transferForm.employeeId" required>
                  <option value="" disabled>请选择</option>
                  <option v-for="employee in selectableTransferEmployees" :key="employee.id" :value="employee.id">
                    {{ employeeDisplay(employee) }}
                  </option>
                </select>
              </label>
              <button class="primary-btn" type="submit">保存</button>
            </form>

            <section class="table-panel">
              <div class="section-title">已有设备</div>
              <table>
                <thead>
                  <tr>
                    <th>当前归属</th>
                    <th>手机号</th>
                    <th>设备类型</th>
                    <th>品牌</th>
                    <th>型号</th>
                    <th>状态</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="device in transferableDevices" :key="device.id">
                    <td>{{ getDeviceOwnerName(device) }}</td>
                    <td>{{ getPhone(device.phoneId)?.number }}</td>
                    <td>{{ device.type }}</td>
                    <td>{{ device.brand }}</td>
                    <td>{{ device.model }}</td>
                    <td><span class="status-tag" :class="statusTone(device.status)">{{ device.status }}</span></td>
                  </tr>
                  <tr v-if="!transferableDevices.length">
                    <td colspan="6">暂无数据</td>
                  </tr>
                </tbody>
              </table>
            </section>
          </section>

          <section v-if="activePage === 'stockIn'" class="archive-layout wide-panel">
            <form class="form-panel" @submit.prevent="submitStockIn">
              <div class="form-title">旧机入库</div>
              <label>
                <span>设备</span>
                <select v-model="stockForm.deviceId" required>
                  <option value="" disabled>请选择</option>
                  <option v-for="device in confirmedRecycleDevices" :key="device.id" :value="device.id">
                    {{ device.brand }} {{ device.model }} / {{ getPhone(device.phoneId)?.number }}
                  </option>
                </select>
              </label>
              <button class="primary-btn" type="submit">保存</button>
            </form>

            <section class="table-panel">
              <div class="section-title">旧机入库</div>
              <table>
                <tbody>
                  <tr v-for="device in stockInDevices" :key="device.id">
                    <td>{{ device.brand }} {{ device.model }}</td>
                    <td>{{ getPhone(device.phoneId)?.number }}</td>
                    <td>{{ device.status }}</td>
                  </tr>
                  <tr v-if="!stockInDevices.length">
                    <td colspan="3">暂无数据</td>
                  </tr>
                </tbody>
              </table>
            </section>
          </section>
        </section>
      </section>
    </section>
  </main>
</template>
