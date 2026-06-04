import { api, type DeviceAsset, type PhoneNumber as PhoneBackend, type ChannelAccount as AccountBackend, type Employee as EmployeeBackend, type Department, type Position, type HandoverTask, type HandoverTargetType, type HandoverStatus, type DeviceStatus, type EmployeeStatus, type Role } from "./client";

export type {
  Department,
  Position,
  HandoverTask,
  HandoverTargetType,
  HandoverStatus,
  DeviceStatus,
  EmployeeStatus,
  Role
};

export type { DeviceSummaryRow, AccountSummaryRow, SummaryRows } from "./client";

export interface Employee {
  id: string;
  employeeNo: string;
  name: string;
  gender: "男" | "女";
  age: number;
  departmentId: string;
  positionId: string;
  hireDate: string;
  status: EmployeeStatus;
  account: string;
  loginPassword: string;
  role: Role;
}

export interface PhoneNumber {
  id: string;
  employeeId: string;
  number: string;
  operator: string;
  status?: DeviceStatus;
  registeredAt?: string;
}

export interface Device {
  id: string;
  employeeId: string;
  departmentId: string;
  type: string;
  brand: string;
  model: string;
  phoneId: string;
  status: DeviceStatus;
  registeredAt?: string;
  recycleInitiatorId?: string;
  recycleInitiatorName?: string;
  recycleSource?: string;
  recycleReason?: string;
  recycleCreatedAt?: string;
}

export interface ChannelAccount {
  id: string;
  employeeId: string;
  phoneId: string;
  channel: string;
  account: string;
  password: string;
  realNameStatus: "已实名" | "未实名";
  realName: string;
  idCardNumber: string;
  status?: DeviceStatus;
  registeredAt?: string;
}

export function adaptEmployee(e: EmployeeBackend): Employee {
  return {
    id: e.id,
    employeeNo: e.employeeNo,
    name: e.name,
    gender: e.gender === "女" ? "女" : "男",
    age: e.age,
    departmentId: e.departmentId,
    positionId: e.positionId,
    hireDate: e.hireDate,
    status: (e.status as EmployeeStatus) ?? "在职",
    account: e.loginAccount,
    loginPassword: "",
    role: (e.roleCode as Role) ?? "employee"
  };
}

export function adaptPhone(p: PhoneBackend): PhoneNumber {
  return {
    id: p.id,
    employeeId: p.employeeId,
    number: p.phoneNumber,
    operator: p.operator,
    status: (p.status as DeviceStatus) ?? "在用",
    registeredAt: p.registeredAt
  };
}

export function adaptDevice(d: DeviceAsset): Device {
  return {
    id: d.id,
    employeeId: d.employeeId ?? "",
    departmentId: d.departmentId,
    type: d.deviceType,
    brand: d.brand,
    model: d.model,
    phoneId: d.phoneId,
    status: (d.status as DeviceStatus) ?? "在用",
    registeredAt: d.registeredAt,
    recycleInitiatorId: d.recycleInitiatorEmployeeId ?? "",
    recycleInitiatorName: d.recycleInitiatorName ?? "",
    recycleSource: d.recycleSource ?? "",
    recycleReason: d.recycleReason ?? "",
    recycleCreatedAt: d.recycleCreatedAt ?? ""
  };
}

export function adaptAccount(a: AccountBackend): ChannelAccount {
  return {
    id: a.id,
    employeeId: a.employeeId,
    phoneId: a.phoneId,
    channel: a.channel,
    account: a.accountName,
    password: "******",
    realNameStatus: (a.realNameStatus as "已实名" | "未实名") ?? "已实名",
    realName: a.realName,
    idCardNumber: a.idCardCipher ?? "",
    status: (a.status as DeviceStatus) ?? "在用",
    registeredAt: a.registeredAt
  };
}

export { api };
