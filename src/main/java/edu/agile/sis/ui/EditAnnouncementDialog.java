package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.AnnouncementService;
import edu.agile.sis.service.EventService;
import edu.agile.sis.service.FileService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import org.bson.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

public class EditAnnouncementDialog extends Dialog<Boolean> {
    private final AnnouncementService announcementService = new AnnouncementService();
    private final EventService eventService = new EventService();
    private final FileService fileService = new FileService();
    private File chosenImage = null;
    private final Document existingDoc;
    private final boolean isEvent;

    public EditAnnouncementDialog(Document doc) {
        this.existingDoc = doc;
        this.isEvent = doc.getBoolean("isEvent", false);
        setTitle("Edit Post");
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        // Type selection (disabled for edit)
        Label typeLabel = new Label("Post Type:");
        RadioButton rbNews = new RadioButton("Announcement (News)");
        RadioButton rbEvent = new RadioButton("Event (RSVP)");
        rbNews.setSelected(!isEvent);
        rbEvent.setSelected(isEvent);
        rbNews.setDisable(true); // Prevent changing type during edit
        rbEvent.setDisable(true);

        // Shared fields
        TextField title = new TextField(existingDoc.getString("title"));
        TextArea body = new TextArea(isEvent ? existingDoc.getString("description") : existingDoc.getString("body"));
        body.setPromptText("Enter details here...");
        body.setPrefRowCount(4);

        ComboBox<String> category = new ComboBox<>();
        category.getItems().addAll("Academic", "Sports", "Social", "Career");
        category.setValue(existingDoc.getString("category"));

        CheckBox pinned = new CheckBox("Pin this post");
        pinned.setSelected(existingDoc.getBoolean("isPinned", false));

        // Event-specific fields
        DatePicker startDate = new DatePicker();
        Spinner<Integer> startHour = new Spinner<>(0, 23, 12);
        Spinner<Integer> startMin = new Spinner<>(0, 59, 0);
        if (isEvent) {
            Date startAt = existingDoc.getDate("startAt");
            if (startAt != null) {
                LocalDateTime ldt = startAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                startDate.setValue(ldt.toLocalDate());
                startHour.getValueFactory().setValue(ldt.getHour());
                startMin.getValueFactory().setValue(ldt.getMinute());
            }
        }

        // Image upload (simplified, reuse from create dialog)
        Button chooseImageBtn = new Button("Choose Image");
        Label imageLabel = new Label("No image selected");
        chooseImageBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File f = fc.showOpenDialog(getDialogPane().getScene().getWindow());
            if (f != null) {
                chosenImage = f;
                imageLabel.setText(f.getName());
            }
        });

        // Layout
        grid.add(typeLabel, 0, 0);
        HBox typeBox = new HBox(10, rbNews, rbEvent);
        grid.add(typeBox, 1, 0);

        grid.add(new Label("Title:"), 0, 1);
        grid.add(title, 1, 1);

        grid.add(new Label("Details:"), 0, 2);
        grid.add(body, 1, 2);

        grid.add(new Label("Category:"), 0, 3);
        grid.add(category, 1, 3);

        grid.add(pinned, 1, 4);

        if (isEvent) {
            grid.add(new Label("Start Date:"), 0, 5);
            grid.add(startDate, 1, 5);
            HBox timeBox = new HBox(5, new Label("Time:"), startHour, new Label(":"), startMin);
            grid.add(timeBox, 1, 6);
        }

        grid.add(chooseImageBtn, 0, 7);
        grid.add(imageLabel, 1, 7);

        getDialogPane().setContent(grid);

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    Document updates = new Document("title", title.getText())
                            .append("category", category.getValue())
                            .append("isPinned", pinned.isSelected());

                    if (isEvent) {
                        updates.append("description", body.getText());
                        if (startDate.getValue() != null) {
                            LocalDateTime ldt = LocalDateTime.of(startDate.getValue(), LocalTime.of(startHour.getValue(), startMin.getValue()));
                            updates.append("startAt", Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()));
                        }
                    } else {
                        updates.append("body", body.getText());
                    }

                    // Handle image update (if new image chosen)
                    if (chosenImage != null) {
                        try (InputStream is = new FileInputStream(chosenImage)) {
                            String fileId = fileService.upload(is, chosenImage.getName(), "image/jpeg", new Document("uploadedBy", AuthSession.getInstance().getUsername()));
                            updates.append("thumbnailFileId", fileId);
                        }
                    }

                    // Update in DB
                    String id = existingDoc.getObjectId("_id").toHexString();
                    boolean success = isEvent ? eventService.updateEvent(id, updates) : announcementService.update(id, updates);
                    return success;
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to update: " + ex.getMessage());
                    alert.showAndWait();
                    return false;
                }
            }
            return false;
        });
    }
}