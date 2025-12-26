package edu.agile.sis.ui;

import edu.agile.sis.service.InventoryService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * JavaFX Controller for Inventory/Resource Management (Admin View).
 * Allows viewing, creating, editing, and allocating inventory items.
 * Also handles pending request approvals.
 * 
 * - Laptop/Equipment: Single user assignment
 * - License: Multi-user assignment
 */
public class InventoryController {
    private final VBox view = new VBox(20);
    private final InventoryService inventoryService = new InventoryService();

    private TableView<Document> table;
    private TableView<Document> requestsTable;
    private ObservableList<Document> allItems = FXCollections.observableArrayList();
    private ObservableList<Document> displayedItems = FXCollections.observableArrayList();

    private TextField searchField;
    private ComboBox<String> statusFilter;
    private ComboBox<String> typeFilter;

    private int currentPage = 0;
    private final int pageSize = 15;
    private Label pageLabel;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public InventoryController() {
        view.setPadding(new Insets(20));
        view.setStyle("-fx-background-color: #f4f6f7;");

        // Header
        Label header = new Label("📦 Inventory Management (Admin)");
        header.setFont(Font.font("System", FontWeight.BOLD, 24));
        header.setStyle("-fx-text-fill: #2c3e50;");

        // Tab Pane
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab itemsTab = new Tab("📦 All Items");
        itemsTab.setContent(createItemsTab());

        Tab requestsTab = new Tab("📋 Pending Requests");
        requestsTab.setContent(createPendingRequestsTab());

        tabPane.getTabs().addAll(itemsTab, requestsTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Card wrapper
        VBox card = new VBox(tabPane);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        VBox.setVgrow(card, Priority.ALWAYS);

        view.getChildren().addAll(header, card);

        loadItems();
        loadPendingRequests();
    }

    private VBox createItemsTab() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(15));

        // Filters Row
        HBox filtersRow = createFiltersRow();

        // Table
        table = new TableView<>();
        setupTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        // Pagination
        HBox pagingControls = createPagingControls();

        // Action Buttons
        HBox actionButtons = createActionButtons();

        content.getChildren().addAll(filtersRow, table, pagingControls, actionButtons);
        VBox.setVgrow(content, Priority.ALWAYS);
        return content;
    }

    @SuppressWarnings("unchecked")
    private VBox createPendingRequestsTab() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(15));

        Label info = new Label("Review and process pending requests from staff and students.");
        info.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
        info.setWrapText(true);

        requestsTable = new TableView<>();

        TableColumn<Document, String> requesterCol = new TableColumn<>("Requester");
        requesterCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "requesterName") + " (" + safeGetString(data.getValue(), "requesterType")
                        + ")"));
        requesterCol.setPrefWidth(180);

        TableColumn<Document, String> itemCol = new TableColumn<>("Item");
        itemCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "itemName")));
        itemCol.setPrefWidth(180);

        TableColumn<Document, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "itemType")));
        typeCol.setPrefWidth(80);

        TableColumn<Document, String> dateCol = new TableColumn<>("Request Date");
        dateCol.setCellValueFactory(data -> {
            Date d = safeGetDate(data.getValue(), "requestDate");
            return new javafx.beans.property.SimpleStringProperty(d != null ? dateFormat.format(d) : "—");
        });
        dateCol.setPrefWidth(130);

        TableColumn<Document, String> notesCol = new TableColumn<>("Requester Notes");
        notesCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "notes")));
        notesCol.setPrefWidth(200);

        requestsTable.getColumns().addAll(requesterCol, itemCol, typeCol, dateCol, notesCol);
        VBox.setVgrow(requestsTable, Priority.ALWAYS);

        // Action buttons
        Button approveBtn = styledButton("✅ Approve", "#27ae60");
        approveBtn.setOnAction(e -> handleApproveRequest());

        Button rejectBtn = styledButton("❌ Reject", "#e74c3c");
        rejectBtn.setOnAction(e -> handleRejectRequest());

        Button refreshBtn = styledButton("🔄 Refresh", "#3498db");
        refreshBtn.setOnAction(e -> loadPendingRequests());

        HBox buttons = new HBox(10, approveBtn, rejectBtn, refreshBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().addAll(info, requestsTable, buttons);
        VBox.setVgrow(content, Priority.ALWAYS);
        return content;
    }

    private void loadPendingRequests() {
        try {
            List<Document> requests = inventoryService.getPendingRequests();
            requestsTable.setItems(FXCollections.observableArrayList(requests));
        } catch (Exception e) {
            // Admin check failed or other error - just show empty
            requestsTable.setItems(FXCollections.observableArrayList());
        }
    }

    private void handleApproveRequest() {
        Document selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Please select a request to approve.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Approve Request");
        dialog.setHeaderText("Approve request from: " + safeGetString(selected, "requesterName"));
        dialog.setContentText("Admin notes (optional):");

        dialog.showAndWait().ifPresent(notes -> {
            try {
                String requestId = getIdString(selected);
                inventoryService.approveRequest(requestId, notes);
                showAlert(Alert.AlertType.INFORMATION, "Request approved! Item has been assigned.");
                loadPendingRequests();
                loadItems();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error: " + e.getMessage());
            }
        });
    }

    private void handleRejectRequest() {
        Document selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Please select a request to reject.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Request");
        dialog.setHeaderText("Reject request from: " + safeGetString(selected, "requesterName"));
        dialog.setContentText("Reason for rejection:");

        dialog.showAndWait().ifPresent(reason -> {
            if (reason.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Please provide a reason for rejection.");
                return;
            }
            try {
                String requestId = getIdString(selected);
                inventoryService.rejectRequest(requestId, reason);
                showAlert(Alert.AlertType.INFORMATION, "Request rejected.");
                loadPendingRequests();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error: " + e.getMessage());
            }
        });
    }

    // ========== Original Items Tab Methods ==========

    private HBox createFiltersRow() {
        searchField = new TextField();
        searchField.setPromptText("🔍 Search by name...");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All Status", "Available", "Assigned", "Under Repair");
        statusFilter.setValue("All Status");
        statusFilter.setOnAction(e -> applyFilters());

        typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("All Types", "Laptop", "License", "Equipment");
        typeFilter.setValue("All Types");
        typeFilter.setOnAction(e -> applyFilters());

        Button refreshBtn = styledButton("🔄 Refresh", "#3498db");
        refreshBtn.setOnAction(e -> loadItems());

        HBox row = new HBox(10, searchField, statusFilter, typeFilter, refreshBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    @SuppressWarnings("unchecked")
    private void setupTable() {
        TableColumn<Document, String> nameCol = new TableColumn<>("Item Name");
        nameCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(safeGetString(data.getValue(), "name")));
        nameCol.setPrefWidth(180);

        TableColumn<Document, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(safeGetString(data.getValue(), "itemType")));
        typeCol.setPrefWidth(100);

        TableColumn<Document, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(safeGetString(data.getValue(), "status")));
        statusCol.setPrefWidth(100);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "Available" -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        case "Assigned" -> setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        case "Under Repair" -> setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                        default -> setStyle("");
                    }
                }
            }
        });

        TableColumn<Document, String> assignedCol = new TableColumn<>("Assigned To");
        assignedCol.setCellValueFactory(data -> {
            Document doc = data.getValue();
            String itemType = safeGetString(doc, "itemType");

            if ("License".equals(itemType)) {
                // For licenses, show count of assigned users
                @SuppressWarnings("unchecked")
                List<Document> users = doc.get("assignedUsers", List.class);
                if (users != null && !users.isEmpty()) {
                    if (users.size() == 1) {
                        return new javafx.beans.property.SimpleStringProperty(users.get(0).getString("userName"));
                    }
                    return new javafx.beans.property.SimpleStringProperty(users.size() + " users");
                }
                return new javafx.beans.property.SimpleStringProperty("—");
            } else {
                // For Laptop/Equipment, show single user
                String name = safeGetString(doc, "assignedToName");
                return new javafx.beans.property.SimpleStringProperty(name.isEmpty() ? "—" : name);
            }
        });
        assignedCol.setPrefWidth(150);

        TableColumn<Document, String> dateCol = new TableColumn<>("Purchase Date");
        dateCol.setCellValueFactory(data -> {
            Date d = safeGetDate(data.getValue(), "purchaseDate");
            return new javafx.beans.property.SimpleStringProperty(d != null ? dateFormat.format(d) : "—");
        });
        dateCol.setPrefWidth(130);

        table.getColumns().addAll(nameCol, typeCol, statusCol, assignedCol, dateCol);
        table.setItems(displayedItems);
        table.setRowFactory(tv -> {
            TableRow<Document> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showDetailDialog(row.getItem());
                }
            });
            return row;
        });
    }

    private HBox createPagingControls() {
        Button prevBtn = styledButton("◀ Prev", "#7f8c8d");
        Button nextBtn = styledButton("Next ▶", "#7f8c8d");
        pageLabel = new Label("Page 1");
        pageLabel.setStyle("-fx-font-size: 14px;");

        prevBtn.setOnAction(e -> {
            if (currentPage > 0) {
                currentPage--;
                updatePagination();
            }
        });
        nextBtn.setOnAction(e -> {
            if ((currentPage + 1) * pageSize < allItems.size()) {
                currentPage++;
                updatePagination();
            }
        });

        HBox box = new HBox(10, prevBtn, pageLabel, nextBtn);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private HBox createActionButtons() {
        Button addBtn = styledButton("➕ Add Item", "#27ae60");
        Button assignBtn = styledButton("👤 Assign", "#3498db");
        Button unassignBtn = styledButton("↩️ Unassign", "#e67e22");
        Button repairBtn = styledButton("🔧 Mark Repair", "#f39c12");
        Button deleteBtn = styledButton("🗑️ Delete", "#e74c3c");
        Button historyBtn = styledButton("📜 Audit History", "#9b59b6");

        addBtn.setOnAction(e -> showAddItemDialog());
        assignBtn.setOnAction(e -> handleAssign());
        unassignBtn.setOnAction(e -> handleUnassign());
        repairBtn.setOnAction(e -> handleMarkRepair());
        deleteBtn.setOnAction(e -> handleDelete());
        historyBtn.setOnAction(e -> showAuditHistory());

        HBox box = new HBox(10, addBtn, assignBtn, unassignBtn, repairBtn, historyBtn, deleteBtn);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 0, 0, 0));
        return box;
    }

    private Button styledButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-font-size: 13px; -fx-padding: 8 15; -fx-background-radius: 5;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: derive(" + color + ", -10%); " +
                "-fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 15; -fx-background-radius: 5;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-font-size: 13px; -fx-padding: 8 15; -fx-background-radius: 5;"));
        return btn;
    }

    private void loadItems() {
        allItems.clear();
        allItems.addAll(inventoryService.listAllItems());
        currentPage = 0;
        applyFilters();
    }

    private void applyFilters() {
        String query = searchField.getText().toLowerCase().trim();
        String statusVal = statusFilter.getValue();
        String typeVal = typeFilter.getValue();

        List<Document> filtered = new ArrayList<>();
        for (Document doc : allItems) {
            String name = safeGetString(doc, "name").toLowerCase();
            String status = safeGetString(doc, "status");
            String type = safeGetString(doc, "itemType");

            boolean matchQuery = query.isEmpty() || name.contains(query);
            boolean matchStatus = "All Status".equals(statusVal) || status.equals(statusVal);
            boolean matchType = "All Types".equals(typeVal) || type.equals(typeVal);

            if (matchQuery && matchStatus && matchType) {
                filtered.add(doc);
            }
        }

        displayedItems.setAll(filtered);
        currentPage = 0;
        updatePagination();
    }

    private void updatePagination() {
        int totalPages = Math.max(1, (int) Math.ceil(displayedItems.size() / (double) pageSize));
        pageLabel.setText("Page " + (currentPage + 1) + " of " + totalPages);

        int fromIndex = currentPage * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, displayedItems.size());

        if (fromIndex < displayedItems.size()) {
            table.setItems(FXCollections.observableArrayList(displayedItems.subList(fromIndex, toIndex)));
        } else {
            table.setItems(FXCollections.observableArrayList());
        }
    }

    // === Dialog Methods ===

    private void showAddItemDialog() {
        Dialog<Document> dialog = new Dialog<>();
        dialog.setTitle("Add New Inventory Item");
        dialog.setHeaderText("Enter item details");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("e.g., Dell Laptop #001");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Laptop", "License", "Equipment");
        typeCombo.setValue("Laptop");

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Optional notes...");
        notesArea.setPrefRowCount(3);

        grid.add(new Label("Item Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Type:"), 0, 1);
        grid.add(typeCombo, 1, 1);
        grid.add(new Label("Notes:"), 0, 2);
        grid.add(notesArea, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Item name is required!");
                    return null;
                }
                try {
                    inventoryService.createItem(name, typeCombo.getValue(), notesArea.getText().trim());
                    showAlert(Alert.AlertType.INFORMATION, "Item created successfully!");
                    loadItems();
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void handleAssign() {
        Document selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Please select an item to assign.");
            return;
        }

        String itemType = safeGetString(selected, "itemType");

        if ("License".equals(itemType)) {
            // For licenses, show multi-user management dialog
            showLicenseUsersDialog(selected);
        } else {
            // For Laptop/Equipment, use single-user assignment
            if ("Assigned".equals(safeGetString(selected, "status"))) {
                showAlert(Alert.AlertType.WARNING, "Item is already assigned to: " +
                        safeGetString(selected, "assignedToName"));
                return;
            }
            showSingleUserAssignDialog(selected);
        }
    }

    private void showSingleUserAssignDialog(Document item) {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Assign Item");
        dialog.setHeaderText("Assign: " + safeGetString(item, "name"));

        ButtonType assignType = new ButtonType("Assign", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(assignType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField userIdField = new TextField();
        userIdField.setPromptText("Entity ID (e.g., STU-001)");
        TextField userNameField = new TextField();
        userNameField.setPromptText("Full Name");

        Label infoLabel = new Label("⚠️ User ID and Name must match a person in the system.");
        infoLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

        grid.add(new Label("User ID:"), 0, 0);
        grid.add(userIdField, 1, 0);
        grid.add(new Label("User Name:"), 0, 1);
        grid.add(userNameField, 1, 1);
        grid.add(infoLabel, 0, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == assignType) {
                return new String[] { userIdField.getText().trim(), userNameField.getText().trim() };
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result[0].isEmpty() || result[1].isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Please provide both User ID and Name.");
                return;
            }
            try {
                inventoryService.allocateItem(getIdString(item), result[0], result[1]);
                showAlert(Alert.AlertType.INFORMATION, "Item assigned successfully!");
                loadItems();
            } catch (IllegalArgumentException e) {
                showAlert(Alert.AlertType.ERROR, "Validation Error: " + e.getMessage());
            } catch (IllegalStateException e) {
                showAlert(Alert.AlertType.WARNING, e.getMessage());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error: " + e.getMessage());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void showLicenseUsersDialog(Document license) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Manage License Users");
        dialog.setHeaderText("License: " + safeGetString(license, "name"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(500);

        VBox content = new VBox(15);
        content.setPadding(new Insets(15));

        // Current users list
        Label usersLabel = new Label("Assigned Users:");
        usersLabel.setStyle("-fx-font-weight: bold;");

        VBox usersList = new VBox(5);
        List<Document> assignedUsers = license.get("assignedUsers", List.class);
        if (assignedUsers == null) {
            assignedUsers = new ArrayList<>();
        }

        if (assignedUsers.isEmpty()) {
            usersList.getChildren().add(new Label("No users assigned yet."));
        } else {
            for (Document user : assignedUsers) {
                String userId = user.getString("userId");
                String userName = user.getString("userName");
                Date assignedDate = user.getDate("assignedDate");

                HBox userRow = new HBox(10);
                userRow.setAlignment(Pos.CENTER_LEFT);

                Label userLabel = new Label("👤 " + userName + " (" + userId + ")");
                userLabel.setStyle("-fx-font-size: 13px;");

                if (assignedDate != null) {
                    Label dateLabel = new Label(" - " + dateFormat.format(assignedDate));
                    dateLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
                    userRow.getChildren().addAll(userLabel, dateLabel);
                } else {
                    userRow.getChildren().add(userLabel);
                }

                Button removeBtn = new Button("✕");
                removeBtn.setStyle(
                        "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 2 6;");
                final String userIdToRemove = userId;
                removeBtn.setOnAction(e -> {
                    try {
                        inventoryService.removeUserFromLicense(getIdString(license), userIdToRemove);
                        showAlert(Alert.AlertType.INFORMATION, "User removed from license.");
                        dialog.close();
                        loadItems();
                    } catch (Exception ex) {
                        showAlert(Alert.AlertType.ERROR, "Error: " + ex.getMessage());
                    }
                });

                HBox.setHgrow(userLabel, Priority.ALWAYS);
                userRow.getChildren().add(removeBtn);
                usersList.getChildren().add(userRow);
            }
        }

        // Add user section
        Separator separator = new Separator();

        Label addLabel = new Label("Add New User:");
        addLabel.setStyle("-fx-font-weight: bold;");

        GridPane addGrid = new GridPane();
        addGrid.setHgap(10);
        addGrid.setVgap(10);

        TextField userIdField = new TextField();
        userIdField.setPromptText("Entity ID (e.g., STU-001)");
        TextField userNameField = new TextField();
        userNameField.setPromptText("Full Name");
        Button addBtn = styledButton("➕ Add User", "#27ae60");

        addGrid.add(new Label("User ID:"), 0, 0);
        addGrid.add(userIdField, 1, 0);
        addGrid.add(new Label("User Name:"), 0, 1);
        addGrid.add(userNameField, 1, 1);
        addGrid.add(addBtn, 1, 2);

        Label infoLabel = new Label("⚠️ User ID and Name must match a person in the system.");
        infoLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

        addBtn.setOnAction(e -> {
            String userId = userIdField.getText().trim();
            String userName = userNameField.getText().trim();
            if (userId.isEmpty() || userName.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Please provide both User ID and Name.");
                return;
            }
            try {
                inventoryService.addUserToLicense(getIdString(license), userId, userName);
                showAlert(Alert.AlertType.INFORMATION, "User added to license!");
                dialog.close();
                loadItems();
            } catch (IllegalArgumentException ex) {
                showAlert(Alert.AlertType.ERROR, "Validation Error: " + ex.getMessage());
            } catch (IllegalStateException ex) {
                showAlert(Alert.AlertType.WARNING, ex.getMessage());
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Error: " + ex.getMessage());
            }
        });

        content.getChildren().addAll(usersLabel, usersList, separator, addLabel, addGrid, infoLabel);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(350);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.showAndWait();
    }

    private void handleUnassign() {
        Document selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Please select an item to unassign.");
            return;
        }

        String itemType = safeGetString(selected, "itemType");

        if ("License".equals(itemType)) {
            // For licenses, show the management dialog instead
            showLicenseUsersDialog(selected);
            return;
        }

        if (!"Assigned".equals(safeGetString(selected, "status"))) {
            showAlert(Alert.AlertType.WARNING, "Item is not currently assigned.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Unassign");
        confirm.setHeaderText("Unassign item from: " + safeGetString(selected, "assignedToName") + "?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    inventoryService.deallocateItem(getIdString(selected));
                    showAlert(Alert.AlertType.INFORMATION, "Item unassigned successfully!");
                    loadItems();
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error: " + e.getMessage());
                }
            }
        });
    }

    private void handleMarkRepair() {
        Document selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Please select an item.");
            return;
        }

        try {
            inventoryService.markUnderRepair(getIdString(selected));
            showAlert(Alert.AlertType.INFORMATION, "Item marked as Under Repair.");
            loadItems();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error: " + e.getMessage());
        }
    }

    private void handleDelete() {
        Document selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Please select an item to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete item: " + safeGetString(selected, "name") + "?");
        confirm.setContentText("This action cannot be undone.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    inventoryService.deleteItem(getIdString(selected));
                    showAlert(Alert.AlertType.INFORMATION, "Item deleted.");
                    loadItems();
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error: " + e.getMessage());
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void showDetailDialog(Document doc) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Item Details");
        dialog.setHeaderText(safeGetString(doc, "name"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;
        grid.add(new Label("Type:"), 0, row);
        grid.add(new Label(safeGetString(doc, "itemType")), 1, row++);

        grid.add(new Label("Status:"), 0, row);
        grid.add(new Label(safeGetString(doc, "status")), 1, row++);

        String itemType = safeGetString(doc, "itemType");
        if ("License".equals(itemType)) {
            grid.add(new Label("Assigned Users:"), 0, row);
            List<Document> users = doc.get("assignedUsers", List.class);
            if (users != null && !users.isEmpty()) {
                VBox usersBox = new VBox(3);
                for (Document user : users) {
                    usersBox.getChildren()
                            .add(new Label("• " + user.getString("userName") + " (" + user.getString("userId") + ")"));
                }
                grid.add(usersBox, 1, row++);
            } else {
                grid.add(new Label("—"), 1, row++);
            }
        } else {
            grid.add(new Label("Assigned To:"), 0, row);
            String assigned = safeGetString(doc, "assignedToName");
            grid.add(new Label(assigned.isEmpty() ? "—" : assigned), 1, row++);
        }

        Date purchaseDate = safeGetDate(doc, "purchaseDate");
        grid.add(new Label("Purchase Date:"), 0, row);
        grid.add(new Label(purchaseDate != null ? dateFormat.format(purchaseDate) : "—"), 1, row++);

        Date assignedDate = safeGetDate(doc, "assignedDate");
        grid.add(new Label("Assigned Date:"), 0, row);
        grid.add(new Label(assignedDate != null ? dateFormat.format(assignedDate) : "—"), 1, row++);

        grid.add(new Label("Notes:"), 0, row);
        grid.add(new Label(safeGetString(doc, "notes")), 1, row++);

        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
    }

    private void showAuditHistory() {
        Document selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Please select an item to view history.");
            return;
        }

        List<Document> logs = inventoryService.getAuditHistory(getIdString(selected));

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Audit History");
        dialog.setHeaderText("History for: " + safeGetString(selected, "name"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(500);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        if (logs.isEmpty()) {
            content.getChildren().add(new Label("No audit history found."));
        } else {
            for (Document log : logs) {
                Date timestamp = safeGetDate(log, "timestamp");
                String action = safeGetString(log, "action");
                String details = safeGetString(log, "details");
                String performedBy = safeGetString(log, "performedByName");

                String timeStr = timestamp != null ? dateFormat.format(timestamp) : "Unknown";
                Label entry = new Label(String.format("[%s] %s - %s (by %s)",
                        timeStr, action, details, performedBy));
                entry.setWrapText(true);
                entry.setStyle("-fx-padding: 5; -fx-background-color: #ecf0f1; -fx-background-radius: 5;");
                content.getChildren().add(entry);
            }
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.showAndWait();
    }

    // === Utilities ===

    private String getIdString(Document doc) {
        Object id = doc.get("_id");
        if (id instanceof org.bson.types.ObjectId) {
            return ((org.bson.types.ObjectId) id).toHexString();
        }
        return id != null ? id.toString() : "";
    }

    private String safeGetString(Document doc, String key) {
        Object val = doc.get(key);
        return val != null ? val.toString() : "";
    }

    private Date safeGetDate(Document doc, String key) {
        Object val = doc.get(key);
        return val instanceof Date ? (Date) val : null;
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type, msg);
        alert.showAndWait();
    }

    public VBox getView() {
        return view;
    }
}
