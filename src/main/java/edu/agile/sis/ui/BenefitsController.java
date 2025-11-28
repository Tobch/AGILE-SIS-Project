package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.BenefitsService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class BenefitsController {
    private final VBox view = new VBox(12);
    private final BenefitsService service = new BenefitsService();

    private final ObservableList<Document> data = FXCollections.observableArrayList();
    private final TableView<Document> table = new TableView<>();

    private final Label lastRefLabel = new Label("");

    public BenefitsController() {
        view.setPadding(new Insets(18));
        view.setStyle("-fx-background-color: #f4f6f8;");

        HBox topBar = buildTopBar();
        view.getChildren().add(topBar);

        boolean isAdmin = AuthSession.getInstance().hasRole("Admin");
        if (isAdmin) {
            VBox adminArea = buildAdminArea();
            view.getChildren().add(adminArea);
            refresh();
        } else {
          
            VBox staffArea = buildStaffArea();
            view.getChildren().add(staffArea);
        }
    }

    private HBox buildTopBar() {
        Label title = new Label("Benefits");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #2c3e50;");

        Label subtitle = new Label("Overview of your assigned benefits");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #7b8a93;");

        VBox left = new VBox(2, title, subtitle);
        left.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = styledButton("\u21bb Refresh", "neutral");
        refreshBtn.setOnAction(e -> {
            refresh();
            lastRefLabel.setText("Last refreshed: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        });

        lastRefLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a98a3;");

        HBox rightBox = new HBox(10, lastRefLabel, refreshBtn);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        HBox topBar = new HBox(12, left, spacer, rightBox);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(4, 0, 6, 0));
        return topBar;
    }

    private VBox buildAdminArea() {
        TextField search = new TextField();
        search.setPromptText("Search staff id, type or details...");
        search.setMinWidth(320);

        Button addBtn = styledButton("Add", "primary");
        Button editBtn = styledButton("Edit", "neutral");
        Button deleteBtn = styledButton("Delete", "danger");

        addBtn.setOnAction(e -> adminAdd());
        editBtn.setOnAction(e -> adminEdit());
        deleteBtn.setOnAction(e -> adminDelete());

        editBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        deleteBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        HBox actions = new HBox(8, search, addBtn, editBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(8, 8, 8, 8));

        TableColumn<Document, String> staffCol = new TableColumn<>("Staff ID");
        staffCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safeString(c.getValue(), "staffId")));
        staffCol.setPrefWidth(140);

        TableColumn<Document, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safeString(c.getValue(), "type")));
        typeCol.setPrefWidth(200);

        TableColumn<Document, String> detailsCol = new TableColumn<>("Details Summary");
        detailsCol.setPrefWidth(420);

        detailsCol.setCellValueFactory(c -> {
            Object raw = c.getValue().get("details");
            if (raw == null) return new javafx.beans.property.SimpleStringProperty("");
            if (raw instanceof Document) {
                Document d = (Document) raw;
                String summary = d.keySet().stream()
                        .limit(3)
                        .map(k -> k + ": " + safeString(d, k))
                        .collect(Collectors.joining(" • "));
                if (d.size() > 3) summary += " …";
                return new javafx.beans.property.SimpleStringProperty(summary);
            } else {
                String s = raw.toString();
                return new javafx.beans.property.SimpleStringProperty(s.length() > 120 ? s.substring(0, 120) + "…" : s);
            }
        });

        detailsCol.setCellFactory(col -> new TableCell<>() {
            private final Label label = new Label();
            {
                label.setWrapText(true);
                label.setMaxWidth(400);
                label.setPadding(new Insets(6, 4, 6, 4));
            }
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setGraphic(null);
                    setTooltip(null);
                    setText(null);
                } else {
                    label.setText(val);
                    setGraphic(label);
                    Document d = getTableView().getItems().get(getIndex());
                    Object raw = d == null ? null : d.get("details");
                    if (raw != null) {
                        Tooltip t = new Tooltip(raw.toString());
                        t.setWrapText(true);
                        t.setMaxWidth(600);
                        setTooltip(t);
                    } else {
                        setTooltip(null);
                    }
                }
            }
        });

        table.getColumns().setAll(staffCol, typeCol, detailsCol);
        table.setItems(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.setRowFactory(tv -> {
            TableRow<Document> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) adminEdit();
            });
            return row;
        });

        search.textProperty().addListener((obs, oldV, newV) -> {
            if (newV == null || newV.isBlank()) {
                table.setItems(data);
                return;
            }
            String q = newV.toLowerCase();
            ObservableList<Document> filtered = data.stream()
                    .filter(d -> safeString(d, "staffId").toLowerCase().contains(q)
                            || safeString(d, "type").toLowerCase().contains(q)
                            || (d.get("details") != null && d.get("details", Document.class).toJson().toLowerCase().contains(q)))
                    .collect(FXCollections::observableArrayList, ObservableList::add, ObservableList::addAll);
            table.setItems(filtered);
        });

        VBox container = new VBox(10, actions, table);
        container.setPadding(new Insets(10));
        container.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e6ecef; -fx-border-radius: 8;");
        return container;
    }


    private VBox buildStaffArea() {
        String staffId = AuthSession.getInstance().getLinkedEntityId();
        if (staffId == null || staffId.isBlank()) staffId = AuthSession.getInstance().getUsername();

        Document ben = service.getBenefitsForStaff(staffId);

        VBox container = new VBox(14);
        container.setPadding(new Insets(10));
        container.setStyle("-fx-background-color: transparent;");

    
        Label header = new Label("Your Benefits");
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #2c3e50;");
        container.getChildren().add(header);

        if (ben == null) {
   
            VBox emptyBox = new VBox(8);
            emptyBox.setAlignment(Pos.CENTER_LEFT);
            emptyBox.setPadding(new Insets(18));
            Label emptyTitle = new Label("You currently have no assigned benefits.");
            emptyTitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
            emptyBox.getChildren().add(emptyTitle);
            container.getChildren().add(emptyBox);
            return container;
        }


        String typeField = safeString(ben, "type");
        List<String> types = Arrays.stream(typeField.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        if (types.isEmpty()) types = Collections.singletonList(typeField.isBlank() ? "Benefit" : typeField);

        Document details = ben.get("details", Document.class);


        FlowPane flow = new FlowPane();
        flow.setHgap(16);
        flow.setVgap(16);
        flow.setPadding(new Insets(6));
        flow.setPrefWrapLength(960);

        for (String t : types) {
            VBox card = new VBox(10);
            card.setPadding(new Insets(14));
            card.setPrefWidth(420);
            card.setMinHeight(72);
  
            card.setStyle(
                    "-fx-background-color: white;" +
                    "-fx-background-radius: 10;" +
                    "-fx-border-radius: 10;" +
                    "-fx-border-color: #e9eef1;"
            );

            DropShadow hoverShadow = new DropShadow();
            hoverShadow.setRadius(6);
            hoverShadow.setOffsetY(1.5);
            hoverShadow.setColor(Color.rgb(0,0,0,0.08));


            HBox cardHeader = new HBox(8);
            cardHeader.setAlignment(Pos.CENTER_LEFT);
            Label icon = new Label("\uD83C\uDF81");
            icon.setStyle("-fx-font-size: 16px;");
            Label title = new Label(t);
            title.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label toggle = new Label("\u25BC"); // down arrow
            toggle.setStyle("-fx-text-fill: #9aa6af; -fx-font-size: 12px;");

            cardHeader.getChildren().addAll(icon, title, spacer, toggle);

   
            VBox detailsBox = new VBox(6);
            detailsBox.setPadding(new Insets(6, 0, 0, 0));


            if (details != null && !details.isEmpty()) {
               
                String targetNorm = normalizeKey(t);
                Optional<String> matchKey = details.keySet().stream()
                        .filter(k -> normalizeKey(k).equals(targetNorm))
                        .findFirst();

                
                if (matchKey.isEmpty()) {
                    matchKey = details.keySet().stream()
                            .filter(k -> {
                                String kn = normalizeKey(k);
                                return kn.contains(targetNorm) || targetNorm.contains(kn);
                            }).findFirst();
                }

                if (matchKey.isPresent()) {
                    Object found = details.get(matchKey.get());
                    if (found instanceof Document) {
                        Document nested = (Document) found;
                        GridPane grid = new GridPane();
                        grid.setHgap(12);
                        grid.setVgap(8);
                        int row = 0;
                        for (String k : nested.keySet()) {
                            Label kLbl = new Label(k + ":");
                            kLbl.setStyle("-fx-font-weight:600; -fx-text-fill:#2f3b45;");
                            Label vLbl = new Label(safeString(nested, k));
                            vLbl.setStyle("-fx-text-fill:#44515a;");
                            vLbl.setWrapText(true);
                            vLbl.setMaxWidth(260);
                            grid.add(kLbl, 0, row);
                            grid.add(vLbl, 1, row);
                            row++;
                        }
                        detailsBox.getChildren().add(grid);
                    } else {
                        Label vLbl = new Label(safeString(details, matchKey.get()));
                        vLbl.setWrapText(true);
                        vLbl.setStyle("-fx-text-fill:#44515a;");
                        detailsBox.getChildren().add(vLbl);
                    }
                } else {

                    GridPane grid = new GridPane();
                    grid.setHgap(12);
                    grid.setVgap(8);
                    int row = 0;
                    for (String k : details.keySet()) {
                        Label kLbl = new Label(k + ":");
                        kLbl.setStyle("-fx-font-weight:600; -fx-text-fill:#2f3b45;");
                        Label vLbl = new Label(safeString(details, k));
                        vLbl.setStyle("-fx-text-fill:#44515a;");
                        vLbl.setWrapText(true);
                        vLbl.setMaxWidth(260);
                        grid.add(kLbl, 0, row);
                        grid.add(vLbl, 1, row);
                        row++;
                    }
                    detailsBox.getChildren().add(grid);
                }
            } else {
                Label no = new Label("No extra details available for this benefit.");
                no.setStyle("-fx-text-fill: #7f8c8d;");
                detailsBox.getChildren().add(no);
            }

            cardHeader.setOnMouseClicked(ev -> {
                boolean showing = detailsBox.isVisible();
                detailsBox.setVisible(!showing);
                detailsBox.setManaged(!showing);
                toggle.setText(showing ? "\u25B6" : "\u25BC"); 
            });

   
            card.setOnMouseEntered(e -> {
                card.setEffect(hoverShadow);
                card.setStyle(
                        "-fx-background-color: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-color: #e1e8ec;" +
                        "-fx-translate-y: -2;"
                );
            });
            card.setOnMouseExited(e -> {
                card.setEffect(null);
                card.setStyle(
                        "-fx-background-color: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-color: #e9eef1;"
                );
            });

       
            detailsBox.setVisible(true);
            detailsBox.setManaged(true);

            card.getChildren().addAll(cardHeader, detailsBox);
            flow.getChildren().add(card);
        }


        ScrollPane sp = new ScrollPane(flow);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        sp.setPrefViewportHeight(380);

        container.getChildren().add(sp);
        return container;
    }

    private void adminAdd() {
        TextInputDialog ask = new TextInputDialog();
        ask.setTitle("Add Benefit - select staff");
        ask.setHeaderText("Enter staffId to add benefit for (e.g., S1001)");
        ask.showAndWait().ifPresent(staffId -> {
            AddBenefitDialog dlg = new AddBenefitDialog(staffId, null);
            dlg.showAndWait().ifPresent(doc -> {
                service.createOrUpdate(doc.getString("staffId"), doc);
                refresh();
                new Alert(Alert.AlertType.INFORMATION, "Benefit added.").showAndWait();
            });
        });
    }

    private void adminEdit() {
        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { new Alert(Alert.AlertType.WARNING, "Select a benefit to edit.").showAndWait(); return; }
        String staffId = safeString(sel, "staffId");
        Document existing = service.getBenefitsForStaff(staffId);
        AddBenefitDialog dlg = new AddBenefitDialog(staffId, existing);
        dlg.showAndWait().ifPresent(updated -> {
            service.createOrUpdate(staffId, updated);
            refresh();
            new Alert(Alert.AlertType.INFORMATION, "Benefit updated.").showAndWait();
        });
    }

    private void adminDelete() {
        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { new Alert(Alert.AlertType.WARNING, "Select a benefit to delete.").showAndWait(); return; }
        String id = sel.getObjectId("_id").toHexString();
        boolean ok = service.deleteById(id);
        if (ok) {
            refresh();
            new Alert(Alert.AlertType.INFORMATION, "Benefit removed.").showAndWait();
        } else {
            new Alert(Alert.AlertType.ERROR, "Delete failed.").showAndWait();
        }
    }

    private void refresh() {
        data.clear();
        List<Document> list = service.listAllBenefits();
        if (list != null) data.addAll(list);
        lastRefLabel.setText("Last refreshed: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    private String safeString(Document d, String key) {
        if (d == null) return "";
        Object v = d.get(key);
        return v == null ? "" : v.toString();
    }


    private static String normalizeKey(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("\\s+", "").replaceAll("[^a-z0-9]", "");
    }

    private Button styledButton(String text, String type) {
        Button b = new Button(text);
        String base = """
            -fx-border-radius:6;
            -fx-background-radius:6;
            -fx-padding:6 10;
            -fx-font-size:12px;
            -fx-font-weight:600;
            """;

        String style;
        switch (type) {
            case "primary" -> style = base + "-fx-background-color:#1976d2; -fx-text-fill:white; -fx-border-color:#1565c0;";
            case "danger" -> style = base + "-fx-background-color:#d32f2f; -fx-text-fill:white; -fx-border-color:#b71c1c;";
            default -> style = base + "-fx-background-color:#ffffff; -fx-text-fill:#333333; -fx-border-color:#d5dbe1;";
        }
        b.setStyle(style);

        b.setOnMouseEntered(e -> {
            String hover;
            switch (type) {
                case "primary" -> hover = base + "-fx-background-color:#1565c0; -fx-text-fill:white;";
                case "danger" -> hover = base + "-fx-background-color:#c62828; -fx-text-fill:white;";
                default -> hover = base + "-fx-background-color:#f4f7f9; -fx-text-fill:#333333;";
            }
            b.setStyle(hover);
        });

        b.setOnMouseExited(e -> b.setStyle(style));
        return b;
    }

    public VBox getView() { return view; }
}
