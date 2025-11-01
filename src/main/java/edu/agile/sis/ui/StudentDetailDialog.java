package edu.agile.sis.ui;

import edu.agile.sis.service.EntityService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.util.*;


public class StudentDetailDialog extends Dialog<ButtonType> {
    private final Document studentDoc;
    private final EntityService entityService;
    private final boolean readOnly;

    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd");
    private final TextField entityIdField = new TextField();
    private final TextField firstNameField = new TextField();
    private final TextField lastNameField = new TextField();
    private final TextField emailField = new TextField();
    private final DatePicker enrolledSincePicker = new DatePicker();

    private final ObservableList<Document> attributes = FXCollections.observableArrayList();
    private final ListView<String> attributesListView = new ListView<>();

    public StudentDetailDialog(Document studentDoc, EntityService entityService, boolean readOnly) {
        this.studentDoc = studentDoc;
        this.entityService = entityService;
        this.readOnly = readOnly;

        setTitle("🎓 Student Profile");
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        if (readOnly) {
            getDialogPane().getButtonTypes().clear();
            getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        }

        buildContent();
        populateFromDocument();

        Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        if (okBtn != null) {
            okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
                if (!validateAndSave()) ev.consume();
            });
        }
    }

    private void buildContent() {
      
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(180);
        sidebar.setAlignment(Pos.CENTER);
        sidebar.setStyle("-fx-background-color: linear-gradient(to bottom right, #1976d2, #1565c0);");

        Label nameLabel = new Label("Student");
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        nameLabel.setTextFill(Color.WHITE);

        Label roleLabel = new Label("Profile Details");
        roleLabel.setTextFill(Color.rgb(230, 230, 230));

        sidebar.getChildren().addAll(nameLabel, roleLabel);
        VBox.setMargin(nameLabel, new Insets(20, 0, 5, 0));
        VBox.setMargin(roleLabel, new Insets(0, 0, 15, 0));

        // --- Info Card (Right Side) ---
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        Label idLabel = new Label("Entity ID:");
        Label fnLabel = new Label("First Name:");
        Label lnLabel = new Label("Last Name:");
        Label emLabel = new Label("Email:");
        Label enLabel = new Label("Enrolled Since:");

        for (Label lbl : List.of(idLabel, fnLabel, lnLabel, emLabel, enLabel)) {
            lbl.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        }

        entityIdField.setDisable(true);
        firstNameField.setDisable(readOnly);
        lastNameField.setDisable(readOnly);
        emailField.setDisable(readOnly);
        enrolledSincePicker.setDisable(readOnly);

        grid.add(idLabel, 0, 0); grid.add(entityIdField, 1, 0);
        grid.add(fnLabel, 0, 1); grid.add(firstNameField, 1, 1);
        grid.add(lnLabel, 0, 2); grid.add(lastNameField, 1, 2);
        grid.add(emLabel, 0, 3); grid.add(emailField, 1, 3);
        grid.add(enLabel, 0, 4); grid.add(enrolledSincePicker, 1, 4);

        VBox infoCard = new VBox(15, grid);
        infoCard.setPadding(new Insets(20));
        infoCard.setStyle("""
            -fx-background-color: white;
            -fx-border-color: #ddd;
            -fx-border-radius: 8;
            -fx-background-radius: 8;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 5, 0, 0, 1);
        """);


        attributesListView.setPrefHeight(140);
        attributesListView.setStyle("""
            -fx-background-color: #fafafa;
            -fx-border-color: #ddd;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
        """);

        Button addBtn = new Button("Add");
        Button editBtn = new Button("Edit");
        Button delBtn = new Button("Delete");

        stylePrimaryButton(addBtn);
        styleSecondaryButton(editBtn);
        styleDangerButton(delBtn);

        addBtn.setDisable(readOnly);
        editBtn.setDisable(readOnly);
        delBtn.setDisable(readOnly);

        addBtn.setOnAction(e -> addAttribute());
        editBtn.setOnAction(e -> editSelectedAttribute());
        delBtn.setOnAction(e -> deleteSelectedAttribute());

        HBox attrBtns = new HBox(10, addBtn, editBtn, delBtn);
        attrBtns.setAlignment(Pos.CENTER_RIGHT);

        VBox attrSection = new VBox(10,
                new Label("Custom Attributes (Key → Value)"),
                attributesListView,
                attrBtns
        );

        attrSection.setPadding(new Insets(10));

        VBox content = new VBox(20, infoCard, attrSection);
        content.setPadding(new Insets(20, 30, 20, 30));
        content.setAlignment(Pos.TOP_CENTER);

     
        HBox root = new HBox(sidebar, content);
        root.setStyle("-fx-background-color: #f7f9fc;");
        HBox.setHgrow(content, Priority.ALWAYS);

        getDialogPane().setContent(root);
        getDialogPane().setPrefWidth(700);
    }

    private void stylePrimaryButton(Button b) {
        b.setStyle("""
            -fx-background-color: #1976d2;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-background-radius: 6;
        """);
    }

    private void styleSecondaryButton(Button b) {
        b.setStyle("""
            -fx-background-color: #eeeeee;
            -fx-text-fill: #333;
            -fx-background-radius: 6;
        """);
    }

    private void styleDangerButton(Button b) {
        b.setStyle("""
            -fx-background-color: #e53935;
            -fx-text-fill: white;
            -fx-background-radius: 6;
        """);
    }

    
    private void populateFromDocument() {
        if (studentDoc == null) return;
        Document core = studentDoc.get("core", Document.class);
        if (core != null) {
            entityIdField.setText(core.getString("entityId"));
            firstNameField.setText(core.getString("firstName"));
            lastNameField.setText(core.getString("lastName"));
            emailField.setText(core.getString("email"));
            Date enrolled = core.getDate("enrolledSince");
            if (enrolled != null) {
                enrolledSincePicker.setValue(new java.sql.Date(enrolled.getTime()).toLocalDate());
            }
        }
        List<Document> attrs = studentDoc.getList("attributes", Document.class, new ArrayList<>());
        attributes.clear();
        if (attrs != null) attributes.addAll(attrs);
        refreshAttrListView();
    }

    private void refreshAttrListView() {
        attributesListView.getItems().clear();
        for (Document a : attributes) {
            String key = a.getString("key");
            Object value = a.get("value");
            String display = key + " → " + (value == null ? "" : value.toString());
            attributesListView.getItems().add(display);
        }
    }

    private void addAttribute() {
        AttributeEditorDialog dlg = new AttributeEditorDialog(null);
        dlg.showAndWait().ifPresent(doc -> {
            if (doc.get("version") == null) doc.put("version", 1);
            attributes.add(doc);
            refreshAttrListView();
        });
    }

    private void editSelectedAttribute() {
        int idx = attributesListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        AttributeEditorDialog dlg = new AttributeEditorDialog(attributes.get(idx));
        dlg.showAndWait().ifPresent(doc -> {
            attributes.set(idx, doc);
            refreshAttrListView();
        });
    }

    private void deleteSelectedAttribute() {
        int idx = attributesListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            attributes.remove(idx);
            refreshAttrListView();
        }
    }

    private boolean validateAndSave() {
        String eid = entityIdField.getText();
        if (eid == null || eid.isBlank()) {
            showAlert("Entity ID is missing!");
            return false;
        }

        Map<String, Object> coreUpdates = new HashMap<>();
        coreUpdates.put("firstName", firstNameField.getText().trim());
        coreUpdates.put("lastName", lastNameField.getText().trim());
        coreUpdates.put("email", emailField.getText().trim());
        if (enrolledSincePicker.getValue() != null) {
            coreUpdates.put("enrolledSince", java.sql.Date.valueOf(enrolledSincePicker.getValue()));
        }

        return entityService.updateEntityMerge(eid, coreUpdates, new ArrayList<>(attributes));
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }

    // --- Inner Dialog for Attributes ---
    private static class AttributeEditorDialog extends Dialog<Document> {
        private final TextField keyField = new TextField();
        private final TextField valueField = new TextField();
        private final TextField versionField = new TextField("1");

        public AttributeEditorDialog(Document existing) {
            setTitle(existing == null ? "Add Attribute" : "Edit Attribute");
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            GridPane g = new GridPane();
            g.setHgap(12);
            g.setVgap(12);
            g.setPadding(new Insets(15));

            g.add(new Label("Key:"), 0, 0);
            g.add(keyField, 1, 0);
            g.add(new Label("Value (comma-separated for list):"), 0, 1);
            g.add(valueField, 1, 1);
            g.add(new Label("Version:"), 0, 2);
            g.add(versionField, 1, 2);

            if (existing != null) {
                keyField.setText(existing.getString("key"));
                Object v = existing.get("value");
                valueField.setText(v == null ? "" : v.toString());
                versionField.setText(String.valueOf(existing.getInteger("version", 1)));
            }

            getDialogPane().setContent(g);

            setResultConverter(btn -> {
                if (btn == ButtonType.OK) {
                    String key = keyField.getText().trim();
                    if (key.isEmpty()) return null;

                    String rawVal = valueField.getText().trim();
                    Object val = rawVal.contains(",")
                            ? Arrays.stream(rawVal.split("\\s*,\\s*"))
                            .filter(s -> !s.isBlank()).toList()
                            : rawVal;

                    int ver = 1;
                    try { ver = Integer.parseInt(versionField.getText().trim()); } catch (Exception ignored) {}

                    return new Document("key", key).append("value", val).append("version", ver);
                }
                return null;
            });
        }
    }
}
