// Shared theme toggling logic

// Immediately Invoked Function Expression (IIFE) to set the initial theme
(function () {
  const theme = localStorage.getItem("theme");
  if (theme === "dark") {
    document.documentElement.classList.add("dark-mode");
    console.log("Initial theme set to dark."); // Added console log for verification
  } else {
    console.log("Initial theme set to light or not specified."); // Added console log for verification
  }
})();

// Event listener for the theme toggle button, runs after DOM is loaded
document.addEventListener("DOMContentLoaded", function () {
  const themeToggleButton = document.getElementById("theme-toggle-button");

  if (themeToggleButton) {
    console.log("Theme toggle button found."); // Added console log for verification

    // Function to update the icon based on the current mode
    function updateIcon(isDarkMode) {
      const icon = themeToggleButton.querySelector("i");
      if (icon) {
        icon.className = isDarkMode ? "fas fa-moon" : "fas fa-sun";
        console.log(`Icon updated to: ${icon.className}`); // Added console log
      }
    }

    // Add click listener
    themeToggleButton.addEventListener("click", function (e) {
      e.preventDefault();
      document.documentElement.classList.toggle("dark-mode");
      const isDarkMode =
        document.documentElement.classList.contains("dark-mode");
      localStorage.setItem("theme", isDarkMode ? "dark" : "light");
      console.log(
        `Theme toggled. Dark mode: ${isDarkMode}. Stored theme: ${localStorage.getItem(
          "theme"
        )}`
      ); // Added console log
      updateIcon(isDarkMode);
    });

    // Set the initial icon state based on the theme applied by the IIFE
    const initialDarkMode =
      document.documentElement.classList.contains("dark-mode");
    console.log(`Setting initial icon state. Dark mode: ${initialDarkMode}`); // Added console log
    updateIcon(initialDarkMode);
  } else {
    console.log(
      "Theme toggle button (#theme-toggle-button) not found on this page."
    ); // Added console log
  }
});
