package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.EnrollmentService;
import edu.agile.sis.service.QuizService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.bson.Document;

import java.util.*;
import java.util.stream.Collectors;

public class QuizzesController {
    private final VBox view = new VBox(10);
    private final QuizService quizService = new QuizService();
    private final EnrollmentService enrollmentService = new EnrollmentService();

    private final ObservableList<Document> items = FXCollections.observableArrayList();
    private final TableView<Document> table = new TableView<>();

    private final boolean isAdmin = AuthSession.getInstance().hasRole("Admin");
    private final boolean isProf = AuthSession.getInstance().hasRole("Professor");
    private final boolean isTA = AuthSession.getInstance().hasRole("TA");
    private final boolean isStudent = AuthSession.getInstance().hasRole("Student");

    public QuizzesController() {
        view.setPadding(new Insets(10));
        Label title = new Label("Quizzes");

        TableColumn<Document, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getString("courseCode")));
        courseCol.setPrefWidth(120);

        TableColumn<Document, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getString("title")));
        titleCol.setPrefWidth(320);

        TableColumn<Document, String> createdCol = new TableColumn<>("Created");
        createdCol.setCellValueFactory(c -> {
            Object d = c.getValue().get("createdAt");
            return new javafx.beans.property.SimpleStringProperty(d == null ? "" : d.toString());
        });
        createdCol.setPrefWidth(180);

        table.getColumns().addAll(courseCol, titleCol, createdCol);
        table.setItems(items);

        Button createBtn = new Button("Create Quiz"); createBtn.setDisable(!(isProf || isAdmin));
        Button editBtn = new Button("Edit"); editBtn.setDisable(!(isProf || isAdmin));
        Button delBtn = new Button("Delete"); delBtn.setDisable(!(isProf || isAdmin));
        Button takeBtn = new Button("Take Quiz"); takeBtn.setDisable(!isStudent);
        Button viewAttemptsBtn = new Button("View Attempts"); viewAttemptsBtn.setDisable(!(isProf || isAdmin || isTA));
        Button refreshBtn = new Button("Refresh");

        createBtn.setOnAction(e -> {
            QuizEditorDialog dlg = new QuizEditorDialog();
            Optional<String> res = dlg.showAndWait();
            res.ifPresent(id -> { refresh(null); new Alert(Alert.AlertType.INFORMATION, "Quiz created id: " + id).showAndWait(); });
        });

        editBtn.setOnAction(e -> {
            Document sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { new Alert(Alert.AlertType.INFORMATION, "Select a quiz").showAndWait(); return; }
            // Simplified: editing not implemented in this version
            new Alert(Alert.AlertType.INFORMATION, "Edit not implemented; please delete and recreate if needed").showAndWait();
        });

        delBtn.setOnAction(e -> {
            Document sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            String id = getDocIdHex(sel);
            boolean ok = quizService.deleteQuiz(id);
            if (ok) { refresh(null); new Alert(Alert.AlertType.INFORMATION, "Deleted"); }
            else new Alert(Alert.AlertType.ERROR, "Delete failed").showAndWait();
        });

        takeBtn.setOnAction(e -> {
    Document sel = table.getSelectionModel().getSelectedItem();
    if (sel == null) {
        new Alert(Alert.AlertType.INFORMATION, "Select a quiz").showAndWait();
        return;
    }

    String quizId = getDocIdHex(sel);
    String studentId = AuthSession.getInstance().getLinkedEntityId();

    try {
        // Check if student has already submitted
        List<Document> attempts = quizService.listAttemptsForQuiz(quizId);
        boolean alreadySubmitted = attempts.stream()
                .anyMatch(a -> studentId.equals(a.getString("studentId")));

        if (alreadySubmitted) {
            new Alert(Alert.AlertType.WARNING,
                    "You have already submitted this quiz. Multiple attempts are not allowed.")
                    .showAndWait();
            return;
        }

        // Otherwise allow taking quiz
        QuizRunnerDialog dlg = new QuizRunnerDialog(quizId);
        Optional<String> res = dlg.showAndWait();
        res.ifPresent(aid -> { /* QuizRunnerDialog handles submission */ });

    } catch (Exception ex) {
        new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage()).showAndWait();
    }
});


        // View Attempts handler
        viewAttemptsBtn.setOnAction(e -> {
            Document sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { new Alert(Alert.AlertType.INFORMATION, "Select a quiz first").showAndWait(); return; }
            String quizId = getDocIdHex(sel);
            if (quizId == null) { new Alert(Alert.AlertType.ERROR, "Quiz id missing").showAndWait(); return; }

            // Debug log
            System.out.println("[QuizzesController] viewAttempts -> quizId used: " + quizId);

            List<Document> attempts = quizService.listAttemptsForQuiz(quizId);
            if (attempts == null || attempts.isEmpty()) {
                System.out.println("[QuizzesController] viewAttempts: No attempts found for quizId=" + quizId);
                new Alert(Alert.AlertType.INFORMATION, "No attempts found for this quiz.").showAndWait();
                return;
            }

            Dialog<Void> dlg = new Dialog<>();
            dlg.setTitle("Attempts for quiz: " + sel.getString("title"));
            dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

            TableView<Document> atTable = new TableView<>();
            ObservableList<Document> atItems = FXCollections.observableArrayList();
            atItems.addAll(attempts);
            atTable.setItems(atItems);

            TableColumn<Document, String> sCol = new TableColumn<>("StudentId");
            sCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getString("studentId")));
            sCol.setPrefWidth(120);

            TableColumn<Document, String> scoreCol = new TableColumn<>("Score");
            scoreCol.setCellValueFactory(c -> {
                Object sc = c.getValue().get("score");
                Object mx = c.getValue().get("maxScore");
                String s = (sc == null ? "" : sc.toString()) + (mx == null ? "" : (" / " + mx.toString()));
                return new javafx.beans.property.SimpleStringProperty(s);
            });
            scoreCol.setPrefWidth(120);

            TableColumn<Document, String> pctCol = new TableColumn<>("Percent");
            pctCol.setCellValueFactory(c -> {
                Object pct = c.getValue().get("percent");
                if (pct instanceof Number) {
                    return new javafx.beans.property.SimpleStringProperty(String.format("%.1f%%", ((Number) pct).doubleValue()));
                }
                return new javafx.beans.property.SimpleStringProperty(pct == null ? "" : pct.toString());
            });
            pctCol.setPrefWidth(100);

            TableColumn<Document, String> atCol = new TableColumn<>("Submitted At");
            atCol.setCellValueFactory(c -> {
                Object d = c.getValue().get("submittedAt");
                return new javafx.beans.property.SimpleStringProperty(d == null ? "" : d.toString());
            });
            atCol.setPrefWidth(180);

            TableColumn<Document, String> fbCol = new TableColumn<>("Feedback");
            fbCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getString("feedback")));
            fbCol.setPrefWidth(260);

            atTable.getColumns().addAll(sCol, scoreCol, pctCol, atCol, fbCol);

            Button viewBtn = new Button("View Details");
            Button gradeBtn = new Button("Grade / Override");
            viewBtn.setDisable(true);
            gradeBtn.setDisable(true);

            atTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                viewBtn.setDisable(newV == null);
                gradeBtn.setDisable(newV == null || !(isProf || isTA || isAdmin));
            });

            viewBtn.setOnAction(ev -> {
                Document chosen = atTable.getSelectionModel().getSelectedItem();
                if (chosen == null) return;
                showAttemptDetailsDialog(chosen);
            });

            gradeBtn.setOnAction(ev -> {
                Document chosen = atTable.getSelectionModel().getSelectedItem();
                if (chosen == null) return;
                showGradeAttemptDialog(chosen, atItems);
            });

            VBox content = new VBox(8);
            content.setPadding(new Insets(8));
            content.getChildren().addAll(atTable, new HBox(8, viewBtn, gradeBtn));
            dlg.getDialogPane().setContent(content);
            dlg.showAndWait();
        });

        refreshBtn.setOnAction(e -> refresh(null));
        HBox ctrl = new HBox(8, createBtn, editBtn, delBtn, takeBtn, viewAttemptsBtn, refreshBtn);
        ctrl.setPadding(new Insets(6));
        view.getChildren().addAll(title, table, ctrl);

        refresh(null);
    }

    /**
     * Get a clean hex id string from a document's _id field.
     * Handles ObjectId and plain string id forms.
     */
    private String getDocIdHex(Document d) {
        if (d == null) return null;
        Object id = d.get("_id");
        if (id == null) return null;

        // If it's an ObjectId, return the hex string
        try {
            if (id instanceof org.bson.types.ObjectId) {
                return ((org.bson.types.ObjectId) id).toHexString();
            }
        } catch (NoClassDefFoundError ignored) { }

        // If driver wrapped as Document with "$oid" field (rare), try that:
        if (id instanceof Document) {
            Document idDoc = (Document) id;
            Object oid = idDoc.get("$oid");
            if (oid != null) return oid.toString();
        }

        // Otherwise, assume it's already a hex string (or some string representation).
        String s = id.toString();
        // If it looks like ObjectId('hex'), extract hex:
        if (s.startsWith("ObjectId(\"") && s.endsWith("\")")) {
            return s.substring(10, s.length() - 2);
        }
        if (s.startsWith("ObjectId('") && s.endsWith("')")) {
            return s.substring(10, s.length() - 2);
        }
        // fallback: return as-is
        return s;
    }

    /**
     * Show a dialog that displays attempt details, student answers, files (if any).
     */
    private void showAttemptDetailsDialog(Document attempt) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Attempt details - " + attempt.getString("studentId"));
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        StringBuilder sb = new StringBuilder();
        sb.append("Student: ").append(attempt.getString("studentId")).append("\n");
        sb.append("Score: ").append(attempt.get("score")).append(" / ").append(attempt.get("maxScore")).append("\n");
        sb.append("Percent: ").append(attempt.get("percent")).append("\n");
        sb.append("SubmittedAt: ").append(attempt.get("submittedAt")).append("\n");
        sb.append("\nAnswers:\n");

        Object answersObj = attempt.get("answers");
        if (answersObj instanceof Document) {
            Document answers = (Document) answersObj;
            for (String k : answers.keySet()) {
                sb.append(k).append(": ").append(String.valueOf(answers.get(k))).append("\n");
            }
        } else if (answersObj instanceof Map) {
            Map<?,?> answers = (Map<?,?>) answersObj;
            for (Map.Entry<?,?> e : answers.entrySet()) {
                sb.append(String.valueOf(e.getKey())).append(": ").append(String.valueOf(e.getValue())).append("\n");
            }
        } else {
            sb.append(String.valueOf(answersObj)).append("\n");
        }

        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false);
        ta.setPrefRowCount(20);

        dlg.getDialogPane().setContent(ta);
        dlg.showAndWait();
    }

    /**
     * Show grading dialog for an attempt (prof/TA/admin) and update the attempt if changed.
     */
    private void showGradeAttemptDialog(Document attempt, ObservableList<Document> listToUpdate) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Grade / Override - " + attempt.getString("studentId"));
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        javafx.scene.layout.GridPane gp = new javafx.scene.layout.GridPane();
        gp.setHgap(8); gp.setVgap(8); gp.setPadding(new Insets(8));

        TextField scoreField = new TextField(attempt.get("score") == null ? "" : attempt.get("score").toString());
        TextArea feedbackArea = new TextArea(attempt.getString("feedback"));
        feedbackArea.setPrefRowCount(6);

        gp.add(new Label("Score:"), 0, 0); gp.add(scoreField, 1, 0);
        gp.add(new Label("Feedback:"), 0, 1); gp.add(feedbackArea, 1, 1);
        dlg.getDialogPane().setContent(gp);

        Optional<ButtonType> res = dlg.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                double newScore = Double.parseDouble(scoreField.getText().trim());
                String feedback = feedbackArea.getText().trim();
                String grader = AuthSession.getInstance().getUsername();

                // safe id extraction for attempt._id
                String attemptId;
                Object aid = attempt.get("_id");
                if (aid instanceof org.bson.types.ObjectId) attemptId = ((org.bson.types.ObjectId) aid).toHexString();
                else if (aid instanceof Document && ((Document) aid).get("$oid") != null) attemptId = ((Document) aid).getString("$oid");
                else attemptId = aid == null ? null : aid.toString();

                boolean ok = quizService.gradeAttempt(attemptId, newScore, feedback, grader);
                if (ok) {
                    // reload attempts and refresh the visible list
                    String quizId = attempt.getString("quizId");
                    List<Document> refreshed = quizService.listAttemptsForQuiz(quizId);
                    listToUpdate.clear();
                    if (refreshed != null) listToUpdate.addAll(refreshed);
                    new Alert(Alert.AlertType.INFORMATION, "Attempt graded/updated.").showAndWait();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Failed to save grade.").showAndWait();
                }
            } catch (NumberFormatException nfe) {
                new Alert(Alert.AlertType.ERROR, "Invalid score value.").showAndWait();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage()).showAndWait();
            }
        }
    }

    /**
     * Refresh quizzes. If courseCode supplied, filter by it. If null:
     * - Admin/Prof/TA: show all quizzes
     * - Student: show only quizzes for courses they are enrolled in
     */
public void refresh(String courseCode) {
    items.clear();
    List<Document> list = new ArrayList<>();

    if (isStudent) {
        // Student: only enrolled courses
        String studentId = AuthSession.getInstance().getLinkedEntityId();
        if (studentId == null || studentId.isBlank()) return;

        List<Document> regs = enrollmentService.listByStudent(studentId);
        if (regs == null) return;

        Set<String> enrolledCourses = regs.stream()
                .map(d -> d.getString("courseCode"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (String code : enrolledCourses) {
            if (courseCode == null || courseCode.equals(code)) {
                list.addAll(quizService.listByCourse(code));
            }
        }

    } else if (isProf || isTA) {
        // Professor / TA: only assigned courses
        String staffId = AuthSession.getInstance().getLinkedEntityId();
        if (staffId == null || staffId.isBlank()) return;

        List<Document> teachingCourses = new edu.agile.sis.service.CourseService().listByStaff(staffId);
        for (Document c : teachingCourses) {
            String code = c.getString("code");
            if (code != null && (courseCode == null || courseCode.equals(code))) {
                list.addAll(quizService.listByCourse(code));
            }
        }

    } else if (isAdmin) {
        // Admin: all quizzes
        list = quizService.listByCourse(courseCode);
    }

    if (list != null) items.addAll(list);
}


    public VBox getView() { return view; }
}
