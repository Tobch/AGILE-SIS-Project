package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.util.FileStorageUtil;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import org.bson.Document;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PayrollDialog extends Dialog<Document> {

    private File chosenFile = null;


    public PayrollDialog(String staffId) {
        this(staffId, null);
    }

 
    public PayrollDialog(String staffId, Document existing) {

        boolean editing = (existing != null);

        setTitle(editing ? "Edit Payslip" : "Create Payslip");
        setHeaderText((editing ? "Update" : "Create") + " payslip for: " + staffId);

        ButtonType saveBtnType = new ButtonType(editing ? "Update" : "Create", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField staffField = new TextField(staffId);
        staffField.setEditable(false);

        DatePicker periodPicker = new DatePicker();
        TextField grossField = new TextField();
        TextField deductionsField = new TextField();
        TextField netField = new TextField();
        TextArea notes = new TextArea();

        notes.setPrefRowCount(3);
        netField.setEditable(false);

        Label fileLabel = new Label(editing ? safe(existing, "fileName") : "No file selected");
        Button chooseFile = new Button("Choose PDF");

        chooseFile.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File f = fc.showOpenDialog(getDialogPane().getScene().getWindow());
            if (f != null) {
                chosenFile = f;
                fileLabel.setText(f.getName());
            }
        });

        // Prefill in edit mode
        if (editing) {
            periodPicker.setValue(LocalDate.parse(existing.getString("period")));
            grossField.setText(existing.get("gross").toString());
            deductionsField.setText(existing.get("deductions").toString());
            netField.setText(existing.get("net").toString());
            notes.setText(existing.getString("notes"));
        } else {
            periodPicker.setValue(LocalDate.now());
            deductionsField.setText("0");
        }

 
        Runnable recalc = () -> {
            try {
                double g = Double.parseDouble(grossField.getText().trim());
                double d = Double.parseDouble(deductionsField.getText().trim());
                netField.setText(String.format("%.2f", (g - d)));
            } catch (Exception ex) {
                netField.setText("");
            }
        };

        grossField.textProperty().addListener((o, a, b) -> recalc.run());
        deductionsField.textProperty().addListener((o, a, b) -> recalc.run());

        grid.add(new Label("Staff ID:"), 0, 0);
        grid.add(staffField, 1, 0);
        grid.add(new Label("Period:"), 0, 1);
        grid.add(periodPicker, 1, 1);
        grid.add(new Label("Gross:"), 0, 2);
        grid.add(grossField, 1, 2);
        grid.add(new Label("Deductions:"), 0, 3);
        grid.add(deductionsField, 1, 3);
        grid.add(new Label("Net:"), 0, 4);
        grid.add(netField, 1, 4);
        grid.add(new Label("Notes:"), 0, 5);
        grid.add(notes, 1, 5);

        grid.add(new Label("File:"), 0, 6);
        grid.add(new HBox(8, chooseFile, fileLabel), 1, 6);

        getDialogPane().setContent(grid);


        Node saveBtn = getDialogPane().lookupButton(saveBtnType);
        saveBtn.setDisable(!editing);   

        grossField.textProperty().addListener((o, oldVal, newVal) ->
                saveBtn.setDisable(newVal.trim().isEmpty())
        );

        setResultConverter(btn -> {
            if (btn == saveBtnType) {

                Document doc = new Document();
                doc.append("staffId", staffId);
                doc.append("period", periodPicker.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE));

                double g = parse(grossField.getText());
                double d = parse(deductionsField.getText());
                doc.append("gross", g);
                doc.append("deductions", d);
                doc.append("net", g - d);

                doc.append("notes", notes.getText());

      
                if (editing && chosenFile == null) {
                    doc.append("fileId", safe(existing, "fileId"));
                    doc.append("fileName", safe(existing, "fileName"));
                }

          
                if (chosenFile != null) {
                    try (FileInputStream fis = new FileInputStream(chosenFile)) {
                        String fileId = FileStorageUtil.uploadFile(
                                chosenFile.getName(), fis, "application/pdf"
                        );
                        doc.append("fileId", fileId);
                        doc.append("fileName", chosenFile.getName());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                doc.append("createdBy", AuthSession.getInstance().getUsername());
                doc.append("createdAt", System.currentTimeMillis());

                return doc;
            }
            return null;
        });
    }

    private double parse(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0.0; }
    }

    private String safe(Document d, String key) {
        if (d == null || !d.containsKey(key)) return "";
        Object v = d.get(key);
        return v == null ? "" : v.toString();
    }
}
