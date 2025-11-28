package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.PayrollService;
import edu.agile.sis.util.FileStorageUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.bson.Document;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PayrollController {

    private final VBox view = new VBox(12);
    private final PayrollService service = new PayrollService();
    private final ObservableList<Document> data = FXCollections.observableArrayList();
    private final TableView<Document> table = new TableView<>();

    private final Label lastRefLabel = new Label("");
    private final TextField adminStaffSearch = new TextField();

    public PayrollController() {

        view.setPadding(new Insets(16));
        view.setStyle("-fx-background-color: #f4f6f8;");

        Label title = new Label("Payroll");
        title.setStyle("-fx-font-size:20px; -fx-font-weight:700; -fx-text-fill:#2c3e50;");

        Label subtitle = new Label("Manage staff salary slips and payroll documents.");
        subtitle.setStyle("-fx-font-size:12px; -fx-text-fill:#7a8a97;");

        VBox titleBox = new VBox(2, title, subtitle);

        Button refreshBtn = styled("↻ Refresh");
        Button addBtn = styled("Add");
        Button editBtn = styled("Edit");
        Button downloadBtn = styled("Download");
        Button deleteBtn = styled("Delete");

        boolean isAdmin = AuthSession.getInstance().hasRole("Admin");
        addBtn.setVisible(isAdmin);
        editBtn.setVisible(isAdmin);
        deleteBtn.setVisible(isAdmin);

        if (isAdmin) {
            adminStaffSearch.setPromptText("Enter staffId (leave empty for ALL)");
            adminStaffSearch.setPrefWidth(220);
        } else {
            adminStaffSearch.setVisible(false);
            adminStaffSearch.setManaged(false);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(8, adminStaffSearch, lastRefLabel, refreshBtn, addBtn, editBtn, downloadBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        HBox header = new HBox(12, titleBox, spacer, actions);
        header.setAlignment(Pos.CENTER_LEFT);

        setupTable();

        VBox card = new VBox(table);
        card.setPadding(new Insets(12));
        card.setStyle("""
            -fx-background-color:white;
            -fx-background-radius:10;
            -fx-border-radius:10;
            -fx-border-color:#d8dee6;
            -fx-effect:dropshadow(gaussian, rgba(0,0,0,0.04), 8, 0, 0, 2);
        """);

        view.getChildren().addAll(header, card);

        refreshBtn.setOnAction(e -> refresh());
        adminStaffSearch.setOnAction(e -> refresh());
        addBtn.setOnAction(e -> addPayroll());
        editBtn.setOnAction(e -> editPayroll());
        downloadBtn.setOnAction(e -> downloadPayroll());
        deleteBtn.setOnAction(e -> deletePayroll());

        refresh();
    }

    private void setupTable() {

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Document, String> staffCol = new TableColumn<>("Staff ID");
        staffCol.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(safe(c.getValue(), "staffId")));

        TableColumn<Document, String> periodCol = new TableColumn<>("Period");
        periodCol.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(safe(c.getValue(), "period")));

        TableColumn<Document, String> netCol = new TableColumn<>("Net (EGP)");
        netCol.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        String.format("%.2f", safeDouble(c.getValue(), "net"))
                )
        );

        TableColumn<Document, String> fileCol = new TableColumn<>("File");
        fileCol.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(safe(c.getValue(), "fileName")));

        table.getColumns().setAll(staffCol, periodCol, netCol, fileCol);
        table.setItems(data);
    }


    private void addPayroll() {
        TextInputDialog ask = new TextInputDialog();
        ask.setHeaderText("Enter staffId for the new payslip (e.g., S1001)");
        ask.showAndWait().ifPresent(staffId -> {
            PayrollDialog dlg = new PayrollDialog(staffId, null);
            dlg.showAndWait().ifPresent(doc -> {
                boolean ok = service.createPayroll(staffId, doc);
                alert(ok, "Payslip created.", "Failed to create payslip.");
                if (ok) refresh();
            });
        });
    }

    private void editPayroll() {

        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            warn("Select a payslip to edit.");
            return;
        }

        String staffId = safe(sel, "staffId");
        PayrollDialog dlg = new PayrollDialog(staffId, sel);

        dlg.showAndWait().ifPresent(updated -> {

            String id = sel.getObjectId("_id").toHexString();
            boolean ok = service.updatePayroll(id, updated);

            alert(ok, "Payslip updated.", "Failed to update payslip.");
            if (ok) refresh();
        });
    }

    private void downloadPayroll() {

        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            warn("Select a payslip to download.");
            return;
        }

        String fileId = safe(sel, "fileId");
        if (fileId.isBlank()) {
            info("No file attached for this payslip.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Payslip PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName(safe(sel, "fileName"));

        File dest = chooser.showSaveDialog(view.getScene().getWindow());
        if (dest != null) {
            try {
                FileStorageUtil.downloadFile(fileId, dest.getAbsolutePath());
                info("Saved to: " + dest.getAbsolutePath());
            } catch (Exception ex) {
                ex.printStackTrace();
                error("Error downloading file.");
            }
        }
    }

    private void deletePayroll() {

        Document sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            warn("Select a payslip to delete.");
            return;
        }

        String id = sel.getObjectId("_id").toHexString();
        boolean ok = service.deletePayroll(id);

        alert(ok, "Payslip deleted.", "Failed to delete payslip.");
        if (ok) refresh();
    }


    private void refresh() {

        data.clear();
        boolean isAdmin = AuthSession.getInstance().hasRole("Admin");

        if (isAdmin) {

            String q = adminStaffSearch.getText();

            if (q == null || q.isBlank()) {
                List<Document> all = service.listAllPayroll();
                if (all != null) data.addAll(all);

            } else {
                List<Document> list = service.listPayslipsForStaff(q.trim());
                if (list != null) data.addAll(list);
            }

        } else {
            String staffId = AuthSession.getInstance().getLinkedEntityId();
            if (staffId == null || staffId.isBlank())
                staffId = AuthSession.getInstance().getUsername();

            List<Document> list = service.listPayslipsForStaff(staffId);
            if (list != null) data.addAll(list);
        }

        lastRefLabel.setText("Last refreshed: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }



    private String safe(Document d, String key) {
        if (d == null || !d.containsKey(key)) return "";
        Object v = d.get(key);
        return v == null ? "" : v.toString();
    }

    private double safeDouble(Document d, String key) {
        try {
            Object v = d.get(key);
            if (v instanceof Number) return ((Number) v).doubleValue();
            return Double.parseDouble(v.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Button styled(String text) {
        Button b = new Button(text);
        b.setStyle("""
            -fx-background-color:white;
            -fx-border-color:#cad3dd;
            -fx-border-radius:6;
            -fx-background-radius:6;
            -fx-padding:6 12;
        """);
        return b;
    }

    private void warn(String msg) { new Alert(Alert.AlertType.WARNING, msg).showAndWait(); }
    private void info(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
    private void error(String msg) { new Alert(Alert.AlertType.ERROR, msg).showAndWait(); }

    private void alert(boolean ok, String success, String fail) {
        if (ok) info(success);
        else error(fail);
    }

    public VBox getView() { return view; }
}
