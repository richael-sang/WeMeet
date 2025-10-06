document.addEventListener("DOMContentLoaded", () => {
  const themeToggleButton = document.getElementById("theme-toggle-button");
  const htmlElement = document.documentElement; // Target <html> instead of <body>
  const moonIcon = themeToggleButton
    ? themeToggleButton.querySelector("i")
    : null; // Add null check

  // Apply initial icon state based on the class already set on <html> (by inline script)
  if (htmlElement.classList.contains("dark-mode")) {
    // Check <html>
    if (moonIcon) moonIcon.classList.replace("fa-sun", "fa-moon"); // Dark mode -> Show Moon
  } else {
    if (moonIcon) moonIcon.classList.replace("fa-moon", "fa-sun"); // Light mode -> Show Sun
  }

  if (themeToggleButton) {
    // Add null check for the button itself
    themeToggleButton.addEventListener("click", (event) => {
      event.preventDefault(); // Prevent default link behavior

      // Toggle the .dark-mode class on the <html> element
      htmlElement.classList.toggle("dark-mode"); // Toggle on <html>

      // Update the theme in localStorage and change the icon
      if (htmlElement.classList.contains("dark-mode")) {
        // Check <html>
        localStorage.setItem("theme", "dark");
        if (moonIcon) moonIcon.classList.replace("fa-sun", "fa-moon"); // Switched to Dark -> Show Moon
      } else {
        localStorage.setItem("theme", "light");
        if (moonIcon) moonIcon.classList.replace("fa-moon", "fa-sun"); // Switched to Light -> Show Sun
      }
    });
  } else {
    console.warn(
      'Theme toggle button with id "theme-toggle-button" not found.'
    );
  }
});
