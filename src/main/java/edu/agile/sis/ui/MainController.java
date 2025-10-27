package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class MainController {
    private final BorderPane view = new BorderPane();

    public MainController() {
        // --- Sidebar menu ---
        VBox sidebar = new VBox(12);
        sidebar.setPadding(new Insets(20));
        sidebar.setStyle("-fx-background-color: #2c3e50;"); // dark sidebar

        Label brand = new Label("AGILE SIS");
        brand.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        brand.setPadding(new Insets(0, 0, 20, 0));

        sidebar.getChildren().add(brand);
        
     

        // Sidebar buttons with icons
        //Button myProfileBtn     = createMenuButton("👤", "My Profile");
        //Button studentsBtn      = createMenuButton("🎓", "Manage Students");
       // Button reservationsBtn  = createMenuButton("📅", "Room Reservations");
        Button staffBtn         = createMenuButton("👥", "Manage Staff");
        //Button coursesBtn       = createMenuButton("📚", "Courses");
        //Button assignmentsBtn   = createMenuButton("📝", "Assignments");
        //Button quizzesBtn       = createMenuButton("❓", "Quizzes");
        //Button messagesBtn      = createMenuButton("💬", "Messages");
        //Button eavBtn           = createMenuButton("⚙️", "EAV Admin");
        Button logoutBtn        = createMenuButton("🚪", "Logout");

        // --- Event Handlers ---
        
        staffBtn.setOnAction(e -> view.setCenter(new StaffController().getView()));
        
        //myProfileBtn.setOnAction(e -> view.setCenter(new StudentsController(true).getView()));
       // studentsBtn.setOnAction(e -> view.setCenter(new StudentsController().getView()));
       // reservationsBtn.setOnAction(e -> view.setCenter(new ReservationsController().getView()));
        staffBtn.setOnAction(e -> view.setCenter(new StaffController().getView()));
       // coursesBtn.setOnAction(e -> view.setCenter(new CoursesController().getView()));
       // assignmentsBtn.setOnAction(e -> view.setCenter(new AssignmentsController().getView()));
       // quizzesBtn.setOnAction(e -> view.setCenter(new QuizzesController().getView()));
       // messagesBtn.setOnAction(e -> view.setCenter(new MessagesController().getView()));
        //eavBtn.setOnAction(e -> view.setCenter(new EavAdminController().getView()));

        

        logoutBtn.setOnAction(e -> {
            AuthSession.getInstance().clear();
            LoginController loginController = new LoginController();
            view.getScene().setRoot(loginController.getView());
        });

        
        boolean isAdmin = AuthSession.getInstance().hasRole("Admin");
        boolean isProf  = AuthSession.getInstance().hasRole("Professor");
        boolean isTA    = AuthSession.getInstance().hasRole("TA");
        boolean isStaffGeneric = AuthSession.getInstance().hasRole("Staff") || AuthSession.getInstance().hasRole("Lecturer");
        boolean isStudent = AuthSession.getInstance().hasRole("Student");

        if (isAdmin) {
            sidebar.getChildren().addAll(staffBtn);
        } else if (isProf || isTA || isStaffGeneric) {
            sidebar.getChildren().addAll();
        } else if (isStudent) {
            sidebar.getChildren().addAll();
        } else {
            sidebar.getChildren().addAll();
        }

        sidebar.getChildren().add(logoutBtn);
 
        // --- Header Bar ---
        HBox header = new HBox();
        header.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #ddd;");
        header.setPadding(new Insets(10, 20, 10, 20));
        header.setAlignment(Pos.CENTER_RIGHT);

        Label userInfo = new Label("Logged in as: " 
                + AuthSession.getInstance().getUsername() 
                + " (" + String.join(", ", AuthSession.getInstance().getRoles()) + ")");
        userInfo.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14px;");

        header.getChildren().add(userInfo);

        // --- Center Welcome ---
        Label welcome = new Label("Welcome to AGILE SIS\nSelect a module from the left menu.");
        welcome.setStyle("-fx-font-size: 18px; -fx-text-fill: #34495e;");
        welcome.setPadding(new Insets(30));

        // --- Layout ---
        view.setLeft(sidebar);
        view.setTop(header);
        view.setCenter(welcome);
        view.setStyle("-fx-background-color: #f4f6f7;");
    }

    private Button createMenuButton(String icon, String text) {
        Button btn = new Button(icon + "  " + text); // add icon before text
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-alignment: center-left;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-size: 14px; -fx-alignment: center-left;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-alignment: center-left;"));
        return btn;
    }

    public BorderPane getView() {
        return view;
    }
}
