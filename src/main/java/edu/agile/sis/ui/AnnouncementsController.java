package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.AnnouncementService;
import edu.agile.sis.service.EventService;
import edu.agile.sis.service.FileService; // Import FileService
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.bson.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AnnouncementsController {
    private final BorderPane view = new BorderPane();
    private final AnnouncementService announcementService = new AnnouncementService();
    private final EventService eventService = new EventService();
    private final FileService fileService = new FileService(); // 1. Add FileService

    private final ObservableList<Document> items = FXCollections.observableArrayList();
    private final ListView<Document> listView = new ListView<>();

    private String currentCategory = "All";

    public AnnouncementsController() {
        VBox left = new VBox(8);
        left.setPadding(new Insets(10));

        Label title = new Label("Announcements & Events");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        ComboBox<String> catBox = new ComboBox<>();
        catBox.getItems().addAll("All", "Academic", "Sports", "Social", "Career");
        catBox.setValue("All");
        catBox.setOnAction(e -> {
            currentCategory = catBox.getValue();
            refreshFeed();
        });

        Button createBtn = new Button("Create Announcement");
        createBtn.setDisable(!AuthSession.getInstance().hasRole("Admin"));
        createBtn.setOnAction(e -> {
            CreateAnnouncementDialog dlg = new CreateAnnouncementDialog();
            dlg.showAndWait().ifPresent(success -> {
                if (success) refreshFeed();
            });
        });

        left.getChildren().addAll(title, catBox, createBtn);

        // --- 2. UPDATED LIST CELL TO SHOW IMAGES ---
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Document item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                String titleText = item.getString("title");
                String category = item.getString("category");
                boolean isEvent = item.getBoolean("isEvent", false);
                
                // Labels setup
                String dateStr;
                String bodyText;
                String typeLabel;

                if (isEvent) {
                    typeLabel = "[EVENT]";
                    dateStr = "Happens: " + item.get("startAt");
                    bodyText = item.getString("description");
                } else {
                    typeLabel = "[NEWS]";
                    dateStr = "Posted: " + item.get("createdAt");
                    bodyText = item.getString("body");
                }
                if (bodyText == null) bodyText = "";
                String excerpt = bodyText.length() > 140 ? bodyText.substring(0, 140) + "..." : bodyText;

                Label lblTitle = new Label(typeLabel + " " + titleText);
                lblTitle.setStyle("-fx-font-weight:bold");
                Label lblMeta = new Label(category + " • " + dateStr);
                lblMeta.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
                Label lblBody = new Label(excerpt);
                lblBody.setWrapText(true);

                VBox box = new VBox(4);
                box.getChildren().addAll(lblTitle, lblMeta);

                // --- IMAGE DISPLAY LOGIC ---
                String fileId = item.getString("thumbnailFileId");
                if (fileId != null) {
                    try {
                        // Download bytes from GridFS
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        fileService.download(fileId, os);
                        
                        // Convert to JavaFX Image
                        Image img = new Image(new ByteArrayInputStream(os.toByteArray()));
                        ImageView iv = new ImageView(img);
                        
                        // Style the image
                        iv.setFitHeight(150); // Limit height
                        iv.setPreserveRatio(true);
                        
                        box.getChildren().add(iv);
                    } catch (Exception e) {
                        System.err.println("Failed to load image: " + e.getMessage());
                    }
                }

                box.getChildren().add(lblBody);
                setGraphic(box);
            }
        });

        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Document sel = listView.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    EventDetailDialog dlg = new EventDetailDialog(
                        sel.getObjectId("_id").toHexString(), 
                        sel.getBoolean("isEvent", false)
                    );
                    dlg.showAndWait();
                }
            }
        });

        ScrollPane feedPane = new ScrollPane(listView);
        feedPane.setFitToWidth(true);
        Button refreshBtn = new Button("Refresh Feed");
        refreshBtn.setOnAction(e -> refreshFeed());

        VBox center = new VBox(8, feedPane, refreshBtn);
        center.setPadding(new Insets(10));
        view.setLeft(left);
        view.setCenter(center);
        refreshFeed();
    }

    private void refreshFeed() {
        items.clear();
        String catFilter = currentCategory.equals("All") ? null : currentCategory;

        List<Document> announcements = announcementService.listFeed(0, 50, catFilter);
        for(Document d : announcements) d.append("isEvent", false);

        List<Document> events = eventService.listUpcoming(null, 0, 50, catFilter);
        for(Document d : events) d.append("isEvent", true);

        List<Document> merged = new ArrayList<>();
        merged.addAll(announcements);
        merged.addAll(events);

        merged.sort((d1, d2) -> {
            boolean p1 = d1.getBoolean("isPinned", false);
            boolean p2 = d2.getBoolean("isPinned", false);
            if (p1 != p2) return p1 ? -1 : 1;
            Date date1 = d1.getBoolean("isEvent", false) ? d1.getDate("startAt") : d1.getDate("createdAt");
            Date date2 = d2.getBoolean("isEvent", false) ? d2.getDate("startAt") : d2.getDate("createdAt");
            if (date1 == null) return 1;
            if (date2 == null) return -1;
            return date2.compareTo(date1);
        });

        items.addAll(merged);
        listView.getItems().setAll(items);
    }

    public BorderPane getView() { return view; }
}