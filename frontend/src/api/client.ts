const API_BASE = "/api";
const AUTH_TOKEN_KEY = "work-device-registry-token";

class HttpError extends Error {
  status: number;
  body: unknown;
  constructor(status: number, body: unknown, message: string) {
    super(message);
    this.status = status;
    this.body = body;
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const isFormData = init.body instanceof FormData;
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  const headers = new Headers(init.headers);
  if (!isFormData && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers
  });
  const text = await res.text();
  let parsed: unknown = text;
  if (text) {
    try { parsed = JSON.parse(text); } catch { /* keep text */ }
  }
  if (!res.ok) {
    const msg = (parsed && typeof parsed === "object" && "message" in parsed && typeof (parsed as { message: unknown }).message === "string")
      ? (parsed as { message: string }).message
      : `HTTP ${res.status}`;
    throw new HttpError(res.status, parsed, msg);
  }
  return parsed as T;
}

async function requestBlob(path: string): Promise<Blob> {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  const res = await fetch(`${API_BASE}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  });
  if (!res.ok) {
    const text = await res.text();
    throw new HttpError(res.status, text, text || `HTTP ${res.status}`);
  }
  return res.blob();
}

export type Role = "admin" | "supervisor" | "hr" | "employee";
export type EmployeeStatus = "在职" | "离职申请中" | "离职";
export type DeviceStatus = "在用" | "待回收" | "接收待确认" | "已回收" | "已移交" | "旧机入库";
export type HandoverStatus = "待主管审批" | "待接收确认" | "已完成" | "已回退";
export type HandoverTargetType = "本部门员工" | "其它部门员工" | "回收入库" | "资产分配";

export interface Department { id: string; name: string; }
export interface Position { id: string; departmentId: string; name: string; }
export interface SupervisorDataScope {
  id: string;
  supervisorId: string;
  departmentId: string;
  positionId: string | null;
  allPositions: boolean;
  createdAt: string;
  updatedAt: string;
}
export interface SupervisorDataScopeInput {
  departmentId: string;
  allPositions: boolean;
  positionIds: string[];
}
export interface ImportResult {
  totalRows: number;
  successRows: number;
  createdRows: number;
  skippedRows: number;
  errors: string[];
}
export interface Employee {
  id: string;
  employeeNo: string;
  name: string;
  gender: string;
  age: number;
  departmentId: string;
  positionId: string;
  hireDate: string;
  status: EmployeeStatus;
  loginAccount: string;
  roleCode: Role;
  recycleReceiver: boolean;
  createdAt: string;
  updatedAt: string;
}
export interface PhoneNumber { id: string; employeeId: string; phoneNumber: string; operator: string; status?: string; registeredAt: string; }
export interface DeviceAsset {
  id: string;
  employeeId: string | null;
  departmentId: string;
  phoneId: string;
  deviceType: string;
  brand: string;
  model: string;
  status: string;
  registeredAt: string;
  recycleInitiatorEmployeeId?: string | null;
  recycleInitiatorName?: string | null;
  recycleSource?: string | null;
  recycleReason?: string | null;
  recycleCreatedAt?: string | null;
}
export interface ChannelAccount {
  id: string;
  employeeId: string;
  phoneId: string;
  channel: string;
  accountName: string;
  realNameStatus: string;
  realName: string;
  idCardCipher: string;
  status: string;
  registeredAt: string;
}
export interface HandoverTask {
  id: string;
  applicantId: string;
  sourceDepartmentId: string;
  deviceId: string;
  assetType: string;
  assetId: string;
  targetType: HandoverTargetType;
  receiverEmployeeId: string;
  receiverDepartmentId: string;
  approvedById: string;
  approvedByName: string;
  status: HandoverStatus;
  applicantNote: string;
  rejectReason: string;
  createdAt: string;
  updatedAt: string;
}
export interface DeviceSummaryRow {
  id: string;
  employeeNo: string;
  name: string;
  sourceDepartment: string;
  sourceEmployee: string;
  acquisitionType: string;
  registeredAt: string;
  receiveAt: string;
  allocationAt: string;
  phone: string;
  operator: string;
  type: string;
  brand: string;
  model: string;
  accounts: string;
  status: string;
}
export interface AccountSummaryRow {
  id: string;
  employeeNo: string;
  name: string;
  phone: string;
  operator: string;
  channel: string;
  account: string;
  password: string;
  realNameStatus: string;
  realName: string;
  idCardNumber: string;
}
export interface SummaryRows { devices: DeviceSummaryRow[]; accounts: AccountSummaryRow[]; }
export interface LoginResponse { user: Employee; token: string; }

export function saveAuthToken(token: string) {
  localStorage.setItem(AUTH_TOKEN_KEY, token);
}

export function clearAuthToken() {
  localStorage.removeItem(AUTH_TOKEN_KEY);
}

export const api = {
  auth: {
    setupRequired() {
      return request<{ required: boolean }>("/auth/setup-required");
    },
    setup(payload: { name: string; account: string; password: string; departmentName: string; positionName: string; }) {
      return request<LoginResponse>("/auth/setup", { method: "POST", body: JSON.stringify(payload) });
    },
    login(payload: { account: string; password: string; }) {
      return request<LoginResponse>("/auth/login", { method: "POST", body: JSON.stringify(payload) });
    }
  },
  archive: {
    employees: () => request<Employee[]>("/archive/employees"),
    saveEmployee(payload: Record<string, unknown>, includeAccount: boolean) {
      const qs = includeAccount ? "?includeAccount=true" : "";
      return request<Employee>(`/archive/employees${qs}`, { method: "POST", body: JSON.stringify(payload) });
    },
    departments: () => request<Department[]>("/archive/departments"),
    saveDepartment(payload: { id?: string; name: string }) {
      return request<Department>("/archive/departments", { method: "POST", body: JSON.stringify(payload) });
    },
    positions: () => request<Position[]>("/archive/positions"),
    savePosition(payload: { id?: string; departmentId: string; name: string }) {
      return request<Position>("/archive/positions", { method: "POST", body: JSON.stringify(payload) });
    },
    importDepartmentsAndPositions(file: File) {
      const form = new FormData();
      form.append("file", file);
      return request<ImportResult>("/archive/import/departments-positions", { method: "POST", body: form });
    },
    importEmployees(file: File) {
      const form = new FormData();
      form.append("file", file);
      return request<ImportResult>("/archive/import/employees", { method: "POST", body: form });
    },
    departmentPositionTemplate() {
      return requestBlob("/archive/import/templates/departments-positions");
    },
    employeeTemplate() {
      return requestBlob("/archive/import/templates/employees");
    },
    phones: () => request<PhoneNumber[]>("/archive/phones"),
    devices: () => request<DeviceAsset[]>("/archive/devices"),
    accounts: () => request<ChannelAccount[]>("/archive/accounts"),
    openAccount(payload: { employeeId: string; account: string; password: string; role: Role }) {
      return request<Employee>("/archive/accounts/open", { method: "POST", body: JSON.stringify(payload) });
    }
  },
  permissions: {
    supervisorScopes: () => request<SupervisorDataScope[]>("/permissions/supervisors/scopes"),
    supervisorScope(supervisorId: string) {
      return request<SupervisorDataScope[]>(`/permissions/supervisors/${supervisorId}/scopes`);
    },
    saveSupervisorScope(supervisorId: string, scopes: SupervisorDataScopeInput[]) {
      return request<SupervisorDataScope[]>(`/permissions/supervisors/${supervisorId}/scopes`, {
        method: "POST",
        body: JSON.stringify({ scopes })
      });
    }
  },
  registry: {
    registerDevice(employeeId: string, payload: { type: string; brand: string; model: string; phoneNumber: string; operator: string }) {
      return request<DeviceAsset>(`/registry/users/${employeeId}/devices`, { method: "POST", body: JSON.stringify(payload) });
    },
    registerAccount(employeeId: string, payload: { channel: string; account: string; password: string; realNameStatus: string; realName: string; idCardNumber: string; phoneNumber: string; operator: string }) {
      return request<ChannelAccount>(`/registry/users/${employeeId}/accounts`, { method: "POST", body: JSON.stringify(payload) });
    },
    summary(employeeId: string, scope: "mine" | "department" | "all" = "mine") {
      return request<SummaryRows>(`/registry/users/${employeeId}/summary?scope=${scope}`);
    }
  },
  recycle: {
    confirm(deviceId: string, supervisorId: string) {
      return request<DeviceAsset>(`/recycle/${deviceId}/confirm?supervisorId=${encodeURIComponent(supervisorId)}`, { method: "POST" });
    },
    transfer(supervisorId: string, deviceId: string, employeeId: string) {
      return request<DeviceAsset>(`/recycle/transfer?supervisorId=${encodeURIComponent(supervisorId)}`, { method: "POST", body: JSON.stringify({ deviceId, employeeId }) });
    },
    stockIn(deviceId: string, supervisorId: string) {
      return request<DeviceAsset>(`/recycle/${deviceId}/stock-in?supervisorId=${encodeURIComponent(supervisorId)}`, { method: "POST" });
    }
  },
  handover: {
    tasks: () => request<HandoverTask[]>("/handover"),
    submitResignation(employeeId: string, applicantNote: string) {
      return request<HandoverTask[]>(`/handover/users/${employeeId}/resignation`, { method: "POST", body: JSON.stringify({ applicantNote }) });
    },
    approve(supervisorId: string, payload: { taskId: string; targetType: HandoverTargetType; receiverEmployeeId: string }) {
      return request<HandoverTask>(`/handover/approve?supervisorId=${encodeURIComponent(supervisorId)}`, { method: "POST", body: JSON.stringify(payload) });
    },
    allocate(allocatorId: string, payload: { assetType: string; assetId: string; deviceId?: string; receiverEmployeeId: string }) {
      return request<HandoverTask>(`/handover/allocate?allocatorId=${encodeURIComponent(allocatorId)}`, { method: "POST", body: JSON.stringify(payload) });
    },
    confirm(taskId: string, receiverId: string) {
      return request<HandoverTask>(`/handover/${taskId}/confirm?receiverId=${encodeURIComponent(receiverId)}`, { method: "POST" });
    },
    reject(receiverId: string, taskId: string, rejectReason: string) {
      return request<HandoverTask>(`/handover/reject?receiverId=${encodeURIComponent(receiverId)}`, { method: "POST", body: JSON.stringify({ taskId, rejectReason }) });
    }
  }
};

export { HttpError };
