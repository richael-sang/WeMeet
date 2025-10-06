// WARNING: Client-side permission checks using localStorage are insecure!
// Proper authorization MUST be enforced on the server-side.

document.addEventListener("DOMContentLoaded", function () {
  // Attempt to get user info from localStorage
  const userInfoStr = localStorage.getItem("user_info");
  let userInfo = null;

  if (userInfoStr) {
    try {
      userInfo = JSON.parse(userInfoStr);
    } catch (e) {
      console.error("Error parsing user_info from localStorage:", e);
      // Clear invalid data and redirect to login
      localStorage.removeItem("auth_token");
      localStorage.removeItem("user_info");
      window.location.href = "/login";
      return;
    }
  }

  // Redirect to login if no user info or token (assuming token is also needed)
  const authToken = localStorage.getItem("auth_token");
  if (!userInfo || !authToken) {
    console.warn(
      "No user info or auth token found in localStorage. Redirecting to login."
    );
    localStorage.removeItem("auth_token"); // Clean up potentially partial data
    localStorage.removeItem("user_info");
    window.location.href = "/login";
    return;
  }

  // Basic client-side role check (INSECURE - FOR UI HIDING/REDIRECT ONLY)
  if (userInfo.role !== "Admin") {
    alert("You do not have permission to access the admin panel.");
    // Redirect to a non-admin page, e.g., user dashboard
    window.location.href = "/dashboard"; // Adjust as needed
    return;
  }

  // Logout button functionality
  const logoutButton = document.getElementById("logoutBtn");
  if (logoutButton) {
    logoutButton.addEventListener("click", function (e) {
      e.preventDefault();
      console.log("Logging out...");
      localStorage.removeItem("auth_token");
      localStorage.removeItem("user_info");
      window.location.href = "/login";
    });
  } else {
    console.warn('Logout button with ID "logoutBtn" not found.');
  }
});
