package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.AnnouncementService;
import edu.agile.sis.service.EventService;
import edu.agile.sis.service.RSVPService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.bson.Document;

import java.util.List;

public class EventDetailDialog extends Dialog<Void> {

    private final AnnouncementService annService = new AnnouncementService();
    private final EventService eventService = new EventService();
    private final RSVPService rsvpService = new RSVPService();

    public EventDetailDialog(String idHex, boolean isEvent) {

        setTitle("Details");
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));

        Document doc = isEvent
                ? eventService.getEventById(idHex)
                : annService.getAnnouncement(idHex);

        if (doc == null) {
            grid.add(new Label("Item not found"), 0, 0);
            getDialogPane().setContent(grid);
            return;
        }

        // --- CHANGE 1: Safe ID Extraction ---
        // Use .toString() on the object to handle both ObjectId and String formats safely
        String safeId = doc.get("_id").toString();

        grid.add(new Label("Title:"), 0, 0);
        grid.add(new Label(doc.getString("title")), 1, 0);

        grid.add(new Label("Category:"), 0, 1);
        grid.add(new Label(doc.getString("category")), 1, 1);

        if (isEvent) {

            grid.add(new Label("When:"), 0, 2);
            // Handling null dates gracefully just in case
            Object start = doc.get("startAt");
            Object end = doc.get("endAt");
            grid.add(new Label(start + " - " + end), 1, 2);

            grid.add(new Label("Location:"), 0, 3);
            grid.add(new Label(doc.getString("location")), 1, 3);

            boolean isStudent = AuthSession.getInstance().hasRole("Student");
            String linkedId = AuthSession.getInstance().getLinkedEntityId();

            Button rsvpBtn = new Button("RSVP");
            
            // --- CHANGE 2: Check if already attending ---
            boolean alreadyAttending = false;
            if (isStudent && linkedId != null) {
                alreadyAttending = rsvpService.isAttending(safeId, linkedId);
            }

            if (alreadyAttending) {
                rsvpBtn.setText("Registered ✅");
                rsvpBtn.setDisable(true);
            } else {
                rsvpBtn.setDisable(!isStudent || linkedId == null);
            }

            rsvpBtn.setOnAction(e -> {
                try {
                    rsvpService.rsvp(
                            safeId, // Use the safe ID
                            linkedId,
                            AuthSession.getInstance().getUsername()
                    );
                    rsvpBtn.setText("Registered ✅");
                    rsvpBtn.setDisable(true);
                    new Alert(Alert.AlertType.INFORMATION, "RSVP recorded successfully").showAndWait();
                    
                    // Optional: Refresh attendee count immediately here if you wanted
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Failed to RSVP: " + ex.getMessage()).showAndWait();
                }
            });

            grid.add(rsvpBtn, 1, 4);

            try {
                // --- CHANGE 3: Use Safe ID for looking up attendees ---
                List<Document> attendees = rsvpService.listAttendees(safeId);
                grid.add(new Label("Attendees:"), 0, 5);
                grid.add(new Label(String.valueOf(attendees.size())), 1, 5);
            } catch (Exception ignored) {
                grid.add(new Label("Attendees:"), 0, 5);
                grid.add(new Label("0"), 1, 5);
            }

        } else {
            // Logic for Announcements
            grid.add(new Label("Posted:"), 0, 2);
            grid.add(new Label(String.valueOf(doc.get("createdAt"))), 1, 2);

            grid.add(new Label("Body:"), 0, 3);
            Label body = new Label(doc.getString("body"));
            body.setWrapText(true);
            grid.add(body, 1, 3);
        }

        getDialogPane().setContent(grid);
    }
}