package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.security.PermissionService;
import edu.agile.sis.service.MessageService;
import edu.agile.sis.service.ParentService;
import edu.agile.sis.service.StaffService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Platform;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class ParentMessagesController {
    private final BorderPane view = new BorderPane();
    private final MessageService messageService = new MessageService();
    private final ParentService parentService = new ParentService();
    private final StaffService staffService = new StaffService();
    private final PermissionService permissionService = new PermissionService();

    private final ListView<String> leftList = new ListView<>();
    private final VBox chatBox = new VBox(10);
    private final ScrollPane chatScroll = new ScrollPane(chatBox);

    private final String currentUsername = AuthSession.getInstance().getUsername();
    private final String parentId = AuthSession.getInstance().getLinkedEntityId();

    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm");
    // mapping: displayKey -> staffId (displayKey is what is shown in leftList items)
    private final LinkedHashMap<String, String> displayToStaff = new LinkedHashMap<>();
    private final ConcurrentMap<String, String> staffNameCache = new ConcurrentHashMap<>();

    private final ExecutorService bg = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "parent-msg-loader");
        t.setDaemon(true);
        return t;
    });

    public ParentMessagesController() {
        Label title = new Label("👪 Parent Messaging");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        BorderPane.setMargin(title, new Insets(10, 0, 10, 10));
        view.setTop(title);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(36, 36);
        leftList.setPlaceholder(spinner);
        leftList.setPrefWidth(320);

        chatScroll.setFitToWidth(true);
        chatScroll.setStyle("-fx-background: #fbfbfb;");
        chatBox.setPadding(new Insets(10));

        TextField composer = new TextField();
        composer.setPromptText("Type a message to staff...");
        composer.setPrefWidth(420);
        Button sendBtn = new Button("Send ▶");
        sendBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox composerBox = new HBox(8, composer, sendBtn);
        composerBox.setPadding(new Insets(10));
        composerBox.setAlignment(Pos.CENTER_LEFT);
        composerBox.setStyle("-fx-background-color: #ecf0f1;");

        VBox chatPane = new VBox(chatScroll, composerBox);
        VBox.setVgrow(chatScroll, Priority.ALWAYS);

        SplitPane mainSplit = new SplitPane(leftList, chatPane);
        mainSplit.setDividerPositions(0.33);
        view.setCenter(mainSplit);

        leftList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) {
                chatBox.getChildren().clear();
                return;
            }
            String staffId = displayToStaff.get(newV);
            if (staffId == null || staffId.isBlank()) {
                chatBox.getChildren().clear();
                chatBox.getChildren().add(new Label("Cannot resolve conversation. Select another staff."));
                return;
            }
            loadParentThreadWithStaff(staffId);
        });

        sendBtn.setOnAction(e -> {
            String sel = leftList.getSelectionModel().getSelectedItem();
            if (sel == null) {
                new Alert(Alert.AlertType.INFORMATION, "Select a conversation (staff) first.").showAndWait();
                return;
            }
            String body = composer.getText().trim();
            if (body.isEmpty()) return;

            String staffId = displayToStaff.get(sel);
            if (staffId == null || staffId.isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Cannot determine staff for this thread.").showAndWait();
                return;
            }

            try {
                try {
                    java.lang.reflect.Method m = permissionService.getClass().getMethod("parentCanMessageStaff", String.class, String.class);
                    Object res = m.invoke(permissionService, parentId, staffId);
                    if (res instanceof Boolean && !((Boolean) res)) {
                        new Alert(Alert.AlertType.WARNING, "You are not allowed to message this staff.").showAndWait();
                        return;
                    }
                } catch (NoSuchMethodException ignored) { }

                String senderId = (parentId != null && !parentId.isBlank()) ? parentId : currentUsername;
                String messageStudentId = senderId;

                messageService.sendMessage(messageStudentId, staffId, senderId, body);

                loadParentThreadWithStaff(staffId);
                composer.clear();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Failed to send: " + ex.getMessage()).showAndWait();
            }
        });

        leftList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                    setGraphic(null);
                } else {
                    String staffId = displayToStaff.get(item);
                    String name = staffNameCache.get(staffId);
                    String text;
                    if (name != null && !name.isBlank()) {
                        text = name + " (" + staffId + ")";
                    } else {
                        text = staffId;
                    }
                    setText(text);
                    setTooltip(new Tooltip(text));
                }
            }
        });

        bg.submit(this::populateLeftList);
    }

    private void populateLeftList() {
        displayToStaff.clear();

        if (parentId == null || parentId.isBlank()) {
            Platform.runLater(() -> leftList.setItems(FXCollections.observableArrayList()));
            return;
        }

        List<Document> msgs;
        try {
            msgs = messageService.getByStudent(parentId);
        } catch (Throwable t) {
            msgs = Collections.emptyList();
        }

     
        LinkedHashSet<String> staffIdsWithMessages = new LinkedHashSet<>();
        if (msgs != null) {
            for (Document m : msgs) {
                String staffId = Optional.ofNullable(m.getString("staffId")).orElse("");
                if (staffId == null || staffId.isBlank()) continue;
                staffIdsWithMessages.add(staffId);
            }
        }

        List<String> storageKeysOrdered = new ArrayList<>();
        
        for (String staffId : staffIdsWithMessages) {
            String displayKey = staffId; 
           
            int dup = 1;
            String base = displayKey;
            while (displayToStaff.containsKey(displayKey)) displayKey = base + "-" + (++dup);
            displayToStaff.put(displayKey, staffId);
            storageKeysOrdered.add(displayKey);
        }

      
        List<Document> allStaff = Collections.emptyList();
        try { allStaff = staffService.listAll(); } catch (Throwable ignored) { allStaff = Collections.emptyList(); }

        for (Document s : allStaff) {
            String staffId = Optional.ofNullable(s.getString("staffId"))
                    .orElseGet(() -> Optional.ofNullable(s.getString("linkedEntityId")).orElse(""));
            if (staffId == null || staffId.isBlank()) continue;
            if (displayToStaff.containsValue(staffId)) continue; 
            String displayKey = staffId;
            int dup = 1;
            String base = displayKey;
            while (displayToStaff.containsKey(displayKey)) displayKey = base + "-" + (++dup);
            displayToStaff.put(displayKey, staffId);
            storageKeysOrdered.add(displayKey);
        }

        Platform.runLater(() -> {
            leftList.setItems(FXCollections.observableArrayList(storageKeysOrdered));
            if (!leftList.getItems().isEmpty()) leftList.getSelectionModel().selectFirst();
        });

    
        List<String> staffIds = new ArrayList<>(displayToStaff.values()).stream()
                .filter(Objects::nonNull)
                .filter(sid -> !sid.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (!staffIds.isEmpty()) {
            for (String sid : staffIds) {
                String name = null;
                try {
                    Document sd = staffService.getById(sid);
                    if (sd == null) sd = staffService.getByStaffId(sid);
                    if (sd != null) name = Optional.ofNullable(sd.getString("name")).orElse(sd.getString("fullName"));
                } catch (Throwable ignored) { }
                if (name == null || name.isBlank()) name = sid;
                staffNameCache.put(sid, name);
            }
            Platform.runLater(leftList::refresh);
        }
    }

    private void loadParentThreadWithStaff(String staffId) {
        if (parentId == null || parentId.isBlank() || staffId == null || staffId.isBlank()) {
            chatBox.getChildren().clear();
            return;
        }
        List<Document> list;
        try {
            list = messageService.getThread(parentId, staffId);
        } catch (Throwable t) {
            list = Collections.emptyList();
        }
        displayParentThread(list, staffId);
    }

    private void displayParentThread(List<Document> list, String staffId) {
        chatBox.getChildren().clear();
        if (list == null || list.isEmpty()) return;

        Set<String> seen = new HashSet<>();
        for (Document m : list) {
            Object idObj = m.get("_id");
            String dedupeKey = idObj != null ? idObj.toString() :
                    (safeStr(m.getString("senderId")) + "|" + safeStr(m.getString("receiverId")) + "|" + safeStr(m.getString("createdAt")));
            if (seen.contains(dedupeKey)) continue;
            seen.add(dedupeKey);

            String senderId = safeStr(m.getString("senderId"));
            String msgStudentId = safeStr(m.getString("studentId"));
            String msgStaffId = safeStr(m.getString("staffId"));

            if (!Objects.equals(msgStudentId, parentId)) continue;
            if (!Objects.equals(msgStaffId, staffId)) continue;

            String body = safeStr(m.getString("body"));
            Date created = m.getDate("createdAt");
            String time = (created != null) ? timeFmt.format(created) : "";

            Label msg = new Label(body);
            msg.setWrapText(true);
            msg.setMaxWidth(420);
            msg.setPadding(new Insets(8, 12, 8, 12));

            Label timestamp = new Label(time);
            timestamp.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
            VBox bubble = new VBox(msg, timestamp);
            bubble.setSpacing(2);

            HBox row = new HBox();
            row.setPadding(new Insets(4, 10, 4, 10));
            row.setMaxWidth(Double.MAX_VALUE);

            boolean myMsg = Objects.equals(senderId, parentId);

            if (myMsg) {
                msg.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-background-radius: 15;");
                row.setAlignment(Pos.CENTER_RIGHT);
                row.getChildren().add(bubble);
            } else {
                msg.setStyle("-fx-background-color: #ffffff; -fx-text-fill: black; -fx-background-radius: 15;");
                row.setAlignment(Pos.CENTER_LEFT);
                row.getChildren().add(bubble);
            }

            chatBox.getChildren().add(row);
        }

        Platform.runLater(() -> {
            try {
                if (chatScroll != null) chatScroll.setVvalue(1.0);
            } catch (Throwable ignored) { }
        });
    }

    private static String safeStr(Object o) {
        return o == null ? "" : o.toString();
    }

    public BorderPane getView() {
        return view;
    }

    public void shutdown() {
        bg.shutdownNow();
    }
}
