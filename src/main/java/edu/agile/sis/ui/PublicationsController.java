package edu.agile.sis.ui;

import edu.agile.sis.service.PublicationService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * JavaFX Controller for Professors to manage their research publications.
 */
public class PublicationsController {
    private final VBox view = new VBox(20);
    private final PublicationService publicationService = new PublicationService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private TableView<Document> table;

    public PublicationsController() {
        view.setPadding(new Insets(20));
        view.setStyle("-fx-background-color: #f4f6f7;");

        // Header
        Label header = new Label("📄 My Research Publications");
        header.setFont(Font.font("System", FontWeight.BOLD, 24));
        header.setStyle("-fx-text-fill: #2c3e50;");

        Label subtitle = new Label(
                "Manage and publish your research papers, journal articles, and conference presentations.");
        subtitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
        subtitle.setWrapText(true);

        // Table
        table = new TableView<>();
        setupTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        // Action Buttons
        HBox actionButtons = createActionButtons();

        // Card wrapper
        VBox card = new VBox(15, table, actionButtons);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        VBox.setVgrow(card, Priority.ALWAYS);

        view.getChildren().addAll(header, subtitle, card);

        loadPublications();
    }

    @SuppressWarnings("unchecked")
    private void setupTable() {
        TableColumn<Document, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "title")));
        titleCol.setPrefWidth(250);

        TableColumn<Document, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "publicationType")));
        typeCol.setPrefWidth(120);

        TableColumn<Document, String> venueCol = new TableColumn<>("Venue");
        venueCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "venue")));
        venueCol.setPrefWidth(180);

        TableColumn<Document, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> {
            Date d = safeGetDate(data.getValue(), "publicationDate");
            return new javafx.beans.property.SimpleStringProperty(d != null ? dateFormat.format(d) : "—");
        });
        dateCol.setPrefWidth(100);

        TableColumn<Document, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> {
            boolean published = data.getValue().getBoolean("published", false);
            return new javafx.beans.property.SimpleStringProperty(published ? "Published" : "Draft");
        });
        statusCol.setPrefWidth(80);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("Published".equals(status)) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    }
                }
            }
        });

        table.getColumns().addAll(titleCol, typeCol, venueCol, dateCol, statusCol);

        // Double-click to view details
        table.setRowFactory(tv -> {
            TableRow<Document> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showPublicationDetails(row.getItem());
                }
            });
            return row;
        });
    }

    private HBox createActionButtons() {
        Button addBtn = styledButton("➕ Add Publication", "#27ae60");
        Button editBtn = styledButton("✏️ Edit", "#3498db");
        Button publishBtn = styledButton("📢 Toggle Publish", "#9b59b6");
        Button deleteBtn = styledButton("🗑️ Delete", "#e74c3c");
        Button refreshBtn = styledButton("🔄 Refresh", "#7f8c8d");

        addBtn.setOnAction(e -> showAddEditDialog(null));
        editBtn.setOnAction(e -> {
            Document selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showAddEditDialog(selected);
            } else {
                showAlert(Alert.AlertType.WARNING, "Please select a publication to edit.");
            }
        });
        publishBtn.setOnAction(e -> handleTogglePublish());
        deleteBtn.setOnAction(e -> handleDelete());
        refreshBtn.setOnAction(e -> loadPublications());

        HBox box = new HBox(10, addBtn, editBtn, publishBtn, deleteBtn, refreshBtn);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void loadPublications() {
        try {
            List<Document> pubs = publicationService.getMyPublications();
            table.setItems(FXCollections.observableArrayList(pubs));
        } catch (SecurityException e) {
            showAlert(Alert.AlertType.ERROR, "Access denied: " + e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error loading publications: " + e.getMessage());
        }
    }

    private void showAddEditDialog(Document existing) {
        Dialog<Document> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Publication" : "Edit Publication");
        dialog.setHeaderText(existing == null ? "Enter publication details" : "Update publication details");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(550);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField();
        titleField.setPromptText("Publication Title");
        titleField.setPrefWidth(350);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Journal Article", "Conference Paper", "Book Chapter", "Thesis",
                "Technical Report");
        typeCombo.setValue("Journal Article");

        TextField venueField = new TextField();
        venueField.setPromptText("Journal or Conference Name");

        DatePicker datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now());

        TextArea abstractArea = new TextArea();
        abstractArea.setPromptText("Abstract (optional)");
        abstractArea.setPrefRowCount(4);
        abstractArea.setWrapText(true);

        TextField doiField = new TextField();
        doiField.setPromptText("DOI (e.g., 10.1000/xyz123)");

        TextField urlField = new TextField();
        urlField.setPromptText("URL link to paper");

        TextField keywordsField = new TextField();
        keywordsField.setPromptText("Keywords (comma-separated)");

        TextField coAuthorsField = new TextField();
        coAuthorsField.setPromptText("Co-authors (comma-separated)");

        // Populate if editing
        if (existing != null) {
            titleField.setText(safeGetString(existing, "title"));
            typeCombo.setValue(safeGetString(existing, "publicationType"));
            venueField.setText(safeGetString(existing, "venue"));
            Date pubDate = safeGetDate(existing, "publicationDate");
            if (pubDate != null) {
                datePicker.setValue(pubDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            }
            abstractArea.setText(safeGetString(existing, "abstractText"));
            doiField.setText(safeGetString(existing, "doi"));
            urlField.setText(safeGetString(existing, "url"));

            @SuppressWarnings("unchecked")
            List<String> keywords = existing.get("keywords", List.class);
            if (keywords != null) {
                keywordsField.setText(String.join(", ", keywords));
            }

            @SuppressWarnings("unchecked")
            List<String> coAuthors = existing.get("coAuthors", List.class);
            if (coAuthors != null) {
                coAuthorsField.setText(String.join(", ", coAuthors));
            }
        }

        int row = 0;
        grid.add(new Label("Title:*"), 0, row);
        grid.add(titleField, 1, row++);
        grid.add(new Label("Type:"), 0, row);
        grid.add(typeCombo, 1, row++);
        grid.add(new Label("Venue:"), 0, row);
        grid.add(venueField, 1, row++);
        grid.add(new Label("Date:"), 0, row);
        grid.add(datePicker, 1, row++);
        grid.add(new Label("Abstract:"), 0, row);
        grid.add(abstractArea, 1, row++);
        grid.add(new Label("DOI:"), 0, row);
        grid.add(doiField, 1, row++);
        grid.add(new Label("URL:"), 0, row);
        grid.add(urlField, 1, row++);
        grid.add(new Label("Keywords:"), 0, row);
        grid.add(keywordsField, 1, row++);
        grid.add(new Label("Co-Authors:"), 0, row);
        grid.add(coAuthorsField, 1, row);

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        dialog.getDialogPane().setContent(scrollPane);

        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                String title = titleField.getText().trim();
                if (title.isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Title is required!");
                    return null;
                }

                List<String> keywords = parseCommaSeparated(keywordsField.getText());
                List<String> coAuthors = parseCommaSeparated(coAuthorsField.getText());
                Date pubDate = datePicker.getValue() != null
                        ? Date.from(datePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant())
                        : null;

                try {
                    if (existing == null) {
                        publicationService.createPublication(
                                title, typeCombo.getValue(), venueField.getText().trim(),
                                pubDate, abstractArea.getText().trim(), doiField.getText().trim(),
                                urlField.getText().trim(), keywords, coAuthors);
                        showAlert(Alert.AlertType.INFORMATION, "Publication added successfully!");
                    } else {
                        String pubId = getIdString(existing);
                        publicationService.updatePublication(pubId,
                                title, typeCombo.getValue(), venueField.getText().trim(),
                                pubDate, abstractArea.getText().trim(), doiField.getText().trim(),
                                urlField.getText().trim(), keywords, coAuthors);
                        showAlert(Alert.AlertType.INFORMATION, "Publication updated successfully!");
                    }
                    loadPublications();
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void handleTogglePublish() {
        Document selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Please select a publication.");
            return;
        }

        boolean isPublished = selected.getBoolean("published", false);
        String message = isPublished ? "This will unpublish the paper and hide it from public view."
                : "This will publish the paper and make it visible to everyone.";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(isPublished ? "Unpublish?" : "Publish?");
        confirm.setHeaderText(safeGetString(selected, "title"));
        confirm.setContentText(message);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    publicationService.togglePublished(getIdString(selected));
                    showAlert(Alert.AlertType.INFORMATION,
                            isPublished ? "Publication unpublished." : "Publication is now public!");
                    loadPublications();
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error: " + e.getMessage());
                }
            }
        });
    }

    private void handleDelete() {
        Document selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Please select a publication to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Publication");
        confirm.setHeaderText("Delete: " + safeGetString(selected, "title") + "?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    publicationService.deletePublication(getIdString(selected));
                    showAlert(Alert.AlertType.INFORMATION, "Publication deleted.");
                    loadPublications();
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error: " + e.getMessage());
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void showPublicationDetails(Document doc) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Publication Details");
        dialog.setHeaderText(safeGetString(doc, "title"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(500);

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        content.getChildren().add(createDetailRow("Type:", safeGetString(doc, "publicationType")));
        content.getChildren().add(createDetailRow("Venue:", safeGetString(doc, "venue")));

        Date pubDate = safeGetDate(doc, "publicationDate");
        content.getChildren().add(createDetailRow("Date:", pubDate != null ? dateFormat.format(pubDate) : "—"));

        boolean published = doc.getBoolean("published", false);
        Label statusLabel = new Label(published ? "Published ✓" : "Draft (not public)");
        statusLabel.setStyle(published ? "-fx-text-fill: #27ae60; -fx-font-weight: bold;"
                : "-fx-text-fill: #f39c12; -fx-font-weight: bold;");
        content.getChildren().add(createDetailRow("Status:", statusLabel));

        String abstractText = safeGetString(doc, "abstractText");
        if (!abstractText.isEmpty()) {
            Label absLabel = new Label(abstractText);
            absLabel.setWrapText(true);
            absLabel.setStyle("-fx-padding: 10; -fx-background-color: #ecf0f1; -fx-background-radius: 5;");
            content.getChildren().add(new Label("Abstract:"));
            content.getChildren().add(absLabel);
        }

        String doi = safeGetString(doc, "doi");
        if (!doi.isEmpty()) {
            content.getChildren().add(createDetailRow("DOI:", doi));
        }

        String url = safeGetString(doc, "url");
        if (!url.isEmpty()) {
            Hyperlink link = new Hyperlink(url);
            link.setOnAction(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                } catch (Exception ex) {
                    // Ignore
                }
            });
            content.getChildren().add(createDetailRow("URL:", link));
        }

        List<String> coAuthors = doc.get("coAuthors", List.class);
        if (coAuthors != null && !coAuthors.isEmpty()) {
            content.getChildren().add(createDetailRow("Co-Authors:", String.join(", ", coAuthors)));
        }

        List<String> keywords = doc.get("keywords", List.class);
        if (keywords != null && !keywords.isEmpty()) {
            content.getChildren().add(createDetailRow("Keywords:", String.join(", ", keywords)));
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.showAndWait();
    }

    private HBox createDetailRow(String label, String value) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-weight: bold;");
        lbl.setMinWidth(80);
        Label val = new Label(value);
        val.setWrapText(true);
        HBox row = new HBox(10, lbl, val);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox createDetailRow(String label, javafx.scene.Node valueNode) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-weight: bold;");
        lbl.setMinWidth(80);
        HBox row = new HBox(10, lbl, valueNode);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private List<String> parseCommaSeparated(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String[] parts = text.split(",");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private Button styledButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-font-size: 13px; -fx-padding: 8 15; -fx-background-radius: 5;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: derive(" + color + ", -10%); " +
                "-fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 15; -fx-background-radius: 5;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-font-size: 13px; -fx-padding: 8 15; -fx-background-radius: 5;"));
        return btn;
    }

    private String getIdString(Document doc) {
        Object id = doc.get("_id");
        if (id instanceof org.bson.types.ObjectId) {
            return ((org.bson.types.ObjectId) id).toHexString();
        }
        return id != null ? id.toString() : "";
    }

    private String safeGetString(Document doc, String key) {
        Object val = doc.get(key);
        return val != null ? val.toString() : "";
    }

    private Date safeGetDate(Document doc, String key) {
        Object val = doc.get(key);
        return val instanceof Date ? (Date) val : null;
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type, msg);
        alert.showAndWait();
    }

    public VBox getView() {
        return view;
    }
}
