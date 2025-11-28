package edu.agile.sis.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.bson.Document;

import java.util.*;
import java.util.stream.Collectors;


public class AddBenefitDialog extends Dialog<Document> {

    public AddBenefitDialog(String staffId, Document existing) {
        setTitle(existing == null ? "Add Benefit" : "Edit Benefit");
        setHeaderText(existing == null ? "Create a new benefit for staff" : "Edit benefit");

        ButtonType okType = ButtonType.OK;
        getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(8);
        g.setPadding(new Insets(8));

        TextField staffField = new TextField(staffId == null ? "" : staffId);
        staffField.setPromptText("Staff ID ");

        TextField typeField = new TextField();
        typeField.setPromptText("Benefit type (e.g., Car Parking, health insurance)");

        TextArea detailsArea = new TextArea();
        detailsArea.setPrefRowCount(6);
        detailsArea.setWrapText(true);

  
        Label parseHint = new Label("You can paste JSON or key:value pairs separated by commas.");
        parseHint.setStyle("-fx-font-size:11px; -fx-text-fill:#6b7280;");

        g.add(new Label("Staff ID:"), 0, 0);
        g.add(staffField, 1, 0);
        g.add(new Label("Benefit Type:"), 0, 1);
        g.add(typeField, 1, 1);
        g.add(new Label("Details:"), 0, 2);
        g.add(detailsArea, 1, 2);
        g.add(parseHint, 1, 3);

        getDialogPane().setContent(new VBox(g));
        // Prefill when editing
        if (existing != null) {
            if (existing.containsKey("staffId")) staffField.setText(existing.getString("staffId"));
            if (existing.containsKey("type")) typeField.setText(existing.getString("type"));

            Object detObj = existing.get("details");
            if (detObj instanceof Document det && !det.isEmpty()) {
                detailsArea.setText(prettyFromDocument(det));
            } else if (detObj != null) {
                detailsArea.setText(detObj.toString());
            }
            staffField.setEditable(false); 
        }


        Node okButton = getDialogPane().lookupButton(okType);
        if (okButton != null) okButton.setDisable(true);
        Runnable validate = () -> {
            boolean ok = !staffField.getText().trim().isEmpty() && !typeField.getText().trim().isEmpty();
            if (okButton != null) okButton.setDisable(!ok);
        };
        staffField.textProperty().addListener((o, oldV, newV) -> validate.run());
        typeField.textProperty().addListener((o, oldV, newV) -> validate.run());
        validate.run();

  
        setResultConverter(btn -> {
            if (btn == okType) {
                String sid = staffField.getText().trim();
                String type = typeField.getText().trim();
                String detailsRaw = detailsArea.getText().trim();

                
                Document parsedDetails = new Document();

                if (!detailsRaw.isEmpty()) {
                    if (looksLikeJson(detailsRaw)) {
                        try {
                            Document parsed = Document.parse(detailsRaw);
                    
                            parsedDetails.putAll(parsed);
                        } catch (Exception ex) {
                         
                            parsedDetails.append("raw", detailsRaw);
                        }
                    } else {
                        Map<String, String> kv = tryParseKeyValue(detailsRaw);
                        if (kv != null && !kv.isEmpty()) {
                            kv.forEach(parsedDetails::append);
                        } else {
                            parsedDetails.append("raw", detailsRaw);
                        }
                    }
                }


                Document finalDetails = new Document();
                if (!parsedDetails.isEmpty()) {
               
                    List<String> types = Arrays.stream(type.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());

                    
                    Set<String> usedParsedKeys = new HashSet<>();

                  
                    for (String t : types) {
                        Optional<String> match = parsedDetails.keySet().stream()
                                .filter(k -> normalizeKey(k).equals(normalizeKey(t)))
                                .findFirst();

                  
                        if (match.isEmpty()) {
                            final String target = normalizeKey(t);
                            match = parsedDetails.keySet().stream()
                                    .filter(k -> normalizeKey(k).contains(target) || target.contains(normalizeKey(k)))
                                    .findFirst();
                        }

                        if (match.isPresent()) {
                            String matchedKey = match.get();
                            finalDetails.put(t, parsedDetails.get(matchedKey));
                            usedParsedKeys.add(matchedKey);
                        }
                    }

                  
                    for (String k : parsedDetails.keySet()) {
                        if (!usedParsedKeys.contains(k)) finalDetails.put(k, parsedDetails.get(k));
                    }
                }

                Document out = new Document()
                        .append("staffId", sid)
                        .append("type", type)
                        .append("updatedAt", new java.util.Date());

                
                if (!finalDetails.isEmpty()) out.append("details", finalDetails);
                else if (!parsedDetails.isEmpty()) out.append("details", parsedDetails);

                return out;
            }
            return null;
        });
    }

    private static String normalizeKey(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("\\s+", "").replaceAll("[^a-z0-9]", "");
    }

    private static boolean looksLikeJson(String s) {
        String t = s.trim();
        return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"));
    }


    private static Map<String, String> tryParseKeyValue(String s) {
        String[] parts = s.split(",");
        Map<String, String> out = new LinkedHashMap<>();
        for (String part : parts) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            if (!p.contains(":")) {
             
                return null;
            }
            String[] kv = p.split(":", 2);
            String key = kv[0].trim();
            String val = kv.length > 1 ? kv[1].trim() : "";
            if (key.isEmpty()) return null;
            out.put(key, val);
        }
        return out;
    }

    private static String prettyFromDocument(Document d) {
  
        return d.keySet().stream()
                .map(k -> {
                    Object v = d.get(k);
                    if (v instanceof Document nested) {
                        String nestedStr = nested.keySet().stream()
                                .map(nk -> "    " + nk + ": " + (nested.get(nk) == null ? "" : nested.get(nk).toString()))
                                .collect(Collectors.joining("\n"));
                        return k + ":\n" + nestedStr;
                    } else {
                        return k + ": " + (v == null ? "" : v.toString());
                    }
                })
                .collect(Collectors.joining("\n"));
    }
}
