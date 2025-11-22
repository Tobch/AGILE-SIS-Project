package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.QuizService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dialog for creating/editing quizzes (professor/admin).
 * Minimal UI: allows adding MCQ questions with options.
 */
public class QuizEditorDialog extends Dialog<String> {
    private final QuizService service = new QuizService();

    private final TextField courseField = new TextField();
    private final TextField titleField = new TextField();
    private final TextField timeLimitField = new TextField("30");

    private final ListView<String> questionsList = new ListView<>();
    private final List<Document> questionsBacking = new ArrayList<>();

    public QuizEditorDialog() {
        setTitle("Create Quiz");
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        GridPane g = new GridPane();
        g.setHgap(8); g.setVgap(8); g.setPadding(new Insets(10));
        courseField.setPromptText("CS201");
        titleField.setPromptText("Quiz Title");
        timeLimitField.setPromptText("Time limit minutes");

        Button addQBtn = new Button("Add MCQ");
        addQBtn.setOnAction(e -> showAddMcqDialog());

        Button removeQBtn = new Button("Remove Selected");
        removeQBtn.setOnAction(e -> {
            int idx = questionsList.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                questionsBacking.remove(idx);
                questionsList.getItems().remove(idx);
            }
        });

        g.add(new Label("Course Code:"), 0, 0); g.add(courseField, 1, 0);
        g.add(new Label("Title:"), 0, 1); g.add(titleField, 1, 1);
        g.add(new Label("Time (minutes):"), 0, 2); g.add(timeLimitField, 1, 2);
        g.add(new Label("Questions:"), 0, 3); g.add(questionsList, 1, 3);
        g.add(addQBtn, 1, 4); g.add(removeQBtn, 1, 5);

        getDialogPane().setContent(g);

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String course = courseField.getText().trim();
                String title = titleField.getText().trim();
                int timeLimit = 30;
                try { timeLimit = Integer.parseInt(timeLimitField.getText().trim()); } catch (Exception ignored) {}
                if (course.isEmpty() || title.isEmpty()) {
                    new Alert(Alert.AlertType.WARNING, "Course and title required").showAndWait();
                    return null;
                }
                String createdBy = AuthSession.getInstance().getUsername();
                String id = service.createQuiz(course, title, timeLimit, new ArrayList<>(questionsBacking), createdBy);
                return id;
            }
            return null;
        });
    }

    private void showAddMcqDialog() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Add MCQ");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        GridPane g = new GridPane();
        g.setHgap(8); g.setVgap(8); g.setPadding(new Insets(10));
        TextField qText = new TextField();
        qText.setPromptText("Question text");
        TextField optA = new TextField(); optA.setPromptText("Option A");
        TextField optB = new TextField(); optB.setPromptText("Option B");
        TextField optC = new TextField(); optC.setPromptText("Option C");
        TextField optD = new TextField(); optD.setPromptText("Option D");
        ChoiceBox<String> correct = new ChoiceBox<>();
        correct.getItems().addAll("A","B","C","D");
        correct.setValue("A");

        g.add(new Label("Question:"), 0, 0); g.add(qText, 1, 0);
        g.add(new Label("A:"), 0, 1); g.add(optA, 1, 1);
        g.add(new Label("B:"), 0, 2); g.add(optB, 1, 2);
        g.add(new Label("C:"), 0, 3); g.add(optC, 1, 3);
        g.add(new Label("D:"), 0, 4); g.add(optD, 1, 4);
        g.add(new Label("Correct (A/B/C/D):"), 0, 5); g.add(correct, 1, 5);

        dlg.getDialogPane().setContent(g);
        Optional<ButtonType> res = dlg.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            String text = qText.getText().trim();
            String a = optA.getText().trim();
            String b = optB.getText().trim();
            String c = optC.getText().trim();
            String d = optD.getText().trim();
            String corr = correct.getValue();
            if (text.isEmpty()) { new Alert(Alert.AlertType.WARNING, "Question text required").showAndWait(); return; }
            String qid = UUID.randomUUID().toString();
            Document q = new Document("id", qid)
                    .append("text", text)
                    .append("type", "mcq")
                    .append("options", List.of(a, b, c, d))
                    .append("correct", switch (corr) {
                        case "A" -> a;
                        case "B" -> b;
                        case "C" -> c;
                        default -> d;
                    });
            questionsBacking.add(q);
            questionsList.getItems().add(text + " (MCQ)");
        }
    }
}
