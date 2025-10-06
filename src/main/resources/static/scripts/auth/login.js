/**
 * Login page JavaScript
 */
document.addEventListener("DOMContentLoaded", function () {
  console.log("DOM Content Loaded, login.js executing.");

  const loginForm = document.getElementById("loginForm");
  const loginError = document.getElementById("loginError");
  const captchaImage = document.getElementById("captchaImage");
  const refreshCaptcha = document.getElementById("refreshCaptcha");
  const logoutSuccessAlert = document.getElementById("logoutSuccessAlert"); // Get logout success alert

  // --- Make logout success message disappear after 2 seconds ---
  if (logoutSuccessAlert) {
    console.log("Logout success message found, setting timeout to hide."); // Reverted log
    setTimeout(() => {
      // Optional: Add fade-out effect later if desired (removed)
      logoutSuccessAlert.style.display = "none";
      console.log("Hiding logout success message."); // Reverted log
    }, 2000); // 2000 milliseconds = 2 seconds
  }
  // --- End auto-hide logic ---

  // Load captcha on page load
  loadCaptcha();

  // Refresh captcha on click
  if (refreshCaptcha) {
    refreshCaptcha.addEventListener("click", function (event) {
      event.preventDefault();
      loadCaptcha();
    });
  }

  // Refresh captcha on image click as well
  if (captchaImage) {
    captchaImage.addEventListener("click", function (event) {
      loadCaptcha();
    });
  }

  // Handle form submission
  if (loginForm) {
    loginForm.addEventListener("submit", function (event) {
      event.preventDefault();

      const username = document.getElementById("username").value;
      const password = document.getElementById("password").value;
      const captchaCode = document.getElementById("captchaCode").value;
      const captchaKey = document.getElementById("captchaKey").value;

      if (!username || !password || !captchaCode || !captchaKey) {
        showError("Please fill in all required fields"); // Modified
        return;
      }

      // Submit login request
      console.log("Submitting login request for username:", username); // Modified console log

      fetch("/api/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          username: username,
          password: password,
          captchaCode: captchaCode,
          captchaKey: captchaKey,
        }),
      })
        .then((response) => {
          console.log("Login request status:", response.status); // Modified console log
          return response.json();
        })
        .then((data) => {
          console.log("Login response data:", data); // Modified console log

          if (data.code === "1") {
            // Login successful - store token
            localStorage.setItem("auth_token", data.data.token);

            // Extract and store user info
            const userInfo = {
              userId: data.data.userId,
              username: data.data.username,
              avatar: data.data.avatar,
              role: data.data.role,
              email: data.data.email,
            };
            localStorage.setItem("user_info", JSON.stringify(userInfo));

            console.log("Login successful, user role:", data.data.role); // Modified console log

            // Store token and user info
            localStorage.setItem("auth_token", data.data.token);
            localStorage.setItem("user_info", JSON.stringify(userInfo));

            // Show success message
            showMessage("Login successful, redirecting...", "success"); // Modified

            // Determine target URL
            const targetPath =
              data.data.role === "Admin" ? "/admin" : "/user/dashboard";

            // Prepare to redirect
            console.log("Preparing to redirect to:", targetPath); // Modified console log

            // Delay redirection slightly to show success message
            setTimeout(() => {
              window.location.href = targetPath;
            }, 800);
          } else {
            // Login failed - show error and refresh captcha
            console.error("Login failed:", data.message);
            showError(data.message || "Login failed. Please try again."); // Modified
            loadCaptcha();
          }
        })
        .catch((error) => {
          console.error("Login error:", error);
          showError("Login request failed. Please try again later."); // Modified
          loadCaptcha();
        });
    });
  }

  /**
   * Load a new captcha image
   */
  function loadCaptcha() {
    console.log("loadCaptcha function called.");

    // Display loading state
    if (captchaImage) {
      captchaImage.style.opacity = "0.5";
    }

    console.log("Attempting to fetch captcha from /api/auth/captchaImage");
    fetch("/api/auth/captchaImage")
      .then((response) => {
        console.log(
          "Captcha fetch response received, status:",
          response.status,
          "ok:",
          response.ok
        );
        // Check if the response is actually ok AND is likely JSON before parsing
        if (!response.ok) {
          // Try to get text first to see what the server returned (e.g., HTML error page)
          return response.text().then((text) => {
            console.error("Captcha fetch failed. Response text:", text);
            throw new Error(`Server responded with status ${response.status}`);
          });
        }
        // Assuming OK responses are JSON for captcha
        return response.json();
      })
      .then((data) => {
        console.log("Captcha data received:", data);
        if (data.code === "1") {
          captchaImage.src = data.captchaImage;
          captchaImage.style.opacity = "1";
          document.getElementById("captchaKey").value = data.captchaKey;

          // Clear any existing captcha input
          const captchaInput = document.getElementById("captchaCode");
          if (captchaInput) {
            captchaInput.value = "";
            captchaInput.focus();
          }
        } else {
          console.error(
            "Failed to load captcha - API returned error code:",
            data.message
          );
          showError(
            data.message ||
              "Failed to load captcha. Please refresh the page and try again."
          );
        }
      })
      .catch((error) => {
        console.error("Captcha fetch .catch error:", error);
        showError(
          `Failed to load captcha (${
            error.message || "Network error"
          }). Please refresh the page.`
        );
      });
  }

  /**
   * Show error message
   */
  function showError(message) {
    loginError.textContent = message;
    loginError.style.display = "block";
  }

  /**
   * Show message with type
   */
  function showMessage(message, type) {
    loginError.innerHTML = message;
    loginError.style.display = "block";

    // Set alert class based on type
    if (type === "success") {
      loginError.classList.remove("alert-danger");
      loginError.classList.add("alert-success");
    } else if (type === "warning") {
      loginError.classList.remove("alert-danger");
      loginError.classList.add("alert-warning");
    } else {
      loginError.classList.remove("alert-success");
      loginError.classList.remove("alert-warning");
      loginError.classList.add("alert-danger");
    }
  }
});
