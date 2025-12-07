package edu.agile.sis.ui;

import edu.agile.sis.dao.UserDAO;
import edu.agile.sis.service.EntityService;
import edu.agile.sis.service.UserService;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.bson.Document;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;


public class ParentRegistrationPane extends StackPane {

    private final EntityService entityService = new EntityService("parents");
    private final UserService userService = new UserService();
    private final UserDAO userDAO = new UserDAO();

    private final TextField fullNameField = new TextField();
    private final Label fullNameError = new Label();

    private final TextField emailField = new TextField();

    private final TextField entityIdField = new TextField();
    private final Button genEntityBtn = new Button("Generate");
    private final Button copyEntityBtn = new Button("Copy");
    private final Button loadBtn = new Button("Load");
    private final Button deleteByIdBtn = new Button("Delete by ID"); // quick delete

    private final TextField usernameField = new TextField();
    private final Label usernameError = new Label();

    private final PasswordField passwordField = new PasswordField();
    private final ProgressBar passwordStrengthBar = new ProgressBar(0);
    private final Label passwordStrengthLabel = new Label();

    private final TextField studentsField = new TextField();
    private final Button previewStudentsBtn = new Button("Preview");
    private final FlowPane studentsChips = new FlowPane();

    private final Button createBtn = new Button("Create");
    private final Button updateBtn = new Button("Update");
    private final Button deleteBtn = new Button("Delete");
    private final Button clearBtn = new Button("Clear");

    private final ProgressIndicator progressIndicator = new ProgressIndicator();

 
    private final BooleanProperty busy = new SimpleBooleanProperty(false);

 
    private final BooleanProperty editMode = new SimpleBooleanProperty(false);


    private String loadedEntityId = null;

    public ParentRegistrationPane() {
        setPadding(new Insets(28));
        setStyle("-fx-background-color: linear-gradient(to bottom, #f7f9fb, #eef2f5);");


        VBox container = new VBox();
        container.setAlignment(Pos.TOP_CENTER);
        container.setSpacing(12);

 
        VBox card = new VBox(14);
        card.setPadding(new Insets(20));
        card.setMaxWidth(760);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(two-pass-box, rgba(0,0,0,0.08), 8, 0, 0, 2);");

      
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(12);
        Label title = new Label("Create / Manage Parent");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#2b3a42"));
        Label subtitle = new Label("Create new parents, load and edit existing ones, or remove them.");
        subtitle.setFont(Font.font("Segoe UI", 12));
        subtitle.setTextFill(Color.web("#6b7a80"));
        header.getChildren().addAll(title, new Region(), subtitle);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);

     
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        ColumnConstraints leftCol = new ColumnConstraints(150);
        ColumnConstraints rightCol = new ColumnConstraints();
        rightCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(leftCol, rightCol);


        fullNameField.setPromptText("Full name (required)");
        fullNameError.setStyle("-fx-text-fill: #d9534f;");
        fullNameError.setVisible(false);
        grid.add(new Label("Full name:"), 0, 0);
        grid.add(fullNameField, 1, 0);
        grid.add(fullNameError, 1, 1);

     
        emailField.setPromptText("Email (optional)");
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);

    
        entityIdField.setPromptText("Entity ID (optional)");
        genEntityBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #d0d7dd;");
        copyEntityBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #d0d7dd;");
        loadBtn.setStyle("-fx-background-color: #2d9cdb; -fx-text-fill: white;");
        deleteByIdBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        genEntityBtn.setOnAction(e -> entityIdField.setText(generateEntityId(fullNameField.getText().trim())));
        copyEntityBtn.setOnAction(e -> copyToClipboard(entityIdField.getText().trim()));
        loadBtn.setOnAction(e -> onLoadEntity());
        deleteByIdBtn.setOnAction(e -> onDeleteByIdConfirm());
        HBox eidBox = new HBox(8, entityIdField, loadBtn, genEntityBtn, copyEntityBtn, deleteByIdBtn);
        eidBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(new Label("Entity ID:"), 0, 3);
        grid.add(eidBox, 1, 3);

     
        usernameField.setPromptText("Username (required)");
        usernameError.setStyle("-fx-text-fill: #d9534f;");
        usernameError.setVisible(false);
        grid.add(new Label("Username:"), 0, 4);
        grid.add(usernameField, 1, 4);
        grid.add(usernameError, 1, 5);

        
        passwordField.setPromptText("Password (min 6 chars) — leave empty to keep existing (edit)");
        passwordStrengthBar.setPrefWidth(160);
        passwordStrengthLabel.setStyle("-fx-font-size: 11;");
        HBox pwBox = new HBox(8, passwordField, passwordStrengthBar, passwordStrengthLabel);
        pwBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(new Label("Password:"), 0, 6);
        grid.add(pwBox, 1, 6);

     
        studentsField.setPromptText("Student IDs (comma-separated)");
        previewStudentsBtn.setOnAction(e -> renderStudentChips());
        HBox studsBox = new HBox(8, studentsField, previewStudentsBtn);
        studsBox.setAlignment(Pos.CENTER_LEFT);
        studentsChips.setHgap(6);
        studentsChips.setVgap(6);
        studentsChips.setPadding(new Insets(6));
        grid.add(new Label("Student IDs:"), 0, 7);
        grid.add(studsBox, 1, 7);
        grid.add(studentsChips, 1, 8);

        
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        createBtn.setStyle("-fx-background-color: #2d9cdb; -fx-text-fill: white;");
        updateBtn.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white;");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        clearBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #cbd5db;");
        updateBtn.setDisable(true);
        deleteBtn.setDisable(true);
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(18, 18);
        actions.getChildren().addAll(progressIndicator, createBtn, updateBtn, deleteBtn, clearBtn);

        Label help = new Label("Tip: Load by Entity ID to edit. You may also delete quickly using 'Delete by ID'.");
        help.setStyle("-fx-font-size: 11; -fx-text-fill: #6b7a80;");

        card.getChildren().addAll(header, grid, actions, help);
        container.getChildren().add(card);
        getChildren().add(container);

        BooleanBinding createBase = new BooleanBinding() {
            { bind(fullNameField.textProperty(), usernameField.textProperty(), passwordField.textProperty(), editMode); }
            @Override
            protected boolean computeValue() {
                
                return editMode.get() // do not allow create while editing
                        || fullNameField.getText().trim().isEmpty()
                        || usernameField.getText().trim().isEmpty()
                        || passwordField.getText().length() < 6;
            }
        };

        BooleanBinding updateBase = new BooleanBinding() {
            { bind(fullNameField.textProperty(), usernameField.textProperty(), editMode); }
            @Override
            protected boolean computeValue() {
                
                return !editMode.get() || fullNameField.getText().trim().isEmpty() || usernameField.getText().trim().isEmpty();
            }
        };

        BooleanBinding deleteBase = new BooleanBinding() {
            { bind(entityIdField.textProperty(), editMode); }
            @Override
            protected boolean computeValue() {
                return !editMode.get() || entityIdField.getText().trim().isEmpty();
            }
        };

      
        createBtn.disableProperty().bind(createBase.or(busy));
        updateBtn.disableProperty().bind(updateBase.or(busy));
        deleteBtn.disableProperty().bind(deleteBase.or(busy));

        passwordField.textProperty().addListener((obs, ov, nv) -> {
            int score = passwordScore(nv);
            passwordStrengthBar.setProgress(Math.min(1.0, score / 5.0));
            passwordStrengthLabel.setText(describeStrength(score));
        });

        fullNameField.focusedProperty().addListener((obs, was, isNow) -> { if (!isNow) validateFullName(); });
        usernameField.focusedProperty().addListener((obs, was, isNow) -> { if (!isNow) validateUsername(); });

        createBtn.setOnAction(e -> onCreateAsync());
        updateBtn.setOnAction(e -> onUpdateAsync());
        deleteBtn.setOnAction(e -> onDeleteConfirm());
        clearBtn.setOnAction(e -> clearForm());
    }


    private void onLoadEntity() {
        String eid = entityIdField.getText().trim();
        if (eid.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Load", "Enter an Entity ID to load."); return; }

        setFormDisabled(true);
        if (getScene() != null) getScene().setCursor(Cursor.WAIT);

        Task<Document> task = new Task<Document>() {
            @Override
            protected Document call() {
                try {
                    return entityService.getEntityById(eid);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return null;
                }
            }
        };

        task.setOnSucceeded(ev -> {
            busy.set(false);
            if (getScene() != null) getScene().setCursor(Cursor.DEFAULT);
            setFormDisabled(false);
            Document doc = task.getValue();
            if (doc == null) { showAlert(Alert.AlertType.ERROR, "Not found", "No parent entity found with ID: " + eid); return; }

            Document core = doc.get("core", Document.class);
            String full = core != null ? core.getString("fullName") : null;
            String email = core != null ? core.getString("email") : null;
            loadedEntityId = entityService.getEntityId(doc);

            fullNameField.setText(full == null ? "" : full);
            emailField.setText(email == null ? "" : email);
            entityIdField.setText(loadedEntityId == null ? "" : loadedEntityId);

            try {
                Document u = userService.getUserByEntityId(loadedEntityId);
                usernameField.setText(u == null ? "" : (u.getString("username") == null ? "" : u.getString("username")));
            } catch (Exception ex) {
                usernameField.setText("");
            }

            try {
                List<String> ids = userDAO.getStudentIdsForParent(loadedEntityId);
                studentsField.setText(String.join(", ", ids));
                renderStudentChips();
            } catch (Exception ex) {
                studentsField.setText("");
            }

            setEditMode(true);
            showAlert(Alert.AlertType.INFORMATION, "Loaded", "Parent loaded: " + (full == null ? loadedEntityId : full));
        });

        task.setOnFailed(ev -> {
            busy.set(false);
            if (getScene() != null) getScene().setCursor(Cursor.DEFAULT);
            setFormDisabled(false);
            task.getException().printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load: " + task.getException().getMessage());
        });

        busy.set(true);
        progressIndicator.setVisible(true);
        new Thread(task).start();
    }


    private void onCreateAsync() {
        final String fullName = fullNameField.getText().trim();
        final String email = emailField.getText().trim();
        final String providedEntityId = entityIdField.getText().trim();
        final String username = usernameField.getText().trim();
        final String password = passwordField.getText();
        final String studentsRaw = studentsField.getText().trim();

        if (fullName.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Validation", "Full name required"); return; }
        if (username.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Validation", "Username required"); return; }
        if (password == null || password.length() < 6) { showAlert(Alert.AlertType.WARNING, "Validation", "Password must be at least 6 chars"); return; }

        setFormDisabled(true);
        if (getScene() != null) getScene().setCursor(Cursor.WAIT);

        Task<RegistrationResult> task = new Task<RegistrationResult>() {
            @Override
            protected RegistrationResult call() {
                String entityIdToUse = (providedEntityId != null && !providedEntityId.isBlank()) ? providedEntityId.trim() : generateEntityId(fullName);

                try {
                    if (entityService.getEntityById(entityIdToUse) != null) {
                        return new RegistrationResult(false, "A parent entity with this ID already exists.", null, 0);
                    }
                } catch (Exception ignored) {}

                Document core = new Document("entityId", entityIdToUse)
                        .append("fullName", fullName)
                        .append("email", email == null ? "" : email)
                        .append("createdAt", new Date());

                Document parentEntity = new Document("type", "parent")
                        .append("core", core)
                        .append("attributes", Collections.emptyList())
                        .append("deleted", false);

                try {
                    boolean ok = entityService.createEntity(parentEntity);
                    if (!ok) throw new RuntimeException("createEntity returned false");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return new RegistrationResult(false, "Failed to create parent entity: " + ex.getMessage(), null, 0);
                }

                try {
                    List<String> roles = List.of("parent");
                    boolean created = userService.createUser(username, password, roles, entityIdToUse);
                    if (!created) {
                        try { entityService.deleteEntity(entityIdToUse); } catch (Exception re) { re.printStackTrace(); }
                        return new RegistrationResult(false, "Username exists or user couldn't be created. Rolled back entity.", null, 0);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    try { entityService.deleteEntity(entityIdToUse); } catch (Exception re) { re.printStackTrace(); }
                    return new RegistrationResult(false, "Failed to create user: " + ex.getMessage() + ". Rolled back entity.", null, 0);
                }

                int linked = 0;
                if (!studentsRaw.isEmpty()) {
                    String[] ids = studentsRaw.split(",");
                    for (String s : ids) {
                        String sid = s.trim();
                        if (sid.isEmpty()) continue;
                        try {
                            boolean ok = userDAO.setParentForStudent(sid, entityIdToUse);
                            if (ok) linked++;
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }
                }
                return new RegistrationResult(true, "Parent created", entityIdToUse, linked);
            }
        };

        task.setOnSucceeded(ev -> {
            busy.set(false);
            if (getScene() != null) getScene().setCursor(Cursor.DEFAULT);
            setFormDisabled(false);
            RegistrationResult r = task.getValue();
            if (!r.success) showAlert(Alert.AlertType.ERROR, "Create failed", r.message);
            else {
                String msg = "Parent created.\n\nEntity ID: " + r.entityId;
                if (r.linkedCount > 0) msg += "\nLinked students: " + r.linkedCount;
                showCopyableConfirmation("Success", msg);
                clearForm();
            }
        });

        task.setOnFailed(ev -> {
            busy.set(false);
            if (getScene() != null) getScene().setCursor(Cursor.DEFAULT);
            setFormDisabled(false);
            task.getException().printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unexpected: " + task.getException().getMessage());
        });

        busy.set(true);
        progressIndicator.setVisible(true);
        new Thread(task).start();
    }


    private void onUpdateAsync() {
        if (loadedEntityId == null || loadedEntityId.isBlank()) { showAlert(Alert.AlertType.ERROR, "Update", "No parent loaded"); return; }

        final String fullName = fullNameField.getText().trim();
        final String email = emailField.getText().trim();
        final String username = usernameField.getText().trim();
        final String password = passwordField.getText();
        final String studentsRaw = studentsField.getText().trim();

        if (fullName.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Validation", "Full name required"); return; }
        if (username.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Validation", "Username required"); return; }

        setFormDisabled(true);
        if (getScene() != null) getScene().setCursor(Cursor.WAIT);

        Task<Boolean> task = new Task<Boolean>() {
            String message = "Updated";

            @Override
            protected Boolean call() {
                try {
                    Map<String, Object> coreUpdates = new HashMap<>();
                    coreUpdates.put("fullName", fullName);
                    coreUpdates.put("email", email == null ? "" : email);
                    entityService.updateEntityMerge(loadedEntityId, coreUpdates, null);

                    boolean uok = userService.updateUsernameForEntity(loadedEntityId, username);
                    if (!uok) throw new RuntimeException("Failed to update username (possible duplicate)");

                    if (password != null && !password.isBlank()) {
                        if (password.length() < 6) throw new RuntimeException("Password must be at least 6 chars");
                        boolean pok = userService.updatePasswordForEntity(loadedEntityId, password);
                        if (!pok) throw new RuntimeException("Failed to update password");
                    }

                    userDAO.unlinkParentFromStudents(loadedEntityId);
                    if (!studentsRaw.isEmpty()) {
                        String[] arr = studentsRaw.split(",");
                        for (String s : arr) {
                            String sid = s.trim();
                            if (sid.isEmpty()) continue;
                            userDAO.setParentForStudent(sid, loadedEntityId);
                        }
                    }
                    return true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    message = "Update failed: " + ex.getMessage();
                    return false;
                }
            }
        };

        task.setOnSucceeded(ev -> {
            busy.set(false);
            if (getScene() != null) getScene().setCursor(Cursor.DEFAULT);
            setFormDisabled(false);
            if (task.getValue()) showCopyableConfirmation("Updated", "Parent updated successfully.\n\nEntity ID: " + loadedEntityId);
            else showAlert(Alert.AlertType.ERROR, "Update failed", "See console for details.");
        });

        task.setOnFailed(ev -> {
            busy.set(false);
            if (getScene() != null) getScene().setCursor(Cursor.DEFAULT);
            setFormDisabled(false);
            task.getException().printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unexpected: " + task.getException().getMessage());
        });

        busy.set(true);
        progressIndicator.setVisible(true);
        new Thread(task).start();
    }


    private void onDeleteConfirm() {
        if (loadedEntityId == null || loadedEntityId.isBlank()) { showAlert(Alert.AlertType.ERROR, "Delete", "No parent loaded"); return; }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Delete Parent");
        a.setHeaderText("Delete parent " + (fullNameField.getText().isBlank() ? loadedEntityId : fullNameField.getText()) + "?");
        a.setContentText("This will remove the parent entity and the linked user. Students will be unlinked. This action cannot be undone.");
        ButtonType del = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        a.getButtonTypes().setAll(del, cancel);
        Optional<ButtonType> res = a.showAndWait();
        if (res.isPresent() && res.get() == del) performDeleteAsync(loadedEntityId);
    }

    private void performDeleteAsync(String entityId) {
        setFormDisabled(true);
        if (getScene() != null) getScene().setCursor(Cursor.WAIT);

        Task<Boolean> task = new Task<Boolean>() {
            @Override
            protected Boolean call() {
                try {
                    try { userService.deleteUserByEntityId(entityId); } catch (Exception ex) { ex.printStackTrace(); }
                    userDAO.unlinkParentFromStudents(entityId);
                    entityService.deleteEntity(entityId);
                    return true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return false;
                }
            }
        };

        task.setOnSucceeded(ev -> {
            busy.set(false);
            if (getScene() != null) getScene().setCursor(Cursor.DEFAULT);
            setFormDisabled(false);
            if (task.getValue()) {
                showAlert(Alert.AlertType.INFORMATION, "Deleted", "Parent deleted: " + entityId);
                clearForm();
            } else {
                showAlert(Alert.AlertType.ERROR, "Delete failed", "Failed to delete: " + entityId);
            }
        });

        task.setOnFailed(ev -> {
            busy.set(false);
            if (getScene() != null) getScene().setCursor(Cursor.DEFAULT);
            setFormDisabled(false);
            task.getException().printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unexpected: " + task.getException().getMessage());
        });

        busy.set(true);
        progressIndicator.setVisible(true);
        new Thread(task).start();
    }


    private void onDeleteByIdConfirm() {
        String eid = entityIdField.getText().trim();
        if (eid.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Delete by ID", "Enter Entity ID to delete."); return; }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Delete Parent by ID");
        a.setHeaderText("Delete parent with ID: " + eid + "?");
        a.setContentText("This will remove the parent entity and the linked user. Students will be unlinked.");
        ButtonType del = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        a.getButtonTypes().setAll(del, cancel);
        Optional<ButtonType> res = a.showAndWait();
        if (res.isPresent() && res.get() == del) performDeleteAsync(eid);
    }

    private void renderStudentChips() {
        studentsChips.getChildren().clear();
        String raw = studentsField.getText().trim();
        if (raw.isEmpty()) return;
        String[] ids = raw.split(",");
        for (String s : ids) {
            String sid = s.trim();
            if (sid.isEmpty()) continue;
            Label chip = new Label(sid);
            chip.setStyle("-fx-background-color: #eef6ff; -fx-padding: 6 10 6 10; -fx-border-radius: 16; -fx-background-radius: 16;");
            Button remove = new Button("x");
            remove.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c;");
            HBox box = new HBox(6, chip, remove);
            box.setAlignment(Pos.CENTER_LEFT);
            remove.setOnAction(ev -> {
                studentsChips.getChildren().remove(box);
                List<String> remaining = studentsChips.getChildren().stream()
                        .filter(n -> n instanceof HBox)
                        .map(n -> ((Label)((HBox)n).getChildren().get(0)).getText())
                        .collect(Collectors.toList());
                studentsField.setText(String.join(", ", remaining));
            });
            studentsChips.getChildren().add(box);
        }
    }

    private void validateFullName() {
        String txt = fullNameField.getText().trim();
        if (txt.isEmpty()) { fullNameError.setText("Full name required"); fullNameError.setVisible(true); }
        else fullNameError.setVisible(false);
    }

    private void validateUsername() {
        String un = usernameField.getText().trim();
        if (un.isEmpty()) { usernameError.setText("Username required"); usernameError.setVisible(true); return; }
        if (un.length() < 3) { usernameError.setText("Choose a longer username (≥3)"); usernameError.setVisible(true); }
        else usernameError.setVisible(false);
    }

    private void setEditMode(boolean enable) {
      
        editMode.set(enable);
        
    }

    private void setFormDisabled(boolean disabled) {
      
        busy.set(disabled);

       
        fullNameField.setDisable(disabled);
        emailField.setDisable(disabled);
        entityIdField.setDisable(disabled);
        genEntityBtn.setDisable(disabled);
        copyEntityBtn.setDisable(disabled);
        loadBtn.setDisable(disabled);
        deleteByIdBtn.setDisable(disabled);

        usernameField.setDisable(disabled);
        passwordField.setDisable(disabled);
        studentsField.setDisable(disabled);
        previewStudentsBtn.setDisable(disabled);

        clearBtn.setDisable(disabled);

        progressIndicator.setVisible(disabled);
    }

    private void clearForm() {
        fullNameField.clear();
        emailField.clear();
        entityIdField.clear();
        usernameField.clear();
        passwordField.clear();
        studentsField.clear();
        studentsChips.getChildren().clear();
        passwordStrengthBar.setProgress(0);
        passwordStrengthLabel.setText("");
        fullNameError.setVisible(false);
        usernameError.setVisible(false);
        setEditMode(false);
        loadedEntityId = null;
    }

    private String generateEntityId(String fullName) {
        String base = (fullName == null || fullName.isBlank()) ? "parent" : fullName.toLowerCase().replaceAll("[^a-z0-9]", "-");
        if (base.length() > 30) base = base.substring(0, 30);
        String rnd = Long.toHexString(Math.abs(new SecureRandom().nextLong()));
        if (rnd.length() > 6) rnd = rnd.substring(0, 6);
        return base + "-" + rnd;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert a = new Alert(type, message, ButtonType.OK);
            a.setTitle(title);
            a.setHeaderText(null);
            a.showAndWait();
        });
    }

    private void showCopyableConfirmation(String title, String message) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle(title);
            a.setHeaderText(null);
            TextArea area = new TextArea(message);
            area.setEditable(false);
            area.setWrapText(true);
            area.setPrefRowCount(6);
            a.getDialogPane().setContent(area);
            a.getDialogPane().setExpandableContent(null);
            a.setResizable(true);
            ButtonType copyBtn = new ButtonType("Copy Linked ID");
            a.getDialogPane().getButtonTypes().setAll(copyBtn, ButtonType.OK);
            var result = a.showAndWait();
            if (result.isPresent() && result.get() == copyBtn) {
                String id = null;
                for (String line : message.split("\\r?\\n")) {
                    line = line.trim();
                    if (line.toLowerCase().startsWith("entity id:") || line.toLowerCase().startsWith("linked entity id:")) {
                        id = line.substring(line.indexOf(':') + 1).trim();
                        break;
                    }
                }
                final String toCopy = (id != null && !id.isEmpty()) ? id : message;
                copyToClipboard(toCopy);
            }
        });
    }

    private void copyToClipboard(String text) {
        if (text == null || text.isBlank()) return;
        Clipboard cb = Clipboard.getSystemClipboard();
        ClipboardContent cc = new ClipboardContent();
        cc.putString(text);
        cb.setContent(cc);
    }

    private int passwordScore(String pw) {
        if (pw == null || pw.isEmpty()) return 0;
        int score = 0;
        if (pw.length() >= 6) score++;
        if (pw.length() >= 10) score++;
        boolean hasLower = pw.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = pw.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = pw.chars().anyMatch(Character::isDigit);
        boolean hasSymbol = pw.chars().anyMatch(ch -> "!@#$%^&*()-_=+[]{};:'\",.<>/?\\|`~".indexOf(ch) >= 0);
        int types = 0;
        types += hasLower ? 1 : 0;
        types += hasUpper ? 1 : 0;
        types += hasDigit ? 1 : 0;
        types += hasSymbol ? 1 : 0;
        score += Math.min(2, types);
        return Math.min(5, score);
    }

    private String describeStrength(int score) {
        switch (score) {
            case 0: return "Very weak";
            case 1: return "Weak";
            case 2: return "OK";
            case 3: return "Good";
            case 4: return "Strong";
            case 5: return "Excellent";
            default: return "";
        }
    }

    private static class RegistrationResult {
        final boolean success;
        final String message;
        final String entityId;
        final int linkedCount;
        RegistrationResult(boolean success, String message, String entityId, int linkedCount) {
            this.success = success;
            this.message = message;
            this.entityId = entityId;
            this.linkedCount = linkedCount;
        }
    }
}
