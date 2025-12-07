package edu.agile.sis.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.bson.Document;
import edu.agile.sis.service.*;
import edu.agile.sis.model.Grade;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import javafx.beans.property.ReadOnlyStringWrapper;


public class CourseProgressStage extends Stage {

    private final EnrollmentService enrollmentService;
    private final CourseService courseService;
    private final QuizService quizService;
    private final SubmissionService submissionService;
    private final ParentService parentService;
    private final AssignmentService assignmentService; 

    private final String studentId;


    private final TextField searchField = new TextField();
    private final Button refreshCoursesBtn = new Button("\u21bb"); // refresh glyph
    private final Label coursesHeaderLabel = new Label("Courses");
    private final Label coursesCountLabel = new Label("");
    private final ListView<String> coursesList = new ListView<>();
    private final TabPane tabs = new TabPane();
    private final ProgressIndicator progress = new ProgressIndicator();

    private final ExecutorService pool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "course-progress-bg"); t.setDaemon(true); return t;
    });

   
    private final List<String> allCourses = new ArrayList<>();

    
    private static final double BUCKET_FINAL_MAX = 60.0;

    public CourseProgressStage(EnrollmentService enrollmentService,
                               CourseService courseService,
                               QuizService quizService,
                               SubmissionService submissionService,
                               ParentService parentService,
                               String studentId) {
        this.enrollmentService = enrollmentService;
        this.courseService = courseService;
        this.quizService = quizService;
        this.submissionService = submissionService;
        this.parentService = parentService;
        this.studentId = studentId;

        AssignmentService as;
        try { as = new AssignmentService(); } catch (Throwable t) { as = null; }
        this.assignmentService = as;

        createUI();
        loadCourses();

        setOnCloseRequest(e -> pool.shutdownNow());
    }

    private void createUI() {
        setTitle("Course Progress - Student: " + (studentId == null ? "-" : studentId));

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));


        HBox toolbar = new HBox(8);
        toolbar.setPadding(new Insets(6, 0, 12, 0));
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Course Progress");
        title.setStyle("-fx-font-size:16px; -fx-font-weight:bold;");

        Label studentLabel = new Label("Student:");
        studentLabel.setStyle("-fx-opacity:0.8; -fx-padding:0 6 0 12;");

        Label studentIdLabel = new Label(studentId == null ? "-" : studentId);
        studentIdLabel.setStyle("-fx-font-weight:bold; -fx-background-color:#eef3ff; -fx-padding:4 8 4 8; -fx-border-radius:6; -fx-background-radius:6;");

        searchField.setPromptText("Filter courses (type to search)");
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((obs, oldV, newV) -> filterCourses(newV));

        refreshCoursesBtn.setTooltip(new Tooltip("Refresh courses"));
        refreshCoursesBtn.setOnAction(e -> loadCourses());

        ProgressIndicator smallSpinner = new ProgressIndicator();
        smallSpinner.setPrefSize(16, 16);
        smallSpinner.setVisible(false);

        progress.visibleProperty().addListener((obs, o, n) -> smallSpinner.setVisible(n));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(title, studentLabel, studentIdLabel, spacer, searchField, refreshCoursesBtn, smallSpinner);


        VBox leftCard = new VBox(8);
        leftCard.setPadding(new Insets(10));
        leftCard.setStyle("-fx-background-color: white; -fx-border-color: #e8eef7; -fx-border-radius:8; -fx-background-radius:8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 8,0,0,4);");

        HBox leftHeader = new HBox(8);
        leftHeader.setAlignment(Pos.CENTER_LEFT);
        coursesHeaderLabel.setStyle("-fx-font-weight:bold; -fx-font-size:13px;");
        coursesCountLabel.setStyle("-fx-opacity:0.7;");
        leftHeader.getChildren().addAll(coursesHeaderLabel, coursesCountLabel);

        coursesList.setPrefWidth(300);
        coursesList.setPrefHeight(520);
        coursesList.getStyleClass().add("course-list");
        coursesList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                String code = newV.split(" - ")[0];
                loadCourseDetails(code);
            }
        });

        HBox leftFooter = new HBox(8);
        leftFooter.setAlignment(Pos.CENTER_LEFT);
        Button openStageBtn = new Button("Open in Window");
        openStageBtn.setOnAction(e -> {
            String sel = coursesList.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert(Alert.AlertType.INFORMATION, "Pick a course", "Please select a course on the left first.");
                return;
            }
            showAlert(Alert.AlertType.INFORMATION, "Course selected", "Course: " + sel);
        });
        leftFooter.getChildren().addAll(openStageBtn);

        leftCard.getChildren().addAll(leftHeader, new Separator(), coursesList, leftFooter);


        VBox centerCard = new VBox(10);
        centerCard.setPadding(new Insets(12));
        centerCard.setStyle("-fx-background-color: white; -fx-border-color: #e8eef7; -fx-border-radius:8; -fx-background-radius:8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.03), 6,0,0,3);");

        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab quizzesTab = new Tab("Quizzes", new Label("Select a course to load quizzes"));
        Tab assignmentsTab = new Tab("Assignments", new Label("Select a course to load assignments"));
        Tab finalTab = new Tab("Final / Summary", new Label("Select a course to load final exam grade"));
        tabs.getTabs().addAll(quizzesTab, assignmentsTab, finalTab);
        tabs.setPrefHeight(520);

 
        HBox tabActions = new HBox(8);
        tabActions.setAlignment(Pos.CENTER_RIGHT);
        Button refreshDetailsBtn = new Button("Refresh details");
        refreshDetailsBtn.setOnAction(e -> {
            String sel = coursesList.getSelectionModel().getSelectedItem();
            if (sel != null) loadCourseDetails(sel.split(" - ")[0]);
        });
        tabActions.getChildren().addAll(refreshDetailsBtn);

        StackPane centerStack = new StackPane(centerCard);
        centerStack.setPadding(new Insets(2));
        StackPane.setAlignment(progress, Pos.CENTER);
        progress.setVisible(false);

        centerCard.getChildren().addAll(tabs, tabActions);

        root.setTop(toolbar);

        HBox main = new HBox(12, leftCard, centerStack);
        HBox.setHgrow(centerStack, Priority.ALWAYS);
        main.setAlignment(Pos.TOP_LEFT);

        root.setCenter(main);

        Scene sc = new Scene(root, 980, 600);
        sc.getRoot().setStyle("-fx-font-family: 'Segoe UI', 'Arial'; -fx-base: #ffffff;");
        setScene(sc);
    }

    private void filterCourses(String q) {
        Platform.runLater(() -> {
            if (q == null || q.isBlank()) {
                coursesList.setItems(FXCollections.observableArrayList(allCourses));
                coursesCountLabel.setText(allCourses.size() + "");
                return;
            }
            String low = q.toLowerCase();
            List<String> filtered = new ArrayList<>();
            for (String item : allCourses) if (item.toLowerCase().contains(low)) filtered.add(item);
            coursesList.setItems(FXCollections.observableArrayList(filtered));
            coursesCountLabel.setText(filtered.size() + " / " + allCourses.size());
        });
    }

    private void setLoading(boolean loading) {
        Platform.runLater(() -> progress.setVisible(loading));
    }

    private void loadCourses() {
        setLoading(true);
        pool.submit(() -> {
            try {
                Object rawRegs = safeInvokeSingleArg(enrollmentService,
                        new String[]{"listByStudent", "findByStudent", "listEnrolmentsByStudent", "list"},
                        studentId);

                List<String> display = new ArrayList<>();
                if (rawRegs != null) {
                    List<?> regs = (rawRegs instanceof List) ? (List<?>) rawRegs : Arrays.asList(rawRegs);
                    for (Object r : regs) {
                        String code = extractCourseCode(r);
                        if (code == null) continue;
                        String name = code;
                        try {
                            Document courseDoc = safeInvokeGetCourseDoc(code);
                            if (courseDoc != null) {
                                String n = courseDoc.getString("name");
                                if (n != null && !n.isBlank()) name = n;
                            }
                        } catch (Throwable ignored) {}
                        display.add(code + " - " + name);
                    }
                }

                Collections.sort(display);
                allCourses.clear();
                allCourses.addAll(display);

                Platform.runLater(() -> {
                    coursesList.setItems(FXCollections.observableArrayList(allCourses));
                    coursesCountLabel.setText(allCourses.size() + "");
                    setLoading(false);
                });
            } catch (Throwable ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    coursesList.setItems(FXCollections.observableArrayList());
                    coursesCountLabel.setText("0");
                    setLoading(false);
                });
            }
        });
    }


    private void loadCourseDetails(String courseCode) {
        if (courseCode == null) return;

        Tab quizzesTab = tabs.getTabs().get(0);
        Tab assignmentsTab = tabs.getTabs().get(1);
        Tab finalTab = tabs.getTabs().get(2);

        setLoading(true);
        Platform.runLater(() -> {
            quizzesTab.setContent(new VBox(new Label("Loading quizzes...")));
            assignmentsTab.setContent(new VBox(new Label("Loading assignments...")));
            finalTab.setContent(new VBox(new Label("Loading final exam...")));
        });

        pool.submit(() -> {
            ExecutorService fetchPool = Executors.newFixedThreadPool(4);
            try {
                Callable<List<Document>> fetchQuizzesTask = () -> safeInvokeList(quizService,
                        new String[]{"listByCourse","findQuizzesByCourse","listQuizzes","list"}, courseCode);

                Callable<List<Document>> fetchAssignmentsTask = () -> {
                    List<Document> assignments = Collections.emptyList();
                    if (assignmentService != null) {
                        assignments = safeInvokeList(assignmentService,
                                new String[]{"listByCourse","listAssignmentsForCourse","listAssignmentsByCourse","list"}, courseCode);
                    }
                    if ((assignments == null || assignments.isEmpty()) && submissionService != null) {
                        assignments = safeInvokeList(submissionService,
                                new String[]{"listByCourse","listAssignmentsForCourse","listAssignmentsByCourse","list"}, courseCode);
                    }
                    return assignments == null ? Collections.emptyList() : assignments;
                };

                Callable<List<Document>> fetchSubmissionsForCourseTask = () -> {
                    if (submissionService == null) return Collections.emptyList();
                    List<Document> subs = safeInvokeList(submissionService,
                            new String[]{"listByCourse","listSubmissionsForCourse","listByCourseCode","list"}, courseCode);
                    if ((subs == null || subs.isEmpty())) {
                        List<Document> sby = safeInvokeList(submissionService,
                                new String[]{"listByStudent","listSubmissionsForStudent","listForStudent","list"}, studentId);
                        if (sby != null) {
                            List<Document> filtered = new ArrayList<>();
                            for (Document d : sby) {
                                String cc = d.getString("courseCode");
                                if (cc == null) cc = d.getString("course");
                                if (cc == null && d.containsKey("assignmentId")) {
                                    Object aid = d.get("assignmentId");
                                    if (aid != null) {
                                        String s = aid.toString();
                                        if (s.contains("::" + courseCode + "::") || s.endsWith("::" + courseCode)) cc = courseCode;
                                    }
                                }
                                if (courseCode.equalsIgnoreCase(cc)) filtered.add(d);
                            }
                            return filtered;
                        }
                    }
                    return subs == null ? Collections.emptyList() : subs;
                };

                Future<List<Document>> fQuizzes = fetchPool.submit(fetchQuizzesTask);
                Future<List<Document>> fAssignments = fetchPool.submit(fetchAssignmentsTask);
                Future<List<Document>> fSubmissionsForCourse = fetchPool.submit(fetchSubmissionsForCourseTask);

                List<Document> quizzes = fQuizzes.get(6, TimeUnit.SECONDS);
                List<Document> assignments = fAssignments.get(6, TimeUnit.SECONDS);
                List<Document> submissionsForCourse = fSubmissionsForCourse.get(6, TimeUnit.SECONDS);

                // Map quizId -> student's attempt
                Map<String, Document> quizAttemptByQuizId = new HashMap<>();
                if (quizzes != null && !quizzes.isEmpty()) {
                    List<Callable<Void>> quizAttemptTasks = new ArrayList<>();
                    for (Document q : quizzes) {
                        quizAttemptTasks.add(() -> {
                            try {
                                String qid = extractId(q);
                                if (qid == null) return null;
                                List<Document> attempts = safeInvokeList(quizService, new String[]{"listAttemptsForQuiz","listAttempts","findAttemptsByQuiz"}, qid);
                                if (attempts != null) {
                                    for (Document a : attempts) {
                                        String sid = a.getString("studentId");
                                        if (studentId.equals(sid)) {
                                            synchronized (quizAttemptByQuizId) { quizAttemptByQuizId.put(qid, a); }
                                            break;
                                        }
                                    }
                                }
                            } catch (Throwable ignored) {}
                            return null;
                        });
                    }
                    fetchPool.invokeAll(quizAttemptTasks, 6, TimeUnit.SECONDS);
                }


                Map<String, Document> submissionByAssignment = new HashMap<>();
                if (assignments != null) {
                    for (Document a : assignments) {
                        String aid = extractId(a);
                        if (aid == null) continue;
                        Document found = null;
                        if (submissionsForCourse != null) {
                            for (Document s : submissionsForCourse) {
                                Object asgId = s.get("assignmentId");
                                if (asgId != null && aid.equals(asgId.toString())) {
                                    if (studentId.equals(s.getString("studentId")) || studentId.equals(s.getString("student"))) { found = s; break; }
                                }
                                if (s.containsKey("assignment") && s.get("assignment") instanceof Document) {
                                    Document ad = (Document) s.get("assignment");
                                    Object aid2 = ad.get("_id");
                                    if (aid2 != null && aid.equals(aid2.toString())) { if (studentId.equals(s.getString("studentId"))) { found = s; break; } }
                                }
                                if (studentId.equals(s.getString("studentId")) || studentId.equals(s.getString("student"))) {
                                    String subTitle = s.getString("assignmentTitle");
                                    String asgTitle = a.getString("title");
                                    if (subTitle != null && asgTitle != null && subTitle.equalsIgnoreCase(asgTitle)) { found = s; break; }
                                }
                            }
                        }

                        if (found == null && submissionService != null) {
                            Document sub = safeInvokeSingleReturnDoc(submissionService, new String[]{
                                    "getSubmissionForStudent","getSubmission","findSubmissionForStudent","getStudentSubmission","getSubmissionByAssignmentAndStudent","findByAssignmentAndStudent"
                            }, aid, studentId);
                            if (sub != null) found = sub;
                        }

                        if (found == null && a.containsKey("submissions")) {
                            Object emb = a.get("submissions");
                            if (emb instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Object> embList = (List<Object>) emb;
                                for (Object ev : embList) {
                                    if (ev instanceof Document) {
                                        Document sdoc = (Document) ev;
                                        if (studentId.equals(sdoc.getString("studentId")) || studentId.equals(sdoc.getString("student"))) { found = sdoc; break; }
                                    }
                                }
                            }
                        }

                        submissionByAssignment.put(aid, found);
                    }
                }

                Document professorFinalSubmission = null;
                if (submissionsForCourse != null && !submissionsForCourse.isEmpty()) {
                    for (Document s : submissionsForCourse) {
                        boolean belongsToStudent = studentId.equals(s.getString("studentId")) || studentId.equals(s.getString("student"));
                        if (!belongsToStudent) continue;
                        Object type = s.get("type");
                        if (type != null && "final".equalsIgnoreCase(type.toString())) { professorFinalSubmission = s; break; }
                        Object asgId = s.get("assignmentId");
                        if (asgId != null && asgId.toString().toLowerCase().contains("final::")) { professorFinalSubmission = s; break; }
                        Object isf = s.get("isFinal");
                        if ((isf instanceof Boolean && (Boolean) isf) || "final".equalsIgnoreCase((s.getString("kind") == null ? "" : s.getString("kind")))) {
                            professorFinalSubmission = s; break;
                        }
                    }
                }

                final List<Document> finalQuizzes = (quizzes == null) ? Collections.emptyList() : quizzes;
                final List<Document> finalAssignments = (assignments == null) ? Collections.emptyList() : assignments;
                final Map<String, Document> finalQuizAttempts = quizAttemptByQuizId;
                final Map<String, Document> finalSubs = submissionByAssignment;
                final Document finalProfessorFinal = professorFinalSubmission;

                Platform.runLater(() -> {
      
                    TableView<Document> qTable = new TableView<>();
                    qTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                    TableColumn<Document, String> qt = new TableColumn<>("Quiz");
                    qt.setCellValueFactory(c -> new ReadOnlyStringWrapper(safeString(c.getValue().getString("title"), extractId(c.getValue()))));
                    TableColumn<Document, String> qs = new TableColumn<>("Score");
                    qs.setCellValueFactory(c -> {
                        String qid = extractId(c.getValue());
                        Document att = finalQuizAttempts.get(qid);
                        String text = "-";
                        if (att != null) {
                            Object s = att.get("score"); Object m = att.get("maxScore");
                            if (s != null) text = s.toString() + (m == null ? "" : (" / " + m.toString()));
                            else {
                                Object pct = att.get("percent");
                                if (pct != null) text = pct.toString() + (pct.toString().endsWith("%") ? "" : "%");
                            }
                        }
                        return new ReadOnlyStringWrapper(text);
                    });
                    qTable.getColumns().addAll(qt, qs);
                    if (!finalQuizzes.isEmpty()) qTable.getItems().addAll(finalQuizzes);
                    quizzesTab.setContent(new VBox(8, new Label("Quizzes for " + courseCode), qTable));


                    TableView<Document> aTable = new TableView<>();
                    aTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                    TableColumn<Document, String> at = new TableColumn<>("Assignment");
                    at.setCellValueFactory(c -> new ReadOnlyStringWrapper(safeString(c.getValue().getString("title"), extractId(c.getValue()))));

                    TableColumn<Document, String> ast = new TableColumn<>("Status / Score");
                    ast.setCellValueFactory(c -> {
                        String aid = extractId(c.getValue());
                        Document sub = finalSubs.get(aid);
                        if (sub == null) {
                            return new ReadOnlyStringWrapper("Not submitted");
                        }

                        Object scoreObj = sub.get("score");
                        if (scoreObj == null) scoreObj = sub.get("points");
                        if (scoreObj == null) scoreObj = sub.get("value");
                        if (scoreObj == null) scoreObj = sub.get("grade");

                        if (scoreObj != null) {
                            Object maxObj = sub.get("maxScore");
                            if (maxObj == null) maxObj = sub.get("pointsPossible");
                            if (maxObj == null) {
                                if (c.getValue().containsKey("points")) maxObj = c.getValue().get("points");
                            }
                            if (maxObj != null) {
                                return new ReadOnlyStringWrapper("Submitted (score: " + scoreObj.toString() + " / " + maxObj.toString() + ")");
                            } else {
                                return new ReadOnlyStringWrapper("Submitted (score: " + scoreObj.toString() + ")");
                            }
                        }

                        Object pct = sub.get("percent");
                        if (pct == null) pct = sub.get("percentage");
                        if (pct != null) {
                            String pctStr = pct.toString();
                            if (!pctStr.endsWith("%")) pctStr = pctStr + "%";
                            return new ReadOnlyStringWrapper("Submitted (percent: " + pctStr + ")");
                        }

                        return new ReadOnlyStringWrapper("Submitted");
                    });

                    aTable.getColumns().addAll(at, ast);
                    if (!finalAssignments.isEmpty()) aTable.getItems().addAll(finalAssignments);
                    assignmentsTab.setContent(new VBox(8, new Label("Assignments for " + courseCode), aTable));

                   
                    VBox finalBox = new VBox(10);
                    finalBox.setPadding(new Insets(8));
                    Label titleLabel = new Label("Course Summary for " + courseCode);
                    titleLabel.setStyle("-fx-font-weight:bold; -fx-font-size:13px;");
                    finalBox.getChildren().add(titleLabel);
                    finalBox.getChildren().add(new Separator());

                    GridPane grid = new GridPane();
                    grid.setHgap(10);
                    grid.setVgap(8);
                    ColumnConstraints c1 = new ColumnConstraints();
                    c1.setPercentWidth(30);
                    ColumnConstraints c2 = new ColumnConstraints();
                    c2.setPercentWidth(70);
                    grid.getColumnConstraints().addAll(c1, c2);

                    int row = 0;

                    Label labelFinal = new Label("Final exam:");
                    labelFinal.setStyle("-fx-font-weight:bold;");
                    Label valFinalName = new Label("Final exam");
                    valFinalName.setWrapText(true);
                    grid.add(labelFinal, 0, row);
                    grid.add(valFinalName, 1, row++);
                    
                    if (finalProfessorFinal != null) {
                       
                        String humanTitle = finalProfessorFinal.getString("assignmentTitle");
                        if (humanTitle == null) humanTitle = finalProfessorFinal.getString("assignmentName");
                        if (humanTitle == null) humanTitle = finalProfessorFinal.getString("title");
                        if (humanTitle != null && !humanTitle.isBlank()) {
                            valFinalName.setText(humanTitle);
                        }


                        Double rawScore = null;
                        Double rawMax = null;
                        Object sObj = finalProfessorFinal.get("score");
                        if (sObj == null) sObj = finalProfessorFinal.get("grade");
                        if (sObj == null) sObj = finalProfessorFinal.get("value");
                        if (sObj == null) sObj = finalProfessorFinal.get("points");
                        if (sObj != null) {
                            try { rawScore = Double.parseDouble(sObj.toString()); } catch (Throwable ignored) {}
                        }
                        Object mObj = finalProfessorFinal.get("maxScore");
                        if (mObj == null) mObj = finalProfessorFinal.get("pointsPossible");
                        if (mObj == null) {
         
                            Object asgId = finalProfessorFinal.get("assignmentId");
                            if (asgId != null) {
                                for (Document ad : finalAssignments) {
                                    String aid = extractId(ad);
                                    if (aid != null && aid.equals(asgId.toString())) {
                                        if (ad.containsKey("points")) mObj = ad.get("points");
                                        break;
                                    }
                                }
                            }
                        }
                        if (mObj != null) {
                            try { rawMax = Double.parseDouble(mObj.toString()); } catch (Throwable ignored) {}
                        }

                     
                        double displayScore = (rawScore == null) ? Double.NaN : rawScore;
                        double displayMax = (rawMax == null || rawMax <= 0) ? Double.NaN : rawMax;

                      
                        if (!Double.isNaN(displayScore) && displayScore > BUCKET_FINAL_MAX) {
                            displayScore = BUCKET_FINAL_MAX;
                            
                            if (Double.isNaN(displayMax) || displayMax > BUCKET_FINAL_MAX) {
                                displayMax = BUCKET_FINAL_MAX;
                            }
                        }

                       
                        if (!Double.isNaN(displayScore) && Double.isNaN(displayMax)) {
                            displayMax = displayScore;
                        }

                
                        Label scoreLabel = new Label("Score:");
                        scoreLabel.setStyle("-fx-font-weight:bold;");
                        String scoreVal;
                        if (Double.isNaN(displayScore)) scoreVal = "Not available";
                        else if (Double.isNaN(displayMax)) scoreVal = String.format(Locale.ROOT,"%.2f", displayScore);
                        else scoreVal = String.format(Locale.ROOT,"%.2f / %.2f", displayScore, displayMax);
                        Label scoreValue = new Label(scoreVal);
                        grid.add(scoreLabel, 0, row);
                        grid.add(scoreValue, 1, row++);
                        
                        
                        Label pctLabel = new Label("Percent:");
                        pctLabel.setStyle("-fx-font-weight:bold;");
                        String pctVal = "Not available";
                        if (!Double.isNaN(displayScore) && !Double.isNaN(displayMax) && displayMax > 0) {
                            double pct = (displayScore / displayMax) * 100.0;
                            pctVal = String.format(Locale.ROOT,"%.2f%%", pct);
                        } else {
                            Object p = finalProfessorFinal.get("percent");
                            if (p != null) {
                                try { pctVal = (p.toString().endsWith("%") ? p.toString() : p.toString() + "%"); } catch (Throwable ignored) {}
                            }
                        }
                        Label pctValue = new Label(pctVal);
                        grid.add(pctLabel, 0, row);
                        grid.add(pctValue, 1, row++);

                    } else {
                        Label notAvail = new Label("Professor's final submission not available for this course.");
                        notAvail.setWrapText(true);
                        grid.add(new Label("Status:"), 0, row);
                        grid.add(notAvail, 1, row++);
                    }

                    finalBox.getChildren().add(grid);
                    finalTab.setContent(finalBox);

                    setLoading(false);
                });

            } catch (Throwable ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    quizzesTab.setContent(new VBox(new Label("Failed to load quizzes: " + ex.getMessage())));
                    assignmentsTab.setContent(new VBox(new Label("Failed to load assignments")));
                    finalTab.setContent(new VBox(new Label("Failed to load final exam grade")));
                    setLoading(false);
                });
            } finally {
                fetchPool.shutdownNow();
            }
        });
    }


    @SuppressWarnings("unchecked")
    private List<Document> safeInvokeList(Object svc, String[] candidates, Object... args) {
        if (svc == null) return Collections.emptyList();
        for (String name : candidates) {
            try {
                Method m = findMethodByNameAndArgCount(svc.getClass(), name, args.length);
                if (m == null) continue;
                Object res = m.invoke(svc, args);
                if (res == null) continue;
                if (res instanceof List) return (List<Document>) res;
            } catch (Throwable ignored) { }
        }
        return Collections.emptyList();
    }

    private Document safeInvokeSingleReturnDoc(Object svc, String[] candidates, Object... args) {
        if (svc == null) return null;
        for (String name : candidates) {
            try {
                Method m = findMethodByNameAndArgCount(svc.getClass(), name, args.length);
                if (m == null) continue;
                Object res = m.invoke(svc, args);
                if (res instanceof Document) return (Document) res;
            } catch (Throwable ignored) {  }
        }
        return null;
    }

    private Object safeInvokeSingleArg(Object svc, String[] candidates, String arg) {
        if (svc == null) return null;
        for (String name : candidates) {
            try {
                Method m = findMethodByNameAndArgCount(svc.getClass(), name, 1);
                if (m == null) continue;
                Object res = m.invoke(svc, arg);
                if (res != null) return res;
            } catch (Throwable ignored) {  }
        }
        return null;
    }

    private Document safeInvokeGetCourseDoc(String code) {
        if (courseService == null) return null;
        String[] candidates = new String[]{"getByCode","findByCode","get","find","findCourseByCode"};
        for (String name : candidates) {
            try {
                Method m = findMethodByNameAndArgCount(courseService.getClass(), name, 1);
                if (m == null) continue;
                Object res = m.invoke(courseService, code);
                if (res instanceof Document) return (Document) res;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private Method findMethodByNameAndArgCount(Class<?> cls, String name, int argCount) {
        for (Method m : cls.getMethods()) {
            if (!m.getName().equals(name)) continue;
            if (m.getParameterCount() != argCount) continue;
            return m;
        }
        return null;
    }

  
    private static String extractId(Document d) {
        if (d == null) return null;
        Object id = d.get("_id");
        if (id == null) return d.getString("id") != null ? d.getString("id") : d.getString("linkedEntityId");
        return id.toString();
    }

    private String extractCourseCode(Object r) {
        if (r == null) return null;
        if (r instanceof Document) {
            Document d = (Document) r;
            String cc = d.getString("courseCode");
            if (cc != null && !cc.isBlank()) return cc;
            cc = d.getString("code");
            if (cc != null && !cc.isBlank()) return cc;
        }
        if (r instanceof Map) {
            Map<?,?> m = (Map<?,?>) r;
            Object v = m.get("courseCode"); if (v == null) v = m.get("code"); if (v == null) v = m.get("course");
            if (v != null) return v.toString();
        }
        String s = r.toString();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:courseCode|code|course)[:=]\\s*([A-Za-z0-9_-]+)").matcher(s);
        if (m.find()) return m.group(1);
        return null;
    }

    private Double tryComputeCourseGPA(String courseCode) {

        if (courseCode == null || courseCode.isBlank()) return null;
        try {
            List<Grade> grades = parentService.getGradesForStudentFromSubmissions(studentId);
            if (grades == null || grades.isEmpty()) return null;

            List<Grade> courseGrades = new ArrayList<>();
            for (Grade g : grades) {
                if (g == null) continue;
                String code = safeGetSubjectId(g);
                if (code != null && code.equalsIgnoreCase(courseCode)) courseGrades.add(g);
            }
            if (courseGrades.isEmpty()) return null;
            return parentService.computeGPA(courseGrades);
        } catch (Throwable ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private String safeGetSubjectId(Grade g) {
        try { Method m = g.getClass().getMethod("getSubjectId"); Object o = m.invoke(g); if (o != null) return o.toString(); } catch (Throwable ignored) {}
        try { Method m = g.getClass().getMethod("getCourseCode"); Object o = m.invoke(g); if (o != null) return o.toString(); } catch (Throwable ignored) {}
        try { Method m = g.getClass().getMethod("getCourseId"); Object o = m.invoke(g); if (o != null) return o.toString(); } catch (Throwable ignored) {}
        return null;
    }

    private static String safeString(String val, String fallback) {
        if (val != null && !val.isBlank()) return val;
        return fallback != null ? fallback : "-";
    }

    private void showAlert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle(title);
        a.showAndWait();
    }
}
