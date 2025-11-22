package edu.agile.sis.ui;

import edu.agile.sis.service.AttributeService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EavAdminController {
    private final VBox view = new VBox(10);
    private final AttributeService attrService = new AttributeService();
    private final ListView<String> listView = new ListView<>();
    private final ObservableList<String> keys = FXCollections.observableArrayList();

    public EavAdminController(){
        view.setPadding(new Insets(10));
        Label title = new Label("EAV Attribute Metadata Admin");

        Button add = new Button("Add Attribute");
        Button viewBtn = new Button("View/Edit");
        Button del = new Button("Delete");

        add.setOnAction(e -> addAttr());
        viewBtn.setOnAction(e -> {
            String sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) viewEdit(sel);
        });
        del.setOnAction(e -> {
            String sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) { attrService.delete(sel); load(); }
        });

        view.getChildren().addAll(title, listView, new VBox(6, add, viewBtn, del));
        load();
    }

    private void load(){
        keys.clear();
        List<Document> all = attrService.listAll();
        for (Document d : all) keys.add(d.getString("key"));
        listView.setItems(keys);
    }

    private void addAttr(){
        Dialog<Document> dlg = new Dialog<>();
        dlg.setTitle("Add Attribute Metadata");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g = new GridPane(); g.setHgap(8); g.setVgap(8); g.setPadding(new Insets(8));
        TextField key = new TextField(); TextField type = new TextField(); CheckBox required = new CheckBox("Required");
        TextField regex = new TextField(); TextField allowed = new TextField();
        g.add(new Label("Key:"),0,0); g.add(key,1,0);
        g.add(new Label("Type (string, number, date):"),0,1); g.add(type,1,1);
        g.add(required,1,2);
        g.add(new Label("Validation regex (optional):"),0,3); g.add(regex,1,3);
        g.add(new Label("Allowed values (comma):"),0,4); g.add(allowed,1,4);
        dlg.getDialogPane().setContent(g);

        dlg.setResultConverter(btn -> {
            if (btn == ButtonType.OK){
                Document meta = new Document("key", key.getText().trim())
                        .append("type", type.getText().trim())
                        .append("required", required.isSelected())
                        .append("regex", regex.getText().trim())
                        .append("allowedValues", List.of(allowed.getText().split("\\s*,\\s*")));
                return meta;
            }
            return null;
        });

        dlg.showAndWait().ifPresent(d -> { attrService.createAttributeMeta(d); load(); });
    }

    private void viewEdit(String key){
        Document meta = attrService.findByKey(key);
        if (meta == null) return;
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Edit Attribute " + key);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        GridPane g = new GridPane(); g.setHgap(8); g.setVgap(8); g.setPadding(new Insets(8));
        TextField type = new TextField(meta.getString("type"));
        CheckBox required = new CheckBox("Required"); required.setSelected(meta.getBoolean("required", false));
        TextField regex = new TextField(meta.getString("regex"));
        TextField allowed = new TextField(String.join(",", asStringList(meta.get("allowedValues"))));
        g.add(new Label("Type:"),0,0); g.add(type,1,0);
        g.add(required,1,1);
        g.add(new Label("Validation regex:"),0,2); g.add(regex,1,2);
        g.add(new Label("Allowed values (comma):"),0,3); g.add(allowed,1,3);
        dlg.getDialogPane().setContent(g);
        Optional<ButtonType> res = dlg.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK){
            Document updated = new Document("type", type.getText().trim())
                    .append("required", required.isSelected())
                    .append("regex", regex.getText().trim())
                    .append("allowedValues", List.of(allowed.getText().split("\\s*,\\s*")));
            attrService.update(key, updated);
            load();
        }
    }
            // Add this helper method anywhere inside the controller class (e.g., near other helpers)
        private List<String> asStringList(Object obj) {
            if (obj == null) return List.of();
            if (obj instanceof List<?>) {
                List<?> raw = (List<?>) obj;
                List<String> out = new ArrayList<>(raw.size());
                for (Object o : raw) {
                    if (o == null) continue;
                    out.add(o.toString());
                }
                return out;
            }
            // fallback: single value -> single-element list
            return List.of(obj.toString());
        }

    public VBox getView(){ return view; }
}
