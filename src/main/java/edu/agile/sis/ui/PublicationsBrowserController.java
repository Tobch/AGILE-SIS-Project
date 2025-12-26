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
import java.util.*;

/**
 * JavaFX Controller for browsing faculty publications (public view).
 * Anyone can search and view published research papers.
 */
public class PublicationsBrowserController {
    private final VBox view = new VBox(20);
    private final PublicationService publicationService = new PublicationService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private TextField searchField;
    private ComboBox<String> typeFilter;
    private ComboBox<String> yearFilter;
    private TableView<Document> table;

    public PublicationsBrowserController() {
        view.setPadding(new Insets(20));
        view.setStyle("-fx-background-color: #f4f6f7;");

        // Header
        Label header = new Label("📚 Faculty Research Publications");
        header.setFont(Font.font("System", FontWeight.BOLD, 24));
        header.setStyle("-fx-text-fill: #2c3e50;");

        Label subtitle = new Label("Browse and search research papers published by faculty members.");
        subtitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
        subtitle.setWrapText(true);

        // Search and Filters
        HBox filtersRow = createFiltersRow();

        // Table
        table = new TableView<>();
        setupTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        // Card wrapper
        VBox card = new VBox(15, filtersRow, table);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        VBox.setVgrow(card, Priority.ALWAYS);

        view.getChildren().addAll(header, subtitle, card);

        loadPublications();
    }

    private HBox createFiltersRow() {
        searchField = new TextField();
        searchField.setPromptText("🔍 Search by title, author, or keyword...");
        searchField.setPrefWidth(250);

        Button searchBtn = styledButton("Search", "#3498db");
        searchBtn.setOnAction(e -> performSearch());

        // Also search on Enter key
        searchField.setOnAction(e -> performSearch());

        typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("All Types", "Journal Article", "Conference Paper", "Book Chapter", "Thesis",
                "Technical Report");
        typeFilter.setValue("All Types");
        typeFilter.setOnAction(e -> performSearch());

        // Year filter
        yearFilter = new ComboBox<>();
        yearFilter.getItems().add("All Years");
        int currentYear = java.time.Year.now().getValue();
        for (int y = currentYear; y >= 2010; y--) {
            yearFilter.getItems().add(String.valueOf(y));
        }
        yearFilter.setValue("All Years");
        yearFilter.setOnAction(e -> performSearch());

        Button refreshBtn = styledButton("🔄 Refresh", "#7f8c8d");
        refreshBtn.setOnAction(e -> {
            searchField.clear();
            typeFilter.setValue("All Types");
            yearFilter.setValue("All Years");
            loadPublications();
        });

        HBox row = new HBox(10, searchField, searchBtn, typeFilter, yearFilter, refreshBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    @SuppressWarnings("unchecked")
    private void setupTable() {
        TableColumn<Document, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "title")));
        titleCol.setPrefWidth(280);

        TableColumn<Document, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "authorName")));
        authorCol.setPrefWidth(120);

        TableColumn<Document, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "publicationType")));
        typeCol.setPrefWidth(120);

        TableColumn<Document, String> venueCol = new TableColumn<>("Venue");
        venueCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safeGetString(data.getValue(), "venue")));
        venueCol.setPrefWidth(180);

        TableColumn<Document, String> dateCol = new TableColumn<>("Year");
        dateCol.setCellValueFactory(data -> {
            Date d = safeGetDate(data.getValue(), "publicationDate");
            if (d != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        String.valueOf(d.getYear() + 1900));
            }
            return new javafx.beans.property.SimpleStringProperty("—");
        });
        dateCol.setPrefWidth(60);

        table.getColumns().addAll(titleCol, authorCol, typeCol, venueCol, dateCol);

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

    private void loadPublications() {
        try {
            List<Document> pubs = publicationService.getAllPublishedPublications();
            table.setItems(FXCollections.observableArrayList(pubs));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error loading publications: " + e.getMessage());
        }
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        String type = typeFilter.getValue();
        String year = yearFilter.getValue();

        try {
            List<Document> results;

            if (query.isEmpty() && "All Types".equals(type) && "All Years".equals(year)) {
                results = publicationService.getAllPublishedPublications();
            } else if (!query.isEmpty()) {
                results = publicationService.searchPublications(query);
                // Apply filters
                if (!"All Types".equals(type)) {
                    results = filterByType(results, type);
                }
                if (!"All Years".equals(year)) {
                    results = filterByYear(results, Integer.parseInt(year));
                }
            } else {
                results = publicationService.getAllPublishedPublications();
                if (!"All Types".equals(type)) {
                    results = filterByType(results, type);
                }
                if (!"All Years".equals(year)) {
                    results = filterByYear(results, Integer.parseInt(year));
                }
            }

            table.setItems(FXCollections.observableArrayList(results));

            if (results.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "No publications found matching your search.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Search error: " + e.getMessage());
        }
    }

    private List<Document> filterByType(List<Document> docs, String type) {
        List<Document> filtered = new ArrayList<>();
        for (Document doc : docs) {
            if (type.equals(doc.getString("publicationType"))) {
                filtered.add(doc);
            }
        }
        return filtered;
    }

    private List<Document> filterByYear(List<Document> docs, int year) {
        List<Document> filtered = new ArrayList<>();
        for (Document doc : docs) {
            Date pubDate = safeGetDate(doc, "publicationDate");
            if (pubDate != null) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(pubDate);
                if (cal.get(java.util.Calendar.YEAR) == year) {
                    filtered.add(doc);
                }
            }
        }
        return filtered;
    }

    @SuppressWarnings("unchecked")
    private void showPublicationDetails(Document doc) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Publication Details");
        dialog.setHeaderText(safeGetString(doc, "title"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(550);

        VBox content = new VBox(12);
        content.setPadding(new Insets(15));

        // Author info
        Label authorLabel = new Label("👤 " + safeGetString(doc, "authorName"));
        authorLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        content.getChildren().add(authorLabel);

        // Type and venue
        String type = safeGetString(doc, "publicationType");
        String venue = safeGetString(doc, "venue");
        if (!venue.isEmpty()) {
            content.getChildren().add(new Label("📰 " + type + " in " + venue));
        } else {
            content.getChildren().add(new Label("📰 " + type));
        }

        // Date
        Date pubDate = safeGetDate(doc, "publicationDate");
        if (pubDate != null) {
            content.getChildren().add(new Label("📅 " + dateFormat.format(pubDate)));
        }

        // Co-authors
        List<String> coAuthors = doc.get("coAuthors", List.class);
        if (coAuthors != null && !coAuthors.isEmpty()) {
            content.getChildren().add(new Label("👥 Co-authors: " + String.join(", ", coAuthors)));
        }

        // Abstract
        String abstractText = safeGetString(doc, "abstractText");
        if (!abstractText.isEmpty()) {
            content.getChildren().add(new Separator());
            Label absTitle = new Label("Abstract:");
            absTitle.setStyle("-fx-font-weight: bold;");
            Label absText = new Label(abstractText);
            absText.setWrapText(true);
            absText.setStyle("-fx-padding: 10; -fx-background-color: #ecf0f1; -fx-background-radius: 5;");
            content.getChildren().addAll(absTitle, absText);
        }

        // Keywords
        List<String> keywords = doc.get("keywords", List.class);
        if (keywords != null && !keywords.isEmpty()) {
            HBox keywordsBox = new HBox(5);
            keywordsBox.setAlignment(Pos.CENTER_LEFT);
            keywordsBox.getChildren().add(new Label("🏷️ "));
            for (String kw : keywords) {
                Label tag = new Label(kw);
                tag.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                        "-fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 11px;");
                keywordsBox.getChildren().add(tag);
            }
            content.getChildren().add(keywordsBox);
        }

        // DOI and URL
        content.getChildren().add(new Separator());

        String doi = safeGetString(doc, "doi");
        if (!doi.isEmpty()) {
            Hyperlink doiLink = new Hyperlink("DOI: " + doi);
            doiLink.setOnAction(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI("https://doi.org/" + doi));
                } catch (Exception ex) {
                    /* ignore */ }
            });
            content.getChildren().add(doiLink);
        }

        String url = safeGetString(doc, "url");
        if (!url.isEmpty()) {
            Hyperlink urlLink = new Hyperlink("🔗 View Full Paper");
            urlLink.setStyle("-fx-font-size: 14px;");
            urlLink.setOnAction(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Could not open URL: " + url);
                }
            });
            content.getChildren().add(urlLink);
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(450);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.showAndWait();
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
