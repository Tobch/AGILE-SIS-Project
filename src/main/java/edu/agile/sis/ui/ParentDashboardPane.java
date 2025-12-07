package edu.agile.sis.ui;

import edu.agile.sis.security.AuthSession;
import edu.agile.sis.service.*;
import edu.agile.sis.dao.SubmissionDAO;
import edu.agile.sis.dao.UserDAO;
import edu.agile.sis.model.Grade;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;

/**
 * ParentDashboardPane - messaging removed version.
 */
public class ParentDashboardPane extends BorderPane {

    // services
    private final ParentService parentService = new ParentService();
    private final EntityService parentEntityService = new EntityService("parents");
    private final UserDAO userDAO = new UserDAO();

    // new: assignment service + cache for assignmentId -> subject/course key
    private final AssignmentService assignmentService = tryCreateAssignmentService();
    private final java.util.Map<String, String> assignmentIdToSubjectCache = new java.util.concurrent.ConcurrentHashMap<>();

    // Controls shared across tabs
    private final ComboBox<String> childSelector = new ComboBox<>();
    private final TableView<Grade> gradesTable = new TableView<>();
    private final Label gpaLabel = new Label("GPA: -");

    // Profile tab controls
    private final TextField pFullName = new TextField();
    private final TextField pEmail = new TextField();
    private final Button saveProfileBtn = new Button("Save Profile");

    // Linked students
    private final ListView<String> linkedStudentsList = new ListView<>();

    // Submission service (assignments/submissions)
    private final SubmissionService submissionService = tryCreateSubmissionService();

    // helper: current parent linkedEntityId
    private final String parentLinkedId;

    // Course list (per selected student)
    private final ListView<String> coursesList = new ListView<>();
    private final CourseService courseService = new CourseService();
    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final QuizService quizService = new QuizService();

    // UI panes
    private final BorderPane studentPane = new BorderPane();
    private final BorderPane managePane = new BorderPane();
    private final ToggleGroup viewToggle = new ToggleGroup();

    // Background executor and simple spinner counter
    private final ExecutorService bgPool = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()/2));
    private final ProgressIndicator headerSpinner = new ProgressIndicator();

    // simple counter for concurrent tasks; used to show/hide spinner
    private final AtomicInteger runningTasks = new AtomicInteger(0);

    // cache courseCode -> courseTitle
    private final java.util.Map<String,String> courseCodeToNameCache = new java.util.concurrent.ConcurrentHashMap<>();

    // aggregated numeric score per subject key used by table cell factory and GPA
    private final java.util.Map<String, Double> aggregatedScores = new java.util.concurrent.ConcurrentHashMap<>();

    
    
    // map shown string -> real student id (keeps id hidden from UI)
private final java.util.Map<String,String> linkedStudentDisplayToId = new java.util.HashMap<>();

    public ParentDashboardPane() {
        setPadding(new Insets(12));
        parentLinkedId = AuthSession.getInstance().getLinkedEntityId();

        setupHeader();
        setupScreens();

        // initial lightweight load
        loadChildren();

        // when child selection changes refresh overview if visible
        childSelector.valueProperty().addListener((obs, oldV, newV) -> {
            String sid = getSelectedStudentId();
            if (sid != null && !sid.isBlank()) {
                if (getCenter() == studentPane) {
                    loadGrades(sid);
                    loadCourses(sid);
                }
            }
        });
    }

    /* ---------- header ---------- */
    private void setupHeader() {
        Label title = new Label("👪 Parent Dashboard");
        title.setStyle("-fx-font-size:18px; -fx-font-weight:bold;");

        VBox titleBox = new VBox(title, new Label("Manage linked students, profile and courses"));
        titleBox.setSpacing(2);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        childSelector.setPromptText("Select child (ID - Name)");
        childSelector.setPrefWidth(420);

        Button refresh = new Button("↻ Refresh");
        refresh.setOnAction(e -> {
            loadChildren();
            String sid = getSelectedStudentId();
            if (sid != null && !sid.isBlank()) {
                loadGrades(sid);
                loadCourses(sid);
            }
            loadLinkedStudents();
        });

        HBox controls = new HBox(8, new Label("Child:"), childSelector, refresh);
        controls.setAlignment(Pos.CENTER_RIGHT);

        headerSpinner.setPrefSize(18, 18);
        headerSpinner.setVisible(false);

        HBox headerBar = new HBox();
        headerBar.setPadding(new Insets(10));
        headerBar.setSpacing(12);
        headerBar.setStyle("-fx-background-color: linear-gradient(#f7fbff, #eef6ff); -fx-border-color: #d9e6ff; -fx-border-radius:8; -fx-background-radius:8;");
        headerBar.getChildren().addAll(titleBox, spacer, headerSpinner, controls);

        setTop(headerBar);
    }

    /* ---------- screens ---------- */
    private void setupScreens() {
        ToggleButton studentBtn = new ToggleButton("Student View");
        ToggleButton manageBtn  = new ToggleButton("Manage");
        studentBtn.setToggleGroup(viewToggle);
        manageBtn.setToggleGroup(viewToggle);
        studentBtn.setSelected(true);

        HBox switcher = new HBox(8, studentBtn, manageBtn);
        switcher.setAlignment(Pos.CENTER_RIGHT);
        switcher.setPadding(new Insets(6));

        if (getTop() instanceof HBox) {
            HBox headerBar = (HBox) getTop();
            headerBar.getChildren().add(switcher);
        }

        studentPane.setCenter(buildOverviewPane());

        TabPane manageTabs = new TabPane();
        manageTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        // Messages tab removed
        manageTabs.getTabs().addAll(
                new Tab("Profile", buildProfilePane()),
                new Tab("Linked Students", buildLinkedStudentsPane())
        );

        manageTabs.getSelectionModel().selectedItemProperty().addListener((obs, oldT, newT) -> {
            if (newT != null && "Linked Students".equals(newT.getText())) loadLinkedStudents();
            if (newT != null && "Profile".equals(newT.getText())) loadParentProfile();
        });

        managePane.setCenter(manageTabs);
        setCenter(studentPane);

        studentBtn.setOnAction(e -> {
            setCenter(studentPane);
            String sid = getSelectedStudentId();
            if (sid != null && !sid.isBlank()) {
                loadGrades(sid);
                loadCourses(sid);
            }
        });

        manageBtn.setOnAction(e -> {
            setCenter(managePane);
            loadParentProfile();
            loadLinkedStudents();
        });
    }

    /* ---------- background runner ---------- */
    private <T> void runBackground(Callable<T> backgroundWork,
                                   java.util.function.Consumer<T> onSuccess,
                                   java.util.function.Consumer<Throwable> onError) {
        Platform.runLater(() -> headerSpinner.setVisible(true));
        runningTasks.incrementAndGet();

        bgPool.submit(() -> {
            try {
                T result = backgroundWork.call();
                Platform.runLater(() -> {
                    try {
                        if (onSuccess != null) onSuccess.accept(result);
                    } catch (Exception e) { e.printStackTrace(); }
                });
            } catch (Throwable ex) {
                Platform.runLater(() -> {
                    try {
                        if (onError != null) onError.accept(ex);
                        else ex.printStackTrace();
                    } catch (Exception e) { e.printStackTrace(); }
                });
            } finally {
                int remaining = runningTasks.decrementAndGet();
                if (remaining <= 0) Platform.runLater(() -> headerSpinner.setVisible(false));
            }
        });
    }


    private BorderPane buildOverviewPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(8));

        TableColumn<Grade, String> subjCol = new TableColumn<>("Subject");
        subjCol.setCellValueFactory(p -> {
            Grade g = p.getValue();
            String title = g.getSubjectName();
            String code  = g.getSubjectId();
            if (title != null && !title.isBlank()) return new ReadOnlyStringWrapper(title);
            if (code != null && !code.isBlank()) return new ReadOnlyStringWrapper(code);
            return new ReadOnlyStringWrapper("-");
        });
        subjCol.setPrefWidth(360);

        TableColumn<Grade, String> gradeCol = new TableColumn<>("Grade");
        gradeCol.setCellValueFactory(p -> {
            Grade g = p.getValue();
            if (g == null) return new ReadOnlyStringWrapper("-");
            Object val = g.getGradeValue();
            // show letter and numeric if numeric available
            Double numeric = tryParseNumeric(val);
            String letter = formatGradeToLetter(val);
            if (numeric != null) return new ReadOnlyStringWrapper(letter + " (" + String.format("%.2f", numeric) + ")");
            return new ReadOnlyStringWrapper(letter);
        });
        gradeCol.setPrefWidth(160);

        gradesTable.getColumns().setAll(subjCol, gradeCol);
        gradesTable.setPrefHeight(360);
        gradesTable.setPlaceholder(new Label("No grades available for the selected child."));

        Label sectionTitle = new Label("Progress & Grades");
        sectionTitle.setStyle("-fx-font-weight:bold; -fx-font-size:13px;");

        VBox tableCard = new VBox(6);
        tableCard.setPadding(new Insets(10));
        tableCard.setStyle("-fx-background-color: white; -fx-border-color: #e0e6ef; -fx-border-radius:6; -fx-background-radius:6;");
        tableCard.getChildren().addAll(sectionTitle, gradesTable);

        HBox gpaRow = new HBox();
        gpaRow.setAlignment(Pos.CENTER_RIGHT);
        Label spacer = new Label();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        gpaLabel.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-padding:6 0 0 0;");
        gpaRow.getChildren().addAll(spacer, gpaLabel);
        tableCard.getChildren().add(gpaRow);

        coursesList.setPrefHeight(360);
        coursesList.setPlaceholder(new Label("No courses found for selected child."));
        coursesList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String sel = coursesList.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    if (getSelectedStudentId() != null) {
                        new CourseProgressStage(enrollmentService, courseService, quizService, submissionService, parentService, getSelectedStudentId()).show();
                    }
                }
            }
        });

        Button viewCourseBtn = new Button("View Course");
        viewCourseBtn.setOnAction(e -> {
            String sel = coursesList.getSelectionModel().getSelectedItem();
            if (sel == null) { alert(Alert.AlertType.INFORMATION, "Select course", "Please select a course."); return; }
            new CourseProgressStage(enrollmentService, courseService, quizService, submissionService, parentService, getSelectedStudentId()).show();
        });

        VBox coursesCard = new VBox(8);
        coursesCard.setPadding(new Insets(10));
        coursesCard.setStyle("-fx-background-color: white; -fx-border-color: #f0f4f8; -fx-border-radius:6; -fx-background-radius:6;");
        HBox courseHeader = new HBox(8, new Label("Courses:"), viewCourseBtn);
        courseHeader.setAlignment(Pos.CENTER_LEFT);
        coursesCard.getChildren().addAll(courseHeader, coursesList);

        HBox content = new HBox(12, tableCard, coursesCard);
        HBox.setHgrow(tableCard, Priority.ALWAYS);
        tableCard.setPrefWidth(820);
        coursesCard.setPrefWidth(340);

        pane.setCenter(content);
        return pane;
    }

 private BorderPane buildProfilePane() {
    BorderPane pane = new BorderPane();
    pane.setPadding(new Insets(12));

    VBox card = new VBox(14);
    card.setPadding(new Insets(18));
    card.setStyle(
        "-fx-background-color: white;" +
        "-fx-border-color: #dfe6f3;" +
        "-fx-border-radius: 10;" +
        "-fx-background-radius: 10;"
    );
    card.setMaxWidth(600);

    // Title
    Label title = new Label("Profile");
    title.setStyle("-fx-font-size:16px; -fx-font-weight:bold;");

    Label subtitle = new Label("Update your full name and email.");
    subtitle.setStyle("-fx-text-fill: #555; -fx-font-size:12px;");
    subtitle.setWrapText(true);

    // Full name field
    VBox nameBox = new VBox(4);
    Label nameLabel = new Label("Full Name");
    nameLabel.setStyle("-fx-font-size:12px;");
    pFullName.setPromptText("e.g. Ahmed Ali");
    pFullName.setStyle("-fx-font-size:13px;");
    nameBox.getChildren().addAll(nameLabel, pFullName);

    // Email field
    VBox emailBox = new VBox(4);
    Label emailLabel = new Label("Email");
    emailLabel.setStyle("-fx-font-size:12px;");
    pEmail.setPromptText("e.g. example@mail.com");
    pEmail.setStyle("-fx-font-size:13px;");
    emailBox.getChildren().addAll(emailLabel, pEmail);

    // Save button
    saveProfileBtn.setText("Save Profile");
    saveProfileBtn.setStyle(
        "-fx-background-color:#2b7cff;" +
        "-fx-text-fill:white;" +
        "-fx-font-weight:bold;" +
        "-fx-padding:8 18;" +
        "-fx-background-radius:6;"
    );
    saveProfileBtn.setOnAction(e -> onSaveProfile());

    HBox buttonRow = new HBox(saveProfileBtn);
    buttonRow.setAlignment(Pos.CENTER_RIGHT);

    // Add all to card
    card.getChildren().addAll(title, subtitle, nameBox, emailBox, buttonRow);

    // Center layout
    StackPane center = new StackPane(card);
    center.setPadding(new Insets(12));
    pane.setCenter(center);

    return pane;
}


    private void loadParentProfile() {
        if (parentLinkedId == null) return;
        runBackground(() -> parentEntityService.getEntityById(parentLinkedId),
                doc -> {
                    if (doc == null) {
                        Document u = userDAO.findByLinkedEntityId(parentLinkedId);
                        if (u != null) pFullName.setText(u.getString("username"));
                        return;
                    }
                    Document core = doc.get("core", Document.class);
                    if (core != null) {
                        pFullName.setText(core.getString("fullName"));
                        pEmail.setText(core.getString("email"));
                    }
                }, ex -> {
                    ex.printStackTrace();
                });
    }

    private void onSaveProfile() {
        if (parentLinkedId == null) {
            alert(Alert.AlertType.WARNING, "Missing ID", "Parent linkedEntityId not available.");
            return;
        }

        runBackground(() -> {
            Document existing = parentEntityService.getEntityById(parentLinkedId);
            if (existing == null) throw new RuntimeException("Parent entity not found for ID: " + parentLinkedId);

            Document core = existing.get("core", Document.class);
            if (core == null) core = new Document();
            core.put("fullName", pFullName.getText().trim());
            core.put("email", pEmail.getText().trim());
            existing.put("core", core);

            List<Document> attrs = existing.getList("attributes", Document.class);
            if (attrs == null) attrs = new ArrayList<>();

            boolean saved = parentEntityService.updateEntityMerge(parentLinkedId, core, attrs);
            if (!saved) throw new RuntimeException("Failed to save profile.");
            return null;
        }, r -> {
            alert(Alert.AlertType.INFORMATION, "Saved", "Profile saved successfully.");
        }, ex -> {
            ex.printStackTrace();
            alert(Alert.AlertType.ERROR, "Error", "Failed to save profile: " + ex.getMessage());
        });
    }

    /* ---------- Linked Students Pane ---------- */
    private BorderPane buildLinkedStudentsPane() {
    BorderPane pane = new BorderPane();
    pane.setPadding(new Insets(8));

    linkedStudentsList.setPrefWidth(420);
    linkedStudentsList.setPlaceholder(new Label("No linked students."));

    Button viewBtn = new Button("View Profile");
    Button unlinkBtn = new Button("Unlink Student");
    Button refreshBtn = new Button("Refresh");

    // Determine whether current user is allowed to unlink (try a few common AuthSession APIs reflectively)
    boolean canUnlink = false;
    try {
        Object session = AuthSession.getInstance();
        // try common forms: hasRole("admin") or getRole()/getRoles()
        try {
            java.lang.reflect.Method m = session.getClass().getMethod("hasRole", String.class);
            Object r = m.invoke(session, "admin");
            if (r instanceof Boolean && ((Boolean) r)) canUnlink = true;
        } catch (NoSuchMethodException ignored) {}
        if (!canUnlink) {
            try {
                java.lang.reflect.Method m2 = session.getClass().getMethod("getRole");
                Object r2 = m2.invoke(session);
                if (r2 != null && r2.toString().toLowerCase().contains("admin")) canUnlink = true;
            } catch (NoSuchMethodException ignored) {}
        }
        if (!canUnlink) {
            try {
                java.lang.reflect.Method m3 = session.getClass().getMethod("getRoles");
                Object r3 = m3.invoke(session);
                if (r3 != null && r3.toString().toLowerCase().contains("admin")) canUnlink = true;
            } catch (NoSuchMethodException ignored) {}
        }
    } catch (Throwable ignored) {}

    // If the user is a parent (or detection failed), keep unlink hidden
    unlinkBtn.setVisible(canUnlink);
    unlinkBtn.setManaged(canUnlink); // prevents layout gap when hidden

    viewBtn.setOnAction(e -> {
        String display = linkedStudentsList.getSelectionModel().getSelectedItem();
        if (display == null) return;
        String sid = linkedStudentDisplayToId.get(display);
        if (sid == null) {
            alert(Alert.AlertType.WARNING, "Not found", "Student id not available for this entry.");
            return;
        }
        runBackground(() -> new EntityService("students").getEntityById(sid),
                doc -> {
                    if (doc == null) { alert(Alert.AlertType.WARNING, "Not found", "Student not found: " + sid); return; }
                    new StudentDetailDialog(doc, new EntityService("students"), true).showAndWait();
                }, ex -> {
                    ex.printStackTrace();
                });
    });

    unlinkBtn.setOnAction(e -> {
        String display = linkedStudentsList.getSelectionModel().getSelectedItem();
        if (display == null) return;
        String sid = linkedStudentDisplayToId.get(display);
        if (sid == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Unlink student " + display + " from this parent?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(b -> {
            if (b == ButtonType.YES) {
                runBackground(() -> {
                    boolean ok = userDAO.setParentForStudent(sid, null);
                    if (!ok) throw new RuntimeException("Failed to unlink student.");
                    return null;
                }, r -> {
                    alert(Alert.AlertType.INFORMATION, "Unlinked", "Student unlinked successfully.");
                    loadLinkedStudents();
                    loadChildren();
                }, ex -> {
                    alert(Alert.AlertType.ERROR, "Failed", "Failed to unlink student.");
                    ex.printStackTrace();
                });
            }
        });
    });

    refreshBtn.setOnAction(e -> loadLinkedStudents());

    HBox controls = new HBox(8, viewBtn, unlinkBtn, refreshBtn);
    controls.setPadding(new Insets(8));
    controls.setAlignment(Pos.CENTER_LEFT);

    VBox container = new VBox(8, new Label("Linked Students"), controls, linkedStudentsList);
    container.setPadding(new Insets(8));
    container.setStyle("-fx-background-color:#ffffff; -fx-border-color:#eef3ff; -fx-border-radius:6; -fx-background-radius:6;");

    pane.setCenter(container);
    return pane;
}
    
    
    
    private void loadLinkedStudents() {
    linkedStudentsList.getItems().clear();
    linkedStudentDisplayToId.clear();

    if (parentLinkedId == null) return;
    linkedStudentsList.setPlaceholder(new Label("Loading..."));

    runBackground(() -> parentService.getLinkedStudents(parentLinkedId), students -> {
        linkedStudentsList.getItems().clear();
        linkedStudentDisplayToId.clear();

        if (students == null || students.isEmpty()) {
            linkedStudentsList.setPlaceholder(new Label("No linked students."));
            return;
        }

        for (Document s : students) {
            String id = extractId(s);

            // Resolve a friendly name for display
            String name = null;
            if (s.containsKey("fullName")) name = s.getString("fullName");
            if ((name == null || name.isBlank()) && s.containsKey("username")) name = s.getString("username");

            if ((name == null || name.isBlank())) {
                Document core = s.get("core", Document.class);
                if (core != null) {
                    String fn = core.getString("firstName"), ln = core.getString("lastName");
                    name = ((fn == null ? "" : fn) + " " + (ln == null ? "" : ln)).trim();
                }
            }

            if (name == null || name.isBlank()) {
                // final fallback: shortened id (optional)
                if (id != null && id.length() > 8) name = id.substring(0,8) + "...";
                else name = safeString(id, "Unknown student");
            }

            // display only the name (no hash). ensure display is unique by appending a numeric suffix if needed
            String display = name;
            int suffix = 1;
            while (linkedStudentDisplayToId.containsKey(display)) {
                display = name + " (" + (++suffix) + ")";
            }

            linkedStudentDisplayToId.put(display, id);
            linkedStudentsList.getItems().add(display);
        }
    }, ex -> {
        linkedStudentsList.setPlaceholder(new Label("Failed to load linked students."));
        ex.printStackTrace();
    });
}



    private void loadChildren() {
        childSelector.getItems().clear();
        if (parentLinkedId == null) return;

        childSelector.setPromptText("Loading children...");
        childSelector.setDisable(true);

        runBackground(() -> parentService.getLinkedStudents(parentLinkedId), students -> {
            childSelector.getItems().clear();
            if (students == null || students.isEmpty()) {
                childSelector.setPromptText("No linked children");
            } else {
                for (Document s : students) {
                    String entityId = parentEntityService.getEntityId(s);
                    if (entityId == null || entityId.isBlank()) entityId = extractId(s);

                    String name = s.getString("fullName");
                    if (name == null || name.isBlank()) {
                        Document core = s.get("core", Document.class);
                        if (core != null) {
                            String fn = core.getString("firstName"), ln = core.getString("lastName");
                            name = ((fn == null ? "" : fn) + " " + (ln == null ? "" : ln)).trim();
                        }
                    }
                    if (name == null || name.isBlank()) name = s.getString("username");
                    childSelector.getItems().add(entityId + " - " + name);
                }
                if (!childSelector.getItems().isEmpty() && childSelector.getSelectionModel().isEmpty()) {
                    childSelector.getSelectionModel().selectFirst();
                }
            }
            childSelector.setDisable(false);
            childSelector.setPromptText("Select child (ID - Name)");
        }, ex -> {
            childSelector.setDisable(false);
            childSelector.setPromptText("Failed to load children");
            ex.printStackTrace();
        });
    }

    private void loadGrades(String studentId) {
        gradesTable.getItems().clear();
        gpaLabel.setText("GPA: -");
        aggregatedScores.clear();

        if (studentId == null || studentId.isBlank()) return;

        gradesTable.setPlaceholder(new Label("Loading grades..."));

        runBackground(() -> parentService.getAggregatedGradesForStudent(studentId),
            rawGrades -> {
                if (rawGrades == null || rawGrades.isEmpty()) {

                    Platform.runLater(() -> {
                        gradesTable.getItems().clear();
                        gradesTable.setPlaceholder(new Label("No grades available for the selected child."));
                        gpaLabel.setText("GPA: -");
                    });
                    return;
                }

                List<Grade> displayRows = new ArrayList<>();

                for (Grade g : rawGrades) {

                    String resolved = resolveSubjectKeyForGrade(g);
                    if (resolved == null || resolved.isBlank()) {
                        resolved = (g.getSubjectId() == null || g.getSubjectId().isBlank())
                                ? ("__anon__" + UUID.randomUUID().toString())
                                : g.getSubjectId();
                    }

                    String subjectTitle = null;
                    try {
                        subjectTitle = courseCodeToNameCache.get(resolved);
                        if (subjectTitle == null) {

                            Document courseDoc = null;
                            try {
                                courseDoc = courseService.findByCode(resolved);
                            } catch (Throwable t) {
                            }
                            if (courseDoc != null) {
                                subjectTitle = courseDoc.getString("title");
                                if (subjectTitle == null) subjectTitle = courseDoc.getString("name");
                                if (subjectTitle == null) subjectTitle = courseDoc.getString("courseTitle");
                                if (subjectTitle == null) subjectTitle = courseDoc.getString("displayName");
                                if (subjectTitle == null) subjectTitle = courseDoc.getString("courseName");
                            }

                            if ((subjectTitle == null || subjectTitle.isBlank()) && g.getSubjectName() != null && !g.getSubjectName().isBlank()) {
                                subjectTitle = g.getSubjectName();
                            }

                            if ((subjectTitle == null || subjectTitle.isBlank())) {
                                try {
                                    Object gv = g.getGradeValue();
                                    if (gv instanceof Document) {
                                        Document d = (Document) gv;
                                        Object hint = d.get("courseTitle");
                                        if (hint == null) hint = d.get("title");
                                        if (hint == null) hint = d.get("name");
                                        if (hint != null) subjectTitle = hint.toString();
                                    }
                                } catch (Throwable ignored) {}
                            }

                            if (subjectTitle == null || subjectTitle.isBlank()) subjectTitle = resolved;

                            courseCodeToNameCache.put(resolved, subjectTitle);
                        }
                    } catch (Throwable t) {
                        if (subjectTitle == null || subjectTitle.isBlank()) {
                            subjectTitle = (g.getSubjectName() != null && !g.getSubjectName().isBlank()) ? g.getSubjectName() : resolved;
                        }
                    }

                    Double numeric = tryParseNumeric(g.getGradeValue());
                    double subjectTotal = numeric == null ? Double.NaN : numeric.doubleValue();

                    try {
                        aggregatedScores.put(resolved, Double.isNaN(subjectTotal) ? 0.0 : subjectTotal);
                    } catch (Throwable ignored) {}

                    Double credits = null;
                    try {
                        java.lang.reflect.Method mc = g.getClass().getMethod("getCredits");
                        Object cred = mc.invoke(g);
                        if (cred instanceof Number) credits = ((Number) cred).doubleValue();
                        else if (cred != null) credits = Double.parseDouble(cred.toString());
                    } catch (Throwable ignored) {}

                    String gradeValStr;
                    if (!Double.isNaN(subjectTotal)) {
                        gradeValStr = String.format(Locale.ROOT, "%.2f", subjectTotal);
                    } else {
                        Object raw = g.getGradeValue();
                        gradeValStr = raw == null ? "-" : raw.toString();
                    }

                    Grade displayGrade = new Grade(resolved, subjectTitle, gradeValStr, credits == null ? 1.0 : credits);
                    displayRows.add(displayGrade);
                }

                // stable sort by subjectName (human title) or id
                displayRows.sort((a, b) -> {
                    String an = a.getSubjectName() == null ? a.getSubjectId() : a.getSubjectName();
                    String bn = b.getSubjectName() == null ? b.getSubjectId() : b.getSubjectName();
                    if (an == null) an = ""; if (bn == null) bn = "";
                    return an.compareToIgnoreCase(bn);
                });

                double totalPoints = 0.0;
                int count = 0;
                for (Double val : aggregatedScores.values()) {
                    if (val == null) continue;
                    String letter = numericToLetter(val);
                    int pts = letterToPoints(letter);
                    totalPoints += pts;
                    count++;
                }
                double gpa = count == 0 ? 0.0 : (totalPoints / (double) count);

                Platform.runLater(() -> {
                    gradesTable.getItems().setAll(displayRows);
                    if (displayRows.isEmpty()) {
                        gradesTable.setPlaceholder(new Label("No grades available for the selected child."));
                        gpaLabel.setText("GPA: -");
                    } else {
                        gpaLabel.setText("GPA: " + String.format(Locale.ROOT, "%.2f", gpa));
                    }
                });
            },
            ex -> {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    gradesTable.setPlaceholder(new Label("Failed to load grades."));
                    gpaLabel.setText("GPA: -");
                });
            });
    }

    private String resolveSubjectKeyForGrade(Grade g) {
        if (g == null) return null;

        // 1) direct getters
        String sid = g.getSubjectId();
        if (sid != null && !sid.isBlank()) return sid;
        String sname = g.getSubjectName();
        if (sname != null && !sname.isBlank()) return sname;

        try {
            Object gv = g.getGradeValue();
            if (gv instanceof Document) {
                Document d = (Document) gv;
                Object subj = d.get("subjectId");
                if (subj == null) subj = d.get("courseId");
                if (subj == null) subj = d.get("courseCode");
                if (subj != null) return subj.toString();
            }
        } catch (Throwable ignored) {}

        String assignmentId = null;
        try {
            // first try gradeValue Document
            Object gv = g.getGradeValue();
            if (gv instanceof Document) {
                Object a = ((Document) gv).get("assignmentId");
                if (a != null) assignmentId = a.toString();
            }
        } catch (Throwable ignored) {}

        if ((assignmentId == null || assignmentId.isBlank())) {
            try {
                java.lang.reflect.Method m = g.getClass().getMethod("getAssignmentId");
                Object v = m.invoke(g);
                if (v != null) assignmentId = v.toString();
            } catch (NoSuchMethodException ignored) {} catch (Throwable ignored) {}
        }

        if (assignmentId != null && !assignmentId.isBlank()) {
            String cached = assignmentIdToSubjectCache.get(assignmentId);
            if (cached != null) return cached;

            if (assignmentService != null) {
                try {
                    Document a = assignmentService.getById(assignmentId);
                    if (a != null) {
                        String courseCode = a.getString("courseCode");
                        if (courseCode == null || courseCode.isBlank()) courseCode = a.getString("subjectId");
                        if ((courseCode == null || courseCode.isBlank()) && a.containsKey("core")) {
                            Document core = a.get("core", Document.class);
                            if (core != null) courseCode = core.getString("subjectId");
                        }
                        if (courseCode != null && !courseCode.isBlank()) {
                            assignmentIdToSubjectCache.put(assignmentId, courseCode);
                            return courseCode;
                        }
                    }
                } catch (Throwable t) {
                }
            }

            String synthetic = "assignment::" + assignmentId;
            assignmentIdToSubjectCache.put(assignmentId, synthetic);
            return synthetic;
        }

        try {
            java.lang.reflect.Method mid = g.getClass().getMethod("getId");
            Object vid = mid.invoke(g);
            if (vid != null) return "grade::" + vid.toString();
        } catch (Throwable ignored) {}

        // final fallback
        return "grade::" + UUID.randomUUID().toString();
    }

    private String getSelectedStudentId() {
        String v = childSelector.getValue();
        if (v == null) return null;
        return v.split(" - ")[0];
    }

    private String extractId(Document d) {
        if (d == null) return "";
        Object idObj = d.get("_id");
        if (idObj instanceof org.bson.types.ObjectId) return ((org.bson.types.ObjectId) idObj).toHexString();
        return idObj != null ? idObj.toString() : (d.getString("linkedEntityId") == null ? "" : d.getString("linkedEntityId"));
    }

    private static String safeString(String val, String fallback) {
        if (val != null && !val.isBlank()) return val;
        return fallback != null ? fallback : "-";
    }

    private Double getNumericFromGrade(Grade g) {
        Double d = getNumericScoreFromGrade(g);
        if (d == null) return Double.NaN;
        return d;
    }

    private Double tryParseNumeric(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            return Double.parseDouble(v.toString());
        } catch (Throwable ignored) {}
        return null;
    }

    private Double getNumericScoreFromGrade(Grade g) {
        if (g == null) return null;
        Object gv = g.getGradeValue();
        if (gv != null) {
            if (gv instanceof Number) return ((Number) gv).doubleValue();
            try { return Double.parseDouble(gv.toString()); } catch (Throwable ignored) {}
        }

        String[] getters = new String[] { "getScore", "getValue", "getNumericValue", "getPoints", "getMark" };
        for (String mname : getters) {
            try {
                java.lang.reflect.Method mm = g.getClass().getMethod(mname);
                Object v = mm.invoke(g);
                if (v == null) continue;
                if (v instanceof Number) return ((Number) v).doubleValue();
                try { return Double.parseDouble(v.toString()); } catch (Throwable ignored) {}
            } catch (NoSuchMethodException ignored) {}
            catch (Throwable ignored) {}
        }

        // last attempt: attributes map if gradeValue is Document
        try {
            if (gv instanceof Document) {
                Document doc = (Document) gv;
                Object v = doc.get("score");
                if (v == null) v = doc.get("value");
                if (v != null) {
                    if (v instanceof Number) return ((Number) v).doubleValue();
                    try { return Double.parseDouble(v.toString()); } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }

    private String gradeTypeHint(Grade g) {
        if (g == null) return null;
        // attempt reflection-ish for common fields
        String[] candidates = new String[] {
                "getType", "getAssessmentType", "getCategory", "getItemType", "getKind", "getName", "getTitle"
        };
        for (String m : candidates) {
            try {
                java.lang.reflect.Method mm = g.getClass().getMethod(m);
                Object o = mm.invoke(g);
                if (o != null) return o.toString();
            } catch (NoSuchMethodException ignored) {} catch (Throwable ignore) {}
        }

        // try gradeValue Document "type"/"category"
        Object gv = g.getGradeValue();
        if (gv instanceof Document) {
            Document d = (Document) gv;
            Object t = d.get("type");
            if (t != null) return t.toString();
            t = d.get("category");
            if (t != null) return t.toString();
        }

        return null;
    }

    private static String formatGradeToLetter(Object gradeVal) {
        if (gradeVal == null) return "-";
        // if numeric value
        if (gradeVal instanceof Number) {
            return numericToLetter(((Number) gradeVal).doubleValue());
        }
        // if string letter already
        if (gradeVal instanceof String) {
            String s = ((String) gradeVal).trim();
            if (s.matches("(?i)^[ABCDF]$")) return s.toUpperCase();
            // try parse numeric
            try {
                double v = Double.parseDouble(s);
                return numericToLetter(v);
            } catch (Exception ignored) {}
        }
        // try parse to double
        try {
            double v = Double.parseDouble(gradeVal.toString());
            return numericToLetter(v);
        } catch (Throwable ignored) {}
        return "-";
    }

    private static String numericToLetter(double v) {
        if (v >= 90.0) return "A";
        if (v >= 80.0) return "B";
        if (v >= 70.0) return "C";
        if (v >= 60.0) return "D";
        return "F";
    }

    private void alert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t, msg, ButtonType.OK);
        a.setTitle(title);
        a.showAndWait();
    }

    private static int letterToPoints(String letter) {
        if (letter == null) return 0;
        switch (letter.toUpperCase()) {
            case "A": return 4;
            case "B": return 3;
            case "C": return 2;
            case "D": return 1;
            default:  return 0;
        }
    }

  

    private void loadCourses(String studentId) {
        // quick UI reset on FX thread
        Platform.runLater(() -> {
            coursesList.getItems().clear();
            if (studentId == null || studentId.isBlank()) {
                coursesList.setPlaceholder(new Label("No courses found for selected child."));
                return;
            }
            coursesList.setPlaceholder(new Label("Loading courses..."));
        });

        if (studentId == null || studentId.isBlank()) {
            Platform.runLater(() -> coursesList.setPlaceholder(new Label("No courses found for selected child.")));
            return;
        }

        runBackground(() -> {
            // fetch enrollments
            Object rawRegs;
            try {
                rawRegs = enrollmentService.listByStudent(studentId);
            } catch (Throwable t) {
                return null;
            }

            List<?> regs;
            if (rawRegs instanceof List) regs = (List<?>) rawRegs;
            else regs = java.util.Arrays.asList(rawRegs);

            Set<String> codes = new LinkedHashSet<>();
            Map<String,String> enrollmentTitleHints = new HashMap<>();

            for (Object r : regs) {
                String courseCode = null;
                String titleHint = null;

                if (r instanceof org.bson.Document) {
                    org.bson.Document doc = (org.bson.Document) r;
                    courseCode = doc.getString("courseCode");
                    if (courseCode == null) courseCode = doc.getString("code");
                    titleHint = doc.getString("courseTitle");
                    if (titleHint == null) titleHint = doc.getString("title");
                    if (titleHint == null) titleHint = doc.getString("name");
                } else if (r instanceof Map) {
                    Map<?,?> map = (Map<?,?>) r;
                    Object v = map.get("courseCode");
                    if (v == null) v = map.get("code");
                    if (v != null) courseCode = v.toString();
                    Object t = map.get("courseTitle");
                    if (t == null) t = map.get("title");
                    if (t == null) t = map.get("name");
                    if (t != null) titleHint = t.toString();
                } else {
                    String s = r.toString();
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:courseCode|code|course)[:=]\\s*([A-Za-z0-9_-]+)").matcher(s);
                    if (m.find()) courseCode = m.group(1);
                }

                if (courseCode == null || courseCode.isBlank()) continue;
                codes.add(courseCode);
                if (titleHint != null && !titleHint.isBlank()) enrollmentTitleHints.put(courseCode, titleHint);
            }

            // resolve names using cache + CourseService.findByCode
            Map<String,String> resolved = new LinkedHashMap<>();
            for (String code : codes) {
                String name = courseCodeToNameCache.get(code);
                if (name != null) {
                    resolved.put(code, name);
                    continue;
                }

                String hint = enrollmentTitleHints.get(code);
                if (hint != null && !hint.isBlank()) {
                    courseCodeToNameCache.put(code, hint);
                    resolved.put(code, hint);
                    continue;
                }

                // use CourseService.findByCode (direct call - no reflection)
                try {
                    org.bson.Document courseDoc = courseService.findByCode(code);
                    if (courseDoc != null) {
                        String title = courseDoc.getString("title");
                        if (title == null) title = courseDoc.getString("name");
                        if (title == null) title = courseDoc.getString("courseTitle");
                        if (title == null) title = courseDoc.getString("displayName");
                        if (title == null) title = courseDoc.getString("courseName");
                        if (title != null && !title.isBlank()) {
                            courseCodeToNameCache.put(code, title);
                            resolved.put(code, title);
                            continue;
                        }
                    }
                } catch (Throwable t) {
                    // ignore and fallback to code
                }

                // fallback
                courseCodeToNameCache.put(code, code);
                resolved.put(code, code);
            }

            return resolved;
        }, (Object result) -> {
            // UI update
            Platform.runLater(() -> {
                coursesList.getItems().clear();
                if (result == null) {
                    coursesList.setPlaceholder(new Label("No courses found for selected child."));
                    return;
                }
                @SuppressWarnings("unchecked")
                Map<String,String> resolved = (Map<String,String>) result;
                for (Map.Entry<String,String> e : resolved.entrySet()) {
                    String code = e.getKey();
                    String title = e.getValue();
                    coursesList.getItems().add(code + " — " + title);
                }

                // better renderer + tooltip
                coursesList.setCellFactory(lv -> new ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setTooltip(null);
                        } else {
                            setText(item);
                            setTooltip(new Tooltip(item));
                        }
                    }
                });

                if (coursesList.getItems().isEmpty()) {
                    coursesList.setPlaceholder(new Label("No courses found for selected child."));
                }
            });
        }, ex -> {
            ex.printStackTrace();
            Platform.runLater(() -> coursesList.setPlaceholder(new Label("Failed to load courses.")));
        });
    }

    /**
     * Force refresh grades & courses for the given student.
     * Clears local caches to avoid stale displays and triggers reload.
     */
    public void refreshGradesForStudent(String studentId) {
        if (studentId == null || studentId.isBlank()) return;

        try { courseCodeToNameCache.clear(); } catch (Throwable ignored) {}
        try { assignmentIdToSubjectCache.clear(); } catch (Throwable ignored) {}
        try { aggregatedScores.clear(); } catch (Throwable ignored) {}

        // loadGrades/loadCourses already use runBackground/Platform.runLater, so safe to call here
        loadGrades(studentId);
        loadCourses(studentId);
    }

    // Ensure pool is shutdown if this pane is disposed (caller/window should call this on close)
    public void shutdownBackgroundPool() {
        bgPool.shutdownNow();
    }

    /* ----- helpers to construct services safely ----- */
    private static SubmissionService tryCreateSubmissionService() {
        try { return new SubmissionService(); } catch (Throwable ignored) { return null; }
    }
    private static AssignmentService tryCreateAssignmentService() {
        try { return new AssignmentService(); } catch (Throwable ignored) { return null; }
    }
}
