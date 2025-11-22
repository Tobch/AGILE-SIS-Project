package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.AssignmentService;
import edu.agile.sis.service.SubmissionService;
import edu.agile.sis.util.FileStorageUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.bson.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AssignmentsController {
    private final VBox view = new VBox(10);
    private final AssignmentService assignmentService = new AssignmentService();
    private final SubmissionService submissionService = new SubmissionService();

    private final ObservableList<Document> items = FXCollections.observableArrayList();
    private final TableView<Document> table = new TableView<>();

    private final boolean isAdmin = AuthSession.getInstance().hasRole("Admin");
    private final boolean isProf = AuthSession.getInstance().hasRole("Professor");
    private final boolean isTA = AuthSession.getInstance().hasRole("TA");
    private final boolean isStudent = AuthSession.getInstance().hasRole("Student");

    public AssignmentsController() {
        view.setPadding(new Insets(10));
        Label title = new Label("Assignments");

        TableColumn<Document, String> codeCol = new TableColumn<>("Course");
        codeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getString("courseCode")));
        codeCol.setPrefWidth(100);

        TableColumn<Document, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getString("title")));
        titleCol.setPrefWidth(300);

        TableColumn<Document, String> dueCol = new TableColumn<>("Due");
        dueCol.setCellValueFactory(c -> {
            Object d = c.getValue().get("dueDate");
            return new javafx.beans.property.SimpleStringProperty(d == null ? "" : d.toString());
        });
        dueCol.setPrefWidth(180);

        table.getColumns().addAll(codeCol, titleCol, dueCol);
        table.setItems(items);

        Button createBtn = new Button("Create Assignment");
        createBtn.setDisable(!(isProf || isAdmin));
        createBtn.setOnAction(e -> {
            AssignmentEditorDialog dlg = new AssignmentEditorDialog();
            Optional<String> res = dlg.showAndWait();
            res.ifPresent(id -> {
                refresh(null);
                new Alert(Alert.AlertType.INFORMATION, "Created assignment id: " + id).showAndWait();
            });
        });

        Button detailsBtn = new Button("View Details");
        detailsBtn.setOnAction(e -> {
            Document sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { new Alert(Alert.AlertType.INFORMATION, "Select an assignment").showAndWait(); return; }
            AssignmentDetailsDialog details = new AssignmentDetailsDialog(sel);
            details.showAndWait();
        });

        Button submitBtn = new Button("Submit Selected");
        submitBtn.setDisable(!isStudent);
        submitBtn.setOnAction(e -> {
            Document sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { new Alert(Alert.AlertType.INFORMATION, "Select an assignment").showAndWait(); return; }
            String id = sel.getObjectId("_id").toHexString();
            SubmissionDialog dlg = new SubmissionDialog(id);
            Optional<String> res = dlg.showAndWait();
            res.ifPresent(subId -> new Alert(Alert.AlertType.INFORMATION, "Submission saved: " + subId).showAndWait());
        });

        Button gradeBtn = new Button("Grade Selected");
        gradeBtn.setDisable(!(isProf || isTA || isAdmin));
        gradeBtn.setOnAction(e -> {
            Document sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { new Alert(Alert.AlertType.INFORMATION, "Select an assignment").showAndWait(); return; }
            String assignmentId = sel.getObjectId("_id").toHexString();
            List<Document> subs = submissionService.listByAssignment(assignmentId);
            if (subs == null || subs.isEmpty()) { new Alert(Alert.AlertType.INFORMATION, "No submissions found").showAndWait(); return; }

            // Build readable list
            java.util.Map<String, Document> labelToDoc = new java.util.LinkedHashMap<>();
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (Document s : subs) {
                String sid = s.getString("studentId");
                Object dt = s.get("submittedAt");
                String dtText = dt == null ? "no-date" : dt.toString();
                try { if (dt instanceof java.util.Date) dtText = fmt.format((java.util.Date) dt); } catch (Exception ignored) {}
                String label = (sid == null ? "<unknown student>" : sid) + " - " + dtText + "  (id:" + (s.getObjectId("_id") != null ? s.getObjectId("_id").toHexString() : s.get("_id")) + ")";
                int suffix = 1; String base = label;
                while (labelToDoc.containsKey(label)) { label = base + " (" + (suffix++) + ")"; }
                labelToDoc.put(label, s);
            }

            ChoiceDialog<String> chooser = new ChoiceDialog<>(labelToDoc.keySet().iterator().next(), labelToDoc.keySet());
            chooser.setTitle("Select submission to grade");
            chooser.setHeaderText("Select a submission (student - submittedAt):");
            Optional<String> picked = chooser.showAndWait();
            picked.ifPresent(lbl -> {
                Document selectedSubmission = labelToDoc.get(lbl);
                if (selectedSubmission == null) return;

                // grading dialog with download option for student files
                Dialog<ButtonType> gradeDialog = new Dialog<>();
                gradeDialog.setTitle("Grade Submission - " + lbl);
                gradeDialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
                javafx.scene.layout.GridPane gp = new javafx.scene.layout.GridPane();
                gp.setHgap(8); gp.setVgap(8); gp.setPadding(new Insets(10));
                TextField gradeField = new TextField();
                gradeField.setPromptText("numeric grade (e.g., 85)");
                TextArea feedbackArea = new TextArea();
                feedbackArea.setPromptText("Feedback for student (visible to student)");
                feedbackArea.setPrefRowCount(6);

                // download student files button
                Button downloadSubmissionFiles = new Button("Download Submission Files");
                downloadSubmissionFiles.setOnAction(ev -> {
                    List<Document> files = (List<Document>) selectedSubmission.get("files");
                    if (files == null || files.isEmpty()) { new Alert(Alert.AlertType.INFORMATION, "No files attached by student.").showAndWait(); return; }
                    // for each file, ask for save location
                    for (Document fdoc : files) {
                        String filename = fdoc.getString("filename");
                        String storageRef = fdoc.getString("storageRef");
                        FileChooser fc = new FileChooser();
                        fc.setInitialFileName(filename);
                        File dest = fc.showSaveDialog(gradeDialog.getOwner());
                        if (dest == null) continue;
                        try (InputStream is = FileStorageUtil.getFileStream(storageRef);
                             FileOutputStream fos = new FileOutputStream(dest)) {
                            byte[] buf = new byte[8192];
                            int r;
                            while ((r = is.read(buf)) > 0) fos.write(buf, 0, r);
                            new Alert(Alert.AlertType.INFORMATION, "Saved file: " + dest.getAbsolutePath()).showAndWait();
                        } catch (Exception ex) {
                            new Alert(Alert.AlertType.ERROR, "Download failed: " + ex.getMessage()).showAndWait();
                        }
                    }
                });

                // prefill if existing
                Object existingGrade = selectedSubmission.get("grade");
                Object existingFeedback = selectedSubmission.get("feedback");
                if (existingGrade != null) gradeField.setText(existingGrade.toString());
                if (existingFeedback != null) feedbackArea.setText(existingFeedback.toString());

                gp.add(new Label("Grade:"), 0, 0); gp.add(gradeField, 1, 0);
                gp.add(new Label("Feedback:"), 0, 1); gp.add(feedbackArea, 1, 1);
                gp.add(downloadSubmissionFiles, 1, 2);
                gradeDialog.getDialogPane().setContent(gp);

                Optional<ButtonType> dialogRes = gradeDialog.showAndWait();
                if (dialogRes.isPresent() && dialogRes.get() == ButtonType.OK) {
                    String gradeText = gradeField.getText().trim();
                    String feedbackText = feedbackArea.getText().trim();
                    try {
                        double val = Double.parseDouble(gradeText);
                        String grader = AuthSession.getInstance().getUsername();
                        boolean ok = submissionService.gradeSubmission(selectedSubmission.getObjectId("_id").toHexString(), val, feedbackText, grader);
                        if (ok) { new Alert(Alert.AlertType.INFORMATION, "Submission graded and feedback saved.").showAndWait(); } 
                        else { new Alert(Alert.AlertType.ERROR, "Failed to save grade/feedback.").showAndWait(); }
                    } catch (NumberFormatException nfe) {
                        new Alert(Alert.AlertType.ERROR, "Invalid grade number.").showAndWait();
                    }
                }
            });
        });

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> refresh(null));

        HBox buttons = new HBox(8, createBtn, detailsBtn, submitBtn, gradeBtn, refreshBtn);
        buttons.setPadding(new Insets(6));

        view.getChildren().addAll(title, table, buttons);
        refresh(null);
    }

    /**
     * Refresh assignments list. If courseCode non-null, filter by course.
     */
   public void refresh(String courseCode) {
    items.clear();
    List<Document> list = new java.util.ArrayList<>();

    if (isStudent) {
        // STUDENT: only assignments for enrolled courses
        String studentId = AuthSession.getInstance().getLinkedEntityId();
        System.out.println("[AssignmentsController] Refreshing for studentId=" + studentId);

        if (courseCode != null) {
            list.addAll(assignmentService.listByCourseForStudent(courseCode, studentId));
        } else {
            List<Document> enrollments = new edu.agile.sis.service.EnrollmentService().listByStudent(studentId);
            for (Document c : enrollments) {
                String code = c.getString("courseCode");
                if (code != null) {
                    list.addAll(assignmentService.listByCourseForStudent(code, studentId));
                }
            }
        }

    } else if (isProf || isTA) {
        // PROFESSOR / TA: only courses they teach (assignedStaff contains their linkedEntityId)
        String staffId = AuthSession.getInstance().getLinkedEntityId();
        System.out.println("[AssignmentsController] Refreshing for staffId=" + staffId);

        List<Document> teachingCourses = new edu.agile.sis.service.CourseService().listByStaff(staffId);
        for (Document course : teachingCourses) {
            String code = course.getString("code"); // field in courses collection
            if (code != null && (courseCode == null || courseCode.equals(code))) {
                list.addAll(assignmentService.listByCourse(code));
            }
        }

    } else {
        // ADMIN: see all assignments
        list = assignmentService.listByCourse(courseCode);
    }

    System.out.println("[AssignmentsController] Loaded assignments = " + (list == null ? 0 : list.size()));
    if (list != null) items.addAll(list);
}




    public VBox getView() { return view; }
}
