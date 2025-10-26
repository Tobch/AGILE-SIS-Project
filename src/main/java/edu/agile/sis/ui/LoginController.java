package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.bson.Document;

public class LoginController {
    private final BorderPane view = new BorderPane();
    private final AuthService authService = new AuthService();

    public LoginController() {
        view.setPadding(new Insets(30));

        // App Branding
        Label appTitle = new Label("AGILE SIS");
        appTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        appTitle.setStyle("-fx-text-fill: #2c3e50;");

        Label subtitle = new Label("Welcome back! Please sign in to continue.");
        subtitle.setFont(Font.font("Segoe UI", 14));
        subtitle.setStyle("-fx-text-fill: #555;");

        VBox headerBox = new VBox(5, appTitle, subtitle);
        headerBox.setAlignment(Pos.CENTER);

        // Form fields
        TextField username = new TextField();
        username.setPromptText("Username");
        username.setStyle("-fx-background-radius: 6; -fx-border-radius: 6;");

        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        password.setStyle("-fx-background-radius: 6; -fx-border-radius: 6;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);

        // Remember me + Forgot password
        CheckBox rememberMe = new CheckBox("Remember me");
        Hyperlink forgotPassword = new Hyperlink("Forgot Password?");
        forgotPassword.setStyle("-fx-text-fill: #2980b9;");

        // Forgot password dialog logic
        forgotPassword.setOnAction(e -> {
            Dialog<String[]> dialog = new Dialog<>();
            dialog.setTitle("Reset Password");
            dialog.setHeaderText("Reset your password");

            TextField userField = new TextField();
            userField.setPromptText("Username");

            PasswordField newPass = new PasswordField();
            newPass.setPromptText("New Password");

            PasswordField confirmPass = new PasswordField();
            confirmPass.setPromptText("Confirm Password");

            GridPane resetGrid = new GridPane();
            resetGrid.setHgap(10);
            resetGrid.setVgap(10);
            resetGrid.add(new Label("Username:"), 0, 0);
            resetGrid.add(userField, 1, 0);
            resetGrid.add(new Label("New Password:"), 0, 1);
            resetGrid.add(newPass, 1, 1);
            resetGrid.add(new Label("Confirm Password:"), 0, 2);
            resetGrid.add(confirmPass, 1, 2);

            dialog.getDialogPane().setContent(resetGrid);

            ButtonType resetButton = new ButtonType("Reset", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(resetButton, ButtonType.CANCEL);

            dialog.setResultConverter(btn -> {
                if (btn == resetButton) {
                    return new String[]{userField.getText(), newPass.getText(), confirmPass.getText()};
                }
                return null;
            });

            dialog.showAndWait().ifPresent(result -> {
                String u = result[0];
                String p1 = result[1];
                String p2 = result[2];

                if (u.isBlank() || p1.isBlank() || p2.isBlank()) {
                    new Alert(Alert.AlertType.ERROR, "All fields are required.").showAndWait();
                    return;
                }

                if (!p1.equals(p2)) {
                    new Alert(Alert.AlertType.ERROR, "Passwords do not match.").showAndWait();
                    return;
                }

                try {
                    authService.changePassword(u, p1);
                    new Alert(Alert.AlertType.INFORMATION, "Password updated successfully!").showAndWait();
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Failed to reset password.").showAndWait();
                }
            });
        });

        HBox optionsBox = new HBox(10, rememberMe, forgotPassword);
        optionsBox.setAlignment(Pos.CENTER_LEFT);

        // Login button
        Button loginBtn = new Button("Sign In");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; "
                        + "-fx-font-weight: bold; -fx-background-radius: 6;"
        );

        // Login logic (button + Enter key)
        Runnable doLogin = () -> {
            String user = username.getText().trim();
            String pass = password.getText();

            boolean ok;
            try {
                ok = authService.login(user, pass);
            } catch (Exception ex) {
                ok = false;
            }

            if (ok) {
                Document userDoc = authService.getUserByUsername(user);
                AuthSession.getInstance().setCurrentUser(userDoc);

                MainController main = new MainController();
                view.getScene().setRoot(main.getView());
            } else {
                new Alert(Alert.AlertType.ERROR, "Invalid username or password").showAndWait();
            }
        };

        loginBtn.setOnAction(e -> doLogin.run());
        password.setOnAction(e -> doLogin.run());

        // Card layout
        VBox formBox = new VBox(15, grid, optionsBox, loginBtn);
        formBox.setPadding(new Insets(25));
        formBox.setStyle(
                "-fx-background-color: #ffffff; -fx-border-color: #ddd; "
                        + "-fx-border-radius: 10; -fx-background-radius: 10; "
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0.2, 0, 4);"
        );
        formBox.setAlignment(Pos.CENTER);
        formBox.setMaxWidth(350);

        VBox centerBox = new VBox(20, headerBox, formBox);
        centerBox.setAlignment(Pos.CENTER);

        view.setCenter(centerBox);
        view.setStyle("-fx-background-color: linear-gradient(to bottom right, #ecf0f1, #bdc3c7);");
    }

    public Pane getView() {
        return view;
    }
}
