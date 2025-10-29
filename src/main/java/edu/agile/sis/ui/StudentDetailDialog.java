package edu.agile.sis.ui;

import edu.agile.sis.service.EntityService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * StudentDetailDialog - polished UI for student details
 */
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

        setTitle("Student Details");
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        if (readOnly) {
            getDialogPane().getButtonTypes().clear();
            getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        }

        buildContent();
        populateFromDocument();

        // handle OK (save)
        Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        if (okBtn != null) {
            okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
                if (!validateAndSave()) {
                    ev.consume();
                }
            });
        }
    }

    private void buildContent() {
        // --- Profile Info Section ---
        GridPane profileGrid = new GridPane();
        profileGrid.setHgap(12);
        profileGrid.setVgap(12);
        profileGrid.setPadding(new Insets(10));

        entityIdField.setDisable(true);
        firstNameField.setDisable(readOnly);
        lastNameField.setDisable(readOnly);
        emailField.setDisable(readOnly);
        enrolledSincePicker.setDisable(readOnly);

        profileGrid.add(new Label("Entity ID:"), 0, 0);
        profileGrid.add(entityIdField, 1, 0);
        profileGrid.add(new Label("First Name:"), 0, 1);
        profileGrid.add(firstNameField, 1, 1);
        profileGrid.add(new Label("Last Name:"), 0, 2);
        profileGrid.add(lastNameField, 1, 2);
        profileGrid.add(new Label("Email:"), 0, 3);
        profileGrid.add(emailField, 1, 3);
        profileGrid.add(new Label("Enrolled Since:"), 0, 4);
        profileGrid.add(enrolledSincePicker, 1, 4);

        TitledPane profilePane = new TitledPane("Profile Information", profileGrid);
        profilePane.setCollapsible(false);

        // --- Attributes Section ---
        attributesListView.setPrefHeight(180);
        attributesListView.setMouseTransparent(readOnly);
        attributesListView.setFocusTraversable(!readOnly);

        Button addAttrBtn = new Button("➕ Add");
        Button editAttrBtn = new Button("✏️ Edit");
        Button delAttrBtn = new Button("🗑 Delete");

        addAttrBtn.setDisable(readOnly);
        editAttrBtn.setDisable(readOnly);
        delAttrBtn.setDisable(readOnly);

        addAttrBtn.setOnAction(e -> addAttribute());
        editAttrBtn.setOnAction(e -> editSelectedAttribute());
        delAttrBtn.setOnAction(e -> deleteSelectedAttribute());

        HBox attrBtns = new HBox(10, addAttrBtn, editAttrBtn, delAttrBtn);
        attrBtns.setAlignment(Pos.CENTER_RIGHT);
        attrBtns.setPadding(new Insets(6));

        VBox attrBox = new VBox(8,
                new Label("Custom Attributes (Key → Value)"),
                attributesListView,
                attrBtns
        );
        attrBox.setPadding(new Insets(10));
        attrBox.setStyle("-fx-background-color: #fdfdfd; -fx-border-color: #ddd; -fx-border-radius: 6; -fx-background-radius: 6;");

        TitledPane attrPane = new TitledPane("Attributes", attrBox);
        attrPane.setCollapsible(false);

        // --- Layout Root ---
        VBox root = new VBox(15, profilePane, attrPane);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #fafafa;");

        getDialogPane().setContent(root);
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
            String display = (value instanceof List)
                    ? key + " → " + ((List<?>) value)
                    : key + " → " + (value == null ? "" : value.toString());
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
            showAlert("Entity ID missing");
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

    // Attribute editor inner dialog (same as before, just styled nicer)
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
            g.setPadding(new Insets(12));

            g.add(new Label("Key:"), 0, 0);
            g.add(keyField, 1, 0);
            g.add(new Label("Value (comma-separated for list):"), 0, 1);
            g.add(valueField, 1, 1);
            g.add(new Label("Version:"), 0, 2);
            g.add(versionField, 1, 2);

            if (existing != null) {
                keyField.setText(existing.getString("key"));
                Object v = existing.get("value");
                if (v instanceof List<?> list) valueField.setText(String.join(",", list.stream().map(Object::toString).toList()));
                else valueField.setText(v == null ? "" : v.toString());
                versionField.setText(String.valueOf(existing.getInteger("version", 1)));
            }

            getDialogPane().setContent(g);

            setResultConverter(btn -> {
                if (btn == ButtonType.OK) {
                    String key = keyField.getText().trim();
                    if (key.isEmpty()) return null;

                    String rawVal = valueField.getText().trim();
                    Object val = rawVal.contains(",")
                            ? Arrays.stream(rawVal.split("\\s*,\\s*")).filter(s -> !s.isBlank()).toList()
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
