/**
 * Registration page JavaScript
 */
document.addEventListener("DOMContentLoaded", function () {
  const registerForm = document.getElementById("registerForm");
  const registerError = document.getElementById("registerError");
  const registerSuccess = document.getElementById("registerSuccess");
  const sendCodeBtn = document.getElementById("sendCodeBtn");
  const avatarUpload = document.getElementById("avatarUpload");
  const avatarPreview = document.getElementById("avatarPreview");
  const defaultAvatar = "/images/default_avatar.jpeg";
  const croppedAvatarDataInput = document.getElementById("croppedAvatarData"); // Get hidden input

  // Cropper variables
  let cropper;
  const cropperModal = document.getElementById("cropperModal");
  const cropperImage = document.getElementById("cropperImage");
  const cropperCancelBtn = document.getElementById("cropperCancel");
  const cropperSaveBtn = document.getElementById("cropperSave");

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
      fetch("/api/auth/sendRegisterCode", {
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

  // Handle avatar upload
  if (avatarUpload) {
    avatarUpload.addEventListener("change", handleAvatarUpload);
  }

  if (cropperCancelBtn) {
    cropperCancelBtn.addEventListener("click", closeCropperModal);
  }

  if (cropperSaveBtn) {
    cropperSaveBtn.addEventListener("click", cropAndSetData);
  }

  // Handle form submission
  if (registerForm) {
    registerForm.addEventListener("submit", function (event) {
      event.preventDefault();

      const username = document.getElementById("username").value;
      const password = document.getElementById("password").value;
      const confirmPassword = document.getElementById("confirmPassword").value;
      const email = document.getElementById("email").value;
      const registerCode = document.getElementById("registerCode").value;
      const role = document.querySelector('input[name="role"]:checked').value;
      const avatarDataUrl = croppedAvatarDataInput.value; // Get Base64 data
      let adminKey = "";

      if (role === "Admin") {
        adminKey = document.getElementById("adminKey").value;
      }

      // Basic validation
      if (
        !username ||
        !password ||
        !confirmPassword ||
        !email ||
        !registerCode
      ) {
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

      // Create registration payload
      const payload = {
        username: username,
        password: password,
        email: email,
        registerCode: registerCode,
        role: role,
        avatarDataUrl: avatarDataUrl || null, // Send null if no avatar was cropped
      };

      if (role === "Admin") {
        if (!adminKey) {
          showError("Admin Key is required for admin registration."); // Modified
          return;
        }
        payload.adminKey = adminKey;
      }

      console.log("Submitting registration payload:", payload);

      // Submit registration request
      fetch("/api/auth/register", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      })
        .then((response) => response.json()) // Add error handling based on status later if needed
        .then((data) => {
          if (data.code === "1") {
            // Registration successful
            showSuccess(
              "Registration successful. Redirecting to login page..."
            ); // Modified

            // Redirect to login page after 2 seconds
            setTimeout(function () {
              window.location.href = "/login";
            }, 2000);
          } else {
            showError(data.message || "Registration failed. Please try again."); // Modified
          }
        })
        .catch((error) => {
          console.error("Registration error:", error);
          showError("Registration request failed. Please try again later."); // Modified
        });
    });
  }

  // Toggle admin key field visibility
  const roleInputs = document.querySelectorAll('input[name="role"]');
  const adminKeyGroup = document.getElementById("adminKeyGroup");

  if (roleInputs.length > 0 && adminKeyGroup) {
    roleInputs.forEach((input) => {
      input.addEventListener("change", function () {
        if (this.value === "Admin") {
          adminKeyGroup.style.display = "block";
        } else {
          adminKeyGroup.style.display = "none";
        }
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
    if (registerSuccess) registerSuccess.style.display = "none";
    registerError.textContent = message;
    registerError.style.display = "block";
  }

  /**
   * Show success message
   */
  function showSuccess(message) {
    if (registerError) registerError.style.display = "none";
    registerSuccess.textContent = message;
    registerSuccess.style.display = "block";
  }

  /**
   * Validate email format
   */
  function isValidEmail(email) {
    const re =
      /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return re.test(String(email).toLowerCase());
  }

  // --- NEW Cropper Functions (Adapted from profile.html) ---
  function handleAvatarUpload(event) {
    const file = event.target.files[0];
    if (!file) return;

    // Basic file validation
    const fileType = file.type;
    if (!fileType.match(/^image\/(jpeg|png|gif|bmp|webp)$/)) {
      showError("Please upload a valid image file (JPG, PNG, GIF, BMP, WEBP)."); // Modified
      event.target.value = ""; // Clear the invalid file input
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      // 10MB
      showError("Image size cannot exceed 10MB."); // Modified
      event.target.value = ""; // Clear the invalid file input
      return;
    }

    const reader = new FileReader();
    reader.onload = function (e) {
      cropperImage.src = e.target.result;
      openCropperModal();
      if (cropper) {
        cropper.destroy();
      }
      cropper = new Cropper(cropperImage, {
        aspectRatio: 1,
        viewMode: 1,
        background: false,
        autoCropArea: 0.8,
        responsive: true,
        // Ensure the crop box is within the bounds of the canvas
        checkCrossOrigin: false,
        movable: true,
        zoomable: true,
        rotatable: false,
        scalable: false,
      });
    };
    reader.readAsDataURL(file);
  }

  function openCropperModal() {
    if (cropperModal) {
      cropperModal.style.display = "flex";
    }
  }

  function closeCropperModal() {
    if (cropperModal) {
      cropperModal.style.display = "none";
      if (cropper) {
        cropper.destroy();
        cropper = null;
      }
      // Clear the file input if the user cancels cropping
      if (avatarUpload) {
        avatarUpload.value = "";
      }
      // Reset preview to default if needed
      // avatarPreview.src = defaultAvatar; // Optional: reset preview on cancel
      // croppedAvatarDataInput.value = ''; // Clear hidden input on cancel
    }
  }

  function cropAndSetData() {
    if (cropper) {
      const canvas = cropper.getCroppedCanvas({
        width: 200, // Desired output width
        height: 200, // Desired output height
        imageSmoothingQuality: "high",
      });
      const croppedDataUrl = canvas.toDataURL("image/jpeg"); // Or use image/png
      avatarPreview.src = croppedDataUrl;
      croppedAvatarDataInput.value = croppedDataUrl; // Store Base64 in hidden input
      closeCropperModal();
    }
  }
});
