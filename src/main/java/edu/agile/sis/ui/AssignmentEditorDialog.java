package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.AssignmentService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.bson.Document;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Dialog for creating a new assignment. Minimal UI.
 */
public class AssignmentEditorDialog extends Dialog<String> {
    private final TextField courseField = new TextField();
    private final TextField titleField = new TextField();
    private final TextArea descriptionArea = new TextArea();
    private final DatePicker dueDatePicker = new DatePicker();
    private final TextField pointsField = new TextField("100");
    private final List<Document> attachments = new ArrayList<>();
    private final AssignmentService service = new AssignmentService();

    public AssignmentEditorDialog() {
        setTitle("Create Assignment");
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        GridPane g = new GridPane();
        g.setHgap(8); g.setVgap(8); g.setPadding(new Insets(10));
        courseField.setPromptText("CS201 (course code)");
        titleField.setPromptText("Assignment Title");
        descriptionArea.setPrefRowCount(6);

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

        g.add(new Label("Course Code:"), 0, 0); g.add(courseField, 1, 0);
        g.add(new Label("Title:"), 0, 1); g.add(titleField, 1, 1);
        g.add(new Label("Description:"), 0, 2); g.add(descriptionArea, 1, 2);
        g.add(new Label("Due Date:"), 0, 3); g.add(dueDatePicker, 1, 3);
        g.add(new Label("Points:"), 0, 4); g.add(pointsField, 1, 4);
        g.add(new Label("Attachments:"), 0, 5); g.add(attachBtn, 1, 5);

        getDialogPane().setContent(g);

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String course = courseField.getText().trim();
                String title = titleField.getText().trim();
                String desc = descriptionArea.getText().trim();
                LocalDate ld = dueDatePicker.getValue();
                if (course.isEmpty() || title.isEmpty() || ld == null) {
                    new Alert(Alert.AlertType.WARNING, "Course, title and due date are required").showAndWait();
                    return null;
                }
                int pts = 100;
                try { pts = Integer.parseInt(pointsField.getText().trim()); } catch (Exception ignored) {}
                Date due = Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
                String createdBy = AuthSession.getInstance().getUsername();
                String id = service.createAssignment(course, title, desc, due, pts, createdBy, attachments);
                return id;
            }
            return null;
        });
    }
}
