// This file can contain common utility functions, e.g., for DOM manipulation, API calls, etc.
// Example:
// export function debounce(func, wait) { ... }
// export function throttle(func, limit) { ... }

/**
 * Shows an alert message in the designated container and auto-dismisses it.
 * Assumes jQuery is available for alert dismissal.
 * @param {string} message The HTML or text message to display.
 * @param {string} type The Bootstrap alert type (e.g., 'success', 'danger', 'info').
 * @param {string} containerId The ID of the container element for alerts (defaults to 'alertContainer').
 */
export function showAlert(message, type, containerId = "alertContainer") {
  const alertContainer = document.getElementById(containerId);
  if (!alertContainer) {
    console.error(`Alert container with ID "${containerId}" not found.`);
    return;
  }

  const alertDiv = document.createElement("div");
  // Use template literals for cleaner HTML string
  alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
  alertDiv.setAttribute("role", "alert"); // Add role for accessibility
  alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    `; // Use standard Bootstrap 5 close button

  alertContainer.innerHTML = ""; // Clear previous alerts
  alertContainer.appendChild(alertDiv);

  // Auto-dismiss after 5 seconds using Bootstrap's alert instance method
  const bsAlert = new bootstrap.Alert(alertDiv);
  setTimeout(() => {
    bsAlert.close();
  }, 5000);
}

/**
 * Checks URL parameters for specific success or error messages and displays alerts.
 * Removes the parameters from the URL history after displaying the message.
 * @param {Object} messages - An object mapping URL parameter keys to message objects.
 * Example: {
 *   'add-success': { message: 'Item added!', type: 'success' },
 *   'add-error': { message: 'Failed to add.', type: 'danger' }
 * }
 */
export function checkUrlParamsForMessages(messages) {
  const urlParams = new URLSearchParams(window.location.search);
  let messageShown = false;

  for (const param in messages) {
    if (urlParams.has(param)) {
      const { message, type } = messages[param];
      showAlert(message, type);
      messageShown = true;
      break; // Show only the first message found
    }
  }

  // Clean the URL only if a message was shown
  if (messageShown) {
    // Create a new URL without the message parameters
    const currentUrl = new URL(window.location.href);
    const newSearchParams = new URLSearchParams(currentUrl.search);
    for (const param in messages) {
      newSearchParams.delete(param);
    }
    const newUrl = `${currentUrl.pathname}${
      newSearchParams.toString() ? "?" + newSearchParams.toString() : ""
    }${currentUrl.hash}`;
    // Use replaceState to avoid adding to browser history
    history.replaceState(null, "", newUrl);
  }
}

/**
 * Shows a Bootstrap Toast notification.
 * Requires Bootstrap's JS included on the page.
 * @param {string} message The message to display.
 * @param {string} type The background color type (e.g., 'success', 'danger', 'info', 'warning'). Defaults to 'success'.
 * @param {number} delay Auto-hide delay in milliseconds. Defaults to 3000.
 * @param {string} positionClass Position classes for the toast container (e.g., 'bottom-0 end-0'). Defaults to 'bottom-0 end-0'.
 */
export function showToast(
  message,
  type = "success",
  delay = 3000,
  positionClass = "bottom-0 end-0"
) {
  const toastId = `toast-${Date.now()}`;
  // Ensure text color contrasts with background
  const textColor =
    type === "warning" || type === "info" || type === "light"
      ? "text-dark"
      : "text-white";

  const toastHtml = `
    <div id="${toastId}" class="toast align-items-center ${textColor} bg-${type} border-0 position-fixed ${positionClass} m-3" 
         role="alert" aria-live="assertive" aria-atomic="true" data-bs-delay="${delay}">
      <div class="d-flex">
        <div class="toast-body">
          ${message} 
        </div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
      </div>
    </div>
  `;

  // Create a container if it doesn't exist
  let toastContainer = document.getElementById("toast-container-main");
  if (!toastContainer) {
    toastContainer = document.createElement("div");
    toastContainer.id = "toast-container-main";
    // Apply necessary styles for positioning context if needed, depends on positionClass
    toastContainer.style.position = "relative";
    toastContainer.style.zIndex = "1100"; // Ensure it's above most elements
    document.body.appendChild(toastContainer);
  }

  toastContainer.insertAdjacentHTML("beforeend", toastHtml);

  const toastElement = document.getElementById(toastId);
  if (toastElement) {
    const toast = new bootstrap.Toast(toastElement);
    toast.show();

    // Remove the element from DOM after it's hidden
    toastElement.addEventListener("hidden.bs.toast", function () {
      toastElement.remove();
    });
  } else {
    console.error("Failed to find toast element after adding it:", toastId);
  }
}
