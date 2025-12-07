package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.security.PermissionService;
import edu.agile.sis.service.ConversationService;
import edu.agile.sis.service.MessageService;
import edu.agile.sis.service.StaffService;
import edu.agile.sis.dao.UserDAO;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Platform;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MessagesController {
    private final BorderPane view = new BorderPane();
    private final MessageService messageService = new MessageService();
    private final ConversationService conversationService = new ConversationService();
    private final StaffService staffService = new StaffService();
    private final PermissionService permissionService = new PermissionService();
    private final UserDAO userDao = new UserDAO();

    private final ListView<String> leftList = new ListView<>();
    private final VBox chatBox = new VBox(10);
    private final ScrollPane chatScroll = new ScrollPane(chatBox);

    private final boolean isStudent = AuthSession.getInstance().hasRole("Student");
    private final boolean isStaff = AuthSession.getInstance().hasRole("Professor") || AuthSession.getInstance().hasRole("TA");
    private final String currentUsername = AuthSession.getInstance().getUsername();
    private final String linkedEntityId = AuthSession.getInstance().getLinkedEntityId();

    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm");
    private final Map<String, String> nameCache = new ConcurrentHashMap<>();
    private final LinkedHashMap<String, String> displayToStorage = new LinkedHashMap<>();
    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor();
    private static final int MAX_PREVIEWS = 50;

    public MessagesController() {
        Label title = new Label("💬 Messaging");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        BorderPane.setMargin(title, new Insets(10, 0, 10, 10));

        leftList.setPlaceholder(new Label("No messages yet."));
        leftList.setPrefWidth(300);

        chatScroll.setFitToWidth(true);
        chatScroll.setStyle("-fx-background: #f9f9f9;");
        chatBox.setPadding(new Insets(10));

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

        SplitPane mainSplit = new SplitPane(leftList, chatPane);
        mainSplit.setDividerPositions(0.28);

        view.setTop(title);
        view.setCenter(mainSplit);

        leftList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            chatBox.getChildren().clear();
            if (newV == null) return;
            String storage = displayToStorage.getOrDefault(newV, newV);
            String[] parts = storage.split("\\|", 2);
            String storageStudentId = parts.length > 0 ? parts[0] : "";
            String staffId = parts.length > 1 ? parts[1] : "";

            if (isStudent) {
                if (storageStudentId == null || storageStudentId.isBlank()) {
                    String staff = staffId.isBlank() ? newV : staffId;
                    loadThreadForStudent(staff);
                } else {
                    loadThreadForStudent(storageStudentId);
                }
            } else if (isStaff) {
                String studentIdToLoad = storageStudentId;
                if (studentIdToLoad == null || studentIdToLoad.isBlank()) {
                    chatBox.getChildren().add(new Label("Unable to determine which student/parent to load."));
                    return;
                }
                loadThreadForStaff(studentIdToLoad);
            } else {
                if (staffId != null && !staffId.isBlank()) loadThreadForStudent(staffId);
                else loadThreadForStudent(storageStudentId);
            }
        });

        sendBtn.setOnAction(e -> {
            String sel = leftList.getSelectionModel().getSelectedItem();
            if (sel == null) {
                new Alert(Alert.AlertType.INFORMATION, "Select a conversation first.").showAndWait();
                return;
            }
            String body = composer.getText().trim();
            if (body.isEmpty()) return;

            String storage = displayToStorage.getOrDefault(sel, sel);
            String[] parts = storage.split("\\|", 2);
            String storageStudentId = parts.length > 0 ? parts[0] : "";
            String staffId = parts.length > 1 ? parts[1] : "";

            try {
                if (isStudent) {
                    String targetStaffId = (storageStudentId != null && !storageStudentId.isBlank()) ? storageStudentId : staffId;
                    if (targetStaffId == null || targetStaffId.isBlank()) {
                        new Alert(Alert.AlertType.WARNING, "Cannot determine staff to send to.").showAndWait();
                        return;
                    }
                    if (linkedEntityId == null || linkedEntityId.isBlank()) {
                        new Alert(Alert.AlertType.WARNING, "Your account is not linked to a student record.").showAndWait();
                        return;
                    }
                    if (!permissionService.studentCanMessageStaff(linkedEntityId, targetStaffId)) {
                        new Alert(Alert.AlertType.WARNING, "You are not allowed to message this staff member.").showAndWait();
                        return;
                    }
                    messageService.sendMessage(linkedEntityId, targetStaffId, AuthSession.getInstance().getUsername(), body);
                    loadThreadForStudent(targetStaffId);
                } else if (isStaff) {
                    String studentIdForStorage = storageStudentId;
                    String staffIdForSender = (linkedEntityId != null && !linkedEntityId.isBlank()) ? linkedEntityId : currentUsername;
                    if (studentIdForStorage == null || studentIdForStorage.isBlank()) {
                        new Alert(Alert.AlertType.WARNING, "Cannot determine the participant to send to.").showAndWait();
                        return;
                    }
                    messageService.sendMessage(studentIdForStorage, staffIdForSender, currentUsername, body);
                    loadThreadForStaff(studentIdForStorage);
                } else {
                    String targetStaffId = (storageStudentId != null && !storageStudentId.isBlank()) ? storageStudentId : staffId;
                    if (targetStaffId == null || targetStaffId.isBlank()) {
                        new Alert(Alert.AlertType.WARNING, "Cannot determine staff to send to.").showAndWait();
                        return;
                    }
                    String studentId = linkedEntityId == null ? currentUsername : linkedEntityId;
                    messageService.sendMessage(studentId, targetStaffId, currentUsername, body);
                    loadThreadForStudent(targetStaffId);
                }
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
                    String storageKey = item;
                    String id = storageKey.contains("|") ? storageKey.split("\\|", 2)[0] : storageKey;
                    String cached = nameCache.get(id);
                    String text;
                    if (cached != null && !cached.isBlank()) text = cached + " (" + id + ")";
                    else text = id + " (loading...)";
                    setText(text);
                    setTooltip(new Tooltip(text));
                }
            }
        });

        loadConversationsAsync();
    }

    private void loadConversationsAsync() {
        Platform.runLater(() -> leftList.setPlaceholder(new ProgressIndicator()));
        bgExecutor.submit(() -> {
            populateLeftListBlocking();
            Platform.runLater(() -> {
                if (leftList.getItems().isEmpty()) leftList.setPlaceholder(new Label("No messages yet."));
                else leftList.setPlaceholder(new Label("")); // clear placeholder when items exist
            });
        });
    }

    private void populateLeftListBlocking() {
        if (isStudent) {
            List<Document> staff = Collections.emptyList();
            try { staff = staffService.listAll(); } catch (Throwable ignored) { staff = Collections.emptyList(); }

            List<String> keys = new ArrayList<>();
            displayToStorage.clear();
            for (Document s : staff) {
                String id = Optional.ofNullable(s.getString("staffId"))
                        .orElseGet(() -> Optional.ofNullable(s.getString("linkedEntityId")).orElse(""));
                String name = Optional.ofNullable(s.getString("name"))
                        .orElse(Optional.ofNullable(s.getString("fullName")).orElse(id));
                if (id == null || id.isBlank()) continue;
                String storageKey = id;
                keys.add(storageKey);
                displayToStorage.put(storageKey, storageKey);
                nameCache.put(id, name);
            }

            Platform.runLater(() -> {
                leftList.setItems(FXCollections.observableArrayList(keys));
                if (!leftList.getItems().isEmpty()) leftList.getSelectionModel().selectFirst();
            });
            return;
        }

        String staffId = linkedEntityId == null || linkedEntityId.isBlank() ? currentUsername : linkedEntityId;
        List<Document> previews = Collections.emptyList();
        try { previews = conversationService.listThreadsForStaff(staffId); } catch (Throwable ignored) { previews = Collections.emptyList(); }

        previews.sort((a, b) -> {
            Date ta = (Date) Optional.ofNullable(a.get("latestAt")).orElse(new Date(0));
            Date tb = (Date) Optional.ofNullable(b.get("latestAt")).orElse(new Date(0));
            return tb.compareTo(ta);
        });
        if (previews.size() > MAX_PREVIEWS) previews = previews.subList(0, MAX_PREVIEWS);

        LinkedHashSet<String> partnerIdsOrdered = new LinkedHashSet<>();
        Map<String, String> storageForPartner = new LinkedHashMap<>();
        for (Document p : previews) {
            String studentId = Optional.ofNullable(p.getString("studentId")).orElse("");
            Document latest = (Document) p.get("latest");
            String partnerId = "";
            if (studentId != null && !studentId.isBlank()) partnerId = studentId;
            else if (latest != null && latest.getString("senderId") != null) {
                String sender = latest.getString("senderId");
                if (sender != null && !sender.isBlank() && !sender.equals(staffId)) partnerId = sender;
            }
            if (partnerId == null || partnerId.isBlank() || partnerId.equals(staffId)) continue;
            if (partnerIdsOrdered.contains(partnerId)) continue;
            partnerIdsOrdered.add(partnerId);
            String storageKey = partnerId + "|" + staffId;
            storageForPartner.put(partnerId, storageKey);
        }

        List<String> storageKeys = partnerIdsOrdered.stream()
                .map(pid -> storageForPartner.get(pid))
                .collect(Collectors.toList());

        displayToStorage.clear();
        for (String k : storageKeys) displayToStorage.put(k, k);

        Platform.runLater(() -> {
            leftList.setItems(FXCollections.observableArrayList(storageKeys));
            if (!leftList.getItems().isEmpty()) leftList.getSelectionModel().selectFirst();
        });

        List<String> idsForLookup = new ArrayList<>(partnerIdsOrdered);
        for (String pid : idsForLookup) {
            Document u = userDao.findByLinkedEntityId(pid);
            String display = null;
            if (u != null) display = Optional.ofNullable(u.getString("username")).orElse(null);
            if (display == null || display.isBlank()) display = pid;
            nameCache.put(pid, display);
        }

        Platform.runLater(leftList::refresh);
    }

    private void loadThreadForStudent(String staffId) {
        if (staffId == null || staffId.isBlank()) {
            chatBox.getChildren().clear();
            return;
        }
        List<Document> list = messageService.getThread(linkedEntityId, staffId);
        displayMessages(list);
    }

    private void loadThreadForStaff(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            chatBox.getChildren().clear();
            return;
        }
        String staffId = (linkedEntityId != null && !linkedEntityId.isBlank()) ? linkedEntityId : currentUsername;
        List<Document> list = messageService.getThread(studentId, staffId);
        displayMessages(list);
    }

    private void displayMessages(List<Document> list) {
        chatBox.getChildren().clear();
        if (list == null) return;

        String conversationStudentId = null;
        if (isStaff) {
            String sel = leftList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                String raw = displayToStorage.get(sel);
                if (raw != null) conversationStudentId = raw.split("\\|", 2)[0];
            }
        } else if (isStudent) {
            conversationStudentId = linkedEntityId;
        } else {
            String sel = leftList.getSelectionModel().getSelectedItem();
            if (sel != null) conversationStudentId = displayToStorage.get(sel);
        }

        Set<String> seen = new HashSet<>();
        for (Document m : list) {
            Object rawIdObj = m.get("_id");
            String msgId;
            if (rawIdObj != null) msgId = rawIdObj.toString();
            else {
                Date dt = m.getDate("createdAt");
                long ts = dt == null ? 0L : dt.getTime();
                msgId = safeStr(m.getString("senderId")) + "|" + safeStr(m.getString("receiverId")) + "|" + ts + "|" + safeStr(m.getString("body"));
            }
            if (seen.contains(msgId)) continue;
            seen.add(msgId);

            String senderId = safeStr(m.getString("senderId"));
            String studentIdField = safeStr(m.getString("studentId"));

            if (conversationStudentId != null && !conversationStudentId.isBlank()) {
                if (!conversationStudentId.equals(studentIdField)) continue;
            }

            if (isStudent) {
                if (studentIdField != null && !studentIdField.isBlank() && !studentIdField.equals(linkedEntityId)) continue;
            }

            String body = safeStr(m.getString("body"));
            Date created = m.getDate("createdAt");
            String time = (created != null) ? timeFmt.format(created) : "";

            Label msg = new Label(body);
            msg.setWrapText(true);
            msg.setMaxWidth(400);
            msg.setPadding(new Insets(8, 12, 8, 12));

            Label timestamp = new Label(time);
            timestamp.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
            VBox bubble = new VBox(msg, timestamp);
            bubble.setSpacing(2);

            HBox row = new HBox();
            row.setPadding(new Insets(4, 10, 4, 10));
            row.setMaxWidth(Double.MAX_VALUE);

            boolean myMsg = false;
            if (linkedEntityId != null && !linkedEntityId.isBlank() && Objects.equals(senderId, linkedEntityId)) myMsg = true;
            if (!myMsg && currentUsername != null && Objects.equals(senderId, currentUsername)) myMsg = true;

            if (myMsg) {
                msg.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 15;");
                row.setAlignment(Pos.CENTER_RIGHT);
                row.getChildren().add(bubble);
            } else {
                msg.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: black; -fx-background-radius: 15;");
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
        return (o == null) ? "" : o.toString();
    }

    public BorderPane getView() {
        return view;
    }
}
