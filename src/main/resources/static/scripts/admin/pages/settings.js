// Import common utility functions
import { showToast } from "../common/utils.js";

$(document).ready(function () {
  // 保存常规设置
  $("#saveGeneralBtn").click(function () {
    // TODO: Add actual logic to save settings via AJAX or form submission
    showToast("常规设置已保存");
  });

  // 保存预约设置
  $("#saveReservationBtn").click(function () {
    // TODO: Add actual logic to save settings
    showToast("预约设置已保存");
  });

  // 保存管理员设置
  $("#saveAdminBtn").click(function () {
    // TODO: Add actual logic to save settings
    showToast("管理员设置已保存");
  });

  // 生成新管理员注册码
  $("#generateNewCode").click(function () {
    // Note: This is client-side only. Real generation should happen server-side.
    const newCode = "ADMIN" + Math.floor(Math.random() * 10000);
    $("#adminRegisterCode").val(newCode);
    // Consider adding a visual confirmation or disabling the button briefly
  });

  // 测试邮件按钮点击
  $("#testEmailBtn").click(function () {
    $("#testEmailModal").modal("show");
  });

  // 发送测试邮件
  $("#sendTestEmailBtn").click(function () {
    const testEmail = $("#testEmailAddress").val();
    if (
      !testEmail ||
      !document.getElementById("testEmailAddress").checkValidity()
    ) {
      // Basic validation
      alert("请输入有效的邮箱地址");
      return;
    }

    // TODO: Add actual AJAX call to backend to send test email
    console.log("Simulating sending test email to:", testEmail);

    // Assuming success for now
    $("#testEmailModal").modal("hide");
    showToast("测试邮件已发送至 " + testEmail);
  });

  // 保存邮件设置
  $("#saveEmailBtn").click(function () {
    // TODO: Add actual logic to save settings
    showToast("邮件设置已保存");
  });

  // 创建备份
  $("#createBackupBtn").click(function () {
    // TODO: Add actual AJAX call to backend to trigger backup process
    showToast("备份创建中，请稍候...", "info"); // Use info type
    // Simulate backup creation - replace with actual callback/polling logic
    setTimeout(function () {
      showToast("备份已成功创建");
    }, 2000);
  });

  // 保存自动备份设置
  $("#saveAutoBackupBtn").click(function () {
    // TODO: Add actual logic to save settings
    showToast("自动备份设置已保存");
  });

  // TODO: Add logic for Backup History actions (Download, Restore, Delete)
  // Example:
  // $('.backup-download-btn').click(function() { ... });
});
