package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.QuizService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Window;
import javafx.util.Duration;
import org.bson.Document;

import java.util.*;

/**
 * Dialog for students to take a quiz. Auto-grades MCQ and stores attempt.
 * Adds a countdown timer and auto-submit on expiry.
 */
public class QuizRunnerDialog extends Dialog<String> {
    private final QuizService quizService = new QuizService();
    private final String quizId;
    private final Document quiz;
    private Timeline timer;
    private int remainingSeconds = 0;

    public QuizRunnerDialog(String quizId) {
        this.quizId = quizId;
        this.quiz = quizService.getQuizById(quizId);
        setTitle("Take Quiz: " + (quiz == null ? "" : quiz.getString("title")));
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        if (quiz == null) {
            setContentText("Quiz not found");
            return;
        }

        GridPane g = new GridPane();
        g.setHgap(8); g.setVgap(8); g.setPadding(new Insets(10));

        List<Document> questions = quiz.getList("questions", Document.class, List.of());
        Map<String, Object> inputControls = new LinkedHashMap<>();

        int row = 0;
        for (Document q : questions) {
            String qid = q.getString("id");
            String text = q.getString("text");
            String type = q.getString("type");
            g.add(new Label((row+1) + ". " + text), 0, row++);
            if ("mcq".equalsIgnoreCase(type)) {
                List<String> opts = q.getList("options", String.class);
                ToggleGroup tg = new ToggleGroup();
                VBox vbx = new VBox(4);
                for (String opt : opts) {
                    RadioButton rb = new RadioButton(opt);
                    rb.setToggleGroup(tg);
                    vbx.getChildren().add(rb);
                }
                inputControls.put(qid, tg);
                g.add(vbx, 0, row++);
            } else {
                TextArea ta = new TextArea();
                ta.setPrefRowCount(3);
                inputControls.put(qid, ta);
                g.add(ta, 0, row++);
            }
        }

        // Timer display area
        HBox timerBox = new HBox(8);
        Text timerText = new Text();
        timerBox.getChildren().add(new Label("Time left:"));
        timerBox.getChildren().add(timerText);
        g.add(timerBox, 0, row++);

        getDialogPane().setContent(g);

        // Determine time limit (minutes)
        int timeLimit = quiz.getInteger("timeLimitMinutes", 0);
        if (timeLimit > 0) {
            remainingSeconds = timeLimit * 60;
            timerText.setText(formatRemaining(remainingSeconds));
            // Create timeline to tick every second
            timer = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
                remainingSeconds--;
                if (remainingSeconds < 0) {
                    // time's up -> auto-submit
                    timer.stop();
                    // force submission on JavaFX thread
                    Platform.runLater(() -> {
                        new Alert(Alert.AlertType.INFORMATION, "Time is up — auto-submitting the quiz.").showAndWait();
                        // programmatically click OK result
                        submitAnswersAndClose(inputControls);
                    });
                } else {
                    timerText.setText(formatRemaining(remainingSeconds));
                }
            }));
            timer.setCycleCount(Timeline.INDEFINITE);
            // Start timer when dialog shown
            this.setOnShown(ev -> timer.play());
            this.setOnHidden(ev -> { if (timer != null) timer.stop(); });
        }

        // On OK button, gather answers and submit
        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return submitAnswers(inputControls);
            }
            // CANCEL case -> return null
            if (timer != null) timer.stop();
            return null;
        });
    }

    private String submitAnswers(Map<String, Object> inputControls) {
        Map<String, Object> answers = new HashMap<>();
        for (Map.Entry<String, Object> en : inputControls.entrySet()) {
            String qid = en.getKey();
            Object ctl = en.getValue();
            if (ctl instanceof ToggleGroup tg) {
                Toggle sel = tg.getSelectedToggle();
                String val = null;
                if (sel instanceof RadioButton rb) val = rb.getText();
                answers.put(qid, val == null ? "" : val);
            } else if (ctl instanceof TextArea ta) {
                answers.put(qid, ta.getText().trim());
            }
        }
        String studentId = AuthSession.getInstance().getLinkedEntityId();
        if (studentId == null || studentId.isBlank()) {
            new Alert(Alert.AlertType.ERROR, "Your user is not linked to a student entity").showAndWait();
            return null;
        }
        try {
            String attemptId = quizService.submitAttempt(quizId, studentId, answers);
            Document attempt = quizService.getAttemptById(attemptId);
            double score = attempt.getDouble("score");
            double max = attempt.getDouble("maxScore");
            double percent = attempt.getDouble("percent");
            new Alert(Alert.AlertType.INFORMATION, "Quiz submitted. Score: " + score + " / " + max + " (" + String.format("%.1f", percent) + "%)").showAndWait();
            if (timer != null) timer.stop();
            return attemptId;
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Submit failed: " + ex.getMessage()).showAndWait();
            return null;
        }
    }

    /**
     * Helper used by auto-submit (called when time expires)
     */
    private void submitAnswersAndClose(Map<String, Object> inputControls) {
        submitAnswers(inputControls);
        // close dialog by firing Close request on owner window
        Window w = this.getDialogPane().getScene().getWindow();
        w.hide();
    }

    private String formatRemaining(int secs) {
        int m = secs / 60;
        int s = secs % 60;
        return String.format("%02d:%02d", Math.max(0, m), Math.max(0, s));
    }
}
