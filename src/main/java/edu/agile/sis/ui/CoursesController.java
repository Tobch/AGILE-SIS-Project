package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.CourseService;
import edu.agile.sis.service.EnrollmentService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.bson.Document;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CoursesController {
    private final VBox view = new VBox(10);
    private final CourseService courseService = new CourseService();
    private final EnrollmentService enrollmentService = new EnrollmentService();

    private final boolean isAdmin = AuthSession.getInstance().hasRole("Admin");
    private final boolean isProf = AuthSession.getInstance().hasRole("Professor");
    private final boolean isTA = AuthSession.getInstance().hasRole("TA");
    private final boolean isStaff = isProf || isTA || AuthSession.getInstance().hasRole("Staff");
    private final boolean isStudent = AuthSession.getInstance().hasRole("Student");

    // lists
    private final ObservableList<Document> courseItems = FXCollections.observableArrayList();
    private final ObservableList<Document> myCourses = FXCollections.observableArrayList();

    public CoursesController() {
        view.setPadding(new Insets(10));
        Label title = new Label("📚 Course Catalog");
         title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 8 0 8 0;");


        if (isStudent) {
            // Student view: show "My Courses" and "All Courses (Register)"
            TabPane tabs = new TabPane();
            Tab myTab = new Tab("My Courses");
            Tab allTab = new Tab("All Courses");

            // my courses table
            TableView<Document> myTable = createCourseTable();
            myTable.setItems(myCourses);

           Button unregisterBtn = new Button("Unregister Selected");
           unregisterBtn.setPrefWidth(160);
           unregisterBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");

            unregisterBtn.setOnAction(e -> {
                Document sel = myTable.getSelectionModel().getSelectedItem();
                if (sel == null) return;
                String code = sel.getString("code");
                String linked = AuthSession.getInstance().getLinkedEntityId();
                boolean ok = enrollmentService.unregisterStudentFromCourse(linked, code);
                if (ok) {
                    loadMyCourses();
                    new Alert(Alert.AlertType.INFORMATION, "Unregistered from " + code).showAndWait();
                } else {
                    new Alert(Alert.AlertType.WARNING, "You are not registered for " + code).showAndWait();
                }
            });

            VBox myBox = new VBox(8, myTable, unregisterBtn);
            myBox.setPadding(new Insets(6));
            myTab.setContent(myBox);

            // all courses table + register button
            TableView<Document> allTable = createCourseTable();
            allTable.setItems(courseItems);
            
            Button registerBtn = new Button("Register Selected");
            registerBtn.setPrefWidth(160);
            registerBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");
            
            
            registerBtn.setOnAction(e -> {
                Document sel = allTable.getSelectionModel().getSelectedItem();
                if (sel == null) {
                    new Alert(Alert.AlertType.INFORMATION, "Select a course to register.").showAndWait();
                    return;
                }
                String code = sel.getString("code");
                String linked = AuthSession.getInstance().getLinkedEntityId();
                if (linked == null || linked.isBlank()) {
                    new Alert(Alert.AlertType.WARNING, "Your account is not linked to a student record.").showAndWait();
                    return;
                }
                try {
                    boolean ok = enrollmentService.registerStudentToCourse(linked, code);
                    if (ok) {
                        loadMyCourses();
                        new Alert(Alert.AlertType.INFORMATION, "Registered for " + code).showAndWait();
                    } else {
                        // the service should throw exceptions for known failures; this is a fallback
                        new Alert(Alert.AlertType.WARNING, "Could not register for " + code).showAndWait();
                    }
                } catch (IllegalStateException ise) {
                    // business rule (GPA limit, already registered, etc.)
                    new Alert(Alert.AlertType.WARNING, ise.getMessage()).showAndWait();
                } catch (IllegalArgumentException iae) {
                    new Alert(Alert.AlertType.WARNING, iae.getMessage()).showAndWait();
                } catch (SecurityException se) {
                    new Alert(Alert.AlertType.ERROR, "Permission denied: " + se.getMessage()).showAndWait();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Error while registering: " + ex.getMessage()).showAndWait();
                }
            });


            VBox allBox = new VBox(8, allTable, registerBtn);
            allBox.setPadding(new Insets(6));
            allTab.setContent(allBox);

            tabs.getTabs().addAll(myTab, allTab);
            view.getChildren().addAll(title, tabs);
        } else {
            // Admin / Staff / Prof view (single table)
            TableView<Document> table = createCourseTable();
            table.setItems(courseItems);

            Button add = new Button("Add Course");
            Button edit = new Button("View / Edit");
            Button del = new Button("Delete");
            Button assign = new Button("Assign Staff");
            Button refresh = new Button("Refresh");

            add.setDisable(!(isAdmin ));
            del.setDisable(!(isAdmin ));
            edit.setDisable(!(isAdmin || isProf));
            assign.setDisable(!isAdmin); // only admin assigns staff

            add.setOnAction(e -> addCourse());
            refresh.setOnAction(e -> loadCourses());

            edit.setOnAction(e -> {
                Document sel = table.getSelectionModel().getSelectedItem();
                if (sel == null) { new Alert(Alert.AlertType.INFORMATION, "Select a course").showAndWait(); return; }
                viewEditCourse(sel);
            });

            del.setOnAction(e -> {
                Document sel = table.getSelectionModel().getSelectedItem();
                if (sel == null) return;
                try {
                    courseService.delete(sel.getObjectId("_id").toHexString());
                    loadCourses();
                } catch (SecurityException ex) {
                    new Alert(Alert.AlertType.ERROR, "Permission denied: " + ex.getMessage()).showAndWait();
                }
            });

            assign.setOnAction(e -> {
                Document sel = table.getSelectionModel().getSelectedItem();
                if (sel == null) { new Alert(Alert.AlertType.INFORMATION, "Select a course"); return; }
                TextInputDialog dlg = new TextInputDialog();
                dlg.setHeaderText("Enter staffId to assign (e.g., P1001)");
                dlg.showAndWait().ifPresent(staffId -> {
                    try {
                        boolean ok = courseService.assignStaffToCourse(sel.getString("code"), staffId.trim());
                        if (ok) { loadCourses(); new Alert(Alert.AlertType.INFORMATION, "Assigned " + staffId + " to " + sel.getString("code")).showAndWait(); }
                        else new Alert(Alert.AlertType.ERROR, "Course not found").showAndWait();
                    } catch (SecurityException ex) {
                        new Alert(Alert.AlertType.ERROR, "Permission denied: " + ex.getMessage()).showAndWait();
                    }
                });
            });

            HBox ctrl = new HBox(8, add, edit, del, assign, refresh);
            ctrl.setPadding(new Insets(6));
            view.getChildren().addAll(title, table, ctrl);
        }

        loadCourses();
        if (isStudent) loadMyCourses();
    }

    private TableView<Document> createCourseTable() {
        TableView<Document> table = new TableView<>();
        TableColumn<Document, String> code = new TableColumn<>("Code");
        code.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getString("code")));
        code.setPrefWidth(100);

        TableColumn<Document, String> title = new TableColumn<>("Title");
        title.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getString("title")));
        title.setPrefWidth(280);

        TableColumn<Document, String> credits = new TableColumn<>("Credits");
        credits.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().get("credits"))));
        credits.setPrefWidth(80);

        TableColumn<Document, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getBoolean("core", false) ? "Core" : "Elective"));
        type.setPrefWidth(100);

        TableColumn<Document, String> assigned = new TableColumn<>("Assigned Staff");
        assigned.setCellValueFactory(c -> {
        Object o = c.getValue().get("assignedStaff");
        String s = "";
        if (o instanceof List) s = String.join(", ", ((List<?>) o).stream().map(Object::toString).collect(Collectors.toList()));
        return new javafx.beans.property.SimpleStringProperty(s);
    });

        assigned.setPrefWidth(200);

        table.getColumns().addAll(code, title, credits, type, assigned);
        return table;
    }

    /**
     * Load courses into courseItems ObservableList.
     * - Admin/Professor: load all courses.
     * - Staff (Professor/TA/Staff) but NOT Admin/Professor: load only courses where assignedStaff contains their linkedEntityId.
     * - Student: load all courses (handled elsewhere).
     */
            private void loadCourses() {
            courseItems.clear();
            List<Document> all = courseService.listAll();
            if (all == null) return;

            String linked = AuthSession.getInstance().getLinkedEntityId();

            // ✅ Admin sees all
            if (isAdmin) {
                courseItems.addAll(all);
                return;
            }

            // ✅ Professor / TA / Staff see only assigned courses
            if (isStaff && linked != null && !linked.isBlank()) {
                List<Document> filtered = all.stream()
                        .filter(course -> {
                            Object assigned = course.get("assignedStaff");
                            if (assigned instanceof List) {
                                for (Object a : (List<?>) assigned) {
                                    if (a != null && linked.equalsIgnoreCase(a.toString())) {
                                        return true;
                                    }
                                }
                            }
                            return false;
                        })
                        .collect(Collectors.toList());
                courseItems.addAll(filtered);
                return;
            }

            // ✅ Default (student or unknown)
            courseItems.addAll(all);
        }


    private void loadMyCourses() {
        myCourses.clear();
        String linked = AuthSession.getInstance().getLinkedEntityId();
        if (linked == null || linked.isBlank()) return;
        List<Document> regs = enrollmentService.listByStudent(linked);
        if (regs == null) return;
        for (Document r : regs) {
            String code = r.getString("courseCode");
            Document course = courseService.findByCode(code);
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


    public VBox getView(){ return view; }
}
