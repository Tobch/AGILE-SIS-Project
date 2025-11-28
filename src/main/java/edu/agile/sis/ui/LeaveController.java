package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.LeaveService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.scene.text.Text;
import javafx.scene.control.OverrunStyle;
import org.bson.Document;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class LeaveController {
    private final VBox view = new VBox(14);
    private final LeaveService service = new LeaveService();
    private final ObservableList<Document> data = FXCollections.observableArrayList();
    private final TableView<Document> table = new TableView<>();

    
    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final Label lastRefLabel = new Label();
    private final Label countLabel = new Label();


    private final Label lblStaff = new Label();
    private final Label lblPeriod = new Label();
    private final Label lblType = new Label();
    private final Label lblStatusBadge = new Label();
    private final Label lblApprover = new Label();
    private final Label lblSubmitted = new Label();
    private final TextArea txtReason = new TextArea();

    public LeaveController() {
        buildUI();
        hookActions();
        refresh(); 
    }

    private void buildUI() {
        view.setPadding(new Insets(18));
        view.setStyle("-fx-background-color: #f4f6f8;");

        // Header
        Label title = new Label("Leave Requests");
        title.setStyle("-fx-font-size:20px; -fx-font-weight:700; -fx-text-fill:#2c3e50;");
        Label subtitle = new Label("Submit, view and manage staff leave requests.");
        subtitle.setStyle("-fx-font-size:12px; -fx-text-fill:#7a8a97;");

        VBox titleBox = new VBox(2, title, subtitle);

        // Toolbar buttons with nicer colors
        Button btnRequest = styledButton("Request Leave", "primary"); 
        Button btnRefresh = styledButton("Refresh", "neutral");
        Button btnEdit = styledButton("Edit", "neutral");
        Button btnDelete = styledButton("Delete", "danger");
        Button btnApprove = styledButton("Approve", "success");
        Button btnReject = styledButton("Reject", "danger");

        boolean isAdmin = AuthSession.getInstance().hasRole("Admin");
        btnApprove.setVisible(isAdmin);
        btnReject.setVisible(isAdmin);

   
        searchField.setPromptText("Search by staff, type, or reason...");
        searchField.setPrefWidth(320);

    
        statusFilter.getItems().addAll("All", "PENDING", "APPROVED", "REJECTED");
        statusFilter.setValue("All");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        countLabel.setStyle("-fx-text-fill:#6b7280; -fx-font-size:11px;");

        HBox toolbar = new HBox(8,
                searchField,
                statusFilter,
                btnRequest,
                btnRefresh,
                btnEdit,
                btnDelete,
                btnApprove,
                btnReject,
                spacer,
                countLabel,
                lastRefLabel
        );
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6));


        TableColumn<Document, String> staffCol = new TableColumn<>("Staff");
        staffCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safeString(c.getValue(), "staffId")));
        staffCol.setPrefWidth(180);

        TableColumn<Document, String> periodCol = new TableColumn<>("Period");
        periodCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safeString(c.getValue(), "period")));
        periodCol.setPrefWidth(320);

        TableColumn<Document, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safeString(c.getValue(), "type")));
        typeCol.setPrefWidth(150);

        TableColumn<Document, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safeString(c.getValue(), "status")));
        statusCol.setPrefWidth(120);

        statusCol.setCellFactory(col -> new TableCell<>() {
            private final Label chip = new Label();

            {
                chip.setStyle("-fx-padding:4 8; -fx-border-radius:6; -fx-background-radius:6; -fx-font-weight:600;");
                chip.setMinWidth(70);
                chip.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null || status.isBlank()) {
                    setGraphic(null);
                    setText(null);
                } else {
                    String s = status.toUpperCase();
                    chip.setText(s);
                    switch (s) {
                        case "APPROVED" -> chip.setStyle(chip.getStyle()
                                + "-fx-background-color:#e6f8ef; -fx-text-fill:#17a673; -fx-border-color:#cceee0;");
                        case "REJECTED" -> chip.setStyle(chip.getStyle()
                                + "-fx-background-color:#fdecea; -fx-text-fill:#c0392b; -fx-border-color:#f7d4d2;");
                        case "PENDING" -> chip.setStyle(chip.getStyle()
                                + "-fx-background-color:#fff4e6; -fx-text-fill:#b87a00; -fx-border-color:#f2dec2;");
                        default -> chip.setStyle(chip.getStyle()
                                + "-fx-background-color:#f0f3f6; -fx-text-fill:#3b3f44; -fx-border-color:#d8dee6;");
                    }
                    setGraphic(chip);
                    setText(null);
                }
            }
        });

        table.getColumns().setAll(staffCol, periodCol, typeCol, statusCol);
        table.setItems(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No leave requests to display."));

        // table container card
        VBox leftCard = new VBox(table);
        leftCard.setPadding(new Insets(12));
        leftCard.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 8;
            -fx-border-radius: 8;
            -fx-border-color: #e6edf2;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.03), 6, 0, 0, 2);
        """);
        leftCard.setPrefWidth(760);

        // details panel - make labels wrap & ellipsis and give period more space
        lblStaff.setWrapText(true);
        lblStaff.setMaxWidth(160);
        lblStaff.setTextOverrun(OverrunStyle.ELLIPSIS);

        lblPeriod.setWrapText(true);
        lblPeriod.setMaxWidth(220);
        lblPeriod.setTextOverrun(OverrunStyle.ELLIPSIS);

        lblType.setWrapText(true);
        lblType.setMaxWidth(160);
        lblType.setTextOverrun(OverrunStyle.ELLIPSIS);

        lblApprover.setWrapText(true);
        lblApprover.setMaxWidth(160);
        lblApprover.setTextOverrun(OverrunStyle.ELLIPSIS);

        lblSubmitted.setWrapText(true);
        lblSubmitted.setMaxWidth(160);
        lblSubmitted.setTextOverrun(OverrunStyle.ELLIPSIS);

        lblStatusBadge.setMinWidth(80);
        lblStatusBadge.setAlignment(Pos.CENTER);
        lblStatusBadge.setWrapText(true);
        lblStatusBadge.setTextOverrun(OverrunStyle.ELLIPSIS);

        txtReason.setEditable(false);
        txtReason.setWrapText(true);
        txtReason.setPrefRowCount(8);
        txtReason.setPrefHeight(140);

        GridPane details = new GridPane();
        details.setHgap(8);
        details.setVgap(6);
        details.setPadding(new Insets(12));
        details.addRow(0, new Label("Staff:"), lblStaff, new Label("Period:"), lblPeriod);
        details.addRow(1, new Label("Type:"), lblType, new Label("Status:"), lblStatusBadge);
        details.addRow(2, new Label("Approver:"), lblApprover, new Label("Submitted:"), lblSubmitted);
        details.add(new Label("Reason:"), 0, 3);
        details.add(txtReason, 0, 4, 4, 1);


        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPercentWidth(18);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(32);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(18);
        ColumnConstraints c3 = new ColumnConstraints();
        c3.setPercentWidth(32);
        details.getColumnConstraints().addAll(c0, c1, c2, c3);

        VBox rightCard = new VBox(8, new Label("Request Details"), details);
        rightCard.setPadding(new Insets(12));
        rightCard.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 8;
            -fx-border-radius: 8;
            -fx-border-color: #e6edf2;
        """);
        rightCard.setPrefWidth(420); // increase width so fields have room

        HBox content = new HBox(12, leftCard, rightCard);
        HBox.setHgrow(leftCard, Priority.ALWAYS);

     
        view.getChildren().addAll(new VBox(2, titleBox, toolbar), content);


        btnRequest.setOnAction(e -> createLeave());
        btnRefresh.setOnAction(e -> refresh());
        btnEdit.setOnAction(e -> editSelected());
        btnDelete.setOnAction(e -> deleteSelected());
        btnApprove.setOnAction(e -> changeStatus("APPROVED"));
        btnReject.setOnAction(e -> changeStatus("REJECTED"));


        btnEdit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        btnDelete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

     
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> showDetails(n));

   
        table.setRowFactory(tv -> {
            TableRow<Document> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    Document d = row.getItem();
                    if (canEdit(d)) editDocument(d);
                }
            });
            return row;
        });
    }

    private void hookActions() {
        
        searchField.textProperty().addListener((obs, oldV, newV) -> applyLocalFilter());

        
        statusFilter.valueProperty().addListener((obs, oldV, newV) -> refresh());
    }

    private void applyLocalFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String status = statusFilter.getValue() == null ? "All" : statusFilter.getValue();

        // Use the currently loaded master list 'data' as source
        List<Document> all = data.stream().collect(Collectors.toList());

        List<Document> filtered = all.stream().filter(d -> {
            boolean matchQ = q.isEmpty()
                    || safeString(d, "staffId").toLowerCase().contains(q)
                    || safeString(d, "type").toLowerCase().contains(q)
                    || safeString(d, "reason").toLowerCase().contains(q)
                    || safeString(d, "period").toLowerCase().contains(q);
            boolean matchStatus = "All".equalsIgnoreCase(status) || safeString(d, "status").equalsIgnoreCase(status);
            return matchQ && matchStatus;
        }).collect(Collectors.toList());

        table.getItems().setAll(filtered);
        countLabel.setText("Showing: " + filtered.size() + " / " + data.size());
    }



    private void createLeave() {
        String staffId = AuthSession.getInstance().getLinkedEntityId();
        if (staffId == null || staffId.isBlank()) staffId = AuthSession.getInstance().getUsername();
        AddLeaveDialog dlg = new AddLeaveDialog(staffId);
        dlg.showAndWait().ifPresent(doc -> {
            service.createLeave(doc);
            refresh();
        });
    }

    private void editSelected() {
        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { warn("Select a request to edit."); return; }
        if (!canEdit(sel)) { error("You are not allowed to edit this request."); return; }
        editDocument(sel);
    }

    private void editDocument(Document existing) {
        String staffId = safeString(existing, "staffId");
        AddLeaveDialog dlg = new AddLeaveDialog(staffId, existing);
        dlg.showAndWait().ifPresent(updated -> {
            String id = existing.getObjectId("_id").toHexString();
            boolean ok = service.updateLeave(id, updated);
            if (ok) { info("Request updated."); refresh(); } else error("Update failed.");
        });
    }

    private void deleteSelected() {
        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { warn("Select a request to delete."); return; }
        if (!canDelete(sel)) { error("You are not allowed to delete this request."); return; }
        boolean conf = confirm("Delete Request", "Are you sure you want to delete this leave request?");
        if (!conf) return;
        String id = sel.getObjectId("_id").toHexString();
        boolean ok = service.deleteLeave(id);
        if (ok) { info("Request deleted."); refresh(); } else error("Delete failed.");
    }

    private void changeStatus(String status) {
        if (!AuthSession.getInstance().hasRole("Admin")) { error("Only Admin can change status."); return; }
        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { warn("Select a request"); return; }
        boolean conf = confirm(status + " Request", "Mark request as " + status + "?");
        if (!conf) return;
        String id = sel.getObjectId("_id").toHexString();
        String approver = AuthSession.getInstance().getLinkedEntityId();
        if (approver == null || approver.isBlank()) approver = AuthSession.getInstance().getUsername();
        boolean ok = service.updateStatus(id, status, approver, "Handled via UI");
        if (ok) { info("Request " + status.toLowerCase() + "."); refresh(); } else error("Failed to update status.");
    }



    private void refresh() {
        data.clear();

        if (AuthSession.getInstance().hasRole("Admin")) {
            List<Document> all = service.listAllLeaves();
            if (all != null) data.addAll(all);
        } else {
            String staffId = AuthSession.getInstance().getLinkedEntityId();
            if (staffId == null || staffId.isBlank()) staffId = AuthSession.getInstance().getUsername();
            List<Document> list = service.listLeavesForStaff(staffId);
            if (list != null) data.addAll(list);
        }

      
        applyLocalFilter();

        lastRefLabel.setText("Last refreshed: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }



    private void showDetails(Document d) {
        if (d == null) {
            lblStaff.setText("");
            lblPeriod.setText("");
            lblType.setText("");
            setStatusBadge("", "");
            lblApprover.setText("");
            lblSubmitted.setText("");
            txtReason.setText("");
            return;
        }
        lblStaff.setText(safeString(d, "staffId"));
        lblPeriod.setText(safeString(d, "period"));
        lblType.setText(safeString(d, "type"));
        setStatusBadge(safeString(d, "status"), safeString(d, "status"));
        lblApprover.setText(safeString(d, "approver"));
        lblSubmitted.setText(formatMillis(safeLong(d, "createdAt")));
        txtReason.setText(safeString(d, "reason"));
    }

    private void setStatusBadge(String status, String label) {
        String s = status == null ? "" : status.toUpperCase();
        lblStatusBadge.setText(label == null ? "" : label);
        lblStatusBadge.setStyle("-fx-padding:4 8; -fx-border-radius:6; -fx-background-radius:6; -fx-font-weight:600;");
        switch (s) {
            case "APPROVED":
                lblStatusBadge.setStyle(lblStatusBadge.getStyle() + "-fx-background-color:#e6f8ef; -fx-text-fill:#17a673; -fx-border-color: #cceee0;");
                break;
            case "REJECTED":
                lblStatusBadge.setStyle(lblStatusBadge.getStyle() + "-fx-background-color:#fdecea; -fx-text-fill:#c0392b; -fx-border-color: #f7d4d2;");
                break;
            case "PENDING":
            default:
                lblStatusBadge.setStyle(lblStatusBadge.getStyle() + "-fx-background-color:#fff4e6; -fx-text-fill:#b87a00; -fx-border-color: #f2dec2;");
                break;
        }
    }



    private boolean canEdit(Document d) {
        if (d == null) return false;
        if (AuthSession.getInstance().hasRole("Admin")) return true;
        String staffId = AuthSession.getInstance().getLinkedEntityId();
        if (staffId == null || staffId.isBlank()) staffId = AuthSession.getInstance().getUsername();
        return staffId.equals(safeString(d, "staffId")) && "PENDING".equalsIgnoreCase(safeString(d, "status"));
    }

    private boolean canDelete(Document d) {
        if (d == null) return false;
        if (AuthSession.getInstance().hasRole("Admin")) return true;
        String staffId = AuthSession.getInstance().getLinkedEntityId();
        if (staffId == null || staffId.isBlank()) staffId = AuthSession.getInstance().getUsername();
        return staffId.equals(safeString(d, "staffId")) && "PENDING".equalsIgnoreCase(safeString(d, "status"));
    }



    private String safeString(Document d, String key) {
        if (d == null) return "";
        Object v = d.get(key);
        return v == null ? "" : v.toString();
    }

    private long safeLong(Document d, String key) {
        if (d == null) return 0L;
        Object v = d.get(key);
        if (v == null) return 0L;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception ex) { return 0L; }
    }

    private String formatMillis(long millis) {
        if (millis <= 0) return "";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(millis));
    }

    // Alerts & confirmations
    private void info(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
    private void warn(String msg) { new Alert(Alert.AlertType.WARNING, msg).showAndWait(); }
    private void error(String msg) { new Alert(Alert.AlertType.ERROR, msg).showAndWait(); }
    private boolean confirm(String title, String body) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, body, ButtonType.YES, ButtonType.NO);
        a.setTitle(title);
        a.showAndWait();
        return a.getResult() == ButtonType.YES;
    }


    private Button styledButton(String text, String styleType) {
        Button b = new Button(text);
        String base = """
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            -fx-padding: 6 10;
            -fx-font-size: 12px;
            -fx-font-weight: 600;
            """;

        String style;
        switch (styleType) {
            case "primary" -> style = base + "-fx-background-color: #1976d2; -fx-text-fill: white; -fx-border-color: #1565c0;";
            case "success" -> style = base + "-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-border-color: #27632b;";
            case "danger" -> style = base + "-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-border-color: #b71c1c;";
            default -> style = base + "-fx-background-color: #ffffff; -fx-text-fill: #333333; -fx-border-color: #d5dbe1;";
        }

        b.setStyle(style);


        b.setOnMouseEntered(e -> {
            String hover;
            switch (styleType) {
                case "primary" -> hover = base + "-fx-background-color: #1565c0; -fx-text-fill: white; -fx-border-color: #0f4a8a;";
                case "success" -> hover = base + "-fx-background-color: #27632b; -fx-text-fill: white; -fx-border-color: #1f4f22;";
                case "danger" -> hover = base + "-fx-background-color: #c62828; -fx-text-fill: white; -fx-border-color: #9b1a1a;";
                default -> hover = base + "-fx-background-color: #f4f7f9; -fx-text-fill: #333333; -fx-border-color: #c8d2da;";
            }
            b.setStyle(hover);
        });

        b.setOnMouseExited(e -> b.setStyle(style));
        return b;
    }

    public VBox getView() { return view; }
}
