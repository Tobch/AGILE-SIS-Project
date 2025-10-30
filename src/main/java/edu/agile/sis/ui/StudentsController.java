package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.EntityService;
import edu.agile.sis.service.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * StudentsController
 *
 * Two usage modes:
 * - new StudentsController()        => full management (admin/staff)
 * - new StudentsController(true)    => self-view for the logged-in student (shows only linkedEntityId)
 *
 * Note: the controller still respects AuthSession role checks (AuthSession should be populated at login).
 */
public class StudentsController {
    private final VBox view = new VBox(10);
    private final EntityService entityService = new EntityService("students");
    private final TableView<Document> table = new TableView<>();
    private final ObservableList<Document> data = FXCollections.observableArrayList();
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd");
    private final UserService userService = new UserService();

    private final boolean selfViewRequested;
    private final boolean isLoggedStudent;
    private final String linkedEntityId;

    public StudentsController() {
        this(false);
    }

    public StudentsController(boolean selfView) {
        this.selfViewRequested = selfView;
        this.isLoggedStudent = AuthSession.getInstance().hasRole("Student");
        this.linkedEntityId = AuthSession.getInstance().getLinkedEntityId();

        buildView();
        load();
    }

    @SuppressWarnings("unchecked")
    private void buildView() {
        view.setPadding(new Insets(10));
        Label title = new Label(selfViewRequested ? "My Student Profile" : "Students Management");

        // Table columns
        TableColumn<Document, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(resolveEntityId(c.getValue())));
        idCol.setPrefWidth(150);

        TableColumn<Document, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(resolveName(c.getValue())));
        nameCol.setPrefWidth(220);

        TableColumn<Document, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safeGetCoreString(c.getValue(), "email", "")));
        emailCol.setPrefWidth(220);

        TableColumn<Document, String> enrolledCol = new TableColumn<>("Enrolled Since");
        enrolledCol.setCellValueFactory(c -> {
            Date d = safeGetCoreDate(c.getValue(), "enrolledSince");
            return new javafx.beans.property.SimpleStringProperty(d == null ? "" : dateFmt.format(d));
        });
        enrolledCol.setPrefWidth(120);

        table.getColumns().addAll(idCol, nameCol, emailCol, enrolledCol);
        table.setItems(data);

        // Buttons
        Button addBtn = new Button("Add Student");
        Button viewBtn = new Button(selfViewRequested ? "View My Profile" : "View / Edit");
        Button deleteBtn = new Button("Delete Selected");
        Button refreshBtn = new Button("Refresh");

        boolean isAdmin = AuthSession.getInstance().hasRole("Admin");
        boolean isProf = AuthSession.getInstance().hasRole("Professor");
        boolean isTA = AuthSession.getInstance().hasRole("TA");
        boolean canManage = isAdmin || isProf || isTA;

        addBtn.setDisable(selfViewRequested || !canManage);
        deleteBtn.setDisable(selfViewRequested || !canManage);

        addBtn.setOnAction(e -> addStudent());
        viewBtn.setOnAction(e -> {
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
                new Alert(Alert.AlertType.INFORMATION, "No student selected").showAndWait();
            } else {
                boolean readOnly = isLoggedStudent && !canManage;
                StudentDetailDialog dialog = new StudentDetailDialog(sel, entityService, readOnly);
                dialog.showAndWait();
                load();
            }
        });

        deleteBtn.setOnAction(e -> {
            Document sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            String id = resolveEntityId(sel);
            if (id == null || id.isEmpty()) return;
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete student " + id + "?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(b -> {
                if (b == ButtonType.YES) {
                    entityService.deleteEntity(id);
                    load();
                }
            });
        });

        refreshBtn.setOnAction(e -> load());

        HBox controls = new HBox(8, addBtn, viewBtn, deleteBtn, refreshBtn);
        controls.setPadding(new Insets(6));

        view.getChildren().addAll(title, table, controls);
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

    private void addStudent() {
        Dialog<Document> dlg = new Dialog<>();
        dlg.setTitle("Add Student (and create user account)");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox box = new VBox(8);
        box.setPadding(new Insets(8));
        TextField idField = new TextField(); idField.setPromptText("Entity ID (e.g. 20P1234)");
        TextField first = new TextField(); first.setPromptText("First name");
        TextField last = new TextField(); last.setPromptText("Last name");
        TextField email = new TextField(); email.setPromptText("Email");
        DatePicker enrolled = new DatePicker();

        TextField username = new TextField(); username.setPromptText("Username (defaults to entityId)");
        PasswordField password = new PasswordField(); password.setPromptText("Password (required)");

        // Auto-fill username only (NOT password)
        idField.textProperty().addListener((obs, oldV, newV) -> {
            if ((username.getText() == null || username.getText().isBlank()) && newV != null && !newV.isBlank()) {
                username.setText(newV.trim());
            }
        });

        box.getChildren().addAll(
                new Label("ID:"), idField,
                new Label("First name:"), first,
                new Label("Last name:"), last,
                new Label("Email:"), email,
                new Label("Enrolled since:"), enrolled,
                new Label("Account username:"), username,
                new Label("Account password:"), password
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
                new Alert(Alert.AlertType.WARNING, "Entity ID already exists").showAndWait();
                return;
            }

            entityService.createEntity(doc);

            String uname = doc.getString("_tmp_username");
            String pwd = doc.getString("_tmp_password");

            boolean userCreated = false;
            try {
                userCreated = userService.createUser(uname, pwd, List.of("Student"), idv);
            } catch (Exception ex) {
                userCreated = false;
            }

            if (!userCreated) {
                new Alert(Alert.AlertType.WARNING, "Student created, but user account not created (username may already exist). Please add account manually.").showAndWait();
            } else {
                new Alert(Alert.AlertType.INFORMATION,
                        "Student and user account created successfully.\nUsername: " + uname)
                        .showAndWait();
            }

            load();
        });
    }

    // helpers
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
            String name = (fn + " " + ln).trim();
            if (!name.isEmpty()) return name;
            return core.getString("name") != null ? core.getString("name") : "";
        }
        return doc.getString("name") == null ? "" : doc.getString("name");
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
