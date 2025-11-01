package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.ReservationService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * ReservationsController - CRUD UI for reservations with role-based UI restrictions.
 * Staff (Professor/TA/Staff) may CREATE (book) but cannot EDIT or DELETE reservations.
 * Admin may do all operations and can Approve/Reject pending bookings.
 *
 * Visibility:
 * - Admin: sees all reservations
 * - Staff: sees only reservations they created (createdBy == username)
 */
public class ReservationsController {
    private final VBox view = new VBox(10);
    private final ReservationService reservationService = new ReservationService();

    private final ObservableList<Document> allReservations = FXCollections.observableArrayList();
    private final ObservableList<Document> paged = FXCollections.observableArrayList();
    private final TableView<Document> table = new TableView<>();
    private final SimpleDateFormat dateTimeFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    // pagination
    private int pageSize = 10;
    private int currentPage = 1;
    private int totalPages = 1;

    // role flags
    private final boolean isAdmin;
    private final boolean isProfessor;
    private final boolean isTA;
    private final boolean isStaffGeneric;
    private final boolean canCreate; // allowed to book
    private final boolean canModify; // allowed to edit/delete/approve/reject

    // controls
    private final Label pageLabel = new Label();
    private final Button prevBtn = new Button("Prev");
    private final Button nextBtn = new Button("Next");
    private final ComboBox<Integer> pageSizeBox = new ComboBox<>();

    public ReservationsController() {
        view.setPadding(new Insets(12));
        Label title = new Label("Reservations Management");

        // load roles
        isAdmin = AuthSession.getInstance().hasRole("Admin");
        isProfessor = AuthSession.getInstance().hasRole("Professor");
        isTA = AuthSession.getInstance().hasRole("TA");
        isStaffGeneric = AuthSession.getInstance().hasRole("Staff") || AuthSession.getInstance().hasRole("Lecturer");

        // policy: Admin can do everything; Professors/TAs/Staff can create/book but not edit/delete
        canCreate = isAdmin || isProfessor || isTA || isStaffGeneric;
        canModify = isAdmin; // only admin can modify/delete/approve/reject

        // Search field
        TextField searchField = new TextField();
        searchField.setPromptText("Search room, createdBy or purpose...");
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            String q = (newV == null) ? "" : newV.trim().toLowerCase();
            if (q.isEmpty()) {
                currentPage = 1;
                updatePagination();
            } else {
                loadReservationsFiltered(q);
            }
        });

        // Page size selector
        pageSizeBox.getItems().addAll(5, 10, 25, 50);
        pageSizeBox.setValue(pageSize);
        pageSizeBox.setOnAction(e -> {
            pageSize = pageSizeBox.getValue();
            currentPage = 1;
            updatePagination();
        });

        prevBtn.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                updatePagination();
            }
        });
        nextBtn.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                updatePagination();
            }
        });

        HBox pagingControls = new HBox(8, new Label("Page size:"), pageSizeBox, prevBtn, pageLabel, nextBtn);
        pagingControls.setPadding(new Insets(6));

        // Table columns
        TableColumn<Document, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(getIdString(cell.getValue())));
        idCol.setPrefWidth(160);

        TableColumn<Document, String> roomCol = new TableColumn<>("Room");
        roomCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(safeGetString(cell.getValue(), "roomId")));
        roomCol.setPrefWidth(120);

        TableColumn<Document, String> startCol = new TableColumn<>("Start");
        startCol.setCellValueFactory(cell -> {
            Date d = safeGetDate(cell.getValue(), "start");
            return new javafx.beans.property.SimpleStringProperty(d == null ? "" : dateTimeFmt.format(d));
        });
        startCol.setPrefWidth(140);

        TableColumn<Document, String> endCol = new TableColumn<>("End");
        endCol.setCellValueFactory(cell -> {
            Date d = safeGetDate(cell.getValue(), "end");
            return new javafx.beans.property.SimpleStringProperty(d == null ? "" : dateTimeFmt.format(d));
        });
        endCol.setPrefWidth(140);

        TableColumn<Document, String> createdByCol = new TableColumn<>("Created By");
        createdByCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(safeGetString(cell.getValue(), "createdBy")));
        createdByCol.setPrefWidth(120);

        TableColumn<Document, String> purposeCol = new TableColumn<>("Purpose");
        purposeCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(safeGetString(cell.getValue(), "purpose")));
        purposeCol.setPrefWidth(200);

        TableColumn<Document, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(safeGetString(cell.getValue(), "status")));
        statusCol.setPrefWidth(100);

        table.getColumns().add(idCol);
        table.getColumns().add(roomCol);
        table.getColumns().add(startCol);
        table.getColumns().add(endCol);
        table.getColumns().add(createdByCol);
        table.getColumns().add(purposeCol);
        table.getColumns().add(statusCol);

        table.setItems(paged);

        // Double-click to view details (everyone allowed to view)
        table.setRowFactory(tv -> {
            TableRow<Document> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    Document doc = row.getItem();
                    showDetailDialog(doc);
                }
            });
            return row;
        });

        // Control buttons
        Button addBtn = new Button("Add Reservation");
        addBtn.setOnAction(e -> {
            if (!canCreate) {
                new Alert(Alert.AlertType.WARNING, "You don't have permission to create reservations.").showAndWait();
                return;
            }
            addReservation();
        });
        addBtn.setDisable(!canCreate);

        Button viewBtn = new Button("View / Edit");
        viewBtn.setOnAction(e -> {
            Document sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) new Alert(Alert.AlertType.INFORMATION, "Select a reservation first.").showAndWait();
            else {
                if (!canModify) {
                    // non-admins may view details (read-only)
                    showDetailDialogReadOnly(sel);
                } else {
                    showDetailDialog(sel);
                }
            }
        });

        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.setOnAction(e -> {
            Document sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { return; }
            if (!canModify) {
                new Alert(Alert.AlertType.WARNING, "You don't have permission to delete reservations.").showAndWait();
                return;
            }
            final String id = getIdString(sel);
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete reservation " + id + "?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText("Confirm delete");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    reservationService.deleteReservation(id);
                    loadReservations();
                }
            });
        });
        deleteBtn.setDisable(!canModify);
// Admin only approve / reject
Button approveBtn = new Button("Approve");
approveBtn.setOnAction(e -> {
    Document sel = table.getSelectionModel().getSelectedItem();
    if (sel == null) {
        System.out.println("[Approve] No selection");
        return;
    }
    String id = getIdString(sel);
    System.out.println("[Approve] clicked for id = " + id + " (doc: " + sel + ")");
    try {
        boolean ok = reservationService.approveReservation(id);
        System.out.println("[Approve] service returned: " + ok);
        if (ok) {
            // ensure UI updates on FX thread
            javafx.application.Platform.runLater(() -> {
                new Alert(Alert.AlertType.INFORMATION, "Reservation approved.").showAndWait();
                loadReservations();
            });
        } else {
            javafx.application.Platform.runLater(() ->
                new Alert(Alert.AlertType.ERROR, "Approve failed. (no rows modified)").showAndWait()
            );
        }
    } catch (SecurityException ex) {
        javafx.application.Platform.runLater(() ->
            new Alert(Alert.AlertType.ERROR, "Permission denied: " + ex.getMessage()).showAndWait()
        );
    } catch (Exception ex) {
        ex.printStackTrace();
        javafx.application.Platform.runLater(() ->
            new Alert(Alert.AlertType.ERROR, "Approve failed: " + ex.getMessage()).showAndWait()
        );
    }
});
approveBtn.setDisable(!canModify);

Button rejectBtn = new Button("Reject");
rejectBtn.setOnAction(e -> {
    Document sel = table.getSelectionModel().getSelectedItem();
    if (sel == null) {
        System.out.println("[Reject] No selection");
        return;
    }
    String id = getIdString(sel);
    System.out.println("[Reject] clicked for id = " + id + " (doc: " + sel + ")");
    try {
        boolean ok = reservationService.rejectReservation(id);
        System.out.println("[Reject] service returned: " + ok);
        if (ok) {
            javafx.application.Platform.runLater(() -> {
                new Alert(Alert.AlertType.INFORMATION, "Reservation rejected.").showAndWait();
                loadReservations();
            });
        } else {
            javafx.application.Platform.runLater(() ->
                new Alert(Alert.AlertType.ERROR, "Reject failed. (no rows modified)").showAndWait()
            );
        }
    } catch (SecurityException ex) {
        javafx.application.Platform.runLater(() ->
            new Alert(Alert.AlertType.ERROR, "Permission denied: " + ex.getMessage()).showAndWait()
        );
    } catch (Exception ex) {
        ex.printStackTrace();
        javafx.application.Platform.runLater(() ->
            new Alert(Alert.AlertType.ERROR, "Reject failed: " + ex.getMessage()).showAndWait()
        );
    }
});
rejectBtn.setDisable(!canModify);


        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> loadReservations());

        HBox buttons = new HBox(8, addBtn, viewBtn, approveBtn, rejectBtn, deleteBtn, refreshBtn);
        buttons.setPadding(new Insets(6));

        view.getChildren().addAll(title, searchField, pagingControls, table, buttons);

        loadReservations();
    }

    /**
     * Load reservations into the controller's master list applying role-based visibility:
     * - Admin: loads all reservations
     * - Non-admin staff: loads only reservations where createdBy == current username
     */
    private void loadReservations() {
        allReservations.clear();
        List<Document> reservations = reservationService.listAllReservations();
        if (reservations == null) reservations = new ArrayList<>();

        if (isAdmin) {
            allReservations.addAll(reservations);
        } else {
            // staff/users see only their own created reservations
            String me = AuthSession.getInstance().getUsername();
            for (Document r : reservations) {
                String createdBy = safeGetString(r, "createdBy");
                if (createdBy != null && createdBy.equalsIgnoreCase(me)) {
                    allReservations.add(r);
                }
            }
        }

        currentPage = 1;
        updatePagination();
    }

    /**
     * Filtered load: respects the same role-based visibility as loadReservations().
     */
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

            boolean matches = room.contains(q) || cb.contains(q) || purpose.contains(q);
            if (!matches) continue;

            if (showAll) {
                allReservations.add(d);
            } else {
                // only add if createdBy == me
                String createdBy = safeGetString(d, "createdBy");
                if (createdBy != null && createdBy.equalsIgnoreCase(me)) {
                    allReservations.add(d);
                }
            }
        }

        currentPage = 1;
        updatePagination();
    }

    private void updatePagination() {
        int totalItems = allReservations.size();
        totalPages = Math.max(1, (int) Math.ceil((double) totalItems / pageSize));
        if (currentPage > totalPages) currentPage = totalPages;
        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalItems);

        paged.clear();
        if (fromIndex < allReservations.size()) {
            paged.addAll(new ArrayList<>(allReservations).subList(fromIndex, toIndex));
        }

        pageLabel.setText("Page " + currentPage + " / " + totalPages + " (Total: " + totalItems + ")");
        prevBtn.setDisable(currentPage <= 1);
        nextBtn.setDisable(currentPage >= totalPages);
    }

    // ---------------------------
    // Add / Edit / Delete
    // ---------------------------

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

    // ---------------------------
    // Helpers
    // ---------------------------

    private String getIdString(Document doc) {
        if (doc == null) return "";
        Object idObj = doc.get("_id");
        if (idObj == null) return "";
        if (idObj instanceof org.bson.types.ObjectId) {
            return ((org.bson.types.ObjectId) idObj).toHexString();
        } else {
            return idObj.toString();
        }
    }

    private String safeGetString(Document doc, String key) {
        if (doc == null) return "";
        Object o = doc.get(key);
        return o == null ? "" : o.toString();
    }

    private Date safeGetDate(Document doc, String key) {
        if (doc == null) return null;
        Object o = doc.get(key);
        if (o instanceof Date) return (Date) o;
        return null;
    }

    private Date parseDateTime(LocalDate d, String time) {
        if (d == null || time == null || time.trim().isEmpty()) return null;
        try {
            String s = d.format(DateTimeFormatter.ISO_LOCAL_DATE) + " " + time.trim();
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").parse(s);
        } catch (Exception ex) {
            return null;
        }
    }

    public VBox getView() {
        return view;
    }
}
