document.addEventListener("DOMContentLoaded", function () {
  // 锁定用户点击事件
  document.querySelectorAll(".lock-user-btn").forEach((button) => {
    button.addEventListener("click", function () {
      const userId = this.getAttribute("data-user-id");
      const username = this.getAttribute("data-username");

      if (confirm(`Are you sure you want to lock user "${username}"?`)) {
        const reason = prompt("Please enter a reason for locking (optional):");
        if (reason !== null) {
          // 即使用户不输入原因也会继续
          performUserAction(userId, "lock", reason || "");
        }
      }
    });
  });

  // 解锁用户点击事件
  document.querySelectorAll(".unlock-user-btn").forEach((button) => {
    button.addEventListener("click", function () {
      const userId = this.getAttribute("data-user-id");
      const username = this.getAttribute("data-username");

      if (confirm(`Are you sure you want to unlock user "${username}"?`)) {
        const reason = prompt(
          "Please enter a reason for unlocking (optional):"
        );
        if (reason !== null) {
          performUserAction(userId, "unlock", reason || "");
        }
      }
    });
  });

  // 执行用户操作
  function performUserAction(userId, action, reason) {
    // 注意：这里的 URL 可能需要根据你的 Spring Boot 配置调整，
    // 但 /admin/users/... 看起来是合理的，假设你的 Controller 映射是这样设置的。
    const url = `/admin/users/${userId}/${action}`;
    const successMsg = `User ${
      action === "lock" ? "locked" : "unlocked"
    } successfully!`;

    // 显示加载中状态
    const button = document.querySelector(`button[data-user-id="${userId}"]`);
    const originalHtml = button.innerHTML;
    button.disabled = true;
    button.innerHTML =
      '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>';

    // TODO: Add CSRF token handling if Spring Security CSRF is enabled
    // const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    // const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    // const headers = {
    //   "Content-Type": "application/x-www-form-urlencoded",
    // };
    // if (csrfToken && csrfHeader) {
    //   headers[csrfHeader] = csrfToken;
    // }

    // 发送请求 (Using Fetch API)
    fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        // ... add csrf header if needed: ...headers
      },
      body: `reason=${encodeURIComponent(reason)}`,
    })
      .then((response) => {
        if (!response.ok) {
          // 尝试从响应体中读取错误信息 (如果后端返回了的话)
          return response.text().then((text) => {
            throw new Error(text || response.statusText);
          });
        }
        return response.text(); // 或者 response.json() 如果后端返回 JSON
      })
      .then((data) => {
        // 显示成功消息 (Consider using the showToast function from utils.js)
        const successMessage = document.getElementById("successMessage");
        if (successMessage) {
          // 检查元素是否存在
          successMessage.textContent = successMsg;
          successMessage.style.display = "block";
        }

        // 短暂延迟后刷新页面以显示更新后的状态
        setTimeout(() => {
          window.location.reload();
        }, 1000); // 1秒延迟
      })
      .catch((error) => {
        console.error("Error performing user action:", error);
        alert(`Failed to ${action} user: ${error.message}`);
        // 恢复按钮状态
        if (button) {
          // 检查按钮是否存在
          button.disabled = false;
          button.innerHTML = originalHtml;
        }
      });
  }
});
