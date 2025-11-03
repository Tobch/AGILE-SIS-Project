package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.security.PermissionService;
import edu.agile.sis.service.MessageService;
import edu.agile.sis.service.StaffService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import javafx.application.Platform;

public class MessagesController {
    private final BorderPane view = new BorderPane();
    private final MessageService messageService = new MessageService();
    private final StaffService staffService = new StaffService();
    private final PermissionService permissionService = new PermissionService();

    private final ListView<String> leftList = new ListView<>();
    private final VBox chatBox = new VBox(10); // chat bubbles
    private final ObservableList<Document> msgItems = FXCollections.observableArrayList();

    private final boolean isStudent = AuthSession.getInstance().hasRole("Student");
    private final boolean isStaff = AuthSession.getInstance().hasRole("Professor") || AuthSession.getInstance().hasRole("TA");
    private final String currentUsername = AuthSession.getInstance().getUsername();
    private final String linkedEntityId = AuthSession.getInstance().getLinkedEntityId();

    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm");

    public MessagesController() {
        // Title
        Label title = new Label("💬 Messaging");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        BorderPane.setMargin(title, new Insets(10, 0, 10, 10));

        // Sidebar
        if (isStudent) {
            List<Document> staff = staffService.listAll();
            List<String> staffRows = staff.stream()
                    .map(s -> s.getString("staffId") + " - " + Optional.ofNullable(s.getString("name")).orElse(""))
                    .collect(Collectors.toList());
            leftList.setItems(FXCollections.observableArrayList(staffRows));
        } else if (isStaff) {
            List<Document> byStaff = messageService.getByStaff(linkedEntityId == null ? "" : linkedEntityId);
            Set<String> studentIds = new LinkedHashSet<>();
            if (byStaff != null) {
                for (Document m : byStaff) {
                    Object sId = m.get("studentId");
                    if (sId != null) studentIds.add(sId.toString());
                }
            }
            leftList.setItems(FXCollections.observableArrayList(studentIds));
        } else {
            List<Document> staff = staffService.listAll();
            List<String> staffRows = staff.stream()
                    .map(s -> s.getString("staffId") + " - " + Optional.ofNullable(s.getString("name")).orElse(""))
                    .collect(Collectors.toList());
            leftList.setItems(FXCollections.observableArrayList(staffRows));
        }

        leftList.setPrefWidth(220);

        // Chat area
        ScrollPane chatScroll = new ScrollPane(chatBox);
        chatScroll.setFitToWidth(true);
        chatScroll.setStyle("-fx-background: #f9f9f9;");
        chatBox.setPadding(new Insets(10));

        // Composer
        TextField composer = new TextField();
        composer.setPromptText("Type a message...");
        composer.setPrefWidth(400);
        Button sendBtn = new Button("Send ▶");
        sendBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox composerBox = new HBox(8, composer, sendBtn);
        composerBox.setPadding(new Insets(10));
        composerBox.setAlignment(Pos.CENTER_LEFT);
        composerBox.setStyle("-fx-background-color: #ecf0f1;");

        VBox chatPane = new VBox(chatScroll, composerBox);
        VBox.setVgrow(chatScroll, Priority.ALWAYS);

        // Layout
        SplitPane mainSplit = new SplitPane(leftList, chatPane);
        mainSplit.setDividerPositions(0.25);

        view.setTop(title);
        view.setCenter(mainSplit);

        // List selection
        leftList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            chatBox.getChildren().clear();
            if (isStudent) {
                String staffId = newV.split("\\s*-\\s*", 2)[0].trim();
                loadThreadForStudent(staffId);
            } else if (isStaff) {
                String studentId = newV.split("\\s*-\\s*", 2)[0].trim();
                loadThreadForStaff(studentId);
            }
        });

        // Send action
        sendBtn.setOnAction(e -> {
            String sel = leftList.getSelectionModel().getSelectedItem();
            if (sel == null) {
                new Alert(Alert.AlertType.INFORMATION, "Select a conversation first.").showAndWait();
                return;
            }
            String target = sel.split("\\s*-\\s*", 2)[0].trim();
            String body = composer.getText().trim();
            if (body.isEmpty()) return;

            try {
                if (isStudent) {
                    if (linkedEntityId == null || linkedEntityId.isBlank()) {
                        new Alert(Alert.AlertType.WARNING, "Your account is not linked to a student record.").showAndWait();
                        return;
                    }
                    if (!permissionService.studentCanMessageStaff(linkedEntityId, target)) {
                        new Alert(Alert.AlertType.WARNING, "You are not allowed to message this staff member.").showAndWait();
                        return;
                    }
                    messageService.sendMessage(linkedEntityId, target, currentUsername, body);
                    loadThreadForStudent(target);
                } else if (isStaff) {
                    messageService.sendMessage(target, linkedEntityId, currentUsername, body);
                    loadThreadForStaff(target);
                }
                composer.clear();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Failed to send: " + ex.getMessage()).showAndWait();
            }
        });
    }

    private void loadThreadForStudent(String staffId) {
        List<Document> list = messageService.getThread(linkedEntityId, staffId);
        displayMessages(list);
    }

    private void loadThreadForStaff(String studentId) {
        List<Document> list = messageService.getThread(studentId, linkedEntityId);
        displayMessages(list);
    }

   private void displayMessages(List<Document> list) {
    chatBox.getChildren().clear();
    if (list == null) return;

    for (Document m : list) {
        String sender = m.getString("senderId");
        String body = m.getString("body");
        Date created = m.getDate("createdAt");
        String time = (created != null) ? timeFmt.format(created) : "";

        // Bubble text
        Label msg = new Label(body);
        msg.setWrapText(true);
        msg.setMaxWidth(280);
        msg.setPadding(new Insets(8, 12, 8, 12));

        // Timestamp
        Label timestamp = new Label(time);
        timestamp.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        VBox bubble = new VBox(msg, timestamp);
        bubble.setSpacing(2);

        HBox row = new HBox();
        row.setPadding(new Insets(4, 10, 4, 10));
        row.setMaxWidth(Double.MAX_VALUE);

        if (Objects.equals(sender, currentUsername)) {
            // My message bubble (blue, right-aligned)
            msg.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 15;");
            row.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().add(bubble);
        } else {
            // Other user message bubble (gray, left-aligned)
            msg.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: black; -fx-background-radius: 15;");
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().add(bubble);
        }

        chatBox.getChildren().add(row);
    }

    // Auto scroll to bottom
    Platform.runLater(() -> {
        ScrollPane sp = (ScrollPane) chatBox.getParent();
        sp.setVvalue(1.0);
    });
}



    public BorderPane getView() {
        return view;
    }
}
