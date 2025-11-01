package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.EntityService;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javafx.scene.Node;

/**
 * StudentsController
 */
public class StudentsController {
    private final VBox view = new VBox();
    private final EntityService entityService = new EntityService("students");
    private final TableView<Document> table = new TableView<>();
    private final ObservableList<Document> data = FXCollections.observableArrayList();
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd");
    private final UserService userService = new UserService();

    private final boolean selfViewRequested;
    private final boolean isLoggedStudent;
    private final String linkedEntityId;
    
    public StudentsController() {
    // Determine roles from AuthSession
    this.isLoggedStudent = AuthSession.getInstance().hasRole("Student");
    this.selfViewRequested = isLoggedStudent;  // student mode by default
    this.linkedEntityId = AuthSession.getInstance().getLinkedEntityId();

    if (isLoggedStudent) {
    Document studentDoc = null;
    if (linkedEntityId != null && !linkedEntityId.isBlank()) {
        studentDoc = new EntityService("students").getEntityById(linkedEntityId);
    }

    if (studentDoc != null) {
        // Create the profile dialog as usual
        StudentDetailDialog profileDialog = new StudentDetailDialog(studentDoc, new EntityService("students"), true);

        // 🩵 Get the dialog content node (HBox or VBox)
        Node profileContent = profileDialog.getDialogPane().getContent();

        // 🩶 Wrap it in a VBox to center and pad nicely
        VBox container = new VBox(15);
        container.setPadding(new Insets(30));
        container.getChildren().add(profileContent);
        container.setStyle("""
            -fx-background-color: #f5f7fa;
            -fx-alignment: center;
        """);

       
        Label title = new Label("My Student Profile");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2a2a2a;");
        container.getChildren().add(0, title);

   
        view.getChildren().setAll(container);

    } else {
        Label msg = new Label("No student record found for your account.");
        msg.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        view.getChildren().setAll(msg);
    }

} else {
    buildView(); // Admin, TA, Professor
    load();
}

}


  

    public StudentsController(boolean selfView) {
        this.selfViewRequested = selfView;
        this.isLoggedStudent = AuthSession.getInstance().hasRole("Student");
        this.linkedEntityId = AuthSession.getInstance().getLinkedEntityId();

        buildView();
        load();
    }

    private void buildView() {
        view.setPadding(new Insets(15));
        view.setSpacing(15);
        view.setStyle("-fx-background-color: #f7f9fc;");

        Label title = new Label(selfViewRequested ? "My Student Profile" : "Students Management");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#2b2b2b"));
        title.setPadding(new Insets(5, 0, 10, 0));

        // Table styling
        table.setPlaceholder(new Label("No student records found."));
        table.setItems(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("""
            -fx-background-color: white;
            -fx-border-color: #d9d9d9;
            -fx-border-radius: 8;
            -fx-background-radius: 8;
            -fx-selection-bar: #1976d2;
            -fx-selection-bar-text: white;
        """);

   
        TableColumn<Document, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(resolveEntityId(c.getValue())));
        idCol.setMinWidth(120);

        TableColumn<Document, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(resolveName(c.getValue())));
        nameCol.setMinWidth(180);

        TableColumn<Document, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safeGetCoreString(c.getValue(), "email", "")));
        emailCol.setMinWidth(200);

        TableColumn<Document, String> enrolledCol = new TableColumn<>("Enrolled Since");
        enrolledCol.setCellValueFactory(c -> {
            Date d = safeGetCoreDate(c.getValue(), "enrolledSince");
            return new javafx.beans.property.SimpleStringProperty(d == null ? "" : dateFmt.format(d));
        });
        enrolledCol.setMinWidth(140);

        table.getColumns().setAll(idCol, nameCol, emailCol, enrolledCol);

     
        Button addBtn = new Button("➕ Add Student");
        Button viewBtn = new Button(selfViewRequested ? "👁 View My Profile" : "✏️ View / Edit");
        Button deleteBtn = new Button("🗑 Delete Selected");
        Button refreshBtn = new Button("🔄 Refresh");

        boolean isAdmin = AuthSession.getInstance().hasRole("Admin");
        boolean isProf = AuthSession.getInstance().hasRole("Professor");
        boolean isTA = AuthSession.getInstance().hasRole("TA");
        boolean canManage = isAdmin || isProf || isTA;

        addBtn.setDisable(selfViewRequested || !canManage);
        deleteBtn.setDisable(selfViewRequested || !canManage);

 
        stylePrimary(addBtn);
        styleNeutral(viewBtn);
        styleDanger(deleteBtn);
        styleSecondary(refreshBtn);

       
        addBtn.setOnAction(e -> addStudent());
        viewBtn.setOnAction(e -> onViewOrEdit(canManage));
        deleteBtn.setOnAction(e -> onDelete());
        refreshBtn.setOnAction(e -> load());

  
        HBox btnBar = new HBox(10, addBtn, viewBtn, deleteBtn, refreshBtn);
        btnBar.setAlignment(Pos.CENTER_LEFT);
        btnBar.setPadding(new Insets(8));
        btnBar.setStyle("""
            -fx-background-color: #ffffff;
            -fx-border-color: #d0d0d0;
            -fx-border-radius: 8;
            -fx-background-radius: 8;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4,0,0,1);
        """);

        // --- Outer card style ---
        VBox card = new VBox(10, table, btnBar);
        card.setPadding(new Insets(15));
        card.setStyle("""
            -fx-background-color: #ffffff;
            -fx-border-color: #dcdcdc;
            -fx-border-radius: 10;
            -fx-background-radius: 10;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 4,0,0,2);
        """);

        view.getChildren().setAll(title, card);
        VBox.setVgrow(card, Priority.ALWAYS);
    }
private void onViewOrEdit(boolean canManage) {
    Document sel;
    if (selfViewRequested || isLoggedStudent) {
        if (linkedEntityId == null || linkedEntityId.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Your account is not linked to a student record.").showAndWait();
            return;
        }
        sel = entityService.getEntityById(linkedEntityId);
    } else {
        sel = table.getSelectionModel().getSelectedItem();
    }

    if (sel == null) {
        new Alert(Alert.AlertType.INFORMATION, "No student selected.").showAndWait();
        return;
    }

    boolean readOnly = isLoggedStudent && !canManage;
    StudentDetailDialog dialog = new StudentDetailDialog(sel, entityService, readOnly);
    dialog.showAndWait();

    
    Document updatedDoc = entityService.getEntityById(resolveEntityId(sel));
    if (updatedDoc == null) {
        new Alert(Alert.AlertType.ERROR, "Error: Student record no longer exists.").showAndWait();
        load();
        return;
    }


    try {
        String studentId = resolveEntityId(updatedDoc);
        Document core = updatedDoc.get("core", Document.class);

        String firstName = core != null ? core.getString("firstName") : "";
        String lastName = core != null ? core.getString("lastName") : "";
        String fullName = (firstName + " " + lastName).trim();

     
        boolean userUpdated = userService.updateUserByLinkedEntityId(studentId, fullName, List.of("Student"));
        if (!userUpdated) {
            userService.createUser(fullName, "1234", List.of("Student"), studentId);
            new Alert(Alert.AlertType.INFORMATION, 
                "✅ Linked user account created automatically for student '" + fullName + "'.").showAndWait();
        }

    } catch (Exception ex) {
        new Alert(Alert.AlertType.ERROR, "⚠️ Error syncing user account: " + ex.getMessage()).showAndWait();
        ex.printStackTrace();
    }

   
    load();
}


    private void onDelete() {
    Document sel = table.getSelectionModel().getSelectedItem();
    if (sel == null) {
        new Alert(Alert.AlertType.WARNING, "Please select a student to delete.").showAndWait();
        return;
    }

    String id = resolveEntityId(sel);
    if (id == null || id.isEmpty()) {
        new Alert(Alert.AlertType.ERROR, "Selected record has no valid ID.").showAndWait();
        return;
    }

    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Are you sure you want to delete student " + id + " and their linked user account?",
            ButtonType.YES, ButtonType.NO);

    confirm.showAndWait().ifPresent(b -> {
        if (b == ButtonType.YES) {
            try {
              
                entityService.deleteEntity(id);

                
                boolean userDeleted = userService.deleteUserByLinkedEntityId(id);

              
                String msg = "✅ Student record deleted successfully.";
                if (userDeleted)
                    msg += "\n✅ Linked user account deleted successfully.";
                else
                    msg += "\n⚠️ No linked user account found for this student.";

                new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();

                // 4️⃣ Refresh the list
                load();

            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "❌ Error deleting student: " + ex.getMessage()).showAndWait();
                ex.printStackTrace();
            }
        }
    });
}


    private void load() {
        data.clear();
        if (selfViewRequested || isLoggedStudent) {
            if (linkedEntityId == null || linkedEntityId.isBlank()) return;
            Document d = entityService.getEntityById(linkedEntityId);
            if (d != null) data.add(d);
        } else {
            List<Document> list = entityService.getEntitiesByType("student");
            if (list != null) data.addAll(list);
        }
    }

    // --- Add Student Dialog (same logic, cleaned UI) ---
    private void addStudent() {
        Dialog<Document> dlg = new Dialog<>();
        dlg.setTitle("➕ Add New Student");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: #fefefe;");

        TextField idField = new TextField(); idField.setPromptText("Entity ID (e.g., 20P1234)");
        TextField first = new TextField(); first.setPromptText("First name");
        TextField last = new TextField(); last.setPromptText("Last name");
        TextField email = new TextField(); email.setPromptText("Email address");
        DatePicker enrolled = new DatePicker();
        TextField username = new TextField(); username.setPromptText("Username (defaults to ID)");
        PasswordField password = new PasswordField(); password.setPromptText("Password");

        idField.textProperty().addListener((obs, oldV, newV) -> {
            if ((username.getText() == null || username.getText().isBlank()) && newV != null && !newV.isBlank()) {
                username.setText(newV.trim());
            }
        });

        box.getChildren().addAll(
                new Label("Entity ID:"), idField,
                new Label("First Name:"), first,
                new Label("Last Name:"), last,
                new Label("Email:"), email,
                new Label("Enrolled Since:"), enrolled,
                new Separator(),
                new Label("User Account Username:"), username,
                new Label("User Account Password:"), password
        );

        dlg.getDialogPane().setContent(box);

        dlg.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String idv = idField.getText().trim();
                String uname = username.getText().trim();
                String pwd = password.getText();

                if (idv.isEmpty() || uname.isEmpty() || pwd.isEmpty()) {
                    new Alert(Alert.AlertType.ERROR, "ID, username, and password are required.").showAndWait();
                    return null;
                }

                Document core = new Document("entityId", idv)
                        .append("firstName", first.getText().trim())
                        .append("lastName", last.getText().trim())
                        .append("email", email.getText().trim())
                        .append("createdAt", new Date());

                if (enrolled.getValue() != null) {
                    core.append("enrolledSince", java.sql.Date.valueOf(enrolled.getValue()));
                }

                Document student = new Document("type", "student")
                        .append("core", core)
                        .append("attributes", List.of())
                        .append("deleted", false);

                student.append("_tmp_username", uname);
                student.append("_tmp_password", pwd);
                return student;
            }
            return null;
        });

        dlg.showAndWait().ifPresent(doc -> {
            String idv = doc.get("core", Document.class).getString("entityId");
            Document existing = entityService.getEntityById(idv);
            if (existing != null) {
                new Alert(Alert.AlertType.WARNING, "Entity ID already exists.").showAndWait();
                return;
            }

            entityService.createEntity(doc);

            String uname = doc.getString("_tmp_username");
            String pwd = doc.getString("_tmp_password");

            boolean userCreated;
            try {
                userCreated = userService.createUser(uname, pwd, List.of("Student"), idv);
            } catch (Exception ex) {
                userCreated = false;
            }

            if (!userCreated) {
                new Alert(Alert.AlertType.WARNING, "Student created, but user account not created (username may already exist).").showAndWait();
            } else {
                new Alert(Alert.AlertType.INFORMATION, "✅ Student and user account created successfully.").showAndWait();
            }

            load();
        });
    }

    // --- Styling Helpers ---
    private void stylePrimary(Button b) {
        b.setStyle("""
            -fx-background-color: #1976d2;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-background-radius: 6;
        """);
    }

    private void styleDanger(Button b) {
        b.setStyle("""
            -fx-background-color: #e53935;
            -fx-text-fill: white;
            -fx-background-radius: 6;
        """);
    }

    private void styleSecondary(Button b) {
        b.setStyle("""
            -fx-background-color: #eeeeee;
            -fx-text-fill: #333;
            -fx-background-radius: 6;
        """);
    }

    private void styleNeutral(Button b) {
        b.setStyle("""
            -fx-background-color: #ffffff;
            -fx-border-color: #1976d2;
            -fx-text-fill: #1976d2;
            -fx-background-radius: 6;
        """);
    }

    // --- Helper Methods ---
    private String resolveEntityId(Document doc) {
        if (doc == null) return "";
        Document core = doc.get("core", Document.class);
        if (core != null) {
            String id = core.getString("entityId");
            if (id != null) return id;
        }
        return doc.getString("entityId") == null ? "" : doc.getString("entityId");
    }

    private String resolveName(Document doc) {
        if (doc == null) return "";
        Document core = doc.get("core", Document.class);
        if (core != null) {
            String fn = core.getString("firstName");
            String ln = core.getString("lastName");
            if (fn == null) fn = "";
            if (ln == null) ln = "";
            return (fn + " " + ln).trim();
        }
        return "";
    }

    private String safeGetCoreString(Document doc, String key, String def) {
        if (doc == null) return def;
        Document core = doc.get("core", Document.class);
        if (core == null) return def;
        String s = core.getString(key);
        return s == null ? def : s;
    }

    private Date safeGetCoreDate(Document doc, String key) {
        if (doc == null) return null;
        Document core = doc.get("core", Document.class);
        if (core == null) return null;
        return core.getDate(key);
    }

    public VBox getView() {
        return view;
    }
}
