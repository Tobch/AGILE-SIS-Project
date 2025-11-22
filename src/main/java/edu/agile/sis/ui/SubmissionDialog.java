package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.SubmissionService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.bson.Document;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Dialog to allow a student to submit to an assignment — and to view grade & feedback if already graded.
 */
public class SubmissionDialog extends Dialog<String> {
    private final SubmissionService service = new SubmissionService();
    private final List<Document> attachments = new ArrayList<>();
    private final TextArea answersArea = new TextArea();

    public SubmissionDialog(String assignmentId) {
        setTitle("Submit Assignment");
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        GridPane g = new GridPane();
        g.setHgap(8); g.setVgap(8); g.setPadding(new Insets(10));

        // First, check if the logged-in student already has a submission for this assignment
        String linkedStudentId = AuthSession.getInstance().getLinkedEntityId();

        Document existing = null;
        if (linkedStudentId != null && !linkedStudentId.isBlank()) {
            existing = service.getSubmissionForStudent(assignmentId, linkedStudentId);
        }

        // If an existing submission exists, show its grade & feedback
        if (existing != null) {
            Object gradeObj = existing.get("grade");
            Object feedbackObj = existing.get("feedback");
            String gradeText = gradeObj == null ? "Not graded yet" : gradeObj.toString();
            String feedbackText = feedbackObj == null ? "(no feedback yet)" : feedbackObj.toString();

            Label infoLabel = new Label("Previous submission found:");
            Label gradeLabel = new Label("Grade: " + gradeText);
            TextArea feedbackArea = new TextArea(feedbackText);
            feedbackArea.setEditable(false);
            feedbackArea.setWrapText(true);
            feedbackArea.setPrefRowCount(4);

            g.add(infoLabel, 0, 0);
            g.add(gradeLabel, 0, 1);
            g.add(new Label("Feedback:"), 0, 2);
            g.add(feedbackArea, 1, 2);
        }

        Button attachBtn = new Button("Attach File");
        attachBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(getOwner());
            if (f == null) return;
            try {
                Document ref = service.uploadAttachmentFromPath(f.getAbsolutePath(), f.getName(), null);
                attachments.add(ref);
                attachBtn.setText("Attachments: " + attachments.size());
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Upload failed: " + ex.getMessage()).showAndWait();
            }
        });

        answersArea.setPrefRowCount(8);

        g.add(new Label("Answers / Notes:"), 0, 6); g.add(answersArea, 1, 6);
        g.add(new Label("Attachments:"), 0, 7); g.add(attachBtn, 1, 7);

        getDialogPane().setContent(g);

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String studentId = AuthSession.getInstance().getLinkedEntityId();
                if (studentId == null || studentId.isBlank()) {
                    new Alert(Alert.AlertType.ERROR, "Your account is not linked to a student entity").showAndWait();
                    return null;
                }
                Document answers = new Document("text", answersArea.getText().trim());
                try {
                    String subId = service.submit(assignmentId, studentId, attachments, answers);
                    return subId;
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Submit failed: " + ex.getMessage()).showAndWait();
                    return null;
                }
            }
            return null;
        });
    }
}
