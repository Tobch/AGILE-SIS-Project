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
 * JavaFX Controller for Staff/Student Resource Browser.
 * Allows users to browse available items, submit requests, and view their
 * assigned items.
 */
public class InventoryBrowserController {
    private final VBox view = new VBox(20);
    private final InventoryService inventoryService = new InventoryService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private TabPane tabPane;
    private TableView<Document> availableTable;
    private TableView<Document> requestsTable;
    private TableView<Document> assignedTable;

    public InventoryBrowserController() {
        view.setPadding(new Insets(20));
        view.setStyle("-fx-background-color: #f4f6f7;");

        // Header
        Label header = new Label("📦 Request Resources");
        header.setFont(Font.font("System", FontWeight.BOLD, 24));
        header.setStyle("-fx-text-fill: #2c3e50;");

        // Tab Pane
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab availableTab = new Tab("🛒 Available Items");
        availableTab.setContent(createAvailableItemsTab());

        Tab requestsTab = new Tab("📋 My Requests");
        requestsTab.setContent(createMyRequestsTab());

        Tab assignedTab = new Tab("✅ My Assigned Items");
        assignedTab.setContent(createMyAssignedTab());

        tabPane.getTabs().addAll(availableTab, requestsTab, assignedTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Card wrapper
        VBox card = new VBox(tabPane);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        VBox.setVgrow(card, Priority.ALWAYS);

        view.getChildren().addAll(header, card);

        loadAllData();
    }

    @SuppressWarnings("unchecked")
    private VBox createAvailableItemsTab() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(15));

        Label info = new Label(
                "Browse available resources and submit a request. Your request will be reviewed by an admin.");
        info.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
        info.setWrapText(true);

        availableTable = new TableView<>();

        TableColumn<Document, String> nameCol = new TableColumn<>("Item Name");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "name")));
        nameCol.setPrefWidth(200);

        TableColumn<Document, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "itemType")));
        typeCol.setPrefWidth(100);

        TableColumn<Document, String> notesCol = new TableColumn<>("Description");
        notesCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "notes")));
        notesCol.setPrefWidth(250);

        availableTable.getColumns().addAll(nameCol, typeCol, notesCol);
        VBox.setVgrow(availableTable, Priority.ALWAYS);

        // Request button
        Button requestBtn = styledButton("📝 Request Selected Item", "#27ae60");
        requestBtn.setOnAction(e -> handleRequestItem());

        Button refreshBtn = styledButton("🔄 Refresh", "#3498db");
        refreshBtn.setOnAction(e -> loadAvailableItems());

        HBox buttons = new HBox(10, requestBtn, refreshBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().addAll(info, availableTable, buttons);
        VBox.setVgrow(content, Priority.ALWAYS);
        return content;
    }

    @SuppressWarnings("unchecked")
    private VBox createMyRequestsTab() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(15));

        Label info = new Label("View the status of your requests. Pending requests can be cancelled.");
        info.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
        info.setWrapText(true);

        requestsTable = new TableView<>();

        TableColumn<Document, String> itemCol = new TableColumn<>("Item");
        itemCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "itemName")));
        itemCol.setPrefWidth(180);

        TableColumn<Document, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "itemType")));
        typeCol.setPrefWidth(80);

        TableColumn<Document, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "status")));
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
                        case "Pending" -> setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                        case "Approved" -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        case "Rejected" -> setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        default -> setStyle("");
                    }
                }
            }
        });

        TableColumn<Document, String> dateCol = new TableColumn<>("Request Date");
        dateCol.setCellValueFactory(data -> {
            Date d = safeGetDate(data.getValue(), "requestDate");
            return new javafx.beans.property.SimpleStringProperty(d != null ? dateFormat.format(d) : "—");
        });
        dateCol.setPrefWidth(130);

        TableColumn<Document, String> reviewCol = new TableColumn<>("Review Notes");
        reviewCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "reviewNotes")));
        reviewCol.setPrefWidth(200);

        requestsTable.getColumns().addAll(itemCol, typeCol, statusCol, dateCol, reviewCol);
        VBox.setVgrow(requestsTable, Priority.ALWAYS);

        // Cancel button
        Button cancelBtn = styledButton("❌ Cancel Request", "#e74c3c");
        cancelBtn.setOnAction(e -> handleCancelRequest());

        Button refreshBtn = styledButton("🔄 Refresh", "#3498db");
        refreshBtn.setOnAction(e -> loadMyRequests());

        HBox buttons = new HBox(10, cancelBtn, refreshBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().addAll(info, requestsTable, buttons);
        VBox.setVgrow(content, Priority.ALWAYS);
        return content;
    }

    @SuppressWarnings("unchecked")
    private VBox createMyAssignedTab() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(15));

        Label info = new Label("Items currently assigned to you.");
        info.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
        info.setWrapText(true);

        assignedTable = new TableView<>();

        TableColumn<Document, String> nameCol = new TableColumn<>("Item Name");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "name")));
        nameCol.setPrefWidth(200);

        TableColumn<Document, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "itemType")));
        typeCol.setPrefWidth(100);

        TableColumn<Document, String> dateCol = new TableColumn<>("Assigned Date");
        dateCol.setCellValueFactory(data -> {
            Date d = safeGetDate(data.getValue(), "assignedDate");
            return new javafx.beans.property.SimpleStringProperty(d != null ? dateFormat.format(d) : "—");
        });
        dateCol.setPrefWidth(150);

        TableColumn<Document, String> notesCol = new TableColumn<>("Notes");
        notesCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "notes")));
        notesCol.setPrefWidth(250);

        assignedTable.getColumns().addAll(nameCol, typeCol, dateCol, notesCol);
        VBox.setVgrow(assignedTable, Priority.ALWAYS);

        Button refreshBtn = styledButton("🔄 Refresh", "#3498db");
        refreshBtn.setOnAction(e -> loadMyAssignedItems());

        HBox buttons = new HBox(10, refreshBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().addAll(info, assignedTable, buttons);
        VBox.setVgrow(content, Priority.ALWAYS);
        return content;
    }

    private void loadAllData() {
        loadAvailableItems();
        loadMyRequests();
        loadMyAssignedItems();
    }

    private void loadAvailableItems() {
        try {
            List<Document> items = inventoryService.getAvailableItemsForRequest();
            availableTable.setItems(FXCollections.observableArrayList(items));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error loading items: " + e.getMessage());
        }
    }

    private void loadMyRequests() {
        try {
            List<Document> requests = inventoryService.getMyRequests();
            requestsTable.setItems(FXCollections.observableArrayList(requests));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error loading requests: " + e.getMessage());
        }
    }

    private void loadMyAssignedItems() {
        try {
            List<Document> items = inventoryService.getMyAssignedItems();
            assignedTable.setItems(FXCollections.observableArrayList(items));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error loading assigned items: " + e.getMessage());
        }
    }

    private void handleRequestItem() {
        Document selected = availableTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Please select an item to request.");
            return;
        }

        // Show dialog for notes
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Request Item");
        dialog.setHeaderText("Request: " + safeGetString(selected, "name"));
        dialog.setContentText("Reason for request (optional):");

        dialog.showAndWait().ifPresent(notes -> {
            try {
                String itemId = getIdString(selected);
                inventoryService.submitRequest(itemId, notes);
                showAlert(Alert.AlertType.INFORMATION,
                        "Request submitted successfully! It will be reviewed by an admin.");
                loadAllData();
                tabPane.getSelectionModel().select(1); // Switch to My Requests tab
            } catch (IllegalStateException e) {
                showAlert(Alert.AlertType.WARNING, e.getMessage());
            } catch (SecurityException e) {
                showAlert(Alert.AlertType.ERROR, "Access denied: " + e.getMessage());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error: " + e.getMessage());
            }
        });
    }

    private void handleCancelRequest() {
        Document selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Please select a request to cancel.");
            return;
        }

        if (!"Pending".equals(safeGetString(selected, "status"))) {
            showAlert(Alert.AlertType.WARNING, "Only pending requests can be cancelled.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Cancel");
        confirm.setHeaderText("Cancel request for: " + safeGetString(selected, "itemName") + "?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String requestId = getIdString(selected);
                    inventoryService.cancelRequest(requestId);
                    showAlert(Alert.AlertType.INFORMATION, "Request cancelled.");
                    loadAllData();
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error: " + e.getMessage());
                }
            }
        });
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
