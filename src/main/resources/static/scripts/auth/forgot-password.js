/**
 * Forgot Password page JavaScript
 */
document.addEventListener("DOMContentLoaded", function () {
  const forgotPasswordForm = document.getElementById("forgotPasswordForm");
  const forgotPasswordError = document.getElementById("forgotPasswordError");
  const forgotPasswordSuccess = document.getElementById(
    "forgotPasswordSuccess"
  );
  const sendCodeBtn = document.getElementById("sendCodeBtn");

  let codeSent = false;
  let codeCountdown = 0;
  let countdownInterval = null;

  // Handle send code button
  if (sendCodeBtn) {
    sendCodeBtn.addEventListener("click", function (event) {
      event.preventDefault();

      if (codeCountdown > 0) {
        return; // Button in cooldown
      }

      const email = document.getElementById("email").value;
      if (!email || !isValidEmail(email)) {
        showError("Please enter a valid email address"); // Modified
        return;
      }

      // Send code request
      fetch("/api/auth/sendForgetPwdCode", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email: email,
        }),
      })
        .then((response) => response.json())
        .then((data) => {
          if (data.code === "1") {
            // Code sent successfully
            codeSent = true;
            showSuccess("Verification code sent. Please check your email."); // Modified

            // Start countdown
            codeCountdown = 60;
            updateCodeButton();
            countdownInterval = setInterval(function () {
              codeCountdown--;
              updateCodeButton();

              if (codeCountdown <= 0) {
                clearInterval(countdownInterval);
              }
            }, 1000);
          } else {
            showError(
              data.message ||
                "Failed to send verification code. Please try again."
            ); // Modified
          }
        })
        .catch((error) => {
          console.error("Send code error:", error);
          showError(
            "Failed to request verification code. Please try again later."
          ); // Modified
        });
    });
  }

  // Handle form submission
  if (forgotPasswordForm) {
    forgotPasswordForm.addEventListener("submit", function (event) {
      event.preventDefault();

      const email = document.getElementById("email").value;
      const password = document.getElementById("password").value;
      const confirmPassword = document.getElementById("confirmPassword").value;
      const forgetPwdCode = document.getElementById("forgetPwdCode").value;

      // Basic validation
      if (!email || !password || !confirmPassword || !forgetPwdCode) {
        showError("Please fill in all required fields."); // Modified
        return;
      }

      if (password !== confirmPassword) {
        showError("The passwords entered do not match."); // Modified
        return;
      }

      if (!isValidEmail(email)) {
        showError("Please enter a valid email address."); // Modified
        return;
      }

      if (!codeSent) {
        showError("Please obtain the email verification code first."); // Modified
        return;
      }

      // Submit reset password request
      fetch("/api/auth/forgetPwd", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email: email,
          password: password,
          forgetPwdCode: forgetPwdCode,
        }),
      })
        .then((response) => response.json())
        .then((data) => {
          if (data.code === "1") {
            // Password reset successful
            showSuccess(
              "Password reset successfully. Redirecting to login page..."
            ); // Modified

            // Redirect to login page after 2 seconds
            setTimeout(function () {
              window.location.href = "/login";
            }, 2000);
          } else {
            showError(
              data.message || "Password reset failed. Please try again."
            ); // Modified
          }
        })
        .catch((error) => {
          console.error("Password reset error:", error);
          showError("Password reset request failed. Please try again later."); // Modified
        });
    });
  }

  /**
   * Update the send code button text and state
   */
  function updateCodeButton() {
    if (codeCountdown > 0) {
      sendCodeBtn.textContent = `Resend (${codeCountdown}s)`; // Modified
      sendCodeBtn.disabled = true;
    } else {
      sendCodeBtn.textContent = "Send Code"; // Modified
      sendCodeBtn.disabled = false;
    }
  }

  /**
   * Show error message
   */
  function showError(message) {
    if (forgotPasswordSuccess) forgotPasswordSuccess.style.display = "none";
    forgotPasswordError.textContent = message;
    forgotPasswordError.style.display = "block";
  }

  /**
   * Show success message
   */
  function showSuccess(message) {
    if (forgotPasswordError) forgotPasswordError.style.display = "none";
    forgotPasswordSuccess.textContent = message;
    forgotPasswordSuccess.style.display = "block";
  }

  /**
   * Validate email format
   */
  function isValidEmail(email) {
    const re = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    return re.test(email);
  }
});
