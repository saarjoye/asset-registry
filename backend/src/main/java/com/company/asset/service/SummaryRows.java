package com.company.asset.service;

import com.company.asset.entity.ChannelAccount;
import com.company.asset.entity.DeviceAsset;
import java.util.List;

public record SummaryRows(List<DeviceSummaryRow> devices, List<AccountSummaryRow> accounts) {
  public record DeviceSummaryRow(
      String id,
      String employeeNo,
      String name,
      String sourceDepartment,
      String sourceEmployee,
      String acquisitionType,
      String registeredAt,
      String receiveAt,
      String allocationAt,
      String phone,
      String operator,
      String type,
      String brand,
      String model,
      String accounts,
      String status
  ) {
  }

  public record AccountSummaryRow(
      String id,
      String employeeNo,
      String name,
      String phone,
      String operator,
      String channel,
      String account,
      String password,
      String realNameStatus,
      String realName,
      String idCardNumber
  ) {
  }

  public static AccountSummaryRow toAccountRow(ChannelAccount a, String employeeNo, String name, String phone, String operator) {
    return new AccountSummaryRow(
        a.getId(),
        employeeNo,
        name,
        phone,
        operator,
        a.getChannel(),
        a.getAccountName(),
        "******",
        a.getRealNameStatus(),
        a.getRealName(),
        a.getIdCardCipher() == null ? "" : a.getIdCardCipher()
    );
  }

  public static DeviceSummaryRow toDeviceRow(
      DeviceAsset d,
      String employeeNo,
      String name,
      String sourceDepartment,
      String sourceEmployee,
      String acquisitionType,
      String receiveAt,
      String allocationAt,
      String phone,
      String operator,
      String linkedAccounts
  ) {
    return new DeviceSummaryRow(
        d.getId(),
        employeeNo,
        name,
        sourceDepartment,
        sourceEmployee,
        acquisitionType,
        d.getRegisteredAt() == null ? "" : d.getRegisteredAt().toString(),
        receiveAt,
        allocationAt,
        phone,
        operator,
        d.getDeviceType(),
        d.getBrand(),
        d.getModel(),
        linkedAccounts,
        d.getStatus()
    );
  }
}
