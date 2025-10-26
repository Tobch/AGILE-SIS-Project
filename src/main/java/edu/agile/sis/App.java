package edu.agile.sis;

import edu.agile.sis.db.DBConnection;
import edu.agile.sis.ui.LoginController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) {
        DBConnection.getInstance().connectFromConfig();
        LoginController loginController = new LoginController();
        Scene scene = new Scene(loginController.getView(), 400, 300);
        primaryStage.setTitle("AGILE SIS - Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
