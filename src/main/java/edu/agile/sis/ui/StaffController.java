package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.StaffService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.bson.Document;

import java.util.List;
import java.util.Optional;

/**
 * StaffController: if a staff member (non-admin) logs in, they see only their own profile and can edit it.
 * Admins see the full staff list with Add/Delete/Edit.
 */
public class StaffController {
    private final VBox view = new VBox(10);
    private final StaffService staffService = new StaffService();
    private final TableView<Document> table = new TableView<>();
    private final ObservableList<Document> items = FXCollections.observableArrayList();

    private final boolean isAdmin = AuthSession.getInstance().hasRole("Admin");
    private final boolean isStaff = AuthSession.getInstance().hasRole("Professor") || AuthSession.getInstance().hasRole("TA");
    private final String linkedStaffId = AuthSession.getInstance().getLinkedEntityId(); // expected to be staffId

    public StaffController() {
        view.setPadding(new Insets(10));
        Label title = new Label(isAdmin ? "Staff Directory (Admin)" : (isStaff ? "My Profile" : "Staff Directory"));

        TableColumn<Document, String> idCol = new TableColumn<>("Staff ID");
        idCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getString("staffId")));
        idCol.setPrefWidth(120);

        TableColumn<Document, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getString("name")));
        nameCol.setPrefWidth(220);

        TableColumn<Document, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().get("email"))));
        emailCol.setPrefWidth(220);

        TableColumn<Document, String> officeCol = new TableColumn<>("Office Hours");
        officeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().get("officeHours"))));
        officeCol.setPrefWidth(200);

        table.getColumns().add(idCol);
        table.getColumns().add(nameCol);
        table.getColumns().add(emailCol);
        table.getColumns().add(officeCol);
        table.setItems(items);

        Button add = new Button("Add");
        Button viewBtn = new Button(isStaff ? "View / Edit My Profile" : "View / Edit");
        Button del = new Button("Delete");
        Button refresh = new Button("Refresh");

        add.setDisable(!isAdmin);
        del.setDisable(!isAdmin);

        add.setOnAction(e -> addStaff());
        refresh.setOnAction(e -> load());

        viewBtn.setOnAction(e -> {
            Document sel;
            if (!isAdmin && isStaff) {
                // show only linked staff
                sel = (items.isEmpty() ? null : items.get(0));
            } else {
                sel = table.getSelectionModel().getSelectedItem();
            }
            if (sel == null) { new Alert(Alert.AlertType.INFORMATION, "No staff selected").showAndWait(); return; }
            viewEditStaff(sel);
        });

        del.setOnAction(e -> {
            Document sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            String id = sel.getObjectId("_id").toHexString();
            staffService.delete(id);
            load();
        });

        HBox controls = new HBox(8, add, viewBtn, del, refresh);
        controls.setPadding(new Insets(6));

        view.getChildren().addAll(title, table, controls);

        load();
    }

    private void load(){
    items.clear();
    if (isAdmin) {
        List<Document> list = staffService.listAll();
        if (list != null) items.addAll(list);
    } else if (isStaff) {
        // prefer linkedStaffId; fallback to username
        String lookupId = linkedStaffId;
        if (lookupId == null || lookupId.isBlank()) {
            // try using username (some projects store staffId as username)
            lookupId = AuthSession.getInstance().getUsername();
            System.out.println("[StaffController] linkedStaffId missing; trying username: " + lookupId);
        }
        if (lookupId == null || lookupId.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Your account is not linked to a staff entity. Contact admin.").showAndWait();
            return;
        }
        Document me = staffService.getByStaffId(lookupId);
        if (me != null) {
            items.add(me);
        } else {
            // fallback: try searching all staff for matching username in email or name
            List<Document> all = staffService.listAll();
            for (Document s : all) {
                if (lookupId.equalsIgnoreCase(s.getString("staffId"))
                        || lookupId.equalsIgnoreCase(s.getString("name"))
                        || lookupId.equalsIgnoreCase(s.getString("email"))) {
                    items.add(s);
                    break;
                }
            }
            if (items.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "No staff record found for your account (lookupId=" + lookupId + "). Contact admin.").showAndWait();
            }
        }
    } else {
        // Not admin or staff: show read-only list (or just the directory)
        List<Document> list = staffService.listAll();
        if (list != null) items.addAll(list);
    }
}


    private void addStaff(){
        Dialog<Document> dlg = new Dialog<>();
        dlg.setTitle("Add Staff");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(8); g.setVgap(8); g.setPadding(new Insets(8));
        TextField staffId = new TextField(); staffId.setPromptText("S1001");
        TextField name = new TextField(); TextField email = new TextField(); TextField office = new TextField();
        g.add(new Label("Staff ID:"),0,0); g.add(staffId,1,0);
        g.add(new Label("Name:"),0,1); g.add(name,1,1);
        g.add(new Label("Email:"),0,2); g.add(email,1,2);
        g.add(new Label("Office Hours:"),0,3); g.add(office,1,3);
        dlg.getDialogPane().setContent(g);

        dlg.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Document doc = new Document("staffId", staffId.getText().trim())
                        .append("name", name.getText().trim())
                        .append("email", email.getText().trim())
                        .append("officeHours", office.getText().trim())
                        .append("createdAt", new java.util.Date());
                return doc;
            }
            return null;
        });

        dlg.showAndWait().ifPresent(doc -> {
            staffService.createStaff(doc);
            load();
        });
    }

    private void viewEditStaff(Document doc){
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Edit Staff");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        GridPane g = new GridPane();
        g.setHgap(8); g.setVgap(8); g.setPadding(new Insets(8));
        TextField name = new TextField(doc.getString("name"));
        TextField email = new TextField(String.valueOf(doc.get("email")));
        TextField office = new TextField(String.valueOf(doc.get("officeHours")));
        g.add(new Label("Name:"),0,0); g.add(name,1,0);
        g.add(new Label("Email:"),0,1); g.add(email,1,1);
        g.add(new Label("Office Hours:"),0,2); g.add(office,1,2);
        dlg.getDialogPane().setContent(g);

        Optional<ButtonType> res = dlg.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            Document updated = new Document("name", name.getText().trim())
                    .append("email", email.getText().trim())
                    .append("officeHours", office.getText().trim())
                    .append("updatedAt", new java.util.Date());
            staffService.update(doc.getObjectId("_id").toHexString(), updated);
            load();
        }
    }

    public VBox getView(){ return view; }
}
