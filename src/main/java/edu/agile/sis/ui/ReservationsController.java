package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.ReservationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ReservationsController 
 
 */
public class ReservationsController {
    private final VBox view = new VBox(20);
    private final ReservationService reservationService = new ReservationService();

    private final ObservableList<Document> allReservations = FXCollections.observableArrayList();
    private final ObservableList<Document> paged = FXCollections.observableArrayList();
    private final TableView<Document> table = new TableView<>();
    private final SimpleDateFormat dateTimeFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private int pageSize = 10;
    private int currentPage = 1;
    private int totalPages = 1;

    private final boolean isAdmin;
    private final boolean isProfessor;
    private final boolean isTA;
    private final boolean isStaffGeneric;
    private final boolean canCreate;
    private final boolean canModify;

    private final Label pageLabel = new Label();
    private final Button prevBtn = new Button("◀ Prev");
    private final Button nextBtn = new Button("Next ▶");
    private final ComboBox<Integer> pageSizeBox = new ComboBox<>();

    public ReservationsController() {
   
        view.setPadding(new Insets(25));
        view.setStyle("-fx-background-color: linear-gradient(to bottom right, #f8f9fa, #e9ecef);");
        view.setSpacing(20);

   
        Label title = new Label("🏢 Room Reservations");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#343a40"));

        Label subtitle = new Label("Manage and approve room booking requests");
        subtitle.setFont(Font.font("Segoe UI", 14));
        subtitle.setTextFill(Color.web("#6c757d"));

        VBox headerBox = new VBox(5, title, subtitle);

        
        isAdmin = AuthSession.getInstance().hasRole("Admin");
        isProfessor = AuthSession.getInstance().hasRole("Professor");
        isTA = AuthSession.getInstance().hasRole("TA");
        isStaffGeneric = AuthSession.getInstance().hasRole("Staff") || AuthSession.getInstance().hasRole("Lecturer");
        canCreate = isAdmin || isProfessor || isTA || isStaffGeneric;
        canModify = isAdmin;

        
        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search room, user, or purpose...");
        searchField.setPrefWidth(300);
        searchField.setStyle("-fx-background-radius: 6; -fx-padding: 6 10;");
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            String q = (newV == null) ? "" : newV.trim().toLowerCase();
            if (q.isEmpty()) {
                currentPage = 1;
                updatePagination();
            } else {
                loadReservationsFiltered(q);
            }
        });

   
        pageSizeBox.getItems().addAll(5, 10, 25, 50);
        pageSizeBox.setValue(pageSize);
        pageSizeBox.setOnAction(e -> {
            pageSize = pageSizeBox.getValue();
            currentPage = 1;
            updatePagination();
        });
        prevBtn.setOnAction(e -> { if (currentPage > 1) { currentPage--; updatePagination(); } });
        nextBtn.setOnAction(e -> { if (currentPage < totalPages) { currentPage++; updatePagination(); } });

        HBox pagingControls = new HBox(10, new Label("Page size:"), pageSizeBox, prevBtn, pageLabel, nextBtn);
        pagingControls.setAlignment(Pos.CENTER_LEFT);

        setupTable();

       
        Button addBtn = styledButton("➕ Add Reservation", "#28a745");
        Button viewBtn = styledButton("👁 View / Edit", "#007bff");
        Button approveBtn = styledButton("✅ Approve", "#17a2b8");
        Button rejectBtn = styledButton("❌ Reject", "#ffc107");
        Button deleteBtn = styledButton("🗑 Delete", "#dc3545");
        Button refreshBtn = styledButton("🔄 Refresh", "#6c757d");

        addBtn.setDisable(!canCreate);
        deleteBtn.setDisable(!canModify);
        approveBtn.setDisable(!canModify);
        rejectBtn.setDisable(!canModify);

        addBtn.setOnAction(e -> {
            if (!canCreate) showAlert(Alert.AlertType.WARNING, "You don't have permission to create reservations.");
            else addReservation();
        });

        viewBtn.setOnAction(e -> {
            Document sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert(Alert.AlertType.INFORMATION, "Select a reservation first."); return; }
            if (canModify) showDetailDialog(sel);
            else showDetailDialogReadOnly(sel);
        });

        approveBtn.setOnAction(e -> handleApproval(true));
        rejectBtn.setOnAction(e -> handleApproval(false));

        deleteBtn.setOnAction(e -> handleDelete());
        refreshBtn.setOnAction(e -> loadReservations());

        HBox actionButtons = new HBox(10, addBtn, viewBtn, approveBtn, rejectBtn, deleteBtn, refreshBtn);
        actionButtons.setAlignment(Pos.CENTER_LEFT);
        actionButtons.setPadding(new Insets(10, 0, 0, 0));

     
        VBox card = new VBox(15, searchField, pagingControls, table, actionButtons);
        card.setPadding(new Insets(20));
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(10), Insets.EMPTY)));
        card.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 3);");

        view.getChildren().addAll(headerBox, card);
        loadReservations();
    }

    @SuppressWarnings("unchecked")
    private void setupTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(paged);
        table.setStyle("-fx-background-color: white; -fx-border-radius: 6; -fx-background-radius: 6;");
        table.setPlaceholder(new Label("No reservations found."));

        TableColumn<Document, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(getIdString(c.getValue())));

        TableColumn<Document, String> roomCol = new TableColumn<>("Room");
        roomCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safeGetString(c.getValue(), "roomId")));

        TableColumn<Document, String> startCol = new TableColumn<>("Start");
        startCol.setCellValueFactory(c -> {
            Date d = safeGetDate(c.getValue(), "start");
            return new javafx.beans.property.SimpleStringProperty(d == null ? "" : dateTimeFmt.format(d));
        });

        TableColumn<Document, String> endCol = new TableColumn<>("End");
        endCol.setCellValueFactory(c -> {
            Date d = safeGetDate(c.getValue(), "end");
            return new javafx.beans.property.SimpleStringProperty(d == null ? "" : dateTimeFmt.format(d));
        });

        TableColumn<Document, String> creatorCol = new TableColumn<>("Created By");
        creatorCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safeGetString(c.getValue(), "createdBy")));

        TableColumn<Document, String> purposeCol = new TableColumn<>("Purpose");
        purposeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safeGetString(c.getValue(), "purpose")));

        TableColumn<Document, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safeGetString(c.getValue(), "status")));

        table.getColumns().setAll(idCol, roomCol, startCol, endCol, creatorCol, purposeCol, statusCol);

        table.setRowFactory(tv -> {
            TableRow<Document> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    if (canModify) showDetailDialog(row.getItem());
                    else showDetailDialogReadOnly(row.getItem());
                }
            });
            return row;
        });
    }

    private Button styledButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(String.format("""
                -fx-background-color: %s;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-background-radius: 6;
                -fx-padding: 6 14 6 14;
                -fx-cursor: hand;
            """, color));
        return btn;
    }

    private void showAlert(Alert.AlertType type, String msg) {
        new Alert(type, msg).showAndWait();
    }

    // === Core Logic - unchanged ===

    private void handleApproval(boolean approve) {
        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert(Alert.AlertType.INFORMATION, "Select a reservation first."); return; }

        String id = getIdString(sel);
        try {
            boolean ok = approve
                    ? reservationService.approveReservation(id)
                    : reservationService.rejectReservation(id);
            if (ok) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.INFORMATION, approve ? "Reservation approved." : "Reservation rejected.");
                    loadReservations();
                });
            } else {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "No changes made."));
            }
        } catch (Exception ex) {
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, ex.getMessage()));
        }
    }

    private void handleDelete() {
        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert(Alert.AlertType.INFORMATION, "Select a reservation first."); return; }
        if (!canModify) { showAlert(Alert.AlertType.WARNING, "You cannot delete reservations."); return; }

        String id = getIdString(sel);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete reservation " + id + "?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Deletion");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                reservationService.deleteReservation(id);
                loadReservations();
            }
        });
    }

    private void loadReservations() {
        allReservations.clear();
        List<Document> reservations = reservationService.listAllReservations();
        if (reservations == null) reservations = new ArrayList<>();

        if (isAdmin) allReservations.addAll(reservations);
        else {
            String me = AuthSession.getInstance().getUsername();
            for (Document r : reservations)
                if (me.equalsIgnoreCase(safeGetString(r, "createdBy"))) allReservations.add(r);
        }
        currentPage = 1;
        updatePagination();
    }

    private void loadReservationsFiltered(String q) {
        allReservations.clear();
        List<Document> reservations = reservationService.listAllReservations();
        if (reservations == null) return;

        String me = AuthSession.getInstance().getUsername();
        boolean showAll = isAdmin;

        for (Document d : reservations) {
            String room = safeGetString(d, "roomId").toLowerCase();
            String cb = safeGetString(d, "createdBy").toLowerCase();
            String purpose = safeGetString(d, "purpose").toLowerCase();
            if ((room + cb + purpose).contains(q)) {
                if (showAll || cb.equalsIgnoreCase(me)) allReservations.add(d);
            }
        }
        currentPage = 1;
        updatePagination();
    }

    private void updatePagination() {
        int totalItems = allReservations.size();
        totalPages = Math.max(1, (int) Math.ceil((double) totalItems / pageSize));
        if (currentPage > totalPages) currentPage = totalPages;
        int from = (currentPage - 1) * pageSize;
        int to = Math.min(from + pageSize, totalItems);

        paged.setAll(allReservations.subList(from, to));
        pageLabel.setText("Page " + currentPage + " / " + totalPages + " (Total: " + totalItems + ")");
        prevBtn.setDisable(currentPage <= 1);
        nextBtn.setDisable(currentPage >= totalPages);
    }

    // === Utilities ===
    private String getIdString(Document doc) {
        Object id = doc.get("_id");
        if (id instanceof org.bson.types.ObjectId) return ((org.bson.types.ObjectId) id).toHexString();
        return id == null ? "" : id.toString();
    }
    private String safeGetString(Document doc, String key) {
        Object o = doc.get(key);
        return o == null ? "" : o.toString();
    }
    private Date safeGetDate(Document doc, String key) {
        Object o = doc.get(key);
        return (o instanceof Date) ? (Date) o : null;
    }
    private Date parseDateTime(LocalDate d, String time) {
        if (d == null || time == null || time.isBlank()) return null;
        try {
            String s = d.format(DateTimeFormatter.ISO_LOCAL_DATE) + " " + time.trim();
            return new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(s);
        } catch (Exception ex) { return null; }
    }
    
    
    
    private void addReservation() {
        Dialog<Document> dlg = new Dialog<>();
        dlg.setTitle("Add Reservation");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        TextField roomField = new TextField();
        roomField.setPromptText("room_101");
        DatePicker startDate = new DatePicker();
        TextField startTime = new TextField(); startTime.setPromptText("HH:mm");
        DatePicker endDate = new DatePicker();
        TextField endTime = new TextField(); endTime.setPromptText("HH:mm");
        TextField createdBy = new TextField();
        // default createdBy = username if present
        String username = AuthSession.getInstance().getUsername();
        createdBy.setText(username == null ? "" : username);
        TextField purpose = new TextField(); purpose.setPromptText("purpose");

        grid.add(new Label("Room ID:"), 0, 0); grid.add(roomField, 1, 0);
        grid.add(new Label("Start Date:"), 0, 1); grid.add(startDate, 1, 1);
        grid.add(new Label("Start Time (HH:mm):"), 0, 2); grid.add(startTime, 1, 2);
        grid.add(new Label("End Date:"), 0, 3); grid.add(endDate, 1, 3);
        grid.add(new Label("End Time (HH:mm):"), 0, 4); grid.add(endTime, 1, 4);
        grid.add(new Label("Created By:"), 0, 5); grid.add(createdBy, 1, 5);
        grid.add(new Label("Purpose:"), 0, 6); grid.add(purpose, 1, 6);

        dlg.getDialogPane().setContent(grid);

        Button okBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String room = roomField.getText().trim();
            String cb = createdBy.getText().trim();
            if (room.isEmpty() || cb.isEmpty()) {
                ev.consume();
                new Alert(Alert.AlertType.WARNING, "Room and Created By are required.").showAndWait();
                return;
            }
            Date s = parseDateTime(startDate.getValue(), startTime.getText());
            Date e = parseDateTime(endDate.getValue(), endTime.getText());
            if (s == null || e == null || !e.after(s)) {
                ev.consume();
                new Alert(Alert.AlertType.WARNING, "Invalid start/end date-time.").showAndWait();
            }
        });

        dlg.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Date s = parseDateTime(startDate.getValue(), startTime.getText());
                Date e = parseDateTime(endDate.getValue(), endTime.getText());
                Document doc = new Document("roomId", roomField.getText().trim())
                        .append("start", s)
                        .append("end", e)
                        .append("createdBy", createdBy.getText().trim())
                        .append("purpose", purpose.getText().trim())
                        .append("status", "pending") // service will set correct status for admin/staff
                        .append("createdAt", new Date())
                        .append("updatedAt", new Date());
                return doc;
            }
            return null;
        });

        dlg.showAndWait().ifPresent(doc -> {
            if (doc != null) {
                String createdHexId = reservationService.createReservation(
                        doc.getString("roomId"),
                        doc.getDate("start"),
                        doc.getDate("end"),
                        doc.getString("createdBy"),
                        doc.getString("purpose")
                );
                if (createdHexId != null) {
                    loadReservations();
                    new Alert(Alert.AlertType.INFORMATION, "Reservation created (id: " + createdHexId + ")").showAndWait();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Reservation conflict or failed to create").showAndWait();
                }
            }
        });
    }

    private void showDetailDialogReadOnly(Document doc) {
        if (doc == null) return;
        String id = getIdString(doc);
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Reservation Details - " + id);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8); grid.setPadding(new Insets(8));

        TextField roomField = new TextField(safeGetString(doc, "roomId")); roomField.setDisable(true);
        Date startD = safeGetDate(doc, "start");
        Date endD = safeGetDate(doc, "end");
        DatePicker startPicker = new DatePicker();
        TextField startTime = new TextField(); startTime.setDisable(true);
        DatePicker endPicker = new DatePicker();
        TextField endTime = new TextField(); endTime.setDisable(true);
        if (startD != null) {
            startPicker.setValue(((java.sql.Date) new java.sql.Date(startD.getTime())).toLocalDate());
            startTime.setText(new java.text.SimpleDateFormat("HH:mm").format(startD));
        }
        if (endD != null) {
            endPicker.setValue(((java.sql.Date) new java.sql.Date(endD.getTime())).toLocalDate());
            endTime.setText(new java.text.SimpleDateFormat("HH:mm").format(endD));
        }

        TextField createdByField = new TextField(safeGetString(doc, "createdBy")); createdByField.setDisable(true);
        TextField purposeField = new TextField(safeGetString(doc, "purpose")); purposeField.setDisable(true);
        TextField statusField = new TextField(safeGetString(doc, "status")); statusField.setDisable(true);

        grid.add(new Label("Room ID:"), 0, 0); grid.add(roomField, 1, 0);
        grid.add(new Label("Start Date:"), 0, 1); grid.add(startPicker, 1, 1);
        grid.add(new Label("Start Time (HH:mm):"), 0, 2); grid.add(startTime, 1, 2);
        grid.add(new Label("End Date:"), 0, 3); grid.add(endPicker, 1, 3);
        grid.add(new Label("End Time (HH:mm):"), 0, 4); grid.add(endTime, 1, 4);
        grid.add(new Label("Created By:"), 0, 5); grid.add(createdByField, 1, 5);
        grid.add(new Label("Purpose:"), 0, 6); grid.add(purposeField, 1, 6);
        grid.add(new Label("Status:"), 0, 7); grid.add(statusField, 1, 7);

        dlg.getDialogPane().setContent(grid);
        dlg.showAndWait();
    }

    private void showDetailDialog(Document doc) {
        if (!canModify) { showDetailDialogReadOnly(doc); return; }

        if (doc == null) return;
        String id = getIdString(doc);
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Reservation Details - " + id);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8); grid.setPadding(new Insets(8));

        TextField roomField = new TextField(safeGetString(doc, "roomId"));
        Date startD = safeGetDate(doc, "start");
        Date endD = safeGetDate(doc, "end");
        DatePicker startPicker = new DatePicker();
        TextField startTime = new TextField();
        DatePicker endPicker = new DatePicker();
        TextField endTime = new TextField();
        if (startD != null) {
            startPicker.setValue(((java.sql.Date) new java.sql.Date(startD.getTime())).toLocalDate());
            startTime.setText(new java.text.SimpleDateFormat("HH:mm").format(startD));
        }
        if (endD != null) {
            endPicker.setValue(((java.sql.Date) new java.sql.Date(endD.getTime())).toLocalDate());
            endTime.setText(new java.text.SimpleDateFormat("HH:mm").format(endD));
        }

        TextField createdByField = new TextField(safeGetString(doc, "createdBy"));
        TextField purposeField = new TextField(safeGetString(doc, "purpose"));
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("confirmed", "cancelled", "pending", "rejected"));
        statusBox.setValue(safeGetString(doc, "status") == null ? "pending" : safeGetString(doc, "status"));

        grid.add(new Label("Room ID:"), 0, 0); grid.add(roomField, 1, 0);
        grid.add(new Label("Start Date:"), 0, 1); grid.add(startPicker, 1, 1);
        grid.add(new Label("Start Time (HH:mm):"), 0, 2); grid.add(startTime, 1, 2);
        grid.add(new Label("End Date:"), 0, 3); grid.add(endPicker, 1, 3);
        grid.add(new Label("End Time (HH:mm):"), 0, 4); grid.add(endTime, 1, 4);
        grid.add(new Label("Created By:"), 0, 5); grid.add(createdByField, 1, 5);
        grid.add(new Label("Purpose:"), 0, 6); grid.add(purposeField, 1, 6);
        grid.add(new Label("Status:"), 0, 7); grid.add(statusBox, 1, 7);

        dlg.getDialogPane().setContent(grid);

        final ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().add(saveBtn);

        Optional<ButtonType> result = dlg.showAndWait();
        if (result.isPresent()) {
            ButtonType pressed = result.get();
            if (pressed == saveBtn || pressed == ButtonType.OK) {
                Date s = parseDateTime(startPicker.getValue(), startTime.getText());
                Date e = parseDateTime(endPicker.getValue(), endTime.getText());
                if (s == null || e == null || !e.after(s)) {
                    new Alert(Alert.AlertType.WARNING, "Invalid start/end").showAndWait();
                    return;
                }
                Document updated = new Document("roomId", roomField.getText().trim())
                        .append("start", s)
                        .append("end", e)
                        .append("createdBy", createdByField.getText().trim())
                        .append("purpose", purposeField.getText().trim())
                        .append("status", statusBox.getValue())
                        .append("updatedAt", new Date());

                // persist change (service enforces permission)
                try {
                    boolean ok = reservationService.updateReservation(id, updated);
                    if (!ok) {
                        new Alert(Alert.AlertType.ERROR, "Conflict detected or update failed. Reservation not saved.").showAndWait();
                    } else {
                        loadReservations();
                    }
                } catch (SecurityException ex) {
                    new Alert(Alert.AlertType.ERROR, "Permission denied: " + ex.getMessage()).showAndWait();
                }
            }
        }
    }

   

    public VBox getView() { return view; }
}
