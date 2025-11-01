package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.StaffService;
import edu.agile.sis.service.UserService;
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

import java.util.List;

/**
 * Enhanced StaffController UI
 * - Retains all logic and Firestore operations
 * - Modern UI with card layout, spacing, and clear user actions
 */
public class StaffController {
    private final VBox view = new VBox(12);
    private final StaffService staffService = new StaffService();
    private final TableView<Document> table = new TableView<>();
    private final ObservableList<Document> items = FXCollections.observableArrayList();

    private final boolean isAdmin = AuthSession.getInstance().hasRole("Admin");
    private final boolean isProfessor = AuthSession.getInstance().hasRole("Professor");
    private final boolean isTA = AuthSession.getInstance().hasRole("TA");
    private final String linkedStaffId = AuthSession.getInstance().getLinkedEntityId();

    public StaffController() {
        view.setPadding(new Insets(18));
        view.setSpacing(15);
        view.setStyle("-fx-background-color: linear-gradient(to bottom right, #f8f9fa, #e9ecef);");

        Label title = new Label(isAdmin ? "👥 Staff Management"
                : (isProfessor || isTA ? "👤 My Profile" : "Staff Directory"));
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#343a40"));

        Label subtitle = new Label(isAdmin
                ? "Manage all staff records and linked user accounts"
                : "View and update your personal profile");
        subtitle.setFont(Font.font("Segoe UI", 14));
        subtitle.setTextFill(Color.web("#6c757d"));

        // === Table Columns ===
        TableColumn<Document, String> idCol = new TableColumn<>("Staff ID");
        idCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getString("staffId")));
        idCol.setPrefWidth(110);

        TableColumn<Document, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getString("name")));
        nameCol.setPrefWidth(180);

        TableColumn<Document, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getString("email")));
        emailCol.setPrefWidth(200);

        TableColumn<Document, String> officeCol = new TableColumn<>("Office Hours");
        officeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getString("officeHours")));
        officeCol.setPrefWidth(160);

        TableColumn<Document, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getString("role")));
        roleCol.setPrefWidth(120);

        table.getColumns().addAll(idCol, nameCol, emailCol, officeCol, roleCol);
        table.setItems(items);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8;");
        table.setPlaceholder(new Label("No staff records found."));

        // === Buttons ===
        Button addBtn = new Button("➕ Add Staff");
        Button viewBtn = new Button(isProfessor || isTA ? "✏️ Edit My Profile" : "👁 View / Edit");
        Button delBtn = new Button("🗑 Delete");
        Button refreshBtn = new Button("🔄 Refresh");

        addBtn.setDisable(!isAdmin);
        delBtn.setDisable(!isAdmin);

        addBtn.setStyle(btnStyle("#28a745"));
        viewBtn.setStyle(btnStyle("#007bff"));
        delBtn.setStyle(btnStyle("#dc3545"));
        refreshBtn.setStyle(btnStyle("#17a2b8"));

        addBtn.setOnAction(e -> addStaff());
        viewBtn.setOnAction(e -> handleViewEdit());
        delBtn.setOnAction(e -> handleDelete());
        refreshBtn.setOnAction(e -> load());

        HBox controls = new HBox(10, addBtn, viewBtn, delBtn, refreshBtn);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(8, 0, 8, 0));

        VBox card = new VBox(12, title, subtitle, table, controls);
        card.setPadding(new Insets(20));
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(10), Insets.EMPTY)));
        card.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 2);");

        view.getChildren().addAll(card);
        load();
    }

    private String btnStyle(String color) {
        return String.format("""
            -fx-background-color: %s;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-background-radius: 6;
            -fx-cursor: hand;
            -fx-padding: 6 14 6 14;
            """, color);
    }

    // ================= Existing Logic Preserved =================

    private void handleDelete() {
        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a staff record to delete.").showAndWait();
            return;
        }

        String staffId = sel.getString("staffId");
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete this staff member and their linked user account?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    staffService.delete(sel.getObjectId("_id").toHexString());
                    UserService userService = new UserService();
                    boolean userDeleted = userService.deleteUserByLinkedEntityId(staffId);

                    String msg = "✅ Staff record deleted successfully.";
                    msg += userDeleted
                            ? "\n✅ Linked user account deleted successfully."
                            : "\n⚠️ No linked user account found for this staff.";

                    new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
                    load();
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "❌ Error deleting staff: " + ex.getMessage()).showAndWait();
                    ex.printStackTrace();
                }
            }
        });
    }

    private void load() {
        items.clear();
        if (isAdmin) {
            List<Document> list = staffService.listAll();
            if (list != null) items.addAll(list);
        } else if (isProfessor || isTA) {
            String lookupId = linkedStaffId != null && !linkedStaffId.isBlank()
                    ? linkedStaffId : AuthSession.getInstance().getUsername();

            Document me = staffService.getByStaffId(lookupId);
            if (me != null) items.add(me);
            else new Alert(Alert.AlertType.WARNING,
                    "No staff record found for your account (ID: " + lookupId + "). Contact admin.").showAndWait();
        } else {
            List<Document> list = staffService.listAll();
            if (list != null) items.addAll(list);
        }
    }

    private void handleViewEdit() {
        Document selected;
        if (isProfessor || isTA) {
            selected = items.isEmpty() ? null : items.get(0);
        } else {
            selected = table.getSelectionModel().getSelectedItem();
        }
        if (selected == null) {
            new Alert(Alert.AlertType.INFORMATION, "No staff selected.").showAndWait();
            return;
        }
        viewEditStaff(selected);
    }
    
    
    
    
    private void addStaff() {
    Dialog<Document> dlg = new Dialog<>();
    dlg.setTitle("Add New Staff");
    dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    GridPane g = new GridPane();
    g.setHgap(8);
    g.setVgap(8);
    g.setPadding(new Insets(10));

    TextField staffId = new TextField();
    staffId.setPromptText("e.g., S1001");

    TextField name = new TextField();
    name.setPromptText("Full Name (used for login)");

    TextField email = new TextField();
    TextField office = new TextField();

    PasswordField password = new PasswordField();
    password.setPromptText("Initial password (default 1234)");

    ComboBox<String> roleBox = new ComboBox<>();
    roleBox.getItems().addAll("Professor", "TA");
    roleBox.setValue("Professor");

    g.add(new Label("Staff ID:"), 0, 0); g.add(staffId, 1, 0);
    g.add(new Label("Full Name (Login Username):"), 0, 1); g.add(name, 1, 1);
    g.add(new Label("Email:"), 0, 2); g.add(email, 1, 2);
    g.add(new Label("Office Hours:"), 0, 3); g.add(office, 1, 3);
    g.add(new Label("Initial Password:"), 0, 4); g.add(password, 1, 4);
    g.add(new Label("Role:"), 0, 5); g.add(roleBox, 1, 5);

    dlg.getDialogPane().setContent(g);

    dlg.setResultConverter(btn -> {
        if (btn == ButtonType.OK) {
            String role = roleBox.getValue();
            String sid = staffId.getText().trim();
            String fullName = name.getText().trim();
            String pass = password.getText().isBlank() ? "1234" : password.getText().trim();

            if (sid.isEmpty() || fullName.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Staff ID and Full Name are required!").showAndWait();
                return null;
            }

            return new Document("staffId", sid)
                    .append("name", fullName)
                    .append("email", email.getText().trim())
                    .append("officeHours", office.getText().trim())
                    .append("createdAt", new java.util.Date())
                    .append("role", role)
                    .append("username", fullName)
                    .append("password", pass);
        }
        return null;
    });

    dlg.showAndWait().ifPresent(doc -> {
        try {
            String staffIdVal = doc.getString("staffId");

          
            Document existing = staffService.getByStaffId(staffIdVal);
            if (existing != null) {
                new Alert(Alert.AlertType.WARNING,
                        "⚠️ A staff record with this ID already exists!").showAndWait();
                return;
            }

            UserService userService = new UserService();
            if (userService.deleteUserByLinkedEntityId(staffIdVal)) {
                new Alert(Alert.AlertType.WARNING,
                        "⚠️ A user account linked to this staff ID already exists!").showAndWait();
                return;
            }

          
            staffService.createStaff(doc);

        
            boolean userCreated = userService.createUser(
                    doc.getString("username"),
                    doc.getString("password"),
                    List.of(doc.getString("role")),
                    doc.getString("staffId")
            );

            if (userCreated) {
                new Alert(Alert.AlertType.INFORMATION,
                        "✅ Staff and linked user account created successfully!").showAndWait();
            } else {
                // rollback staff record to prevent orphan entry
                staffService.deleteByStaffId(staffIdVal);
                new Alert(Alert.AlertType.WARNING,
                        "⚠️ User account already exists — staff creation rolled back.").showAndWait();
            }

            load();

        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "❌ Error: " + ex.getMessage()).showAndWait();
            ex.printStackTrace();
        }
    });
}


    /** View/edit existing staff profile and sync with user account */
    private void viewEditStaff(Document doc) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Edit Staff Profile");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(8);
        g.setPadding(new Insets(10));

        TextField name = new TextField(doc.getString("name"));
        TextField email = new TextField(doc.getString("email"));
        TextField office = new TextField(doc.getString("officeHours"));
        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("Professor", "TA");
        roleBox.setValue(doc.getString("role") != null ? doc.getString("role") : "Professor");

        g.add(new Label("Full Name (Login Username):"), 0, 0); g.add(name, 1, 0);
        g.add(new Label("Email:"), 0, 1); g.add(email, 1, 1);
        g.add(new Label("Office Hours:"), 0, 2); g.add(office, 1, 2);
        g.add(new Label("Role:"), 0, 3); g.add(roleBox, 1, 3);

        dlg.getDialogPane().setContent(g);

        dlg.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                String newName = name.getText().trim();
                String newEmail = email.getText().trim();
                String newOffice = office.getText().trim();
                String newRole = roleBox.getValue();
                String staffId = doc.getString("staffId");

                if (newName.isEmpty()) {
                    new Alert(Alert.AlertType.WARNING, "Full name cannot be empty!").showAndWait();
                    return;
                }

                try {
                  
                    Document updated = new Document("name", newName)
                            .append("email", newEmail)
                            .append("officeHours", newOffice)
                            .append("role", newRole)
                            .append("updatedAt", new java.util.Date());
                    staffService.update(doc.getObjectId("_id").toHexString(), updated);

                    // 2️⃣ Sync user info (username + role)
                    UserService userService = new UserService();
                    boolean userUpdated = userService.updateUserByLinkedEntityId(
                            staffId, newName, List.of(newRole)
                    );

                    // 3️⃣ Success message
                    String msg = "✅ Staff record updated successfully.";
                    if (userUpdated)
                        msg += "\n✅ Linked user account updated (username & role).";
                    else
                        msg += "\n⚠️ No linked user account found for this staff.";

                    new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();

                    // 4️⃣ Refresh table
                    load();

                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "❌ Error updating staff: " + ex.getMessage()).showAndWait();
                    ex.printStackTrace();
                }
            }
        });
    }


    public VBox getView() {
        return view;
    }
}
