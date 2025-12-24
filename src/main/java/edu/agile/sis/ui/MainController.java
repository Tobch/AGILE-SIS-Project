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

        VBox sidebar = new VBox(12);
        sidebar.setPadding(new Insets(20));
        sidebar.setStyle("-fx-background-color: #2c3e50;"); // dark sidebar

        Label brand = new Label("AGILE SIS");
        brand.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        brand.setPadding(new Insets(0, 0, 20, 0));
        sidebar.getChildren().add(brand);

        Button myProfileBtn = createMenuButton("👤", "My Profile");
        Button studentsBtn = createMenuButton("🎓", "Manage Students");
        Button reservationsBtn = createMenuButton("📅", "Room Reservations");
        Button staffBtn = createMenuButton("👥", "Manage Staff");
        Button parentsBtn = createMenuButton("👪", "Manage Parent");
        Button parentDashboardBtn = createMenuButton("🏠", "Parent Dashboard");
        Button parentChatBtn = createMenuButton("📨", "Parent Messages");
        Button coursesBtn = createMenuButton("📚", "Courses");
        Button assignmentsBtn = createMenuButton("📝", "Assignments");
        Button quizzesBtn = createMenuButton("❓", "Quizzes");
        Button messagesBtn = createMenuButton("💬", "Messages");
        Button eavBtn = createMenuButton("⚙️", "EAV Admin");
        Button payrollBtn = createMenuButton("💵", "Payroll");
        Button leaveBtn = createMenuButton("🏖️", "Leave Requests");
        Button benefitsBtn = createMenuButton("🎁", "Benefits");
        Button inventoryBtn = createMenuButton("📦", "Inventory");
        Button logoutBtn = createMenuButton("🚪", "Logout");

        myProfileBtn.setOnAction(e -> view.setCenter(new StudentsController().getView()));
        studentsBtn.setOnAction(e -> view.setCenter(new StudentsController().getView()));
        reservationsBtn.setOnAction(e -> view.setCenter(new ReservationsController().getView()));
        staffBtn.setOnAction(e -> view.setCenter(new StaffController().getView()));
        parentsBtn.setOnAction(e -> view.setCenter(new ParentRegistrationPane()));

        parentChatBtn.setOnAction(e -> view.setCenter(new ParentMessagesController().getView()));

        parentDashboardBtn.setOnAction(e -> view.setCenter(new ParentDashboardPane()));
        coursesBtn.setOnAction(e -> view.setCenter(new CoursesController().getView()));
        assignmentsBtn.setOnAction(e -> view.setCenter(new AssignmentsController().getView()));
        quizzesBtn.setOnAction(e -> view.setCenter(new QuizzesController().getView()));
        messagesBtn.setOnAction(e -> view.setCenter(new MessagesController().getView()));
        eavBtn.setOnAction(e -> view.setCenter(new EavAdminController().getView()));

        payrollBtn.setOnAction(e -> view.setCenter(new PayrollController().getView()));
        leaveBtn.setOnAction(e -> view.setCenter(new LeaveController().getView()));
        benefitsBtn.setOnAction(e -> view.setCenter(new BenefitsController().getView()));
        inventoryBtn.setOnAction(e -> view.setCenter(new InventoryController().getView()));

        logoutBtn.setOnAction(e -> {
            AuthSession.getInstance().clear();
            LoginController loginController = new LoginController();

            if (view.getScene() != null)
                view.getScene().setRoot(loginController.getView());
        });

        boolean isAdmin = AuthSession.getInstance().hasRole("Admin");
        boolean isProf = AuthSession.getInstance().hasRole("Professor");
        boolean isTA = AuthSession.getInstance().hasRole("TA");
        boolean isStaffGeneric = AuthSession.getInstance().hasRole("Staff")
                || AuthSession.getInstance().hasRole("Lecturer");
        boolean isStudent = AuthSession.getInstance().hasRole("Student");
        boolean isParent = AuthSession.getInstance().hasRole("parent")
                || AuthSession.getInstance().hasRole("Parent");

        if (isAdmin) {

            sidebar.getChildren().addAll(
                    studentsBtn,
                    staffBtn,
                    parentsBtn,
                    coursesBtn,
                    reservationsBtn,
                    inventoryBtn,
                    payrollBtn,
                    leaveBtn,
                    benefitsBtn,
                    messagesBtn,
                    eavBtn);
        } else if (isProf || isTA || isStaffGeneric) {

            sidebar.getChildren().addAll(
                    coursesBtn,
                    reservationsBtn,
                    inventoryBtn,
                    payrollBtn,
                    leaveBtn,
                    benefitsBtn,
                    assignmentsBtn,
                    quizzesBtn,
                    messagesBtn);
        } else if (isStudent) {

            sidebar.getChildren().addAll(
                    myProfileBtn,
                    coursesBtn,
                    assignmentsBtn,
                    quizzesBtn,
                    messagesBtn);
        } else if (isParent) {

            sidebar.getChildren().addAll(
                    parentDashboardBtn,
                    parentChatBtn

            );

        } else {

            sidebar.getChildren().addAll(
                    myProfileBtn,
                    coursesBtn,
                    messagesBtn);
        }

        sidebar.getChildren().add(logoutBtn);

        HBox header = new HBox();
        header.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #ddd;");
        header.setPadding(new Insets(10, 20, 10, 20));
        header.setAlignment(Pos.CENTER_RIGHT);

        Label userInfo = new Label("Logged in as: "
                + AuthSession.getInstance().getUsername()
                + " (" + String.join(", ", AuthSession.getInstance().getRoles()) + ")");
        userInfo.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14px;");
        header.getChildren().add(userInfo);

        Label welcome = new Label("Welcome to AGILE SIS\nSelect a module from the left menu.");
        welcome.setStyle("-fx-font-size: 18px; -fx-text-fill: #34495e;");
        welcome.setPadding(new Insets(30));

        view.setLeft(sidebar);
        view.setTop(header);
        view.setCenter(welcome);
        view.setStyle("-fx-background-color: #f4f6f7;");
    }

    private Button createMenuButton(String icon, String text) {
        Button btn = new Button(icon + "  " + text); // add icon before text
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-alignment: center-left;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-size: 14px; -fx-alignment: center-left;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-alignment: center-left;"));
        return btn;
    }

    public BorderPane getView() {
        return view;
    }
}
