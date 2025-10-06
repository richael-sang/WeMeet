// Import common utility functions
import { showAlert, checkUrlParamsForMessages } from "../common/utils.js";

$(document).ready(function () {
  // --- Modal & Form Handling ---

  // Add Room form submission
  $("#saveRoomBtn").click(function () {
    const form = $("#addRoomForm")[0];
    if (form.checkValidity()) {
      form.submit();
    } else {
      form.reportValidity();
    }
  });

  // Edit Room form submission
  $("#updateRoomBtn").click(function () {
    const form = $("#editRoomForm")[0];
    if (form.checkValidity()) {
      form.submit();
    } else {
      form.reportValidity();
    }
  });

  // Image Update form submission
  $("#saveImageBtn").click(function () {
    const form = $("#updateImageForm")[0];
    if (form.checkValidity()) {
      form.submit();
    } else {
      form.reportValidity();
    }
  });

  // Reset forms when modals are hidden
  $(".modal").on("hidden.bs.modal", function () {
    $(this)
      .find("form")
      .each(function () {
        this.reset();
      });
    // Also hide image previews
    $("#imagePreview, #imageUrlPreview").addClass("d-none");
  });

  // --- Edit Room Logic ---

  // Function to populate edit modal
  function populateEditModal(data) {
    $("#editRoomId").val(data.id);
    $("#editRoomName").val(data.roomName);
    $("#editCapacity").val(data.capacity);
    $("#editLocation").val(data.location || "");
    $("#editFloor").val(data.floor || "");
    $("#editHasProjector").prop("checked", data.hasProjector === "true");
    $("#editHasScreen").prop("checked", data.hasScreen === "true");
    $("#editHasSpeaker").prop("checked", data.hasSpeaker === "true");
    $("#editHasComputer").prop("checked", data.hasComputer === "true");
    $("#editHasWhiteboard").prop("checked", data.hasWhiteboard === "true");
    $("#editDescription").val(data.description || "");
    $("#editImageUrl").val(data.imageUrl || "");

    updateImagePreview("#editImageUrl", "#imagePreview");
  }

  // Event listener for edit buttons (delegated)
  $(document).on("click", ".edit-room-btn", function () {
    const data = $(this).data(); // Gets all data-* attributes
    // Convert boolean-like strings to actual strings for consistency with original code
    data.hasProjector = String(data.hasProjector);
    data.hasScreen = String(data.hasScreen);
    data.hasSpeaker = String(data.hasSpeaker);
    data.hasComputer = String(data.hasComputer);
    data.hasWhiteboard = String(data.hasWhiteboard);

    populateEditModal(data);
    $("#editRoomModal").modal("show");
  });

  // --- Image Update Logic ---

  // Function to update image preview
  function updateImagePreview(inputId, previewContainerId) {
    const url = $(inputId).val().trim();
    const previewContainer = $(previewContainerId);
    if (url) {
      previewContainer.removeClass("d-none").find("img").attr("src", url);
    } else {
      previewContainer.addClass("d-none");
    }
  }

  // Event listeners for image URL inputs
  $("#addRoomForm #imageUrl").on("input", function () {
    // No preview needed in add form according to HTML structure?
    // If preview needed, add a container and call updateImagePreview
  });
  $("#editImageUrl").on("input", function () {
    updateImagePreview("#editImageUrl", "#imagePreview");
  });
  $("#updateImageUrl").on("input", function () {
    updateImagePreview("#updateImageUrl", "#imageUrlPreview");
  });

  // Function to open image update modal
  function openImageUpdateModal(id, roomName, imageUrl) {
    $("#updateImageRoomId").val(id);
    $("#updateImageUrl").val(imageUrl || "");
    $("#updateImageModalLabel").html(
      `<i class="fas fa-image me-2"></i>Update Image for "${roomName}"`
    );
    updateImagePreview("#updateImageUrl", "#imageUrlPreview");
    $("#updateImageModal").modal("show");
  }

  // Event listener for Update Image button in Edit Modal
  $("#updateImageUrlBtn").click(function () {
    const id = $("#editRoomId").val();
    const roomName = $("#editRoomName").val();
    const imageUrl = $("#editImageUrl").val();
    openImageUpdateModal(id, roomName, imageUrl);
  });

  // Event listener for Update Image buttons on Cards (delegated)
  $(document).on("click", ".update-image-btn", function () {
    const id = $(this).data("id");
    const roomName = $(this).data("room-name");
    const imageUrl = $(this).data("image-url");
    openImageUpdateModal(id, roomName, imageUrl);
  });

  // --- Delete Room Logic ---

  // Function to open delete confirmation modal
  function openDeleteModal(id, roomName) {
    $("#deleteRoomId").val(id);
    $("#deleteRoomName").text(roomName);
    $("#deleteRoomModal").modal("show");
  }

  // Event listener for delete buttons (delegated)
  $(document).on("click", ".delete-room-btn", function () {
    const id = $(this).data("id");
    const roomName = $(this).data("room-name");
    openDeleteModal(id, roomName);
  });

  // --- Filtering Logic ---

  function applyFilters() {
    const name = $("#roomSearch").val().trim();
    const location = $("#locationFilter").val();
    const floor = $("#floorFilter").val();

    const params = new URLSearchParams();
    if (name) params.set("name", name);
    if (location) params.set("location", location);
    if (floor) params.set("floor", floor);

    window.location.href = `${window.location.pathname}?${params.toString()}`;
  }

  $("#applyFilters").on("click", applyFilters);

  $("#roomSearch").keypress(function (e) {
    if (e.which === 13) {
      // Enter key
      e.preventDefault();
      applyFilters();
    }
  });

  $("#clearFilters").on("click", function () {
    window.location.href = window.location.pathname;
  });

  // --- Initialisation ---

  // Define messages for URL parameter checking
  const messages = {
    "success-add": {
      message:
        '<i class="fas fa-check-circle me-2"></i> Meeting room was successfully added.',
      type: "success",
    },
    "success-update": {
      message:
        '<i class="fas fa-check-circle me-2"></i> Meeting room was successfully updated.',
      type: "success",
    },
    "success-delete": {
      message:
        '<i class="fas fa-check-circle me-2"></i> Meeting room was successfully deleted.',
      type: "success",
    },
    "success-image": {
      message:
        '<i class="fas fa-check-circle me-2"></i> Room image was successfully updated.',
      type: "success",
    },
    "error-add": {
      message:
        '<i class="fas fa-exclamation-circle me-2"></i> Failed to add meeting room.',
      type: "danger",
    },
    "error-update": {
      message:
        '<i class="fas fa-exclamation-circle me-2"></i> Failed to update meeting room.',
      type: "danger",
    },
    "error-delete": {
      message:
        '<i class="fas fa-exclamation-circle me-2"></i> Failed to delete meeting room.',
      type: "danger",
    },
    "error-image": {
      message:
        '<i class="fas fa-exclamation-circle me-2"></i> Failed to update room image.',
      type: "danger",
    },
  };

  // Check URL parameters for messages on page load
  checkUrlParamsForMessages(messages);
});
