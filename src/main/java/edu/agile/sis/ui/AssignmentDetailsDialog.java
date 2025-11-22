package edu.agile.sis.ui;

import edu.agile.sis.util.FileStorageUtil;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.bson.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

public class AssignmentDetailsDialog extends Dialog<Void> {
    public AssignmentDetailsDialog(Document assignmentDoc) {
        setTitle("Assignment Details - " + assignmentDoc.getString("title"));
        getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        GridPane g = new GridPane();
        g.setHgap(8); g.setVgap(8); g.setPadding(new Insets(10));

        g.add(new Label("Course:"), 0, 0); g.add(new Label(assignmentDoc.getString("courseCode")), 1, 0);
        g.add(new Label("Title:"), 0, 1); g.add(new Label(assignmentDoc.getString("title")), 1, 1);
        g.add(new Label("Description:"), 0, 2);
        TextArea desc = new TextArea(assignmentDoc.getString("description"));
        desc.setEditable(false);
        desc.setWrapText(true);
        desc.setPrefRowCount(6);
        g.add(desc, 1, 2);

        g.add(new Label("Attachments:"), 0, 3);
        @SuppressWarnings("unchecked")
        List<Document> attachments = (List<Document>) assignmentDoc.get("attachments");
        if (attachments == null || attachments.isEmpty()) {
            g.add(new Label("(none)"), 1, 3);
        } else {
            VBox v = new VBox(6);
            for (Document a : attachments) {
                String fname = a.getString("filename");
                String storageRef = a.getString("storageRef");
                Button dl = new Button("Download: " + fname);
                dl.setOnAction(ev -> {
                    FileChooser fc = new FileChooser();
                    fc.setInitialFileName(fname);
                    File dest = fc.showSaveDialog(getOwner());
                    if (dest == null) return;
                    try (InputStream is = FileStorageUtil.getFileStream(storageRef);
                         FileOutputStream fos = new FileOutputStream(dest)) {
                        byte[] buf = new byte[8192];
                        int r;
                        while ((r = is.read(buf)) > 0) fos.write(buf, 0, r);
                        new Alert(Alert.AlertType.INFORMATION, "Saved file: " + dest.getAbsolutePath()).showAndWait();
                    } catch (Exception ex) {
                        new Alert(Alert.AlertType.ERROR, "Download failed: " + ex.getMessage()).showAndWait();
                    }
                });
                v.getChildren().add(dl);
            }
            g.add(v, 1, 3);
        }

        getDialogPane().setContent(g);
    }
}
