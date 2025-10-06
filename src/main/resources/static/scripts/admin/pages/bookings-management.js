$(document).ready(function () {
  // View booking details modal population
  $(".view-booking-btn").on("click", function () {
    const button = $(this);
    const modal = $("#viewBookingModal");

    // Get data from button attributes
    const roomName = button.data("room-name");
    const roomLocation = button.data("room-location");
    const username = button.data("username");
    const userEmail = button.data("user-email");
    const startTime = button.data("start-time");
    const endTime = button.data("end-time");
    const status = button.data("status").toString(); // Ensure string for comparison
    const reason = button.data("reason");
    const createdAt = button.data("created-at");

    // Populate modal fields
    modal.find("#modalRoomName").text(roomName || "N/A");
    modal.find("#modalRoomLocation").text(roomLocation || "N/A");
    modal.find("#modalUsername").text(username || "N/A");
    modal.find("#modalUserEmail").text(userEmail || "N/A");
    modal.find("#modalStartTime").text(startTime || "N/A");
    modal.find("#modalEndTime").text(endTime || "N/A");
    modal.find("#modalReason").text(reason || "N/A");
    modal.find("#modalCreatedAt").text(createdAt || "N/A");

    // Set status badge class and text
    const statusBadge = modal.find("#modalStatusBadge");
    let badgeClass = "bg-secondary"; // Default
    if (status === "APPROVED") {
      badgeClass = "bg-success";
    } else if (status === "REJECTED") {
      badgeClass = "bg-danger";
    } else if (status === "PENDING") {
      // Although list excludes PENDING, good practice to handle
      badgeClass = "bg-warning text-dark";
    } else if (status === "CANCELLED") {
      badgeClass = "bg-secondary";
    }
    statusBadge
      .removeClass("bg-success bg-danger bg-warning bg-secondary text-dark")
      .addClass("badge " + badgeClass)
      .text(status);

    // Bootstrap 5 modals are shown automatically via data attributes, no need for .modal('show') here
  });

  // Reject booking
  $(".reject-btn").on("click", function () {
    const bookingId = $(this).data("id");

    // 创建对话框获取拒绝原因
    const reason = prompt("Please enter reason for rejection:", "");

    // 如果取消或输入为空，则不执行操作
    if (reason === null || reason.trim() === "") {
      return;
    }

    if (confirm("Are you sure you want to reject this booking?")) {
      // 获取 CSRF token (如果需要)
      const csrfToken = $('meta[name="_csrf"]').attr("content");
      const csrfHeader = $('meta[name="_csrf_header"]').attr("content");

      const headers = {};
      if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
      }
      headers["Content-Type"] = "application/x-www-form-urlencoded"; // 通常 Spring MVC 期望这种格式

      // 发送AJAX请求拒绝预订，并包含拒绝原因
      $.ajax({
        url: "/admin/bookings/" + bookingId + "/reject",
        type: "POST",
        headers: headers, // 添加 headers
        data: { reason: reason }, // data 字段会被 jQuery 自动编码
        success: function (response) {
          alert("Booking rejected successfully!");
          location.reload();
        },
        error: function (xhr) {
          // 尝试显示更友好的错误信息
          let errorMessage = "An unknown error occurred.";
          if (xhr.responseText) {
            try {
              // 尝试解析 JSON 错误信息
              const errorResponse = JSON.parse(xhr.responseText);
              errorMessage = errorResponse.message || xhr.responseText;
            } catch (e) {
              // 如果不是 JSON，直接显示文本
              errorMessage = xhr.responseText;
            }
          } else if (xhr.statusText) {
            errorMessage = xhr.statusText;
          }
          alert("Error rejecting booking: " + errorMessage);
          console.error("AJAX Error:", xhr);
        },
      });
    }
  });
});
