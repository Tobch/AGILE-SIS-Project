package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.CourseService;
import edu.agile.sis.service.EnrollmentService;
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
import java.util.Optional;
import java.util.stream.Collectors;

public class CoursesController {
    private final VBox view = new VBox(20);
    private final CourseService courseService = new CourseService();
    private final EnrollmentService enrollmentService = new EnrollmentService();

    private final boolean isAdmin = AuthSession.getInstance().hasRole("Admin");
    private final boolean isProf = AuthSession.getInstance().hasRole("Professor");
    private final boolean isTA = AuthSession.getInstance().hasRole("TA");
    private final boolean isStaff = isProf || isTA || AuthSession.getInstance().hasRole("Staff");
    private final boolean isStudent = AuthSession.getInstance().hasRole("Student");

    private final ObservableList<Document> courseItems = FXCollections.observableArrayList();
    private final ObservableList<Document> myCourses = FXCollections.observableArrayList();

    public CoursesController() {
        // Base view style
        view.setPadding(new Insets(25));
        view.setStyle("-fx-background-color: linear-gradient(to bottom right, #f8f9fa, #e9ecef);");
        view.setSpacing(20);

        // === Header ===
        Label title = new Label(isStudent ? "🎓 Course Catalog" : "📘 Course Management");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#343a40"));

        Label subtitle = new Label(isStudent
                ? "Register or view your enrolled courses below"
                : (isAdmin
                    ? "Manage all courses, staff assignments, and enrollments"
                    : "View and edit your assigned courses"));
        subtitle.setFont(Font.font("Segoe UI", 14));
        subtitle.setTextFill(Color.web("#6c757d"));

        VBox headerBox = new VBox(5, title, subtitle);

        // === Unified card container ===
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(10), Insets.EMPTY)));
        card.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 3);");

        if (isStudent) {
            // Student: Tabs
            TabPane tabs = new TabPane();
            tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

            TableView<Document> myTable = createCourseTable();
            myTable.setItems(myCourses);
            Button unregisterBtn = styledButton("❌ Unregister Selected", "#dc3545");
            unregisterBtn.setOnAction(e -> handleUnregister(myTable));
            VBox myBox = new VBox(10, myTable, unregisterBtn);
            myBox.setPadding(new Insets(8));

            TableView<Document> allTable = createCourseTable();
            allTable.setItems(courseItems);
            Button registerBtn = styledButton("✅ Register Selected", "#28a745");
            registerBtn.setOnAction(e -> handleRegister(allTable));
            VBox allBox = new VBox(10, allTable, registerBtn);
            allBox.setPadding(new Insets(8));

            Tab myTab = new Tab("📘 My Courses", myBox);
            Tab allTab = new Tab("📚 All Courses", allBox);

            tabs.getTabs().addAll(myTab, allTab);
            card.getChildren().add(tabs);
        } else {
            // Admin / Staff / Professor: Table + Controls
            TableView<Document> table = createCourseTable();
            table.setItems(courseItems);

            Button add = styledButton("➕ Add Course", "#28a745");
            Button edit = styledButton("✏️ Edit/View", "#007bff");
            Button del = styledButton("🗑 Delete", "#dc3545");
            Button assign = styledButton("👨‍🏫 Assign Staff", "#ffc107");
            Button refresh = styledButton("🔄 Refresh", "#17a2b8");

            add.setDisable(!isAdmin);
            del.setDisable(!isAdmin);
            edit.setDisable(!(isAdmin || isProf));
            assign.setDisable(!isAdmin);

            add.setOnAction(e -> addCourse());
            edit.setOnAction(e -> {
                Document sel = table.getSelectionModel().getSelectedItem();
                if (sel == null) { alert(Alert.AlertType.INFORMATION, "Select a course first."); return; }
                viewEditCourse(sel);
            });
            del.setOnAction(e -> handleDelete(table));
            assign.setOnAction(e -> handleAssign(table));
            refresh.setOnAction(e -> loadCourses());

            HBox controls = new HBox(10, add, edit, del, assign, refresh);
            controls.setAlignment(Pos.CENTER_LEFT);
            controls.setPadding(new Insets(5, 0, 0, 0));

            card.getChildren().addAll(table, controls);
        }

        view.getChildren().addAll(headerBox, card);

        loadCourses();
        if (isStudent) loadMyCourses();
    }

    // === Styling helpers ===
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

    private TableView<Document> createCourseTable() {
        TableView<Document> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8;");
        table.setPlaceholder(new Label("No courses available."));

        TableColumn<Document, String> code = new TableColumn<>("Code");
        code.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getString("code")));

        TableColumn<Document, String> title = new TableColumn<>("Title");
        title.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getString("title")));

        TableColumn<Document, String> credits = new TableColumn<>("Credits");
        credits.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().get("credits"))));

        TableColumn<Document, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getBoolean("core", false) ? "Core" : "Elective"));

        TableColumn<Document, String> assigned = new TableColumn<>("Assigned Staff");
        assigned.setCellValueFactory(c -> {
            Object a = c.getValue().get("assignedStaff");
            if (a instanceof List)
                return new javafx.beans.property.SimpleStringProperty(
                        String.join(", ", ((List<?>) a).stream().map(Object::toString).collect(Collectors.toList())));
            return new javafx.beans.property.SimpleStringProperty("");
        });

        table.getColumns().addAll(code, title, credits, type, assigned);
        return table;
    }

    private void alert(Alert.AlertType type, String msg) {
        new Alert(type, msg).showAndWait();
    }

    // === Logic (unchanged) ===
    private void handleUnregister(TableView<Document> table) {
        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { alert(Alert.AlertType.INFORMATION, "Select a course to unregister."); return; }
        String code = sel.getString("code");
        String linked = AuthSession.getInstance().getLinkedEntityId();
        boolean ok = enrollmentService.unregisterStudentFromCourse(linked, code);
        if (ok) { loadMyCourses(); alert(Alert.AlertType.INFORMATION, "Unregistered from " + code); }
        else alert(Alert.AlertType.WARNING, "You are not registered for " + code);
    }

    private void handleRegister(TableView<Document> table) {
        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { alert(Alert.AlertType.INFORMATION, "Select a course to register."); return; }
        String code = sel.getString("code");
        String linked = AuthSession.getInstance().getLinkedEntityId();
        if (linked == null || linked.isBlank()) { alert(Alert.AlertType.WARNING, "No linked student record."); return; }
        try {
            boolean ok = enrollmentService.registerStudentToCourse(linked, code);
            if (ok) { loadMyCourses(); alert(Alert.AlertType.INFORMATION, "Registered for " + code); }
            else alert(Alert.AlertType.WARNING, "Could not register for " + code);
        } catch (Exception ex) { alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage()); }
    }

    private void handleDelete(TableView<Document> table) {
        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { alert(Alert.AlertType.INFORMATION, "Select a course to delete."); return; }
        try {
            courseService.delete(sel.getObjectId("_id").toHexString());
            loadCourses();
        } catch (Exception ex) { alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage()); }
    }

    private void handleAssign(TableView<Document> table) {
        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { alert(Alert.AlertType.INFORMATION, "Select a course to assign."); return; }
        TextInputDialog dlg = new TextInputDialog();
        dlg.setHeaderText("Enter Staff ID to assign (e.g., P1001)");
        dlg.showAndWait().ifPresent(staffId -> {
            try {
                boolean ok = courseService.assignStaffToCourse(sel.getString("code"), staffId.trim());
                if (ok) { loadCourses(); alert(Alert.AlertType.INFORMATION, "Assigned " + staffId + " to " + sel.getString("code")); }
                else alert(Alert.AlertType.ERROR, "Course not found");
            } catch (Exception ex) { alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage()); }
        });
    }

    private void loadCourses() {
        courseItems.clear();
        List<Document> all = courseService.listAll();
        if (all == null) return;
        String linked = AuthSession.getInstance().getLinkedEntityId();

        if (isAdmin) { courseItems.addAll(all); return; }

        if (isStaff && linked != null && !linked.isBlank()) {
            List<Document> filtered = all.stream()
                    .filter(course -> {
                        Object assigned = course.get("assignedStaff");
                        if (assigned instanceof List<?>) {
                            for (Object a : (List<?>) assigned) {
                                if (a != null && linked.equalsIgnoreCase(a.toString())) return true;
                            }
                        }
                        return false;
                    }).collect(Collectors.toList());
            courseItems.addAll(filtered);
        } else courseItems.addAll(all);
    }

    private void loadMyCourses() {
        myCourses.clear();
        String linked = AuthSession.getInstance().getLinkedEntityId();
        if (linked == null || linked.isBlank()) return;
        List<Document> regs = enrollmentService.listByStudent(linked);
        if (regs == null) return;
        for (Document r : regs) {
            Document course = courseService.findByCode(r.getString("courseCode"));
            if (course != null) myCourses.add(course);
        }
    }

   private void addCourse() {
        Dialog<Document> dlg = new Dialog<>();
        dlg.setTitle("Add Course");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8); grid.setPadding(new Insets(8));
        TextField code = new TextField(); TextField title = new TextField();
        TextField credits = new TextField(); CheckBox core = new CheckBox("Core");
        TextField prereq = new TextField(); prereq.setPromptText("comma-separated");
        grid.add(new Label("Code:"),0,0); grid.add(code,1,0);
        grid.add(new Label("Title:"),0,1); grid.add(title,1,1);
        grid.add(new Label("Credits:"),0,2); grid.add(credits,1,2);
        grid.add(core,1,3);
        grid.add(new Label("Prerequisites:"),0,4); grid.add(prereq,1,4);
        dlg.getDialogPane().setContent(grid);

        dlg.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Document doc = new Document("code", code.getText().trim())
                        .append("title", title.getText().trim())
                        .append("credits", Integer.parseInt(credits.getText().trim()))
                        .append("core", core.isSelected())
                        .append("prerequisites", List.of(prereq.getText().trim().split("\\s*,\\s*")))
                        .append("createdAt", new java.util.Date());
                return doc;
            }
            return null;
        });

        dlg.showAndWait().ifPresent(doc -> {
            try {
                courseService.createCourse(doc);
                loadCourses();
            } catch (SecurityException ex) {
                new Alert(Alert.AlertType.ERROR, "Permission denied: " + ex.getMessage()).showAndWait();
            }
        });
    }

        private void viewEditCourse(Document course) {
            // open edit dialog if permitted; otherwise show read-only info
            boolean canEdit = isAdmin || isProf;
            Dialog<ButtonType> dlg = new Dialog<>();
            dlg.setTitle((canEdit ? "Edit Course" : "Course Details") + " - " + course.getString("code"));
            if (canEdit) dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            else dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

            GridPane grid = new GridPane(); grid.setHgap(8); grid.setVgap(8); grid.setPadding(new Insets(8));
            TextField code = new TextField(course.getString("code")); code.setDisable(true);
            TextField title = new TextField(course.getString("title")); title.setDisable(!canEdit);
            TextField credits = new TextField(String.valueOf(course.getInteger("credits", 3))); credits.setDisable(!canEdit);
            CheckBox core = new CheckBox("Core"); core.setSelected(course.getBoolean("core", false)); core.setDisable(!canEdit);

            Object prereqObj = course.get("prerequisites");
            String prereqText = "";
            if (prereqObj instanceof List) {
                List<?> raw = (List<?>) prereqObj;
                prereqText = raw.stream().map(Object::toString).collect(Collectors.joining(","));
            } else if (prereqObj != null) {
                prereqText = prereqObj.toString();
            }
            TextField prereq = new TextField(prereqText);
            prereq.setDisable(!canEdit);

            grid.add(new Label("Code:"),0,0); grid.add(code,1,0);
            grid.add(new Label("Title:"),0,1); grid.add(title,1,1);
            grid.add(new Label("Credits:"),0,2); grid.add(credits,1,2);
            grid.add(core,1,3);
            grid.add(new Label("Prerequisites (comma):"),0,4); grid.add(prereq,1,4);

            // Add enrolled students list (read-only)
            ListView<String> enrolledList = new ListView<>();
            enrolledList.setPrefHeight(150);
            List<Document> regs = enrollmentService.listByCourse(course.getString("code"));
            if (regs != null && !regs.isEmpty()) {
                List<String> studentIds = regs.stream().map(d -> d.getString("studentId")).collect(Collectors.toList());
                enrolledList.getItems().addAll(studentIds);
            }

            grid.add(new Label("Enrolled Students:"), 0, 5);
            grid.add(enrolledList, 1, 5);

            dlg.getDialogPane().setContent(grid);

            Optional<ButtonType> res = dlg.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK && canEdit) {
                List<String> prereqs = new java.util.ArrayList<>();
                String rawPrereq = prereq.getText().trim();
                if (!rawPrereq.isEmpty()) {
                    for (String p : rawPrereq.split("\\s*,\\s*")) {
                        if (!p.isBlank()) prereqs.add(p.trim());
                    }
                }
                Document updated = new Document("title", title.getText().trim())
                        .append("credits", Integer.parseInt(credits.getText().trim()))
                        .append("core", core.isSelected())
                        .append("prerequisites", prereqs)
                        .append("updatedAt", new java.util.Date());

                try {
                    courseService.update(course.getObjectId("_id").toHexString(), updated);
                    loadCourses();
                } catch (SecurityException ex) {
                    new Alert(Alert.AlertType.ERROR, "Permission denied: " + ex.getMessage()).showAndWait();
                }
            }
        }

    public VBox getView() { return view; }
}
