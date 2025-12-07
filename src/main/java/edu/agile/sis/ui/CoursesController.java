package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.*;
import edu.agile.sis.dao.SubmissionDAO;
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;


public class CoursesController {
    private final VBox view = new VBox(20);
    private final CourseService courseService = new CourseService();
    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final SubmissionService submissionService = tryCreateSubmissionService();
    private final AssignmentService assignmentService = tryCreateAssignmentService();

    private final boolean isAdmin = AuthSession.getInstance().hasRole("Admin");
    private final boolean isProf = AuthSession.getInstance().hasRole("Professor");
    private final boolean isTA = AuthSession.getInstance().hasRole("TA");
    private final boolean isStaff = isProf || isTA || AuthSession.getInstance().hasRole("Staff");
    private final boolean isStudent = AuthSession.getInstance().hasRole("Student");

    private final ObservableList<Document> courseItems = FXCollections.observableArrayList();
    private final ObservableList<Document> myCourses = FXCollections.observableArrayList();



    public CoursesController() {
       
        view.setPadding(new Insets(25));
        view.setStyle("-fx-background-color: linear-gradient(to bottom right, #f8f9fa, #e9ecef);");
        view.setSpacing(20);

  
    Label title = new Label(
    isStudent
        ? "\uD83C\uDF93 Course Catalog"     
        : "\uD83D\uDCD8 Course Management"  
);

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

     
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(10), Insets.EMPTY)));
        card.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 3);");

        if (isStudent) {
   
            TabPane tabs = new TabPane();
            tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

            TableView<Document> myTable = createCourseTable();
            myTable.setItems(myCourses);
            Button unregisterBtn = styledButton("\u274C Unregister Selected", "#dc3545"); 
            unregisterBtn.setOnAction(e -> handleUnregister(myTable));
            VBox myBox = new VBox(10, myTable, unregisterBtn);
            myBox.setPadding(new Insets(8));

            TableView<Document> allTable = createCourseTable();
            allTable.setItems(courseItems);
            Button registerBtn = styledButton("\u2705 Register Selected", "#28a745");

            registerBtn.setOnAction(e -> handleRegister(allTable));
            VBox allBox = new VBox(10, allTable, registerBtn);
            allBox.setPadding(new Insets(8));

            Tab myTab  = new Tab("\uD83D\uDCD8 My Courses", myBox);  
            Tab allTab = new Tab("\uD83D\uDCDA All Courses", allBox); 

            tabs.getTabs().addAll(myTab, allTab);
            card.getChildren().add(tabs);
        } else {
            
            TableView<Document> table = createCourseTable();
            table.setItems(courseItems);

            Button add        = styledButton("\u2795 Add Course", "#28a745");                                 // ➕
            Button edit       = styledButton("\u270F\uFE0F Edit/View", "#007bff");                           // ✏️ (pencil + VS16)
            Button del        = styledButton("\uD83D\uDDD1\uFE0F Delete", "#dc3545");                        // 🗑️ (trash + VS16)
            Button assign     = styledButton("\uD83D\uDC69\u200D\uD83C\uDFEB Assign Staff", "#ffc107");     // 👩‍🏫 (woman teacher)
            Button refresh    = styledButton("\uD83D\uDD0D Refresh", "#17a2b8");                             // 🔍
            Button studentsBtn= styledButton("\uD83D\uDC65 Students", "#6f42c1");                            // 👥

            add.setDisable(!isAdmin);
            del.setDisable(!isAdmin);
            edit.setDisable(!(isAdmin || isProf));
            assign.setDisable(!isAdmin);
            studentsBtn.setDisable(!isStaff); 

            add.setOnAction(e -> addCourse());
            edit.setOnAction(e -> {
                Document sel = table.getSelectionModel().getSelectedItem();
                if (sel == null) { alert(Alert.AlertType.INFORMATION, "Select a course first."); return; }
                viewEditCourse(sel);
            });
            del.setOnAction(e -> handleDelete(table));
            assign.setOnAction(e -> handleAssign(table));
            refresh.setOnAction(e -> loadCourses());

            
            studentsBtn.setOnAction(e -> handleStudents(table));

            HBox controls = new HBox(10, add, edit, del, assign, studentsBtn, refresh);
            controls.setAlignment(Pos.CENTER_LEFT);
            controls.setPadding(new Insets(5, 0, 0, 0));

            card.getChildren().addAll(table, controls);
        }

        view.getChildren().addAll(headerBox, card);

        loadCourses();
        if (isStudent) loadMyCourses();
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

    private TableView<Document> createCourseTable() {
        TableView<Document> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8;");
        table.setPlaceholder(new Label("No courses available."));

        TableColumn<Document, String> code = new TableColumn<>("Code");
        code.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safeString(c.getValue().getString("code"), "")));

        TableColumn<Document, String> title = new TableColumn<>("Title");
        title.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safeString(c.getValue().getString("title"), "")));

        TableColumn<Document, String> credits = new TableColumn<>("Credits");
        credits.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getOrDefault("credits", ""))));

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


    private void handleStudents(TableView<Document> table) {
        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { alert(Alert.AlertType.INFORMATION, "Select a course first."); return; }
        String courseCode = sel.getString("code");
        if (courseCode == null || courseCode.isBlank()) { alert(Alert.AlertType.WARNING, "Course code missing."); return; }

       
        List<Document> regs = enrollmentService.listByCourse(courseCode);
        if (regs == null || regs.isEmpty()) {
            alert(Alert.AlertType.INFORMATION, "No students enrolled for " + courseCode);
            return;
        }

 
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Students for " + courseCode);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ListView<String> studentList = new ListView<>();
        studentList.setPrefHeight(320);
        List<String> studentIds = regs.stream().map(d -> safeString(d.getString("studentId"), "")).filter(s -> !s.isBlank()).collect(Collectors.toList());
        studentList.getItems().addAll(studentIds);

       
        VBox detail = new VBox(8);
        detail.setPadding(new Insets(8));
        Label selLabel = new Label("Select a student to grade");
        TextField studentIdField = new TextField();
        studentIdField.setEditable(false);
        studentIdField.setPrefWidth(260);

 
        final Document[] currentSubmission = new Document[1];

        TextField finalGradeField = new TextField();
        finalGradeField.setPromptText("Final exam grade (numeric)");

        TextArea feedbackArea = new TextArea();
        feedbackArea.setPromptText("Optional feedback (visible to student)");
        feedbackArea.setPrefRowCount(4);

     
        Label subIdLabel = new Label();
        subIdLabel.setWrapText(true);
        subIdLabel.setVisible(false);

        Button saveGradeBtn = styledButton("\uD83D\uDCBE Save Final Grade", "#28a745");


        detail.getChildren().addAll(selLabel, new Label("Student ID:"), studentIdField, new Label("Final Exam Grade:"), finalGradeField, new Label("Feedback (optional):"), feedbackArea, subIdLabel, saveGradeBtn);

        HBox content = new HBox(12, studentList, detail);
        content.setPrefSize(760, 420);

        dlg.getDialogPane().setContent(content);

    
        studentList.getSelectionModel().selectedItemProperty().addListener((obs, oldv, newv) -> {
            finalGradeField.clear();
            feedbackArea.clear();
            subIdLabel.setText("");
            currentSubmission[0] = null;

            if (newv != null) {
                studentIdField.setText(newv);
                selLabel.setText("Ready to assign final exam grade for student:");
                
                try {
                    Document found = findSubmissionForStudentInCourse(newv, courseCode);
                    if (found != null) {
                       
                        String foundType = safeString(found.getString("type"), "");
                        boolean isFinalSubmission = "final".equalsIgnoreCase(foundType) || "exam".equalsIgnoreCase(foundType);

                        if (isFinalSubmission) {
                            currentSubmission[0] = found;
                            
                            Object gradeObj = found.get("grade");
                            if (gradeObj instanceof Number) finalGradeField.setText(String.valueOf(((Number) gradeObj).doubleValue()));
                            else if (gradeObj != null) finalGradeField.setText(gradeObj.toString());

                            String fb = safeString(found.getString("feedback"), "");
                            feedbackArea.setText(fb);

                            String sid = extractIdFromDoc(found);
                            if (sid != null) {
                                subIdLabel.setText("Final Submission ID: " + sid);
                                subIdLabel.setVisible(true);
                            } else {
                                subIdLabel.setVisible(false);
                            }
                        } else {
                            
                            subIdLabel.setText("Existing assignment submission detected; a separate final submission will be created.");
                            subIdLabel.setVisible(true);
                            currentSubmission[0] = null; 
                        }
                    } else {
                        subIdLabel.setVisible(false);
                    }
                } catch (Throwable t) {
                  
                }
            }
        });

        saveGradeBtn.setOnAction(e -> {
            String sid = studentIdField.getText();
            String gradeText = finalGradeField.getText();
            String feedback = feedbackArea.getText();
            if (sid == null || sid.isBlank()) { alert(Alert.AlertType.WARNING, "Select a student first."); return; }
            if (gradeText == null || gradeText.isBlank()) { alert(Alert.AlertType.WARNING, "Enter a grade value."); return; }
            // parse numeric
            Double gVal = null;
            try {
                gVal = Double.parseDouble(gradeText.trim());
            } catch (NumberFormatException nfe) {
                alert(Alert.AlertType.ERROR, "Invalid grade. Enter a numeric value (e.g., 87.5).");
                return;
            }

            
            if (currentSubmission[0] != null) {
                String foundId = extractIdFromDoc(currentSubmission[0]);
                if (foundId != null && submissionService != null) {
                    try {
                        boolean ok = submissionService.gradeSubmission(foundId, gVal, safeString(feedback, ""), AuthSession.getInstance().getUsername());
                        if (ok) {
                            alert(Alert.AlertType.INFORMATION, "Updated final grade for " + sid + " in " + courseCode);
                        } else {
                            alert(Alert.AlertType.ERROR, "Could not update grade. Backend returned false.");
                        }
                        return;
                    } catch (Throwable t) {
                                               
                    }
                }
            }

        
            boolean saved = saveFinalGradeForStudentInCourse(sid, courseCode, gVal, feedback);
            if (saved) {
                alert(Alert.AlertType.INFORMATION, "Saved final grade for " + sid + " in " + courseCode);
            } else {
                alert(Alert.AlertType.ERROR, "Could not save grade. Please ensure backend supports grading via SubmissionService.gradeSubmission(submissionId, grade, feedback, grader) or SubmissionDAO.insertSubmission(Document).");
            }
        });

        dlg.showAndWait();
    }


    private Document findSubmissionForStudentInCourse(String studentId, String courseCode) {
        if (studentId == null || studentId.isBlank() || courseCode == null || courseCode.isBlank()) return null;
        String normalizedCourse = courseCode.trim();
        if (submissionService == null) return null;

        try {
            List<Document> subs = submissionService.listByStudent(studentId);
            if (subs != null) {

                for (Document s : subs) {
                    String subCourse = null;
                    if (s.containsKey("courseCode")) subCourse = safeString(s.getString("courseCode"), null);
                    if ((subCourse == null || subCourse.isBlank()) && s.containsKey("subjectId")) subCourse = safeString(s.getString("subjectId"), null);
                    if (subCourse != null && subCourse.trim().equalsIgnoreCase(normalizedCourse)) {
                        return s;
                    }
                }

          
                if (assignmentService != null) {
                    for (Document s : subs) {
                        Object aidObj = s.get("assignmentId");
                        if (aidObj == null) continue;
                        String aid = aidObj.toString();
                        Document assignmentDoc = null;
                        try { assignmentDoc = assignmentService.getById(aid); } catch (Throwable ignore) { assignmentDoc = null; }
                        if (assignmentDoc != null) {
                            String acode = safeString(assignmentDoc.getString("courseCode"), assignmentDoc.getString("subjectId"));
                            if (acode != null && acode.trim().equalsIgnoreCase(normalizedCourse)) {
                                return s;
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            
        }
        return null;
    }

   
    private boolean saveFinalGradeForStudentInCourse(String studentId, String courseCode, Double grade, String feedback) {
        try {
          
            if (studentId == null || studentId.isBlank() || courseCode == null || courseCode.isBlank()) return false;
            String normalizedCourse = courseCode.trim();

            
            try {
                Document existing = findSubmissionForStudentInCourse(studentId, normalizedCourse);
                if (existing != null) {
                    String foundType = safeString(existing.getString("type"), "");
                    boolean isFinalSubmission = "final".equalsIgnoreCase(foundType) || "exam".equalsIgnoreCase(foundType);
                    if (isFinalSubmission) {
                        String submissionId = extractIdFromDoc(existing);
                        if (submissionId != null && submissionService != null) {
                            boolean ok = submissionService.gradeSubmission(submissionId, grade, safeString(feedback, ""), AuthSession.getInstance().getUsername());
                            
                            return ok;
                        }
                    } else {
                       
                    }
                }
            } catch (Throwable t) {
                
            }

            
            Document courseDoc = null;
            try {
                courseDoc = courseService.findByCode(normalizedCourse);
            } catch (Throwable ignore) { courseDoc = null; }

            String subjectId   = courseDoc != null ? safeString(courseDoc.getString("code"), normalizedCourse) : normalizedCourse;
            String subjectName = courseDoc != null ? safeString(courseDoc.getString("title"), normalizedCourse) : normalizedCourse;

        
            Double credits = 3.0;
            if (courseDoc != null && courseDoc.containsKey("credits")) {
                Object credObj = courseDoc.get("credits");
                if (credObj instanceof Number) credits = ((Number) credObj).doubleValue();
                else {
                    try { credits = Double.parseDouble(credObj.toString()); } catch (Throwable ignore) { credits = 3.0; }
                }
            }

            Document newSub = new Document();
            newSub.put("studentId", studentId);
            newSub.put("submittedAt", new Date());
            newSub.put("files", Collections.emptyList());
            newSub.put("answers", new Document());
            newSub.put("status", "graded");
            newSub.put("grade", grade);
            newSub.put("feedback", safeString(feedback, ""));
          
            newSub.put("type", "final");
            
            newSub.put("title", "Final Exam");
            newSub.put("examType", "final");
            
            newSub.put("courseCode", normalizedCourse);
            newSub.put("subjectId", subjectId);
            newSub.put("subjectName", subjectName);
            newSub.put("credits", credits);
            newSub.put("gradedAt", new Date());
            newSub.put("grader", AuthSession.getInstance().getUsername());
            newSub.put("createdAt", new Date());

    
            try {
                
                try {
                    edu.agile.sis.dao.SubmissionDAO dao = new edu.agile.sis.dao.SubmissionDAO();
                    try {
                        Object res = dao.insertSubmission(newSub); 
                        String insertedId = null;
                        if (res != null) {
                            if (res instanceof String) insertedId = (String) res;
                            else if (res instanceof Document && ((Document) res).containsKey("_id")) insertedId = extractIdFromDoc((Document) res);
                            else insertedId = res.toString();
                        }
                        
                        if (insertedId != null && submissionService != null) {
                            try {
                                boolean ok = submissionService.gradeSubmission(insertedId, grade, safeString(feedback, ""), AuthSession.getInstance().getUsername());
                                
                                return ok;
                            } catch (Throwable ign) { return true; }
                        }
                        return true;
                    } catch (NoSuchMethodError nsme) {
                
                    } catch (Throwable t) {
                        
                    }
                } catch (Throwable daoEx) {
                   
                }

            
                try {
                    Class<?> daoCls = Class.forName("edu.agile.sis.dao.SubmissionDAO");
                    Object daoInst = daoCls.getDeclaredConstructor().newInstance();
                    for (Method m : daoCls.getMethods()) {
                        String name = m.getName().toLowerCase();
                        if (!(name.contains("insert") || name.contains("create") || name.contains("save") || name.contains("add"))) continue;
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length == 1) {
                            try {
                                Object res = m.invoke(daoInst, newSub);
                                String insertedId = null;
                                if (res != null) {
                                    if (res instanceof String) insertedId = (String) res;
                                    else if (res instanceof Document && ((Document) res).containsKey("_id")) insertedId = extractIdFromDoc((Document) res);
                                    else insertedId = res.toString();
                                }
                                
                                if (insertedId != null && submissionService != null) {
                                    try {
                                        boolean ok = submissionService.gradeSubmission(insertedId, grade, safeString(feedback, ""), AuthSession.getInstance().getUsername());
                                       
                                        return ok;
                                    } catch (Throwable ign) { return true; }
                                }
                                return true;
                            } catch (Throwable t) {
                               
                            }
                        }
                    }
                } catch (ClassNotFoundException cnf) {
          
                } catch (Throwable t) {
                   
                }

                
                if (submissionService != null) {
                    try {
                        for (Method m : submissionService.getClass().getMethods()) {
                            String n = m.getName().toLowerCase();
                            if (!(n.contains("create") || n.contains("insert") || n.contains("save") || n.contains("add"))) continue;
                            Class<?>[] params = m.getParameterTypes();
                            if (params.length == 1) {
                                try {
                                    Object res = m.invoke(submissionService, newSub);
                                    
                                    String insertedId = null;
                                    if (res instanceof String) insertedId = (String) res;
                                    else if (res instanceof Document && ((Document) res).containsKey("_id")) insertedId = extractIdFromDoc((Document) res);
                                    if (insertedId != null) {
                                        boolean ok = submissionService.gradeSubmission(insertedId, grade, safeString(feedback, ""), AuthSession.getInstance().getUsername());
                                        
                                        return ok;
                                    }
                                    return true;
                                } catch (Throwable t) {
                                    
                                }
                            }
                        }
                    } catch (Throwable t) {
                        
                    }
                }
            } catch (Throwable t) {
                
            }

            System.err.println("[CoursesController] Failed to persist fallback FINAL submission for student=" + studentId + " course=" + normalizedCourse);
            return false;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    
    private static String extractIdFromDoc(Document d) {
        if (d == null) return null;
        Object id = d.get("_id");
        if (id == null) {
            if (d.containsKey("id")) return d.getString("id");
            if (d.containsKey("submissionId")) return d.getString("submissionId");
            return null;
        }
       
        try {
            
            Class<?> oidClass = Class.forName("org.bson.types.ObjectId");
            if (oidClass.isInstance(id)) {
                try {
                    Method toHex = oidClass.getMethod("toHexString");
                    Object hex = toHex.invoke(id);
                    if (hex != null) return hex.toString();
                } catch (Throwable ignore) {}
            }
        } catch (ClassNotFoundException ignore) {
       
        } catch (Throwable ignore) {}

       
        return id.toString();
    }


    private static SubmissionService tryCreateSubmissionService() {
        try { return new SubmissionService(); } catch (Throwable ignored) { return null; }
    }

  
    private static AssignmentService tryCreateAssignmentService() {
        try { return new AssignmentService(); } catch (Throwable ignored) { return null; }
    }

    private static String safeString(String s, String fallback) {
        if (s == null) return fallback == null ? "" : fallback;
        return s;
    }

    public VBox getView() { return view; }
}
