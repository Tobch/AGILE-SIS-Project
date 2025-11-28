package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.bson.Document;

import java.time.format.DateTimeFormatter;


public class AddLeaveDialog extends Dialog<Document> {


    public AddLeaveDialog(String staffId) {
        this(staffId, null);
    }


    public AddLeaveDialog(String staffId, Document existing) {
        boolean editing = existing != null;

        setTitle(editing ? "Edit Leave Request" : "Request Leave");
        setHeaderText((editing ? "Edit leave request for " : "Create leave request for ") + staffId);

        ButtonType saveBtnType = new ButtonType(editing ? "Update" : "Create", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16, 16, 8, 16));


        TextField staffField = new TextField(staffId);
        staffField.setEditable(false);

  
        TextField periodField = new TextField();
        periodField.setPromptText("e.g., 2025-11-01 or 2025-11-01 to 2025-11-05");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Annual", "Sick", "Emergency", "Maternity", "Other");
        typeCombo.setEditable(false);
        typeCombo.setPromptText("Select type");

        TextArea reasonArea = new TextArea();
        reasonArea.setPrefRowCount(4);
        reasonArea.setWrapText(true);

     
        if (editing) {
            periodField.setText(safe(existing, "period"));
            String t = safe(existing, "type");
            if (!t.isEmpty()) typeCombo.setValue(t);
            reasonArea.setText(safe(existing, "reason"));
        }

        grid.add(new Label("Staff ID:"), 0, 0);
        grid.add(staffField, 1, 0);

        grid.add(new Label("Period:"), 0, 1);
        grid.add(periodField, 1, 1);

        grid.add(new Label("Type:"), 0, 2);
        grid.add(typeCombo, 1, 2);

        grid.add(new Label("Reason:"), 0, 3);
        grid.add(reasonArea, 1, 3);


        Label hint = new Label("Period format: single date or 'from to' range");
        hint.setStyle("-fx-font-size:10px; -fx-text-fill:#666;");
        grid.add(new HBox(hint), 1, 4);

        getDialogPane().setContent(grid);


        Node saveBtn = getDialogPane().lookupButton(saveBtnType);
        saveBtn.setDisable(true);

      
        Runnable validate = () -> {
            String per = periodField.getText() == null ? "" : periodField.getText().trim();
            String ty = typeCombo.getValue() == null ? "" : typeCombo.getValue().trim();
            saveBtn.setDisable(per.isEmpty() || ty.isEmpty());
        };

        periodField.textProperty().addListener((o, oldV, newV) -> validate.run());
        typeCombo.valueProperty().addListener((o, oldV, newV) -> validate.run());


        setResultConverter(btn -> {
            if (btn == saveBtnType) {
                Document doc = new Document();
                doc.append("staffId", staffId);
               
                doc.append("period", periodField.getText().trim());
                doc.append("type", typeCombo.getValue());
                doc.append("reason", reasonArea.getText() == null ? "" : reasonArea.getText().trim());

                
                if (editing) {
                    String existingStatus = safe(existing, "status");
                    if (!existingStatus.isBlank()) doc.append("status", existingStatus);
                    else doc.append("status", "PENDING");
                } else {
                    doc.append("status", "PENDING");
                }

               
                doc.append("createdBy", AuthSession.getInstance().getUsername());
                doc.append("createdAt", System.currentTimeMillis());

                return doc;
            }
            return null;
        });


        validate.run();
    }

    private String safe(Document d, String key) {
        if (d == null || !d.containsKey(key)) return "";
        Object v = d.get(key);
        return v == null ? "" : v.toString();
    }
}
