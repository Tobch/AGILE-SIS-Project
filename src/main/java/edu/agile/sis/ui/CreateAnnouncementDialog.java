package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.AnnouncementService;
import edu.agile.sis.service.EventService; // Import EventService
import edu.agile.sis.service.FileService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.bson.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

public class CreateAnnouncementDialog extends Dialog<Boolean> {
    private final AnnouncementService announcementService = new AnnouncementService();
    private final EventService eventService = new EventService(); // Service for Events
    private File chosenImage = null;

    public CreateAnnouncementDialog() {
        setTitle("Create New Post");
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        // --- TYPE SELECTION ---
        Label typeLabel = new Label("Post Type:");
        RadioButton rbNews = new RadioButton("Announcement (News)");
        RadioButton rbEvent = new RadioButton("Event (RSVP)");
        rbNews.setSelected(true); // Default
        ToggleGroup typeGroup = new ToggleGroup();
        rbNews.setToggleGroup(typeGroup);
        rbEvent.setToggleGroup(typeGroup);
        
        // --- SHARED FIELDS ---
        TextField title = new TextField();
        TextArea body = new TextArea(); // Acts as 'Body' for news, 'Description' for events
        body.setPromptText("Enter details here...");
        body.setPrefRowCount(4);
        
        ComboBox<String> cat = new ComboBox<>();
        cat.getItems().addAll("Academic", "Sports", "Social", "Career");
        cat.setValue("Academic");

        // --- ANNOUNCEMENT ONLY FIELDS ---
        CheckBox pin = new CheckBox("Pin to top");
        Button browse = new Button("Choose Image (optional)");
        Label imgName = new Label();

        browse.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.getExtensionFilters().addAll(new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File f = fc.showOpenDialog(this.getOwner());
            if (f != null) {
                chosenImage = f;
                imgName.setText(f.getName());
            }
        });

        // --- EVENT ONLY FIELDS ---
        DatePicker startDate = new DatePicker(LocalDate.now());
        TextField startTime = new TextField("09:00"); // Simple text for time (HH:mm)
        DatePicker endDate = new DatePicker(LocalDate.now());
        TextField endTime = new TextField("10:00");
        TextField location = new TextField();
        Spinner<Integer> capacity = new Spinner<>(1, 1000, 50);
        
        // Layout containers for visibility toggling
        VBox eventFields = new VBox(10);
        eventFields.getChildren().addAll(
            new Label("Start Date & Time (HH:mm):"),
            new HBox(10, startDate, startTime),
            new Label("End Date & Time (HH:mm):"),
            new HBox(10, endDate, endTime),
            new Label("Location:"), location,
            new Label("Capacity:"), capacity
        );
        eventFields.setVisible(false); // Hidden by default
        eventFields.setManaged(false); // Don't take up space

        VBox newsFields = new VBox(10);
        newsFields.getChildren().addAll(browse, imgName, pin);

        // Toggle Logic
        rbNews.setOnAction(e -> {
            eventFields.setVisible(false);
            eventFields.setManaged(false);
            newsFields.setVisible(true);
            newsFields.setManaged(true);
            body.setPromptText("Enter announcement body...");
        });
        
        rbEvent.setOnAction(e -> {
            newsFields.setVisible(false);
            newsFields.setManaged(false);
            eventFields.setVisible(true);
            eventFields.setManaged(true);
            body.setPromptText("Enter event description...");
        });

        // Add to Grid
        grid.add(typeLabel, 0, 0);
        grid.add(new HBox(10, rbNews, rbEvent), 1, 0);
        
        grid.add(new Label("Title:"), 0, 1); grid.add(title, 1, 1);
        grid.add(new Label("Category:"), 0, 2); grid.add(cat, 1, 2);
        grid.add(new Label("Details:"), 0, 3); grid.add(body, 1, 3);
        
        // Add variable fields
        grid.add(newsFields, 1, 4);
        grid.add(eventFields, 1, 5);

        getDialogPane().setContent(grid);

        // --- SUBMISSION LOGIC ---
        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    String createdBy = AuthSession.getInstance().getUsername();
                    
                    if (rbNews.isSelected()) {
                        // === CREATE ANNOUNCEMENT ===
                        InputStream imgStream = null;
                        String imgNameStr = null;
                        String contentType = null;
                        if (chosenImage != null) {
                            imgStream = new FileInputStream(chosenImage);
                            imgNameStr = chosenImage.getName();
                            contentType = "image/" + (imgNameStr.endsWith(".png") ? "png" : "jpeg");
                        }
                        announcementService.createAnnouncement(
                                title.getText().trim(), 
                                body.getText().trim(), 
                                cat.getValue(), 
                                pin.isSelected(),
                                imgStream, imgNameStr, contentType, createdBy
                        );
                    } else {
                        // === CREATE EVENT ===
                        Document eventDoc = new Document();
                        eventDoc.append("title", title.getText().trim());
                        eventDoc.append("description", body.getText().trim());
                        eventDoc.append("category", cat.getValue());
                        eventDoc.append("location", location.getText());
                        eventDoc.append("capacity", capacity.getValue());
                        eventDoc.append("createdBy", createdBy);
                        
                        // Parse Dates
                        LocalDateTime start = LocalDateTime.of(startDate.getValue(), LocalTime.parse(startTime.getText()));
                        LocalDateTime end = LocalDateTime.of(endDate.getValue(), LocalTime.parse(endTime.getText()));
                        
                        eventDoc.append("startAt", Date.from(start.atZone(ZoneId.systemDefault()).toInstant()));
                        eventDoc.append("endAt", Date.from(end.atZone(ZoneId.systemDefault()).toInstant()));
                        
                        eventService.createEvent(eventDoc);
                    }
                    return true;
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage()).showAndWait();
                    return false;
                }
            }
            return null;
        });
    }
}