package studentmanage;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;
import java.util.prefs.Preferences;

public class MainApp extends Application {
    private static StudentService bootstrapService;
    private static final String PREF_NODE = "student-manage";
    private static final String PREF_LAST_SAVED_FILE = "lastSavedFile";
    private static final String TEACHER_USERNAME = "cyt";
    private static final String PREF_TEACHER_PASSWORD = "teacherPassword";
    private static final String DEFAULT_TEACHER_PASSWORD = "cyt070208";

    private enum AccessRole {
        STUDENT,
        TEACHER
    }

    private StudentService service;
    private MenuController controller;
    private final TableView<Student> table = new TableView<>();
    private final Label statusLabel = new Label();
    private final Label actionLabel = new Label();
    private final Label profileLabel = new Label("专业信息：未选择学生");
    private final VBox guideBox = new VBox();
    private final Button quickSaveExitButton = new Button("保存并退出");
    private ScrollPane centerScrollPane;
    private double listResizeStartY;
    private double listResizeStartHeight;
    private boolean listResizeDragging;

    private final ComboBox<StudentType> typeBox = new ComboBox<>();
    private final TextField idField = new TextField();
    private final TextField nameField = new TextField();
    private final ComboBox<String> ageField = new ComboBox<>();
    private final ComboBox<String> classField = new ComboBox<>();

    private final ComboBox<String> provinceField = new ComboBox<>();
    private final ComboBox<String> cityField = new ComboBox<>();
    private final TextField streetField = new TextField();
    private final TextField houseNoField = new TextField();

    private final ComboBox<String> majorField = new ComboBox<>();
    private final TextField supervisorField = new TextField();
    private final TextField directionField = new TextField();
    private final VBox scoreRowsBox = new VBox(8);
    private final Label scoreSectionTip = new Label();
    private final Button addSubjectRowButton = new Button("新增科目");
    private final List<TextField> subjectNameFields = new ArrayList<>();
    private final List<TextField> scoreValueFields = new ArrayList<>();
    private final List<String> subjectNames = new ArrayList<>();
    private final Label totalScoreLabel = new Label("总分：0.00");

    private final ComboBox<String> queryBox = new ComboBox<>();
    private final TextField queryField = new TextField();

    private final ComboBox<String> rangeBox = new ComboBox<>();
    private final ComboBox<String> sortBox = new ComboBox<>();
    private final TextField subjectField = new TextField();
    private final CheckBox ascendingCheck = new CheckBox("升序");
    private final Map<String, List<String>> provinceCityMap = buildProvinceCityMap();
    private final Preferences preferences = Preferences.userRoot().node(PREF_NODE);
    private final Map<String, Integer> rankByStudentId = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> importExtraValuesByStudentId = new HashMap<>();
    private final LinkedHashSet<String> dynamicExtraColumns = new LinkedHashSet<>();
    private boolean hasUnsavedChanges;
    private AccessRole currentRole = AccessRole.STUDENT;
    private MenuItem rowDeleteMenuItem;
    private Button quickAddButton;
    private Button clearButton;
    private Button importButton;
    private Button generateBatchButton;
    private Button activeBrowseButton;
    private TitledPane importPane;

    public static void launchApp(StudentService service, String[] args) {
        if (service == null) {
            // Fallback for miswired entry points.
            bootstrapService = new StudentService(new StudentRepository(2000));
        } else {
            bootstrapService = service;
        }
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        if (bootstrapService == null) {
            // Allow launching MainApp directly in IDE without going through Main.main(...).
            bootstrapService = new StudentService(new StudentRepository(2000));
        }
        service = bootstrapService;
        if (service == null) {
            service = new StudentService(new StudentRepository(2000));
            bootstrapService = service;
        }
        controller = new MenuController(service);

        Scene mainScene = buildMainScene();
        Scene startupScene = buildStartupScene(() -> enterMainInterface(stage, mainScene));

        String stylesheet = resolveStyleSheet();
        if (stylesheet != null) {
            mainScene.getStylesheets().add(stylesheet);
            startupScene.getStylesheets().add(stylesheet);
        }

        stage.setTitle("学生管理系统");
        stage.setScene(startupScene);
        setWindowIcon(stage);
        stage.show();
    }

    private String getTeacherPassword() {
        String saved = preferences.get(PREF_TEACHER_PASSWORD, DEFAULT_TEACHER_PASSWORD);
        return isBlank(saved) ? DEFAULT_TEACHER_PASSWORD : saved.trim();
    }

    private boolean isTeacherCredentialsValid(String username, String password) {
        return TEACHER_USERNAME.equals(username) && getTeacherPassword().equals(password);
    }

    private void updateTeacherPassword(String newPassword) {
        preferences.put(PREF_TEACHER_PASSWORD, newPassword);
    }

    private Scene buildMainScene() {
        BorderPane root = new BorderPane();
        root.setCenter(buildScrollableCenter());
        root.setBottom(buildStatusBar());
        root.getStyleClass().add("app-root");

        refreshTable(controller.browseAllStudents());
        updateStatus();

        return new Scene(root, 1380, 760);
    }

    private Scene buildStartupScene(Runnable enterAction) {
        ImageView background = new ImageView();
        background.setPreserveRatio(false);
        background.setSmooth(true);
        background.setMouseTransparent(true);

        String backgroundPath = resolveImagePath("background.png");
        if (backgroundPath != null) {
            background.setImage(new Image(backgroundPath, true));
        }

        // 头像
        ImageView avatar = new ImageView();
        avatar.getStyleClass().add("launch-avatar");
        avatar.setFitWidth(100);
        avatar.setFitHeight(100);
        avatar.setPreserveRatio(false);
        avatar.setSmooth(true);
        avatar.setMouseTransparent(true);
        String avatarPath = resolveImagePath("ncu.png");
        if (avatarPath != null) {
            avatar.setImage(new Image(avatarPath, true));
        }
        Circle clip = new Circle(50, 50, 50);
        avatar.setClip(clip);

        // 标题
        Label title = new Label("学生管理系统");
        title.getStyleClass().add("launch-title");

        // 英文副标题
        Label subtitle = new Label("STUDENT MANAGEMENT SYSTEM");
        subtitle.getStyleClass().add("launch-subtitle");

        // 装饰分隔线
        Region divider = new Region();
        divider.getStyleClass().add("launch-divider");
        divider.setMaxWidth(60);
        divider.setPrefHeight(2);

        // 版本号
        Label version = new Label("v2.1.0");
        version.getStyleClass().add("launch-version");

        // 按钮
        Button studentButton = new Button("学生端");
        studentButton.getStyleClass().addAll("launch-enter-button", "launch-student-button");
        studentButton.setMaxWidth(Double.MAX_VALUE);
        studentButton.setOnAction(e -> {
            if (enterAction != null) {
                currentRole = AccessRole.STUDENT;
                enterAction.run();
            }
        });

        Button teacherButton = new Button("老师端");
        teacherButton.getStyleClass().addAll("launch-enter-button", "launch-teacher-button");
        teacherButton.setMaxWidth(Double.MAX_VALUE);
        teacherButton.setOnAction(e -> {
            if (showTeacherLoginDialog()) {
                currentRole = AccessRole.TEACHER;
                if (enterAction != null) {
                    enterAction.run();
                }
            }
        });

        HBox buttonRow = new HBox(16, studentButton, teacherButton);
        buttonRow.setAlignment(Pos.CENTER);
        buttonRow.setFillHeight(true);
        HBox.setHgrow(studentButton, Priority.ALWAYS);
        HBox.setHgrow(teacherButton, Priority.ALWAYS);

        VBox card = new VBox(12, avatar, title, subtitle, divider, buttonRow, version);
        card.getStyleClass().add("launch-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(440);
        card.setFillWidth(true);

        StackPane root = new StackPane(background, card);
        root.getStyleClass().add("launch-root");
        StackPane.setAlignment(card, Pos.CENTER);

        Scene scene = new Scene(root, 1380, 760);
        background.fitWidthProperty().bind(scene.widthProperty());
        background.fitHeightProperty().bind(scene.heightProperty());
        return scene;
    }

    private void enterMainInterface(Stage stage, Scene mainScene) {
        stage.setTitle(currentRole == AccessRole.TEACHER ? "学生信息管理系统（老师端）" : "学生信息管理系统（学生端）");
        stage.setScene(mainScene);
        stage.centerOnScreen();
        applyRolePermissions();
        Platform.runLater(() -> {
            idField.requestFocus();
            tryAutoLoadLastSavedFile();
        });
    }

    private boolean showTeacherLoginDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("老师端身份验证");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getStyleClass().add("teacher-login-dialog");
        dialog.getDialogPane().setPrefWidth(420);

        // 头像
        ImageView loginAvatar = new ImageView();
        loginAvatar.setFitWidth(72);
        loginAvatar.setFitHeight(72);
        loginAvatar.setPreserveRatio(false);
        loginAvatar.setSmooth(true);
        String avatarPath = resolveImagePath("ncu.png");
        if (avatarPath != null) {
            loginAvatar.setImage(new Image(avatarPath, true));
        }
        Circle avatarClip = new Circle(36, 36, 36);
        loginAvatar.setClip(avatarClip);
        loginAvatar.getStyleClass().add("login-avatar");

        Label loginTitle = new Label("老师端登录");
        loginTitle.getStyleClass().add("login-title");

        TextField usernameField = new TextField(TEACHER_USERNAME);
        usernameField.setPromptText("管理员账号");
        usernameField.setMaxWidth(Double.MAX_VALUE);
        usernameField.getStyleClass().add("login-field");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码");
        passwordField.setMaxWidth(Double.MAX_VALUE);
        passwordField.getStyleClass().add("login-field");

        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(10, 20, 10, 20));
        content.getChildren().addAll(
                loginAvatar,
                loginTitle,
                new Label("管理员"),
                usernameField,
                new Label("密码"),
                passwordField
        );

        dialog.getDialogPane().setContent(content);
        ButtonType loginType = new ButtonType("登录", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType changePasswordType = new ButtonType("修改密码", javafx.scene.control.ButtonBar.ButtonData.OTHER);
        ButtonType cancelType = ButtonType.CANCEL;
        dialog.getDialogPane().getButtonTypes().addAll(loginType, changePasswordType, cancelType);

        Button loginButton = (Button) dialog.getDialogPane().lookupButton(loginType);
        loginButton.getStyleClass().add("button-primary");
        loginButton.setDefaultButton(true);
        loginButton.setPrefWidth(120);

        Button changePasswordButton = (Button) dialog.getDialogPane().lookupButton(changePasswordType);
        changePasswordButton.getStyleClass().add("button-secondary");
        changePasswordButton.setPrefWidth(120);

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelType);
        cancelButton.setPrefWidth(120);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == changePasswordType) {
            showTeacherPasswordChangeDialog();
            return false;
        }
        if (result.isEmpty() || result.get() != loginType) {
            return false;
        }

        String username = mustNotBlank(usernameField.getText(), "管理员账号不能为空。");
        String password = mustNotBlank(passwordField.getText(), "密码不能为空。");
        if (!isTeacherCredentialsValid(username, password)) {
            showError("登录失败", "管理员账号或密码错误。");
            return false;
        }
        return true;
    }

    private boolean showTeacherPasswordChangeDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("修改管理员密码");
        dialog.setHeaderText("请先验证原密码，再设置新密码");
        dialog.getDialogPane().getStyleClass().add("teacher-login-dialog");
        dialog.getDialogPane().setPrefWidth(500);

        TextField usernameField = new TextField(TEACHER_USERNAME);
        usernameField.setPromptText("管理员账号");
        usernameField.setPrefWidth(300);

        PasswordField oldPasswordField = new PasswordField();
        oldPasswordField.setPromptText("原密码");
        oldPasswordField.setPrefWidth(300);

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("新密码");
        newPasswordField.setPrefWidth(300);

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("再次输入新密码");
        confirmPasswordField.setPrefWidth(300);

        GridPane pane = new GridPane();
        pane.setHgap(10);
        pane.setVgap(12);
        pane.setPadding(new Insets(14, 8, 4, 8));
        pane.setAlignment(Pos.CENTER_LEFT);
        pane.add(new Label("管理员："), 0, 0);
        pane.add(usernameField, 1, 0);
        pane.add(new Label("原密码："), 0, 1);
        pane.add(oldPasswordField, 1, 1);
        pane.add(new Label("新密码："), 0, 2);
        pane.add(newPasswordField, 1, 2);
        pane.add(new Label("确认密码："), 0, 3);
        pane.add(confirmPasswordField, 1, 3);

        dialog.getDialogPane().setContent(pane);
        ButtonType saveType = new ButtonType("保存新密码", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = ButtonType.CANCEL;
        dialog.getDialogPane().getButtonTypes().addAll(saveType, cancelType);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.getStyleClass().add("button-primary");
        saveButton.setDefaultButton(true);
        saveButton.setPrefWidth(130);

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelType);
        cancelButton.setPrefWidth(130);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveType) {
            return false;
        }

        String username = mustNotBlank(usernameField.getText(), "管理员账号不能为空。");
        String oldPassword = mustNotBlank(oldPasswordField.getText(), "原密码不能为空。");
        String newPassword = mustNotBlank(newPasswordField.getText(), "新密码不能为空。");
        String confirmPassword = mustNotBlank(confirmPasswordField.getText(), "确认密码不能为空。");

        if (!TEACHER_USERNAME.equals(username)) {
            showError("修改失败", "管理员账号不正确。");
            return false;
        }
        if (!getTeacherPassword().equals(oldPassword)) {
            showError("修改失败", "原密码不正确。");
            return false;
        }
        if (!newPassword.equals(confirmPassword)) {
            showError("修改失败", "两次输入的新密码不一致。");
            return false;
        }
        if (newPassword.equals(oldPassword)) {
            showError("修改失败", "新密码不能与原密码相同。");
            return false;
        }

        updateTeacherPassword(newPassword);
        showInfo("修改成功", "管理员密码已更新，请使用新密码重新登录。\n\n注意：本次修改已保存到本机配置中。");
        return true;
    }

    private VBox buildHeader() {
        quickSaveExitButton.getStyleClass().addAll("button-primary", "quick-save-button");
        quickSaveExitButton.setVisible(false);
        quickSaveExitButton.setManaged(false);
        quickSaveExitButton.setOnAction(e -> saveAndExit());

        Button studentEntryButton = new Button("学生端入口");
        studentEntryButton.getStyleClass().add("button-secondary");
        studentEntryButton.setOnAction(e -> switchToStudentMode());

        Button teacherEntryButton = new Button("老师端入口");
        teacherEntryButton.getStyleClass().add("button-primary");
        teacherEntryButton.setOnAction(e -> switchToTeacherMode());

        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        ImageView headerLogo = buildHeaderLogo();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);
        HBox actionsRow = new HBox(8, quickSaveExitButton, studentEntryButton, teacherEntryButton, leftSpacer, rightSpacer, headerLogo);
        actionsRow.setAlignment(Pos.CENTER_LEFT);
        actionsRow.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label("学生信息管理系统");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("教学作业版");
        subtitle.getStyleClass().add("app-subtitle");

        Label guideTitle = new Label("新手使用步骤：");
        guideTitle.getStyleClass().add("guide-title");
        Label step1 = new Label("1. 在右侧填写学号、姓名、班级、成绩等信息");
        Label step2 = new Label("2. 点击右侧绿色按钮【新增学生（主按钮）】");
        Label step3 = new Label("3. 在左侧表格查看结果，点中某行后可修改/删除");
        step1.getStyleClass().add("guide-text");
        step2.getStyleClass().add("guide-text");
        step3.getStyleClass().add("guide-text");

        Button closeGuideButton = new Button("关闭新手操作");
        closeGuideButton.getStyleClass().add("button-secondary");
        closeGuideButton.setOnAction(e -> {
            guideBox.setManaged(false);
            guideBox.setVisible(false);
            setActionMessage("已关闭新手操作区域，主界面可用空间已增大。", true);
        });

        guideBox.getChildren().setAll(guideTitle, step1, step2, step3, closeGuideButton);
        guideBox.setSpacing(3);
        guideBox.setAlignment(Pos.CENTER);

        Separator separator = new Separator();

        VBox box = new VBox(4, actionsRow, title, subtitle, guideBox, separator);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(14, 16, 12, 16));
        return box;
    }

    private void switchToStudentMode() {
        currentRole = AccessRole.STUDENT;
        applyRolePermissions();
        updateRoleTitle();
        setActionMessage("已切换为学生端：仅可查看与导入导出。", true);
    }

    private void switchToTeacherMode() {
        if (!showTeacherLoginDialog()) {
            return;
        }
        currentRole = AccessRole.TEACHER;
        applyRolePermissions();
        updateRoleTitle();
        setActionMessage("已切换为老师端：可执行全部操作。", true);
    }

    private void updateRoleTitle() {
        if (table.getScene() == null || table.getScene().getWindow() == null) {
            return;
        }
        Stage stage = (Stage) table.getScene().getWindow();
        if (stage != null) {
            stage.setTitle(currentRole == AccessRole.TEACHER
                    ? "学生信息管理系统（老师端）"
                    : "学生信息管理系统（学生端）");
        }
    }

    private ImageView buildHeaderLogo() {
        ImageView imageView = new ImageView();
        imageView.getStyleClass().add("header-logo");
        imageView.setPreserveRatio(true);
        imageView.setFitHeight(52);
        imageView.setSmooth(true);
        imageView.setMouseTransparent(true);

        String logoPath = resolveImagePath("ncu.png");

        if (logoPath == null) {
            imageView.setVisible(false);
            imageView.setManaged(false);
            return imageView;
        }

        imageView.setImage(new Image(logoPath, true));
        return imageView;
    }

    private String resolveImagePath(String fileName) {
        String[] classpathCandidates = {
                "/Image/" + fileName,
                "/image/" + fileName,
                "/" + fileName
        };
        for (String candidate : classpathCandidates) {
            URL url = MainApp.class.getResource(candidate);
            if (url != null) {
                return url.toExternalForm();
            }
        }

        String[] fileCandidates = {
                "resources/Image/" + fileName,
                "resources/image/" + fileName,
                "src/Image/" + fileName,
                "src/image/" + fileName,
                "StudentManage/resources/Image/" + fileName,
                "StudentManage/resources/image/" + fileName,
                "StudentManage/src/Image/" + fileName,
                "StudentManage/src/image/" + fileName,
                fileName
        };
        for (String candidate : fileCandidates) {
            File file = new File(candidate);
            if (file.exists()) {
                return file.toURI().toString();
            }
        }

        // Fallback for IDE runs where user.dir may point to the workspace root or its parent.
        String userDir = System.getProperty("user.dir", "");
        if (!userDir.trim().isEmpty()) {
            File base = new File(userDir);
            File[] extraCandidates = {
                    new File(base, "resources/Image/" + fileName),
                    new File(base, "src/Image/" + fileName),
                    new File(base, "StudentManage/resources/Image/" + fileName),
                    new File(base, "StudentManage/src/Image/" + fileName),
                    new File(base.getParentFile() == null ? base : base.getParentFile(), "StudentManage/resources/Image/" + fileName),
                    new File(base.getParentFile() == null ? base : base.getParentFile(), "StudentManage/src/Image/" + fileName)
            };
            for (File file : extraCandidates) {
                if (file.exists()) {
                    return file.toURI().toString();
                }
            }
        }
        return null;
    }

    private ScrollPane buildScrollableCenter() {
        SplitPane splitPane = buildCenterPanel();
        VBox content = new VBox(8, buildHeader(), splitPane);
        content.setPadding(new Insets(0, 0, 8, 0));
        content.setFillWidth(true);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        centerScrollPane = new ScrollPane(content);
        centerScrollPane.setFitToWidth(true);
        centerScrollPane.setFitToHeight(true);
        centerScrollPane.setPannable(true);
        centerScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        centerScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        centerScrollPane.getStyleClass().add("main-scroll-pane");
        return centerScrollPane;
    }

    private SplitPane buildCenterPanel() {
        VBox left = buildTable();
        VBox right = buildFormPanel();

        SplitPane splitPane = new SplitPane(left, right);
        splitPane.setDividerPositions(0.65);
        splitPane.setStyle("-fx-background-color: transparent;");
        SplitPane.setResizableWithParent(left, true);
        SplitPane.setResizableWithParent(right, true);
        return splitPane;
    }

    private VBox buildTable() {
        table.setPlaceholder(new Label("暂无学生数据"));
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("student-table");
        table.setFixedCellSize(36);
        rebuildTableColumns();
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> fillForm(selected));
        table.setRowFactory(tv -> {
            TableRow<Student> row = new TableRow<>();
            rowDeleteMenuItem = new MenuItem("删除该学生");
            rowDeleteMenuItem.setOnAction(e -> {
                Student selected = row.getItem();
                if (selected != null) {
                    deleteStudentById(selected.getStudentId());
                }
            });
            rowDeleteMenuItem.setDisable(currentRole != AccessRole.TEACHER);
            ContextMenu menu = new ContextMenu(rowDeleteMenuItem);
            row.emptyProperty().addListener((obs, oldEmpty, empty) -> row.setContextMenu(empty ? null : menu));
            return row;
        });

        Label tableTitle = new Label("数据列表（滚轮可上移主界面）");
        tableTitle.getStyleClass().add("section-title");

        Label tableHint = new Label("可滚轮浏览，选中行后可回填右侧表单。");
        tableHint.getStyleClass().add("hint-text");

        VBox titleCard = new VBox(6, tableTitle, tableHint);
        titleCard.getStyleClass().add("card-pane");
        titleCard.setPadding(new Insets(12));

        VBox tableCard = new VBox(table);
        tableCard.getStyleClass().add("card-pane");
        tableCard.setPadding(new Insets(10));
        tableCard.setMinHeight(220);
        tableCard.setPrefHeight(380);
        tableCard.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(tableCard, Priority.NEVER);

        Region resizeHandle = new Region();
        resizeHandle.getStyleClass().add("list-resize-handle");

        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);

        VBox listContent = new VBox(10, titleCard, tableCard, resizeHandle, bottomSpacer);
        VBox.setVgrow(table, Priority.ALWAYS);

        Label dropHint = new Label("请拖动文件到这里");
        Label dropSubHint = new Label("支持 .xlsx，松开鼠标即可导入");
        dropHint.getStyleClass().add("drop-overlay-title");
        dropSubHint.getStyleClass().add("drop-overlay-subtitle");
        VBox dropOverlay = new VBox(6, dropHint, dropSubHint);
        dropOverlay.getStyleClass().add("drop-overlay");
        dropOverlay.setVisible(false);
        dropOverlay.setManaged(false);
        dropOverlay.setMouseTransparent(true);

        StackPane dropZone = new StackPane(listContent, dropOverlay);
        dropZone.getStyleClass().add("student-drop-zone");
        dropZone.setMinHeight(0);
        dropZone.addEventFilter(ScrollEvent.SCROLL, this::forwardScrollToMainPane);
        installListResizeBehavior(resizeHandle, tableCard, dropZone, titleCard, bottomSpacer);
        bindDropImportEvents(dropZone, listContent, dropOverlay);
        VBox wrapper = new VBox(dropZone);
        wrapper.setMinHeight(0);
        VBox.setVgrow(dropZone, Priority.ALWAYS);
        return wrapper;
    }

    private void installListResizeBehavior(Region resizeHandle,
                                           VBox tableCard,
                                           StackPane dropZone,
                                           VBox titleCard,
                                           Region bottomSpacer) {
        resizeHandle.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            listResizeDragging = true;
            listResizeStartY = event.getSceneY();
            listResizeStartHeight = tableCard.getHeight() > 1 ? tableCard.getHeight() : tableCard.getPrefHeight();
            addStyleClassIfMissing(resizeHandle, "list-resize-handle-active");
            addStyleClassIfMissing(tableCard, "list-resize-target-active");
            event.consume();
        });

        resizeHandle.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!listResizeDragging) {
                return;
            }
            double minHeight = 220;
            double spacing = 10;
            double maxWithSpacer = dropZone.getHeight() - titleCard.getHeight() - resizeHandle.getHeight() - spacing * 3;
            double maxWithoutSpacer = dropZone.getHeight() - titleCard.getHeight() - resizeHandle.getHeight() - spacing * 2;
            double maxHeight = Math.max(minHeight + 40, maxWithoutSpacer);
            double targetHeight = listResizeStartHeight + (event.getSceneY() - listResizeStartY);
            double nextHeight = Math.max(minHeight, Math.min(maxHeight, targetHeight));
            boolean pinToBottom = nextHeight >= maxWithSpacer;
            bottomSpacer.setManaged(!pinToBottom);
            bottomSpacer.setVisible(!pinToBottom);
            tableCard.setPrefHeight(nextHeight);
            bottomSpacer.setMinHeight(0);
            event.consume();
        });

        resizeHandle.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            listResizeDragging = false;
            resizeHandle.getStyleClass().remove("list-resize-handle-active");
            tableCard.getStyleClass().remove("list-resize-target-active");
            setActionMessage("已调整数据列表高度。", true);
            event.consume();
        });

        resizeHandle.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            if (!listResizeDragging) {
                resizeHandle.getStyleClass().remove("list-resize-handle-active");
                tableCard.getStyleClass().remove("list-resize-target-active");
            }
        });
    }

    private void addStyleClassIfMissing(Region node, String styleClass) {
        if (!node.getStyleClass().contains(styleClass)) {
            node.getStyleClass().add(styleClass);
        }
    }

    private void addStyleClassIfMissing(VBox node, String styleClass) {
        if (!node.getStyleClass().contains(styleClass)) {
            node.getStyleClass().add(styleClass);
        }
    }

    private void forwardScrollToMainPane(ScrollEvent event) {
        if (centerScrollPane == null || centerScrollPane.getContent() == null) {
            return;
        }
        double height = centerScrollPane.getContent().getBoundsInLocal().getHeight() - centerScrollPane.getViewportBounds().getHeight();
        if (height <= 0) {
            return;
        }
        double current = centerScrollPane.getVvalue();
        double next = current - event.getDeltaY() / height / 3.0;
        centerScrollPane.setVvalue(Math.max(0.0, Math.min(1.0, next)));
        event.consume();
    }

    private void bindDropImportEvents(StackPane dropZone, Node blurTarget, Node overlay) {
        dropZone.setOnDragEntered(event -> {
            if (isSupportedXlsxDrag(event.getDragboard())) {
                setDropOverlayState(dropZone, blurTarget, overlay, true);
                event.consume();
            }
        });
        dropZone.setOnDragOver(event -> {
            if (isSupportedXlsxDrag(event.getDragboard())) {
                event.acceptTransferModes(TransferMode.COPY);
                event.consume();
            }
        });
        dropZone.setOnDragExited(event -> setDropOverlayState(dropZone, blurTarget, overlay, false));
        dropZone.setOnDragDropped(event -> {
            boolean success = false;
            Dragboard dragboard = event.getDragboard();
            File source = firstXlsxFile(dragboard);
            if (source != null) {
                loadStudentsFromFile(source, false, true);
                success = true;
            }
            event.setDropCompleted(success);
            setDropOverlayState(dropZone, blurTarget, overlay, false);
            event.consume();
        });
        dropZone.setOnDragDone(event -> setDropOverlayState(dropZone, blurTarget, overlay, false));
    }

    private void setDropOverlayState(StackPane dropZone, Node blurTarget, Node overlay, boolean active) {
        overlay.setVisible(active);
        overlay.setManaged(active);
        if (active) {
            blurTarget.setEffect(new BoxBlur(8, 8, 2));
            if (!dropZone.getStyleClass().contains("drag-over")) {
                dropZone.getStyleClass().add("drag-over");
            }
        } else {
            blurTarget.setEffect(null);
            dropZone.getStyleClass().remove("drag-over");
        }
    }

    private boolean isSupportedXlsxDrag(Dragboard dragboard) {
        return firstXlsxFile(dragboard) != null;
    }

    private File firstXlsxFile(Dragboard dragboard) {
        if (dragboard == null || !dragboard.hasFiles()) {
            return null;
        }
        for (File file : dragboard.getFiles()) {
            if (file != null && file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
                return file;
            }
        }
        return null;
    }

    private void rebuildTableColumns() {
        table.getColumns().clear();

        TableColumn<Student, String> idCol = new TableColumn<>("学号");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStudentId()));

        TableColumn<Student, String> nameCol = new TableColumn<>("姓名");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Student, String> typeCol = new TableColumn<>("类型");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(resolveDisplayType(data.getValue())));

        TableColumn<Student, String> classCol = new TableColumn<>("班级");
        classCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getClassName()));

        table.getColumns().addAll(idCol, nameCol, typeCol, classCol);

        TableColumn<Student, String> majorCol = new TableColumn<>("专业");
        majorCol.setCellValueFactory(data -> {
            Student s = data.getValue();
            if (s instanceof Undergraduate) {
                return new SimpleStringProperty(((Undergraduate) s).getMajor());
            }
            return new SimpleStringProperty("");
        });
        table.getColumns().add(majorCol);

        TableColumn<Student, String> supervisorCol = new TableColumn<>("导师");
        supervisorCol.setCellValueFactory(data -> {
            Student s = data.getValue();
            if (s instanceof Postgraduate) {
                return new SimpleStringProperty(((Postgraduate) s).getSupervisor());
            }
            return new SimpleStringProperty("");
        });
        table.getColumns().add(supervisorCol);

        TableColumn<Student, String> directionCol = new TableColumn<>("方向");
        directionCol.setCellValueFactory(data -> {
            Student s = data.getValue();
            if (s instanceof Postgraduate) {
                return new SimpleStringProperty(((Postgraduate) s).getResearchDirection());
            }
            return new SimpleStringProperty("");
        });
        table.getColumns().add(directionCol);

        for (String extraColumn : dynamicExtraColumns) {
            TableColumn<Student, String> extraCol = new TableColumn<>(extraColumn);
            extraCol.setCellValueFactory(data -> {
                String studentId = data.getValue().getStudentId();
                Map<String, String> values = importExtraValuesByStudentId.get(studentId);
                String value = values == null ? "" : values.getOrDefault(extraColumn, "");
                return new SimpleStringProperty(value);
            });
            table.getColumns().add(extraCol);
        }

        for (String subject : subjectNames) {
            TableColumn<Student, String> subjectCol = new TableColumn<>(subject);
            subjectCol.setCellValueFactory(data -> new SimpleStringProperty(
                    String.format(Locale.ROOT, "%.2f", data.getValue().getSubjectScore(subject))
            ));
            subjectCol.setCellFactory(column -> new javafx.scene.control.TableCell<Student, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().remove("score-low");
                    if (empty || item == null) {
                        setText(null);
                        return;
                    }
                    setText(item);
                    Student rowStudent = getTableRow() == null ? null : (Student) getTableRow().getItem();
                    if (rowStudent != null && rowStudent.getSubjectScore(subject) < 60.0) {
                        getStyleClass().add("score-low");
                    }
                }
            });
            table.getColumns().add(subjectCol);
        }

        boolean hasImportedTotalColumn = subjectNames.stream()
                .map(this::normalizeHeader)
                .anyMatch(h -> h.contains("总分") || h.contains("总成绩") || h.equals("score"));

        TableColumn<Student, String> totalCol = new TableColumn<>("总分");
        totalCol.setCellValueFactory(data -> new SimpleStringProperty(
                String.format(Locale.ROOT, "%.2f", data.getValue().getTotalScore())
        ));

        TableColumn<Student, String> rankCol = new TableColumn<>("排名");
        rankCol.setCellValueFactory(data -> {
            Integer rank = rankByStudentId.get(data.getValue().getStudentId());
            return new SimpleStringProperty(rank == null ? "-" : String.valueOf(rank));
        });

        TableColumn<Student, String> addrCol = new TableColumn<>("地址");
        addrCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAddress().toString()));
        if (!hasImportedTotalColumn) {
            table.getColumns().add(totalCol);
        }
        table.getColumns().addAll(rankCol, addrCol);
    }

    private VBox buildFormPanel() {
        typeBox.setItems(FXCollections.observableArrayList(StudentType.values()));
        typeBox.getSelectionModel().select(StudentType.UNDERGRADUATE);
        typeBox.valueProperty().addListener((obs, oldType, newType) -> switchTypeFieldState(newType));
        typeBox.getStyleClass().add("form-control");

        queryBox.setItems(FXCollections.observableArrayList("按学号", "按姓名", "按班级"));
        queryBox.getSelectionModel().selectFirst();
        queryBox.getStyleClass().add("form-control");

        rangeBox.setItems(FXCollections.observableArrayList("所有学生", "仅本科生", "仅研究生"));
        rangeBox.getSelectionModel().selectFirst();
        rangeBox.getStyleClass().add("form-control");

        sortBox.setItems(FXCollections.observableArrayList("按学号", "按总分", "按单科成绩"));
        sortBox.getSelectionModel().selectFirst();
        sortBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean needSubject = "按单科成绩".equals(newVal);
            subjectField.setDisable(!needSubject);
            if (!needSubject) {
                subjectField.clear();
            }
        });
        subjectField.setDisable(true);
        sortBox.getStyleClass().add("form-control");
        subjectField.getStyleClass().add("form-control");
        ascendingCheck.setSelected(true);

        idField.getStyleClass().add("form-control");
        nameField.getStyleClass().add("form-control");
        ageField.getStyleClass().add("form-control");
        classField.getStyleClass().add("form-control");
        provinceField.getStyleClass().add("form-control");
        cityField.getStyleClass().add("form-control");
        streetField.getStyleClass().add("form-control");
        houseNoField.getStyleClass().add("form-control");
        majorField.getStyleClass().add("form-control");
        supervisorField.getStyleClass().add("form-control");
        directionField.getStyleClass().add("form-control");

        initSelectControls();

        idField.setPromptText("必填，例如 20250001");
        nameField.setPromptText("必填");
        ageField.setPromptText("请选择年龄");
        classField.setPromptText("请选择班级");
        provinceField.setPromptText("请选择省份");
        cityField.setPromptText("请先选择省份");
        majorField.setPromptText("本科生必填");
        supervisorField.setPromptText("研究生必填");
        directionField.setPromptText("研究生必填");
        queryField.setPromptText("输入学号/姓名/班级后点执行查询");
        subjectField.setPromptText("按单科排序时填写科目");

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.setMaxWidth(Double.MAX_VALUE);

        int row = 0;
        addRow(form, row++, "学生类型", typeBox);
        addRow(form, row++, "学号", idField);
        addRow(form, row++, "姓名", nameField);
        addRow(form, row++, "年龄", ageField);
        addRow(form, row++, "班级", classField);
        addRow(form, row++, "省份", provinceField);
        addRow(form, row++, "城市", cityField);
        addRow(form, row++, "街道", streetField);
        addRow(form, row++, "门牌号", houseNoField);
        addRow(form, row++, "专业(本科)", majorField);
        addRow(form, row++, "导师(研究生)", supervisorField);
        addRow(form, row++, "方向(研究生)", directionField);

        Label addHint = new Label("填写完成后优先点这里新增：");
        addHint.getStyleClass().add("hint-text");
        quickAddButton = new Button("新增学生（主按钮）");
        quickAddButton.setMaxWidth(Double.MAX_VALUE);
        quickAddButton.getStyleClass().add("button-primary");
        quickAddButton.setOnAction(e -> addStudent());

        clearButton = new Button("清空表单");
        clearButton.setMaxWidth(Double.MAX_VALUE);
        clearButton.getStyleClass().add("button-secondary");
        clearButton.setOnAction(e -> {
            if (confirmClearAllStudents()) {
                clearFormAndAllStudents();
            } else {
                setActionMessage("已取消清空操作。", true);
            }
        });

        Button deleteFormButton = new Button("删除当前学生");
        deleteFormButton.setMaxWidth(Double.MAX_VALUE);
        deleteFormButton.getStyleClass().add("button-danger");
        deleteFormButton.setOnAction(e -> deleteStudent());

        generateBatchButton = new Button("批量生成300人");
        generateBatchButton.setMaxWidth(Double.MAX_VALUE);
        generateBatchButton.getStyleClass().add("button-primary");
        generateBatchButton.setOnAction(e -> generateBatchStudents());

        HBox queryRow = new HBox(8, queryBox, queryField);
        Button queryButton = new Button("执行查询");
        Button resetButton = new Button("重置列表");
        queryButton.getStyleClass().add("button-primary");
        resetButton.getStyleClass().add("button-secondary");
        queryButton.setOnAction(e -> doQuery());
        resetButton.setOnAction(e -> {
            refreshTable(controller.browseAllStudents());
            setActionMessage("已重置为全部学生列表。", true);
        });

        HBox queryButtons = new HBox(8, queryButton, resetButton);

        Button browseAllButton = new Button("全部");
        Button browseUnderButton = new Button("本科生");
        Button browsePostButton = new Button("研究生");
        Button statsButton = new Button("统计");
        browseAllButton.getStyleClass().add("button-primary");
        browseUnderButton.getStyleClass().add("button-secondary");
        browsePostButton.getStyleClass().add("button-secondary");
        statsButton.getStyleClass().add("button-secondary");
        activeBrowseButton = browseAllButton;

        browseAllButton.setOnAction(e -> {
            setBrowseActive(browseAllButton);
            refreshTable(controller.browseAllStudents());
            setActionMessage("当前显示：全部学生。", true);
        });
        browseUnderButton.setOnAction(e -> {
            setBrowseActive(browseUnderButton);
            refreshTable(controller.browseByType(StudentType.UNDERGRADUATE));
            setActionMessage("当前显示：仅本科生。", true);
        });
        browsePostButton.setOnAction(e -> {
            setBrowseActive(browsePostButton);
            refreshTable(controller.browseByType(StudentType.POSTGRADUATE));
            setActionMessage("当前显示：仅研究生。", true);
        });
        statsButton.setOnAction(e -> showInfo("统计信息", controller.buildStatisticsText()));
        HBox browseButtons = new HBox(8, browseAllButton, browseUnderButton, browsePostButton, statsButton);

        HBox sortRow1 = new HBox(8, rangeBox, sortBox);
        HBox sortRow2 = new HBox(8, subjectField, ascendingCheck);
        Button sortButton = new Button("执行排序");
        sortButton.getStyleClass().add("button-secondary");
        sortButton.setOnAction(e -> doSort());

        Button avgButton = new Button("平均分统计");
        avgButton.getStyleClass().add("button-secondary");
        avgButton.setOnAction(e -> showAverageScoreStats());

        Button aboutButton = new Button("关于");
        aboutButton.getStyleClass().add("button-secondary");
        aboutButton.setOnAction(e -> showAbout());

        Button saveButton = new Button("保存到文件");
        saveButton.getStyleClass().add("button-secondary");
        saveButton.setOnAction(e -> saveStudentsToFile(true));

        importButton = new Button("导入文件");
        importButton.getStyleClass().add("button-secondary");
        importButton.setOnAction(e -> importStudentsFromFile());

        VBox scoreSection = buildScoreSection();
        VBox importContent = new VBox(8, scoreSection, form, addHint, new HBox(8, quickAddButton, clearButton, deleteFormButton), generateBatchButton);
        VBox queryContent = new VBox(8, queryRow, queryButtons);
        VBox browseContent = new VBox(8, browseButtons);
        VBox sortContent = new VBox(8, sortRow1, sortRow2, new HBox(8, sortButton, avgButton));
        VBox aboutContent = new VBox(8, importButton, saveButton, aboutButton);

        // 班级管理
        Button viewClassesButton = new Button("查看班级");
        viewClassesButton.setMaxWidth(Double.MAX_VALUE);
        viewClassesButton.getStyleClass().add("button-secondary");
        viewClassesButton.setOnAction(e -> showClassManagement());

        Button crossCompareButton = new Button("横向对比");
        crossCompareButton.setMaxWidth(Double.MAX_VALUE);
        crossCompareButton.getStyleClass().add("button-secondary");
        crossCompareButton.setOnAction(e -> showCrossComparison());

        Button classInfoButton = new Button("查看当前班级信息");
        classInfoButton.setMaxWidth(Double.MAX_VALUE);
        classInfoButton.getStyleClass().add("button-secondary");
        classInfoButton.setOnAction(e -> showCurrentClassInfo());

        VBox classMgmtContent = new VBox(8, viewClassesButton, crossCompareButton, classInfoButton);

        importPane = new TitledPane("学生信息导入", importContent);
        TitledPane queryPane = new TitledPane("查询功能", queryContent);
        TitledPane browsePane = new TitledPane("浏览", browseContent);
        TitledPane sortPane = new TitledPane("排序", sortContent);
        TitledPane aboutPane = new TitledPane("关于", aboutContent);
        TitledPane classMgmtPane = new TitledPane("班级管理", classMgmtContent);
        importPane.setCollapsible(true);
        queryPane.setCollapsible(true);
        browsePane.setCollapsible(true);
        sortPane.setCollapsible(true);
        aboutPane.setCollapsible(true);
        classMgmtPane.setCollapsible(true);
        importPane.setExpanded(true);
        queryPane.setExpanded(false);
        browsePane.setExpanded(false);
        sortPane.setExpanded(false);
        aboutPane.setExpanded(false);
        classMgmtPane.setExpanded(false);

        Accordion accordion = new Accordion(importPane, queryPane, browsePane, sortPane, aboutPane, classMgmtPane);

        ScrollPane rightScrollPane = new ScrollPane(accordion);
        rightScrollPane.setFitToWidth(false);
        rightScrollPane.setPannable(true);
        rightScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        rightScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        rightScrollPane.getStyleClass().add("right-scroll-pane");

        VBox panel = new VBox(10, rightScrollPane);
        panel.getStyleClass().add("card-pane");
        panel.setPadding(new Insets(15));
        panel.setMinWidth(430);
        panel.setPrefWidth(430);
        VBox.setVgrow(rightScrollPane, Priority.ALWAYS);

        switchTypeFieldState(typeBox.getValue());
        applyRolePermissions();
        return panel;
    }

    private void applyRolePermissions() {
        boolean teacher = currentRole == AccessRole.TEACHER;

        if (quickAddButton != null) {
            quickAddButton.setDisable(!teacher);
        }
        if (clearButton != null) {
            clearButton.setDisable(false);
        }
        if (generateBatchButton != null) {
            generateBatchButton.setDisable(!teacher);
        }
        if (importButton != null) {
            importButton.setDisable(false);
        }
        if (rowDeleteMenuItem != null) {
            rowDeleteMenuItem.setDisable(!teacher);
        }

        typeBox.setDisable(!teacher);
        idField.setDisable(!teacher);
        nameField.setDisable(!teacher);
        ageField.setDisable(!teacher);
        classField.setDisable(!teacher);
        provinceField.setDisable(!teacher);
        cityField.setDisable(!teacher);
        streetField.setDisable(!teacher);
        houseNoField.setDisable(!teacher);
        majorField.setDisable(!teacher);
        supervisorField.setDisable(!teacher);
        directionField.setDisable(!teacher);
        addSubjectRowButton.setDisable(!teacher);
    }

    private void setBrowseActive(Button active) {
        if (activeBrowseButton != null && activeBrowseButton != active) {
            activeBrowseButton.getStyleClass().remove("button-primary");
            if (!activeBrowseButton.getStyleClass().contains("button-secondary")) {
                activeBrowseButton.getStyleClass().add("button-secondary");
            }
        }
        active.getStyleClass().remove("button-secondary");
        if (!active.getStyleClass().contains("button-primary")) {
            active.getStyleClass().add("button-primary");
        }
        activeBrowseButton = active;
    }

    private HBox buildStatusBar() {
        statusLabel.getStyleClass().add("status-text");
        actionLabel.getStyleClass().add("status-text");
        profileLabel.getStyleClass().add("status-text");
        actionLabel.setText("提示：请先在右侧表单录入信息。");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox box = new HBox(10, statusLabel, spacer, profileLabel, actionLabel);
        box.getStyleClass().add("status-bar");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 15, 10, 15));
        return box;
    }

    private VBox buildScoreSection() {
        Label title = new Label("科目与成绩");
        title.getStyleClass().add("section-title");

        scoreSectionTip.getStyleClass().add("hint-text");
        addSubjectRowButton.getStyleClass().add("button-secondary");
        addSubjectRowButton.setOnAction(e -> {
            if (subjectNames.isEmpty()) {
                addEditableScoreRow();
                renderScoreEditor();
            }
        });

        renderScoreEditor();

        totalScoreLabel.getStyleClass().add("total-score-label");

        VBox box = new VBox(8, title, scoreSectionTip, scoreRowsBox, totalScoreLabel, addSubjectRowButton);
        box.getStyleClass().add("card-pane");
        box.setPadding(new Insets(12));
        return box;
    }

    private void addEditableScoreRow() {
        TextField subjectField = new TextField();
        subjectField.setPromptText("科目名称，例如 高数");
        subjectField.getStyleClass().add("form-control");

        TextField scoreField = new TextField();
        scoreField.setPromptText("成绩，例如 95");
        scoreField.getStyleClass().add("form-control");

        subjectNameFields.add(subjectField);
        scoreValueFields.add(scoreField);
    }

    private void renderScoreEditor() {
        scoreRowsBox.getChildren().clear();

        if (subjectNames.isEmpty()) {
            scoreSectionTip.setText("首次添加学生时，请先输入科目名称与成绩；之后科目将固定不变。");
            addSubjectRowButton.setVisible(true);
            addSubjectRowButton.setManaged(true);

            if (subjectNameFields.isEmpty()) {
                addEditableScoreRow();
                addEditableScoreRow();
                addEditableScoreRow();
            }

            for (int i = 0; i < subjectNameFields.size(); i++) {
                HBox row = new HBox(8);
                Label subjectIndex = new Label("科目" + (i + 1) + "：");
                subjectIndex.getStyleClass().add("form-label");
                subjectIndex.setMinWidth(70);
                subjectIndex.setPrefWidth(70);

                Label scoreLabel = new Label("成绩：");
                scoreLabel.getStyleClass().add("form-label");
                scoreLabel.setMinWidth(54);

                TextField sf = scoreValueFields.get(i);
                sf.textProperty().addListener((obs, oldVal, newVal) -> recalcFormTotalScore());
                row.getChildren().addAll(subjectIndex, subjectNameFields.get(i), scoreLabel, sf);
                HBox.setHgrow(subjectNameFields.get(i), Priority.ALWAYS);
                HBox.setHgrow(sf, Priority.ALWAYS);
                scoreRowsBox.getChildren().add(row);
            }
            recalcFormTotalScore();
        } else {
            scoreSectionTip.setText("科目已固定：" + String.join("、", subjectNames));
            addSubjectRowButton.setVisible(false);
            addSubjectRowButton.setManaged(false);

            while (scoreValueFields.size() < subjectNames.size()) {
                scoreValueFields.add(new TextField());
            }
            while (scoreValueFields.size() > subjectNames.size()) {
                scoreValueFields.remove(scoreValueFields.size() - 1);
            }

            for (int i = 0; i < subjectNames.size(); i++) {
                HBox row = new HBox(8);
                Label subjectLabel = new Label(subjectNames.get(i) + "：");
                subjectLabel.getStyleClass().add("form-label");
                subjectLabel.setMinWidth(106);
                subjectLabel.setPrefWidth(106);

                TextField scoreField = scoreValueFields.get(i);
                scoreField.getStyleClass().add("form-control");
                scoreField.textProperty().addListener((obs, oldVal, newVal) -> recalcFormTotalScore());
                row.getChildren().addAll(subjectLabel, scoreField);
                HBox.setHgrow(scoreField, Priority.ALWAYS);
                scoreRowsBox.getChildren().add(row);
            }
            recalcFormTotalScore();
        }
    }

    private ScoreInput collectScoresFromForm() {
        LinkedHashMap<String, Double> scores = new LinkedHashMap<>();
        List<String> initialSubjects = new ArrayList<>();

        if (subjectNames.isEmpty()) {
            if (subjectNameFields.isEmpty()) {
                throw new IllegalArgumentException("请至少录入一门科目与成绩。");
            }

            Set<String> seen = new HashSet<>();
            for (int i = 0; i < subjectNameFields.size(); i++) {
                String subject = mustNotBlank(subjectNameFields.get(i).getText(), "科目名称不能为空。第 " + (i + 1) + " 行");
                if (!seen.add(subject)) {
                    throw new IllegalArgumentException("科目名称重复：" + subject);
                }
                double score = parseScore(scoreValueFields.get(i).getText(), "成绩必须是数字。第 " + (i + 1) + " 行");
                scores.put(subject, score);
                initialSubjects.add(subject);
            }
        } else {
            if (scoreValueFields.size() != subjectNames.size()) {
                throw new IllegalArgumentException("科目数量与当前设置不一致，请重新检查科目区域。");
            }

            for (int i = 0; i < subjectNames.size(); i++) {
                double score = parseScore(scoreValueFields.get(i).getText(), subjectNames.get(i) + " 的成绩必须是数字。");
                scores.put(subjectNames.get(i), score);
            }
        }

        if (scores.isEmpty()) {
            throw new IllegalArgumentException("请至少录入一门科目与成绩。");
        }

        return new ScoreInput(scores, initialSubjects);
    }

    private void applySubjectCatalogIfNeeded(List<String> names) {
        if (subjectNames.isEmpty() && names != null && !names.isEmpty()) {
            subjectNames.addAll(names);
            renderScoreEditor();
            rebuildTableColumns();
        }
    }

    private void fillScores(Student student) {
        if (subjectNames.isEmpty()) {
            return;
        }
        for (int i = 0; i < subjectNames.size(); i++) {
            scoreValueFields.get(i).setText(String.format(Locale.ROOT, "%.2f", student.getSubjectScore(subjectNames.get(i))));
        }
    }

    private void recalcFormTotalScore() {
        double sum = 0.0;
        for (TextField field : scoreValueFields) {
            String text = field.getText();
            if (text != null && !text.trim().isEmpty()) {
                try {
                    sum += Double.parseDouble(text.trim());
                } catch (NumberFormatException ignored) {
                    // Ignore non-numeric input while typing.
                }
            }
        }
        totalScoreLabel.setText(String.format(Locale.ROOT, "总分：%.2f", sum));
    }

    private void clearScoreValues() {
        if (subjectNames.isEmpty()) {
            for (TextField field : subjectNameFields) {
                field.clear();
            }
        }
        for (TextField field : scoreValueFields) {
            field.clear();
        }
        recalcFormTotalScore();
    }

    private void addRow(GridPane grid, int row, String text, javafx.scene.Node node) {
        Label label = new Label(text + "：");
        label.getStyleClass().add("form-label");
        label.setMinWidth(106);
        label.setPrefWidth(106);
        label.setWrapText(false);
        grid.add(label, 0, row);
        grid.add(node, 1, row);
        GridPane.setHgrow(node, Priority.ALWAYS);
        if (node instanceof TextArea) {
            GridPane.setVgrow(node, Priority.ALWAYS);
        }
    }

    private void initSelectControls() {
        ageField.setEditable(false);
        classField.setEditable(false);
        provinceField.setEditable(false);
        cityField.setEditable(false);

        List<String> ages = new ArrayList<>();
        for (int i = 1; i <= 150; i++) {
            ages.add(String.valueOf(i));
        }
        ageField.setItems(FXCollections.observableArrayList(ages));

        List<String> classes = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            classes.add(i + "班");
        }
        classField.setItems(FXCollections.observableArrayList(classes));

        majorField.setEditable(false);
        majorField.setItems(FXCollections.observableArrayList(
                "通信工程", "软件工程", "计算机科学与技术", "电子信息工程"
        ));

        provinceField.setItems(FXCollections.observableArrayList(provinceCityMap.keySet()));
        cityField.setItems(FXCollections.observableArrayList());

        provinceField.valueProperty().addListener((obs, oldProvince, newProvince) -> refreshCityOptions(newProvince));
    }

    private void refreshCityOptions(String province) {
        List<String> cities = provinceCityMap.getOrDefault(province, Collections.emptyList());
        cityField.setItems(FXCollections.observableArrayList(cities));
        cityField.setValue(null);
    }

    private void ensureComboItem(ComboBox<String> box, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (!box.getItems().contains(value)) {
            box.getItems().add(value);
        }
    }

    private Map<String, List<String>> buildProvinceCityMap() {
        LinkedHashMap<String, List<String>> map = new LinkedHashMap<>();
        map.put("北京市", Arrays.asList("北京市"));
        map.put("天津市", Arrays.asList("天津市"));
        map.put("上海市", Arrays.asList("上海市"));
        map.put("重庆市", Arrays.asList("重庆市"));
        map.put("河北省", Arrays.asList("石家庄市", "唐山市", "秦皇岛市", "邯郸市", "邢台市", "保定市", "张家口市", "承德市", "沧州市", "廊坊市", "衡水市"));
        map.put("山西省", Arrays.asList("太原市", "大同市", "阳泉市", "长治市", "晋城市", "朔州市", "晋中市", "运城市", "忻州市", "临汾市", "吕梁市"));
        map.put("辽宁省", Arrays.asList("沈阳市", "大连市", "鞍山市", "抚顺市", "本溪市", "丹东市", "锦州市", "营口市", "阜新市", "辽阳市", "盘锦市", "铁岭市", "朝阳市", "葫芦岛市"));
        map.put("吉林省", Arrays.asList("长春市", "吉林市", "四平市", "辽源市", "通化市", "白山市", "松原市", "白城市", "延边朝鲜族自治州"));
        map.put("黑龙江省", Arrays.asList("哈尔滨市", "齐齐哈尔市", "鸡西市", "鹤岗市", "双鸭山市", "大庆市", "伊春市", "佳木斯市", "七台河市", "牡丹江市", "黑河市", "绥化市", "大兴安岭地区"));
        map.put("江苏省", Arrays.asList("南京市", "无锡市", "徐州市", "常州市", "苏州市", "南通市", "连云港市", "淮安市", "盐城市", "扬州市", "镇江市", "泰州市", "宿迁市"));
        map.put("浙江省", Arrays.asList("杭州市", "宁波市", "温州市", "嘉兴市", "湖州市", "绍兴市", "金华市", "衢州市", "舟山市", "台州市", "丽水市"));
        map.put("安徽省", Arrays.asList("合肥市", "芜湖市", "蚌埠市", "淮南市", "马鞍山市", "淮北市", "铜陵市", "安庆市", "黄山市", "滁州市", "阜阳市", "宿州市", "六安市", "亳州市", "池州市", "宣城市"));
        map.put("福建省", Arrays.asList("福州市", "厦门市", "莆田市", "三明市", "泉州市", "漳州市", "南平市", "龙岩市", "宁德市"));
        map.put("江西省", Arrays.asList("南昌市", "景德镇市", "萍乡市", "九江市", "新余市", "鹰潭市", "赣州市", "吉安市", "宜春市", "抚州市", "上饶市"));
        map.put("山东省", Arrays.asList("济南市", "青岛市", "淄博市", "枣庄市", "东营市", "烟台市", "潍坊市", "济宁市", "泰安市", "威海市", "日照市", "临沂市", "德州市", "聊城市", "滨州市", "菏泽市"));
        map.put("河南省", Arrays.asList("郑州市", "开封市", "洛阳市", "平顶山市", "安阳市", "鹤壁市", "新乡市", "焦作市", "濮阳市", "许昌市", "漯河市", "三门峡市", "南阳市", "商丘市", "信阳市", "周口市", "驻马店市", "济源市"));
        map.put("湖北省", Arrays.asList("武汉市", "黄石市", "十堰市", "宜昌市", "襄阳市", "鄂州市", "荆门市", "孝感市", "荆州市", "黄冈市", "咸宁市", "随州市", "恩施土家族苗族自治州", "仙桃市", "潜江市", "天门市", "神农架林区"));
        map.put("湖南省", Arrays.asList("长沙市", "株洲市", "湘潭市", "衡阳市", "邵阳市", "岳阳市", "常德市", "张家界市", "益阳市", "郴州市", "永州市", "怀化市", "娄底市", "湘西土家族苗族自治州"));
        map.put("广东省", Arrays.asList("广州市", "韶关市", "深圳市", "珠海市", "汕头市", "佛山市", "江门市", "湛江市", "茂名市", "肇庆市", "惠州市", "梅州市", "汕尾市", "河源市", "阳江市", "清远市", "东莞市", "中山市", "潮州市", "揭阳市", "云浮市"));
        map.put("海南省", Arrays.asList("海口市", "三亚市", "三沙市", "儋州市"));
        map.put("四川省", Arrays.asList("成都市", "自贡市", "攀枝花市", "泸州市", "德阳市", "绵阳市", "广元市", "遂宁市", "内江市", "乐山市", "南充市", "眉山市", "宜宾市", "广安市", "达州市", "雅安市", "巴中市", "资阳市", "阿坝藏族羌族自治州", "甘孜藏族自治州", "凉山彝族自治州"));
        map.put("贵州省", Arrays.asList("贵阳市", "六盘水市", "遵义市", "安顺市", "毕节市", "铜仁市", "黔西南布依族苗族自治州", "黔东南苗族侗族自治州", "黔南布依族苗族自治州"));
        map.put("云南省", Arrays.asList("昆明市", "曲靖市", "玉溪市", "保山市", "昭通市", "丽江市", "普洱市", "临沧市", "楚雄彝族自治州", "红河哈尼族彝族自治州", "文山壮族苗族自治州", "西双版纳傣族自治州", "大理白族自治州", "德宏傣族景颇族自治州", "怒江傈僳族自治州", "迪庆藏族自治州"));
        map.put("陕西省", Arrays.asList("西安市", "铜川市", "宝鸡市", "咸阳市", "渭南市", "延安市", "汉中市", "榆林市", "安康市", "商洛市"));
        map.put("甘肃省", Arrays.asList("兰州市", "嘉峪关市", "金昌市", "白银市", "天水市", "武威市", "张掖市", "平凉市", "酒泉市", "庆阳市", "定西市", "陇南市", "临夏回族自治州", "甘南藏族自治州"));
        map.put("青海省", Arrays.asList("西宁市", "海东市", "海北藏族自治州", "黄南藏族自治州", "海南藏族自治州", "果洛藏族自治州", "玉树藏族自治州", "海西蒙古族藏族自治州"));
        map.put("台湾省", Arrays.asList("台北市", "新北市", "桃园市", "台中市", "台南市", "高雄市", "基隆市", "新竹市", "嘉义市", "新竹县", "苗栗县", "彰化县", "南投县", "云林县", "嘉义县", "屏东县", "宜兰县", "花莲县", "台东县", "澎湖县", "金门县", "连江县"));
        map.put("内蒙古自治区", Arrays.asList("呼和浩特市", "包头市", "乌海市", "赤峰市", "通辽市", "鄂尔多斯市", "呼伦贝尔市", "巴彦淖尔市", "乌兰察布市", "兴安盟", "锡林郭勒盟", "阿拉善盟"));
        map.put("广西壮族自治区", Arrays.asList("南宁市", "柳州市", "桂林市", "梧州市", "北海市", "防城港市", "钦州市", "贵港市", "玉林市", "百色市", "贺州市", "河池市", "来宾市", "崇左市"));
        map.put("西藏自治区", Arrays.asList("拉萨市", "日喀则市", "昌都市", "林芝市", "山南市", "那曲市", "阿里地区"));
        map.put("宁夏回族自治区", Arrays.asList("银川市", "石嘴山市", "吴忠市", "固原市", "中卫市"));
        map.put("新疆维吾尔自治区", Arrays.asList("乌鲁木齐市", "克拉玛依市", "吐鲁番市", "哈密市", "昌吉回族自治州", "博尔塔拉蒙古自治州", "巴音郭楞蒙古自治州", "阿克苏地区", "克孜勒苏柯尔克孜自治州", "喀什地区", "和田地区", "伊犁哈萨克自治州", "塔城地区", "阿勒泰地区"));
        map.put("香港特别行政区", Arrays.asList("香港岛", "九龙", "新界"));
        map.put("澳门特别行政区", Arrays.asList("澳门半岛", "氹仔", "路环"));
        return map;
    }

    private void switchTypeFieldState(StudentType type) {
        boolean undergraduate = type == StudentType.UNDERGRADUATE;
        majorField.setDisable(!undergraduate);
        supervisorField.setDisable(undergraduate);
        directionField.setDisable(undergraduate);
    }

    private void addStudent() {
        try {
            FormData data = readFormData(false);
            MenuController.OperationResult result = controller.addStudent(toControllerData(data));
            if (!result.isSuccess()) {
                showError("新增失败", result.getMessage());
                setActionMessage(result.getMessage(), false);
                return;
            }

            applySubjectCatalogIfNeeded(data.subjectsToInitialize);

            refreshTable(controller.browseAllStudents());
            updateStatus();
            clearForm();
            setUnsavedChanges(true);
            setActionMessage("新增成功：" + result.getMessage(), true);
            showInfo("新增成功", result.getMessage());
        } catch (IllegalArgumentException ex) {
            showError("输入有误", ex.getMessage());
            setActionMessage(ex.getMessage(), false);
        }
    }

    private void updateStudent() {
        try {
            FormData data = readFormData(true);
            MenuController.OperationResult result = controller.updateStudent(toControllerData(data));
            if (!result.isSuccess()) {
                showError("修改失败", result.getMessage());
                setActionMessage(result.getMessage(), false);
                return;
            }

            applySubjectCatalogIfNeeded(data.subjectsToInitialize);

            refreshTable(controller.browseAllStudents());
            updateStatus();
            setUnsavedChanges(true);
            setActionMessage("修改成功：" + result.getMessage(), true);
            showInfo("修改成功", result.getMessage());
        } catch (IllegalArgumentException ex) {
            showError("输入有误", ex.getMessage());
            setActionMessage(ex.getMessage(), false);
        }
    }

    private void deleteStudent() {
        String id = idField.getText().trim();
        if (id.isEmpty()) {
            showError("删除失败", "请先选择或填写要删除的学号。");
            return;
        }

        MenuController.OperationResult result = controller.deleteStudentById(id);
        if (!result.isSuccess()) {
            showError("删除失败", result.getMessage());
            setActionMessage(result.getMessage(), false);
            return;
        }

        refreshTable(controller.browseAllStudents());
        updateStatus();
        clearForm();
        setUnsavedChanges(true);
        setActionMessage("删除成功：" + result.getMessage(), true);
        showInfo("删除成功", result.getMessage());
    }

    private void doQuery() {
        MenuController.QueryResult result = controller.queryStudents(queryBox.getValue(), queryField.getText());
        if (!result.isSuccess()) {
            showError("查询失败", result.getMessage());
            setActionMessage(result.getMessage(), false);
            return;
        }

        List<Student> matches = result.getStudents();
        if (matches.isEmpty()) {
            table.getSelectionModel().clearSelection();
            setActionMessage("未找到匹配的学生。", false);
            showInfo("查询结果", "未找到匹配的学生。");
            return;
        }

        // 确保数据列表保持当前视图，不清空；只高亮匹配行
        Student firstMatch = matches.get(0);
        table.getSelectionModel().select(firstMatch);
        table.scrollTo(firstMatch);
        table.requestFocus();
        setActionMessage("查询完成，共 " + matches.size() + " 条结果，已高亮第一条。", true);
    }

    private void doSort() {
        MenuController.QueryResult result = controller.sortStudents(
                rangeBox.getValue(),
                sortBox.getValue(),
                subjectField.getText(),
                ascendingCheck.isSelected()
        );
        if (!result.isSuccess()) {
            showError("排序失败", result.getMessage());
            setActionMessage(result.getMessage(), false);
            return;
        }
        refreshTable(result.getStudents());
        setActionMessage("排序完成，共 " + result.getStudents().size() + " 条结果。", true);
    }

    private void showAverageScoreStats() {
        List<Student> source = chooseStudentsByRange(rangeBox.getValue());
        if (source.isEmpty()) {
            showError("统计失败", "当前范围内没有学生数据。请先新增或导入学生。");
            return;
        }

        List<String> subjects = subjectNames.isEmpty()
                ? new ArrayList<>(source.get(0).getSubjectScores().keySet())
                : new ArrayList<>(subjectNames);
        if (subjects.isEmpty()) {
            showError("统计失败", "当前没有可统计的科目。");
            return;
        }

        // 构建统计表格
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("平均分统计");
        dialog.setHeaderText("统计范围：" + rangeBox.getValue() + "（共 " + source.size() + " 人）");
        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().setPrefHeight(400);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<ScoreStatRow> statsTable = new TableView<>();
        statsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ScoreStatRow, String> subjectCol = new TableColumn<>("科目");
        subjectCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().subject));
        subjectCol.setPrefWidth(200);

        TableColumn<ScoreStatRow, String> avgCol = new TableColumn<>("平均分");
        avgCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().avgText));
        avgCol.setPrefWidth(120);

        TableColumn<ScoreStatRow, String> maxCol = new TableColumn<>("最高分");
        maxCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().maxText));
        maxCol.setPrefWidth(100);

        statsTable.getColumns().addAll(subjectCol, avgCol, maxCol);

        // 计算各科统计
        List<ScoreStatRow> rows = new ArrayList<>();
        double totalAvgSum = 0.0;
        double totalMaxSum = 0.0;

        for (String subject : subjects) {
            double sum = 0.0;
            double max = 0.0;
            for (Student student : source) {
                double score = student.getSubjectScore(subject);
                sum += score;
                if (score > max) max = score;
            }
            double avg = sum / source.size();
            totalAvgSum += avg;
            totalMaxSum += max;
            rows.add(new ScoreStatRow(subject, avg, max));
        }

        // 总分行
        double totalAvg = 0.0;
        double totalMax = 0.0;
        for (Student student : source) {
            double ts = student.getTotalScore();
            totalAvg += ts;
            if (ts > totalMax) totalMax = ts;
        }
        totalAvg = totalAvg / source.size();
        rows.add(new ScoreStatRow("【总分】", totalAvg, totalMax));

        statsTable.getItems().setAll(rows);
        dialog.getDialogPane().setContent(statsTable);
        dialog.showAndWait();

        setActionMessage("已完成平均分统计。", true);
    }

    private static class ScoreStatRow {
        final String subject;
        final String avgText;
        final String maxText;

        ScoreStatRow(String subject, double avg, double max) {
            this.subject = subject;
            this.avgText = String.format(Locale.ROOT, "%.2f", avg);
            this.maxText = String.format(Locale.ROOT, "%.2f", max);
        }
    }

    private List<Student> chooseStudentsByRange(String range) {
        if ("仅本科生".equals(range)) {
            return controller.browseByType(StudentType.UNDERGRADUATE);
        }
        if ("仅研究生".equals(range)) {
            return controller.browseByType(StudentType.POSTGRADUATE);
        }
        return controller.browseAllStudents();
    }

    private void showClassManagement() {
        List<Student> all = controller.browseAllStudents();
        if (all.isEmpty()) {
            showError("无数据", "当前没有学生数据，请先新增或导入学生。");
            return;
        }

        // 收集所有专业 + 类型
        Map<String, Map<String, Integer>> majorClassCount = new LinkedHashMap<>();
        Map<String, Integer> postgradClassCount = new LinkedHashMap<>();

        for (Student s : all) {
            if (s instanceof Undergraduate) {
                Undergraduate u = (Undergraduate) s;
                majorClassCount.computeIfAbsent(u.getMajor(), k -> new LinkedHashMap<>())
                        .merge(s.getClassName(), 1, Integer::sum);
            } else if (s instanceof Postgraduate) {
                postgradClassCount.merge(s.getClassName(), 1, Integer::sum);
            }
        }

        List<String> filterOptions = new ArrayList<>();
        filterOptions.add("全部");
        filterOptions.addAll(majorClassCount.keySet());
        if (!postgradClassCount.isEmpty()) {
            filterOptions.add("研究生");
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("班级管理");
        dialog.setHeaderText("查看班级");
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(400);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ComboBox<String> filterCombo = new ComboBox<>();
        filterCombo.setItems(FXCollections.observableArrayList(filterOptions));
        filterCombo.getSelectionModel().selectFirst();
        filterCombo.setMaxWidth(Double.MAX_VALUE);
        filterCombo.getStyleClass().add("form-control");

        TableView<ClassRow> classTable = new TableView<>();
        classTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ClassRow, String> majorCol = new TableColumn<>("专业/类型");
        majorCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().major));
        majorCol.setPrefWidth(160);

        TableColumn<ClassRow, String> classCol = new TableColumn<>("班级");
        classCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().className));
        classCol.setPrefWidth(200);

        TableColumn<ClassRow, String> countCol = new TableColumn<>("人数");
        countCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().count));
        countCol.setPrefWidth(80);

        classTable.getColumns().addAll(majorCol, classCol, countCol);

        Runnable refreshClassTable = () -> {
            String filter = filterCombo.getValue();
            List<ClassRow> rows = new ArrayList<>();

            if (filter == null || "全部".equals(filter)) {
                for (Map.Entry<String, Map<String, Integer>> majorEntry : majorClassCount.entrySet()) {
                    for (Map.Entry<String, Integer> cls : majorEntry.getValue().entrySet()) {
                        rows.add(new ClassRow(majorEntry.getKey(), cls.getKey(), cls.getValue()));
                    }
                }
                for (Map.Entry<String, Integer> cls : postgradClassCount.entrySet()) {
                    rows.add(new ClassRow("研究生", cls.getKey(), cls.getValue()));
                }
            } else if ("研究生".equals(filter)) {
                for (Map.Entry<String, Integer> cls : postgradClassCount.entrySet()) {
                    rows.add(new ClassRow("研究生", cls.getKey(), cls.getValue()));
                }
            } else {
                Map<String, Integer> classes = majorClassCount.get(filter);
                if (classes != null) {
                    for (Map.Entry<String, Integer> cls : classes.entrySet()) {
                        rows.add(new ClassRow(filter, cls.getKey(), cls.getValue()));
                    }
                }
            }
            classTable.getItems().setAll(rows);
        };

        filterCombo.setOnAction(e -> refreshClassTable.run());

        // 双击班级行 → 筛选主表格仅显示该班级学生
        final String[] selectedClass = {null};
        classTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                ClassRow row = classTable.getSelectionModel().getSelectedItem();
                if (row != null) {
                    selectedClass[0] = row.className;
                    dialog.close();
                }
            }
        });

        VBox content = new VBox(10, new Label("筛选专业："), filterCombo, classTable);
        content.setPadding(new Insets(10));
        VBox.setVgrow(classTable, Priority.ALWAYS);

        dialog.getDialogPane().setContent(content);
        refreshClassTable.run();
        dialog.showAndWait();

        if (selectedClass[0] != null) {
            String cn = selectedClass[0];
            List<Student> filtered = controller.browseAllStudents().stream()
                    .filter(s -> s.getClassName().equals(cn))
                    .collect(java.util.stream.Collectors.toList());
            refreshTable(filtered);
            supervisorField.clear();
            directionField.clear();
            setActionMessage("已筛选班级：" + cn + "（" + filtered.size() + "人）", true);
        } else {
            setActionMessage("已展示班级列表。", true);
        }
    }

    private static class ClassRow {
        final String major;
        final String className;
        final String count;

        ClassRow(String major, String className, int count) {
            this.major = major;
            this.className = className;
            this.count = String.valueOf(count);
        }
    }

    private void showCrossComparison() {
        List<Student> all = controller.browseAllStudents();
        if (all.isEmpty()) {
            showError("无数据", "当前没有学生数据。");
            return;
        }
        if (subjectNames.isEmpty()) {
            showError("无科目", "请先确定科目列表。");
            return;
        }

        // 收集所有本科专业
        Set<String> majorSet = new LinkedHashSet<>();
        for (Student s : all) {
            if (s instanceof Undergraduate) {
                String m = ((Undergraduate) s).getMajor();
                if (m != null && !m.isEmpty()) {
                    majorSet.add(m);
                }
            }
        }
        if (majorSet.isEmpty()) {
            showError("无数据", "当前没有本科生数据，无法横向对比。");
            return;
        }

        // 弹出选择专业的对话框
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("横向对比");
        dialog.setHeaderText("请选择要对比的专业");
        dialog.getDialogPane().getStyleClass().add("teacher-login-dialog");
        dialog.getDialogPane().setPrefWidth(450);

        ComboBox<String> majorCombo = new ComboBox<>();
        majorCombo.setItems(FXCollections.observableArrayList(majorSet));
        majorCombo.getSelectionModel().selectFirst();
        majorCombo.setMaxWidth(Double.MAX_VALUE);
        majorCombo.getStyleClass().add("form-control");

        VBox content = new VBox(12, new Label("选择专业："), majorCombo);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> btnResult = dialog.showAndWait();
        if (btnResult.isEmpty() || btnResult.get() != ButtonType.OK) {
            return;
        }
        String selectedMajor = majorCombo.getValue();
        if (selectedMajor == null || selectedMajor.isEmpty()) {
            return;
        }
        // 按班级分组，计算每班各科平均分
        Map<String, List<Undergraduate>> classGroups = new LinkedHashMap<>();
        for (Student s : all) {
            if (s instanceof Undergraduate) {
                Undergraduate u = (Undergraduate) s;
                if (selectedMajor.equals(u.getMajor())) {
                    classGroups.computeIfAbsent(u.getClassName(), k -> new ArrayList<>()).add(u);
                }
            }
        }

        if (classGroups.isEmpty()) {
            showError("无数据", "专业「" + selectedMajor + "」下没有班级数据。");
            return;
        }

        // 总分柱状图（按总分从高到低排序）
        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        xAxis.setLabel("班级");
        xAxis.setTickLabelRotation(0);
        xAxis.setTickLabelGap(4);
        xAxis.setTickLabelFont(javafx.scene.text.Font.font(11));
        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        yAxis.setLabel("总分平均分");
        javafx.scene.chart.BarChart<String, Number> barChart = new javafx.scene.chart.BarChart<>(xAxis, yAxis);
        barChart.setTitle("专业「" + selectedMajor + "」各班总分平均分对比");
        barChart.setLegendVisible(false);
        barChart.setAnimated(false);
        barChart.setBarGap(4);
        barChart.setCategoryGap(20);

        // 计算每班总分并排序（从高到低）
        List<Map.Entry<String, List<Undergraduate>>> sortedClasses = new ArrayList<>(classGroups.entrySet());
        sortedClasses.sort((a, b) -> {
            double avgA = a.getValue().stream().mapToDouble(Student::getTotalScore).average().orElse(0);
            double avgB = b.getValue().stream().mapToDouble(Student::getTotalScore).average().orElse(0);
            return Double.compare(avgB, avgA);
        });

        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        for (Map.Entry<String, List<Undergraduate>> entry : sortedClasses) {
            String className = entry.getKey();
            List<Undergraduate> students = entry.getValue();
            double totalSum = 0.0;
            for (Undergraduate u : students) {
                totalSum += u.getTotalScore();
            }
            double avgTotal = totalSum / students.size();
            series.getData().add(new javafx.scene.chart.XYChart.Data<>(className, avgTotal));
        }
        barChart.getData().add(series);

        Stage chartStage = new Stage();
        chartStage.setTitle("横向对比柱状图 - " + selectedMajor);
        Scene chartScene = new Scene(barChart, 900, 500);
        String stylesheet = resolveStyleSheet();
        if (stylesheet != null) {
            chartScene.getStylesheets().add(stylesheet);
        }
        chartStage.setScene(chartScene);
        setWindowIcon(chartStage);

        // 强制所有 X 轴标签水平排列（JavaFX 在空间不足时会自动旋转标签，需在渲染后重置）
        chartStage.setOnShown(e -> {
            for (Node node : xAxis.lookupAll(".tick-label")) {
                node.setRotate(0);
                node.setStyle("-fx-rotate: 0;");
            }
        });
        chartStage.show();

        setActionMessage("已完成专业「" + selectedMajor + "」的横向对比（含柱状图）。", true);
    }

    private void showCurrentClassInfo() {
        Student selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("未选择", "请先在数据列表中选中一名学生。");
            return;
        }
        String className = selected.getClassName();
        List<Student> all = controller.browseAllStudents();
        List<Student> classMembers = all.stream()
                .filter(s -> s.getClassName().equals(className))
                .collect(java.util.stream.Collectors.toList());

        if (classMembers.isEmpty()) {
            showError("无数据", "未找到班级「" + className + "」的学生。");
            return;
        }

        // 构建信息文本
        StringBuilder sb = new StringBuilder();
        sb.append("班级：").append(className).append("\n");
        sb.append("人数：").append(classMembers.size()).append("人\n\n");

        if (!subjectNames.isEmpty()) {
            sb.append(String.format("%-10s %8s %8s\n", "科目", "平均分", "最高分"));
            sb.append("----------------------------\n");
            for (String subject : subjectNames) {
                double sum = 0.0;
                double max = Double.MIN_VALUE;
                for (Student s : classMembers) {
                    double score = s.getSubjectScore(subject);
                    sum += score;
                    if (score > max) max = score;
                }
                double avg = sum / classMembers.size();
                sb.append(String.format(Locale.ROOT, "%-10s %8.2f %8.2f\n", subject, avg, max));
            }

            double totalAvg = classMembers.stream()
                    .mapToDouble(Student::getTotalScore).average().orElse(0);
            sb.append("----------------------------\n");
            sb.append(String.format(Locale.ROOT, "总分平均：%.2f\n", totalAvg));
        }

        showInfo("班级信息 - " + className, sb.toString());
        setActionMessage("已展示班级「" + className + "」的信息。", true);
    }

    private FormData readFormData(boolean forUpdate) {
        FormData data = new FormData();

        data.type = typeBox.getValue();
        if (data.type == null) {
            throw new IllegalArgumentException("请选择学生类型。");
        }

        data.id = mustNotBlank(idField.getText(), "学号不能为空。");
        data.name = mustNotBlank(nameField.getText(), "姓名不能为空。");
        data.age = parseAge(ageField.getValue());
        data.className = mustNotBlank(classField.getValue(), "班级不能为空。");

        String province = mustNotBlank(provinceField.getValue(), "省份不能为空。");
        String city = mustNotBlank(cityField.getValue(), "城市不能为空。");
        String street = mustNotBlank(streetField.getText(), "街道不能为空。");
        String houseNo = mustNotBlank(houseNoField.getText(), "门牌号不能为空。");
        data.address = new Address(province, city, street, houseNo);

        ScoreInput scoreInput = collectScoresFromForm();
        data.scores = scoreInput.scores;
        data.subjectsToInitialize = scoreInput.initialSubjects;

        if (data.type == StudentType.UNDERGRADUATE) {
            data.major = mustNotBlank(majorField.getValue(), "本科生专业不能为空。");
            data.className = data.major + data.className;
        } else {
            data.supervisor = mustNotBlank(supervisorField.getText(), "研究生导师不能为空。");
            data.direction = mustNotBlank(directionField.getText(), "研究方向不能为空。");
        }

        if (forUpdate) {
            Optional<Student> existing = service.findById(data.id);
            if (existing.isPresent() && existing.get().getStudentType() != data.type) {
                throw new IllegalArgumentException("修改时不支持跨类型变更，请保持与原类型一致。");
            }
        }

        return data;
    }

    private double parseScore(String text, String message) {
        String value = mustNotBlank(text, message);
        try {
            double score = Double.parseDouble(value);
            if (score < 0 || score > 100) {
                throw new IllegalArgumentException("成绩范围必须在 0 到 100 之间。");
            }
            return score;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(message);
        }
    }

    private int parseAge(String text) {
        String value = mustNotBlank(text, "年龄不能为空。");
        try {
            int age = Integer.parseInt(value);
            if (age < 1 || age > 150) {
                throw new IllegalArgumentException("年龄范围必须在 1 到 150 之间。");
            }
            return age;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("年龄必须是整数。");
        }
    }

    private String mustNotBlank(String text, String message) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return trimmed;
    }

    private void fillForm(Student student) {
        if (student == null) {
            return;
        }

        typeBox.setValue(student.getStudentType());
        idField.setText(student.getStudentId());
        nameField.setText(student.getName());
        String age = String.valueOf(student.getAge());
        ensureComboItem(ageField, age);
        ageField.setValue(age);
        ensureComboItem(classField, student.getClassName());
        classField.setValue(student.getClassName());

        // 解析本科生班级名（如"软件工程1班"→ 专业="软件工程"，班级="1班"）
        String className = student.getClassName();
        if (student instanceof Undergraduate) {
            String[] undergradMajors = {"通信工程", "软件工程", "计算机科学与技术", "电子信息工程"};
            for (String m : undergradMajors) {
                if (className.startsWith(m)) {
                    String suffix = className.substring(m.length());
                    ensureComboItem(classField, suffix);
                    classField.setValue(suffix);
                    break;
                }
            }
        }

        Address address = student.getAddress();
        ensureComboItem(provinceField, address.getProvince());
        provinceField.setValue(address.getProvince());
        refreshCityOptions(address.getProvince());
        ensureComboItem(cityField, address.getCity());
        cityField.setValue(address.getCity());
        streetField.setText(address.getStreet());
        houseNoField.setText(address.getHouseNumber());

        fillScores(student);

        if (student instanceof Undergraduate) {
            majorField.setValue(((Undergraduate) student).getMajor());
            supervisorField.clear();
            directionField.clear();
            profileLabel.setText("专业信息：本科生 - " + ((Undergraduate) student).getMajor());
        } else if (student instanceof Postgraduate) {
            supervisorField.setText(((Postgraduate) student).getSupervisor());
            directionField.setText(((Postgraduate) student).getResearchDirection());
            majorField.setValue(null);
            profileLabel.setText("专业信息：研究生 - 导师=" + ((Postgraduate) student).getSupervisor() + "，方向=" + ((Postgraduate) student).getResearchDirection());
        }
    }

    private void clearForm() {
        typeBox.setValue(StudentType.UNDERGRADUATE);
        idField.clear();
        nameField.clear();
        ageField.setValue(null);
        classField.setValue(null);
        queryField.clear();
        if (!subjectField.isDisabled()) {
            subjectField.clear();
        }

        provinceField.setValue(null);
        cityField.setValue(null);
        cityField.setItems(FXCollections.observableArrayList());
        streetField.clear();
        houseNoField.clear();

        majorField.setValue(null);
        supervisorField.clear();
        directionField.clear();

        clearScoreValues();
        table.getSelectionModel().clearSelection();
        idField.requestFocus();
        profileLabel.setText("专业信息：未选择学生");
        setActionMessage("表单已清空，请从学号开始录入。", true);
    }

    private void generateBatchStudents() {
        if (subjectNames.isEmpty()) {
            showError("无法生成", "请先手动新增一名学生以确定科目列表，再使用批量生成。");
            return;
        }
        if (service.isFull()) {
            showError("无法生成", "存储已满（" + service.getCurrentSize() + "/" + service.getCapacity() + "），无法继续生成。");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("批量生成学生");
        confirm.setHeaderText("将批量生成300名学生");
        confirm.setContentText("本科生：4个专业 × 3个班 × 20人 = 240人\n研究生：60人\n随机中文名、随机中国地址、随机成绩。\n是否继续？");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            setActionMessage("已取消批量生成。", true);
            return;
        }

        java.util.Random random = new java.util.Random(42);

        String[] surnames = {
            "赵", "钱", "孙", "李", "周", "吴", "郑", "王", "冯", "陈", "褚", "卫", "蒋", "沈", "韩", "杨",
            "朱", "秦", "尤", "许", "何", "吕", "施", "张", "孔", "曹", "严", "华", "金", "魏", "陶", "姜",
            "戚", "谢", "邹", "喻", "柏", "水", "窦", "章", "云", "苏", "潘", "葛", "奚", "范", "彭", "郎",
            "鲁", "韦", "昌", "马", "苗", "凤", "花", "方", "俞", "任", "袁", "柳", "酆", "鲍", "史", "唐",
            "费", "廉", "岑", "薛", "雷", "贺", "倪", "汤", "滕", "殷", "罗", "毕", "郝", "邬", "安", "常",
            "乐", "于", "时", "傅", "皮", "卞", "齐", "康", "伍", "余", "元", "卜", "顾", "孟", "平", "黄",
            "和", "穆", "萧", "尹", "姚", "邵", "湛", "汪", "祁", "毛", "禹", "狄", "米", "贝", "明", "臧"
        };
        String[] givenNameChars = {
            "伟", "芳", "娜", "敏", "静", "丽", "强", "磊", "洋", "勇", "艳", "杰", "涛", "明", "超", "秀",
            "英", "华", "慧", "建", "文", "军", "斌", "婷", "洁", "峰", "玉", "兰", "萍", "飞", "玲", "鑫",
            "平", "桂", "芬", "燕", "霞", "鹏", "浩", "然", "博", "辉", "林", "宇", "志", "国", "家", "瑞",
            "雪", "梅", "春", "秋", "晨", "曦", "雨", "琪", "宁", "安", "思", "怡", "欣", "瑶", "梦", "丹"
        };
        String[] streets = {
            "中山路", "人民路", "建设路", "解放路", "长安街", "南京路", "北京路", "和平路",
            "文化路", "科技路", "学府路", "朝阳路", "东风路", "花园路", "幸福路", "光明路"
        };
        String[] majors = {
            "通信工程", "软件工程", "计算机科学与技术", "电子信息工程"
        };
        String[] directions = {
            "机器学习", "数据挖掘", "自然语言处理", "计算机视觉", "网络安全",
            "分布式系统", "云计算", "区块链", "人机交互", "嵌入式系统"
        };
        String[] supervisorSurnames = {
            "张", "李", "王", "刘", "陈", "杨", "赵", "黄", "周", "吴", "徐", "孙",
            "胡", "朱", "高", "林", "何", "郭", "马", "罗"
        };

        List<String> provinceList = new ArrayList<>(provinceCityMap.keySet());

        int totalGenerate = 300;
        int added = 0;
        int failed = 0;
        int remaining = service.getCapacity() - service.getCurrentSize();
        int actualGenerate = Math.min(totalGenerate, remaining);

        // 本科生：4专业 × 3班 × 20人 = 240人
        int undergradPerClass = 20;
        int classesPerMajor = 3;
        int undergradTotal = majors.length * classesPerMajor * undergradPerClass; // 240
        // 研究生：3班 × 20人 = 60人
        int postgradPerClass = 20;
        int postgradClasses = 3;
        int postgradTotal = postgradClasses * postgradPerClass; // 60

        for (int i = 0; i < actualGenerate; i++) {
            String surname = surnames[random.nextInt(surnames.length)];
            String given;
            if (random.nextBoolean()) {
                given = givenNameChars[random.nextInt(givenNameChars.length)];
            } else {
                given = givenNameChars[random.nextInt(givenNameChars.length)]
                        + givenNameChars[random.nextInt(givenNameChars.length)];
            }
            String name = surname + given;

            String province = provinceList.get(random.nextInt(provinceList.size()));
            List<String> cities = provinceCityMap.get(province);
            String city = cities.get(random.nextInt(cities.size()));
            String street = streets[random.nextInt(streets.length)];
            String houseNo = (random.nextInt(200) + 1) + "号";
            Address address = new Address(province, city, street, houseNo);

            String studentId = String.format("2025%04d", i + 1);

            LinkedHashMap<String, Double> scores = new LinkedHashMap<>();
            for (String subject : subjectNames) {
                scores.put(subject, 40.0 + random.nextDouble() * 60.0);
            }

            if (i < undergradTotal) {
                int majorIdx = (i / (classesPerMajor * undergradPerClass)) % majors.length;
                int classNum = ((i / undergradPerClass) % classesPerMajor) + 1;
                String major = majors[majorIdx];
                String className = major + classNum + "班";
                int age = random.nextInt(6) + 18; // 18-23
                Undergraduate stu = new Undergraduate(studentId, name, age, className, major, address, scores);
                if (service.addStudent(stu)) {
                    added++;
                } else {
                    failed++;
                    rollbackCounters(stu);
                }
            } else {
                int postIdx = i - undergradTotal;
                int classNum = (postIdx / postgradPerClass) % postgradClasses + 1;
                String className = "计算机研" + classNum + "班";
                int age = random.nextInt(9) + 22; // 22-30
                String supervisor = supervisorSurnames[random.nextInt(supervisorSurnames.length)] + "教授";
                String direction = directions[random.nextInt(directions.length)];
                Postgraduate stu = new Postgraduate(studentId, name, age, className, supervisor, direction, address, scores);
                if (service.addStudent(stu)) {
                    added++;
                } else {
                    failed++;
                    rollbackCounters(stu);
                }
            }
        }

        refreshTable(controller.browseAllStudents());
        updateStatus();
        setUnsavedChanges(true);
        renderScoreEditor();
        rebuildTableColumns();

        String msg = String.format(
                "批量生成完成。\n成功：%d 人\n失败：%d 人\n当前学生总数：%d",
                added, failed, service.getTotalCount()
        );
        setActionMessage(msg, added > 0);
        showInfo("批量生成结果", msg);
    }

    private void rollbackCounters(Student student) {
        Student.decreaseTotalCount();
        if (student instanceof Undergraduate) {
            Undergraduate.decreaseCount();
        } else if (student instanceof Postgraduate) {
            Postgraduate.decreaseCount();
        }
    }

    private void clearFormAndAllStudents() {
        clearForm();
        MenuController.OperationResult result = controller.clearAllStudents();
        if (!result.isSuccess()) {
            setActionMessage(result.getMessage(), false);
            return;
        }
        subjectNames.clear();
        dynamicExtraColumns.clear();
        importExtraValuesByStudentId.clear();
        rankByStudentId.clear();
        renderScoreEditor();
        rebuildTableColumns();
        refreshTable(controller.browseAllStudents());
        updateStatus();
        setUnsavedChanges(true);
        setActionMessage("已清空表单和全部学生数据。", true);
    }

    private boolean confirmClearAllStudents() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认清空");
        alert.setHeaderText("确认清空全部学生数据？");
        alert.setContentText("此操作会清空当前数据列表，且无法撤销。是否继续？");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void refreshTable(List<Student> students) {
        List<Student> safeStudents = students == null ? Collections.emptyList() : students;
        List<Student> allStudents = controller.browseAllStudents();
        List<String> latestSubjects = collectSubjectNames(allStudents);
        if (!latestSubjects.equals(subjectNames)) {
            subjectNames.clear();
            subjectNames.addAll(latestSubjects);
            renderScoreEditor();
            rebuildTableColumns();
        }
        updateRanking(safeStudents);
        table.setItems(FXCollections.observableArrayList(safeStudents));
        table.refresh();
    }

    private List<String> collectSubjectNames(List<Student> students) {
        LinkedHashSet<String> subjects = new LinkedHashSet<>();
        for (Student student : students) {
            subjects.addAll(student.getSubjectScores().keySet());
        }
        return new ArrayList<>(subjects);
    }

    private void updateStatus() {
        statusLabel.setText(String.format(
                "当前统计：本科生 %d 人，研究生 %d 人，总人数 %d 人，存储使用 %d/%d",
                service.getUndergraduateCount(),
                service.getPostgraduateCount(),
                service.getTotalCount(),
                service.getCurrentSize(),
                service.getCapacity()
        ));
    }

    private String typeSpecificText(Student student) {
        if (student instanceof Undergraduate) {
            return "专业=" + ((Undergraduate) student).getMajor();
        }
        if (student instanceof Postgraduate) {
            Postgraduate pg = (Postgraduate) student;
            return "导师=" + pg.getSupervisor() + "，方向=" + pg.getResearchDirection();
        }
        return "";
    }

    private void showAbout() {
        showInfo("关于", controller.buildAboutText());
        setActionMessage("已打开关于信息。", true);
    }

    private void deleteStudentById(String id) {
        if (id == null || id.trim().isEmpty()) {
            showError("删除失败", "学号为空，无法删除。");
            return;
        }
        MenuController.OperationResult result = controller.deleteStudentById(id.trim());
        if (!result.isSuccess()) {
            showError("删除失败", result.getMessage());
            return;
        }
        refreshTable(controller.browseAllStudents());
        updateStatus();
        clearForm();
        setUnsavedChanges(true);
        setActionMessage("删除成功：" + id, true);
    }

    private void importStudentsFromFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导入学生信息文件");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel 文件 (*.xlsx)", "*.xlsx"));
        File source = chooser.showOpenDialog(table.getScene() == null ? null : table.getScene().getWindow());
        if (source == null) {
            setActionMessage("已取消导入操作。", true);
            return;
        }

        loadStudentsFromFile(source, false, true);
    }

    private void tryAutoLoadLastSavedFile() {
        String savedPath = preferences.get(PREF_LAST_SAVED_FILE, "");
        if (savedPath == null || savedPath.trim().isEmpty()) {
            return;
        }
        File source = new File(savedPath.trim());
        if (!source.isFile()) {
            setActionMessage("最近保存文件不存在，已跳过自动读取。", false);
            return;
        }
        loadStudentsFromFile(source, true, false);
    }

    private void loadStudentsFromFile(File source, boolean silentOnSuccess, boolean markUnsavedAfterImport) {

        List<List<String>> rows;
        try {
            rows = SimpleXlsxReader.read(source);
        } catch (IOException ex) {
            showError("导入失败", "读取文件失败：" + ex.getMessage());
            return;
        }

        if (rows.size() < 2) {
            showError("导入失败", "文件中没有可导入的数据行。");
            return;
        }

        ImportSheetAnalyzer.AnalysisResult analysis = ImportSheetAnalyzer.analyze(rows);
        if (!analysis.isSuccess()) {
            showError("导入失败", analysis.getMessage());
            return;
        }

        int dataStartRow = analysis.getDataStartRow();
        List<String> headers = analysis.getHeaders();
        Map<String, Integer> index = analysis.getIndex();
        Map<String, Integer> fieldBindings = analysis.getFieldBindings();
        int idCol = analysis.getIdColumn();
        int nameCol = analysis.getNameColumn();
        Integer classCol = analysis.getClassColumn();
        Integer majorCol = analysis.getMajorColumn();
        Integer ageCol = boundOrAlias(fieldBindings, "age", index, "年龄", "age");
        Integer typeCol = boundOrAlias(fieldBindings, "type", index, "类型", "学生类型", "类别", "身份");
        Integer supervisorCol = boundOrAlias(fieldBindings, "supervisor", index, "导师", "指导教师", "班主任");
        Integer directionCol = boundOrAlias(fieldBindings, "direction", index, "研究方向", "方向");
        Integer provinceCol = boundOrAlias(fieldBindings, "province", index, "省份", "省");
        Integer cityCol = boundOrAlias(fieldBindings, "city", index, "城市", "市");
        Integer streetCol = boundOrAlias(fieldBindings, "street", index, "街道", "详细地址", "地址");
        Integer houseCol = boundOrAlias(fieldBindings, "house", index, "门牌号", "门牌");
        Integer classRankCol = boundOrAlias(fieldBindings, "classRank", index, "班级排名", "班排名", "班内排名");
        Integer gradeRankCol = boundOrAlias(fieldBindings, "gradeRank", index, "年级排名", "校排名");

        LinkedHashMap<String, Integer> scoreColumns = analysis.getScoreColumns();
        if (scoreColumns.isEmpty()) {
            showError("导入失败", "文件中未识别到可用成绩列。至少需要总分列或一个数值成绩列。");
            return;
        }
        LinkedHashMap<String, Integer> extraColumns = analysis.getExtraColumns();
        List<String> extraHeaders = new ArrayList<>(extraColumns.keySet());
        dynamicExtraColumns.addAll(extraHeaders);

        int added = 0;
        int updated = 0;
        int failed = 0;
        int rankingDetected = 0;
        int scoreWarningCount = 0;
        List<String> failureSamples = new ArrayList<>();
        List<String> scoreWarningSamples = new ArrayList<>();

        for (int r = dataStartRow; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            if (ImportSheetAnalyzer.shouldSkipDataRow(row)) {
                continue;
            }

            String id = ImportSheetAnalyzer.normalizeText(getCell(row, idCol));
            if (isBlank(id)) {
                continue;
            }
            try {
                String name = normalizeImportedText(getCell(row, nameCol));
                String typeText = getCellByIndex(row, typeCol);
                String supervisor = getCellByIndex(row, supervisorCol);
                String direction = getCellByIndex(row, directionCol);
                StudentType type = resolveType(typeText, supervisor, direction);
                int age = parseIntegerOrDefault(getCellByIndex(row, ageCol), 16);
                String className = fallbackIfBlank(getCellByIndex(row, classCol), "未分班");
                String classRank = getCellByIndex(row, classRankCol);
                String gradeRank = getCellByIndex(row, gradeRankCol);
                if (!isBlank(classRank) || !isBlank(gradeRank)) {
                    rankingDetected++;
                }
                Address address = new Address(
                        fallbackIfBlank(getCellByIndex(row, provinceCol), "未知省份"),
                        fallbackIfBlank(getCellByIndex(row, cityCol), "未知城市"),
                        fallbackIfBlank(getCellByIndex(row, streetCol), "未知街道"),
                        fallbackIfBlank(getCellByIndex(row, houseCol), "0")
                );

                LinkedHashMap<String, Double> scores = new LinkedHashMap<>();
                for (Map.Entry<String, Integer> scoreColumn : scoreColumns.entrySet()) {
                    String subject = scoreColumn.getKey();
                    String scoreText = getCell(row, scoreColumn.getValue());
                    ScoreParseResult parsedScore = parseFlexibleScore(scoreText, subject);
                    scores.put(subject, parsedScore.value);
                    if (parsedScore.warning) {
                        scoreWarningCount++;
                        if (scoreWarningSamples.size() < 5) {
                            scoreWarningSamples.add("第 " + (r + 1) + " 行 / " + subject + "：" + parsedScore.message);
                        }
                    }
                }

                rememberExtraValuesByColumns(id, extraColumns, row);
                rememberImportedTypeLabel(id, typeText, classRank, gradeRank, supervisor, direction, majorCol, row);

                String major = getCellByIndex(row, majorCol);
                if (isBlank(major) && (!isBlank(classRank) || !isBlank(gradeRank))) {
                    StringBuilder majorBuilder = new StringBuilder("高中生成绩单");
                    if (!isBlank(classRank)) {
                        majorBuilder.append(" 班级排名=").append(classRank);
                    }
                    if (!isBlank(gradeRank)) {
                        majorBuilder.append(" 年级排名=").append(gradeRank);
                    }
                    major = majorBuilder.toString();
                }

                MenuController.StudentFormData data = new MenuController.StudentFormData(
                        type, id, name, age, className, address, scores,
                        type == StudentType.UNDERGRADUATE ? major : null,
                        type == StudentType.POSTGRADUATE ? supervisor : null,
                        type == StudentType.POSTGRADUATE ? direction : null
                );

                if (service.findById(id).isPresent()) {
                    MenuController.OperationResult res = controller.updateStudent(data);
                    if (res.isSuccess()) {
                        updated++;
                    } else {
                        failed++;
                    }
                } else {
                    MenuController.OperationResult res = controller.addStudent(data);
                    if (res.isSuccess()) {
                        added++;
                    } else {
                        failed++;
                    }
                }
            } catch (Exception ex) {
                failed++;
                if (failureSamples.size() < 5) {
                    failureSamples.add("第 " + (r + 1) + " 行：" + ex.getMessage());
                }
            }
        }

        refreshTable(controller.browseAllStudents());
        updateStatus();
        if (markUnsavedAfterImport && (added > 0 || updated > 0)) {
            setUnsavedChanges(true);
        }
        String rankNote = rankingDetected > 0 ? "，识别班级/年级排名 " + rankingDetected + " 行" : "";
        String warningNote = scoreWarningCount > 0 ? "，成绩容错 " + scoreWarningCount + " 处" : "";
        setActionMessage("导入完成：新增 " + added + "，更新 " + updated + "，失败 " + failed + rankNote + warningNote, failed == 0);
        if (silentOnSuccess) {
            if (failed == 0) {
                setActionMessage("已自动读取最近保存文件：" + source.getAbsolutePath(), true);
            } else {
                showInfo("自动读取完成", "最近保存文件读取完成。\n新增：" + added + "\n更新：" + updated + "\n失败：" + failed
                        + (failureSamples.isEmpty() ? "" : "\n示例失败原因：\n- " + String.join("\n- ", failureSamples))
                        + (scoreWarningSamples.isEmpty() ? "" : "\n成绩容错示例：\n- " + String.join("\n- ", scoreWarningSamples)));
            }
        } else {
            showInfo("导入完成", "新增：" + added + "\n更新：" + updated + "\n失败：" + failed
                    + "\n识别排名：" + rankingDetected
                    + "\n成绩容错：" + scoreWarningCount
                    + (failureSamples.isEmpty() ? "" : "\n\n示例失败原因：\n- " + String.join("\n- ", failureSamples))
                    + (scoreWarningSamples.isEmpty() ? "" : "\n\n成绩容错示例：\n- " + String.join("\n- ", scoreWarningSamples)));
        }
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i) == null ? "" : headers.get(i).trim();
            if (!h.isEmpty()) {
                index.put(h, i);
                index.put(normalizeHeader(h), i);
            }
        }
        return index;
    }

    private List<String> detectSubjectHeaders(List<String> headers) {
        List<String> subjects = new ArrayList<>();
        Set<String> seenSubjects = new HashSet<>();

        for (String header : headers) {
            String h = header == null ? "" : header.trim();
            if (!h.isEmpty() && !isMetaHeader(h) && seenSubjects.add(h)) {
                subjects.add(h);
            }
        }
        return subjects;
    }

    private List<String> detectExtraHeaders(List<String> headers, List<String> subjectHeaders) {
        List<String> extras = new ArrayList<>();
        LinkedHashSet<String> subjectSet = new LinkedHashSet<>(subjectHeaders);
        for (String header : headers) {
            String h = header == null ? "" : header.trim();
            if (h.isEmpty()) {
                continue;
            }
            String normalized = normalizeHeader(h);
            if (subjectSet.contains(h)) {
                continue;
            }
            if (!isCoreMetaHeader(normalized)) {
                extras.add(h);
            }
        }
        return extras;
    }

    private int detectHeaderRowIndex(List<List<String>> rows) {
        int scanLimit = Math.min(rows.size(), 40);
        int bestIndex = -1;
        int bestScore = -1;
        for (int i = 0; i < scanLimit; i++) {
            List<String> row = rows.get(i);
            int score = scoreHeaderRow(row);
            if (score > bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }
        if (bestScore >= 3) {
            return bestIndex;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (!shouldSkipDataRow(rows.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private int scoreHeaderRow(List<String> row) {
        if (row == null || row.isEmpty()) {
            return -1;
        }
        int score = 0;
        for (String cell : row) {
            String normalized = normalizeHeader(cell);
            if (normalized.isEmpty()) {
                continue;
            }
            if (normalized.contains("学号") || normalized.contains("考号") || normalized.contains("准考证") || normalized.contains("studentid")) {
                score += 3;
            }
            if (normalized.contains("姓名") || normalized.contains("名字") || "name".equals(normalized)) {
                score += 3;
            }
            if (normalized.contains("成绩") || normalized.contains("分数") || normalized.contains("总分")) {
                score += 2;
            }
            if (normalized.contains("班级") || normalized.contains("专业") || normalized.contains("学院")) {
                score += 1;
            }
        }
        return score;
    }

    private int resolveIdColumn(Map<String, Integer> index, List<String> headers, int dataStartRow, List<List<String>> rows) {
        Integer direct = findColumn(index, "学号", "考号", "准考证号", "学生ID", "studentid", "id");
        if (direct != null) {
            return direct;
        }
        int inferred = inferUniqueLikeColumn(rows, dataStartRow, null);
        if (inferred >= 0) {
            return inferred;
        }
        return inferBestDistinctColumn(rows, dataStartRow, null);
    }

    private int resolveNameColumn(Map<String, Integer> index, List<String> headers, int idCol) {
        Integer direct = findColumn(index, "姓名", "学生姓名", "名字", "name");
        if (direct != null) {
            return direct;
        }
        for (int i = 0; i < headers.size(); i++) {
            if (i == idCol) {
                continue;
            }
            String h = normalizeHeader(headers.get(i));
            if (h.contains("姓名") || h.contains("名字") || "name".equals(h)) {
                return i;
            }
        }
        return -1;
    }

    private int inferUniqueLikeColumn(List<List<String>> rows, int dataStartRow, Integer excludeCol) {
        if (rows.isEmpty() || dataStartRow >= rows.size()) {
            return -1;
        }
        int maxCols = 0;
        for (List<String> row : rows) {
            maxCols = Math.max(maxCols, row.size());
        }
        int bestCol = -1;
        double bestDistinctRatio = 0.0;
        for (int c = 0; c < maxCols; c++) {
            if (excludeCol != null && c == excludeCol) {
                continue;
            }
            Set<String> distinct = new HashSet<>();
            int valid = 0;
            for (int r = dataStartRow; r < rows.size(); r++) {
                String value = normalizeImportedText(getCell(rows.get(r), c));
                if (value.isEmpty()) {
                    continue;
                }
                valid++;
                distinct.add(value);
            }
            if (valid >= 2 && distinct.size() >= 2) {
                double distinctRatio = distinct.size() * 1.0 / valid;
                if (distinctRatio > bestDistinctRatio && distinctRatio >= 0.85) {
                    bestDistinctRatio = distinctRatio;
                    bestCol = c;
                }
            }
        }
        return bestCol;
    }

    private int inferBestDistinctColumn(List<List<String>> rows, int dataStartRow, Integer excludeCol) {
        if (rows.isEmpty() || dataStartRow >= rows.size()) {
            return -1;
        }
        int maxCols = 0;
        for (List<String> row : rows) {
            maxCols = Math.max(maxCols, row.size());
        }
        int bestCol = -1;
        int bestDistinct = -1;
        for (int c = 0; c < maxCols; c++) {
            if (excludeCol != null && c == excludeCol) {
                continue;
            }
            Set<String> distinct = new HashSet<>();
            int valid = 0;
            for (int r = dataStartRow; r < rows.size(); r++) {
                String value = normalizeImportedText(getCell(rows.get(r), c));
                if (value.isEmpty()) {
                    continue;
                }
                valid++;
                distinct.add(value);
            }
            if (valid >= 2 && distinct.size() > bestDistinct) {
                bestDistinct = distinct.size();
                bestCol = c;
            }
        }
        return bestCol;
    }

    private LinkedHashMap<String, Integer> detectScoreColumns(List<String> headers,
                                                               Map<String, Integer> index,
                                                               List<List<String>> rows,
                                                               int dataStartRow,
                                                               int idCol,
                                                               int nameCol,
                                                               Integer classCol,
                                                               Integer majorCol,
                                                               Integer ageCol) {
        LinkedHashMap<String, Integer> columns = new LinkedHashMap<>();

        List<String> detected = detectSubjectHeaders(headers);
        for (String subject : detected) {
            Integer pos = index.get(subject);
            if (pos != null) {
                double ratio = columnNumericRatio(rows, dataStartRow, pos);
                String normalizedHeader = normalizeHeader(subject);
                if (isScoreLikeHeader(normalizedHeader) || ratio >= 0.80) {
                    columns.put(subject, pos);
                }
            }
        }

        // 如果已有其他科目成绩列，则忽略"总分"列，因为getTotalScore()会自动计算。
        // 仅当没有其他科目列时，才把"总分"作为唯一成绩列保留。
        if (columns.isEmpty()) {
            Integer totalCol = findColumn(index, "总分", "总成绩", "总成绩分", "score", "成绩", "分数");
            if (totalCol != null) {
                columns.put(fallbackIfBlank(getCell(headers, totalCol), "总分"), totalCol);
            }

            Set<Integer> exclude = new HashSet<>();
            exclude.add(idCol);
            exclude.add(nameCol);
            if (classCol != null) exclude.add(classCol);
            if (majorCol != null) exclude.add(majorCol);
            if (ageCol != null) exclude.add(ageCol);
            int fallbackCol = inferNumericColumn(rows, dataStartRow, exclude);
            if (fallbackCol >= 0) {
                String title = fallbackIfBlank(getCell(headers, fallbackCol), "成绩");
                columns.put(title, fallbackCol);
            }
        }

        return columns;
    }

    private int inferNumericColumn(List<List<String>> rows, int dataStartRow, Set<Integer> exclude) {
        int maxCols = 0;
        for (List<String> row : rows) {
            maxCols = Math.max(maxCols, row.size());
        }
        int bestCol = -1;
        double bestRatio = 0.0;
        for (int c = 0; c < maxCols; c++) {
            if (exclude.contains(c)) {
                continue;
            }
            int seen = 0;
            int numeric = 0;
            for (int r = dataStartRow; r < rows.size(); r++) {
                String value = normalizeImportedText(getCell(rows.get(r), c));
                if (value.isEmpty()) {
                    continue;
                }
                seen++;
                if (isNumericLike(value)) {
                    numeric++;
                }
            }
            if (seen == 0) {
                continue;
            }
            double ratio = numeric * 1.0 / seen;
            if (seen >= 2 && ratio > bestRatio && ratio >= 0.6) {
                bestRatio = ratio;
                bestCol = c;
            }
        }
        return bestCol;
    }

    private double columnNumericRatio(List<List<String>> rows, int dataStartRow, int col) {
        int seen = 0;
        int numeric = 0;
        for (int r = dataStartRow; r < rows.size(); r++) {
            String value = normalizeImportedText(getCell(rows.get(r), col));
            if (value.isEmpty()) {
                continue;
            }
            seen++;
            if (isNumericLike(value)) {
                numeric++;
            }
        }
        if (seen == 0) {
            return 0.0;
        }
        return numeric * 1.0 / seen;
    }

    private boolean isScoreLikeHeader(String normalizedHeader) {
        return normalizedHeader.contains("分")
                || normalizedHeader.contains("成绩")
                || normalizedHeader.contains("score")
                || normalizedHeader.contains("语文")
                || normalizedHeader.contains("数学")
                || normalizedHeader.contains("英语")
                || normalizedHeader.contains("物理")
                || normalizedHeader.contains("化学")
                || normalizedHeader.contains("生物")
                || normalizedHeader.contains("政治")
                || normalizedHeader.contains("历史")
                || normalizedHeader.contains("地理");
    }

    private boolean shouldSkipDataRow(List<String> row) {
        if (row == null || row.isEmpty()) {
            return true;
        }
        int nonBlank = 0;
        String first = "";
        for (String cell : row) {
            String value = normalizeImportedText(cell);
            if (!value.isEmpty()) {
                nonBlank++;
                if (first.isEmpty()) {
                    first = value;
                }
            }
        }
        if (nonBlank == 0) {
            return true;
        }
        String firstNorm = normalizeHeader(first);
        return nonBlank == 1 && (firstNorm.contains("说明") || firstNorm.contains("备注") || firstNorm.contains("单位") || firstNorm.contains("标题"));
    }

    private String getCellByIndex(List<String> row, Integer idx) {
        if (idx == null || idx < 0) {
            return "";
        }
        return normalizeImportedText(getCell(row, idx));
    }

    private String normalizeImportedText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u3000', ' ').trim();
    }

    private boolean isNumericLike(String text) {
        String value = normalizeImportedText(text).replace(",", "");
        if (value.isEmpty()) {
            return false;
        }
        if (value.matches("-?\\d+(\\.\\d+)?")) {
            return true;
        }
        String numeric = value.replaceAll("[^0-9.\\-]", "");
        return !numeric.isEmpty() && numeric.matches("-?\\d+(\\.\\d+)?");
    }

    private void rememberExtraValues(String studentId, List<String> extraHeaders, List<String> row, Map<String, Integer> index) {
        if (studentId == null || studentId.trim().isEmpty() || extraHeaders.isEmpty()) {
            return;
        }
        Map<String, String> values = importExtraValuesByStudentId.computeIfAbsent(studentId, k -> new LinkedHashMap<>());
        for (String header : extraHeaders) {
            String value = cell(row, index, header);
            values.put(header, value);
        }
    }

    private void rememberExtraValuesByColumns(String studentId, LinkedHashMap<String, Integer> extraColumns, List<String> row) {
        if (studentId == null || studentId.trim().isEmpty() || extraColumns == null || extraColumns.isEmpty()) {
            return;
        }
        Map<String, String> values = importExtraValuesByStudentId.computeIfAbsent(studentId, k -> new LinkedHashMap<>());
        for (Map.Entry<String, Integer> entry : extraColumns.entrySet()) {
            values.put(entry.getKey(), normalizeImportedText(getCell(row, entry.getValue())));
        }
    }

    private Integer boundOrAlias(Map<String, Integer> bindings, String bindingKey, Map<String, Integer> index, String... aliases) {
        if (bindings != null) {
            Integer bound = bindings.get(bindingKey);
            if (bound != null) {
                return bound;
            }
        }
        return findColumn(index, aliases);
    }

    private void rememberImportedTypeLabel(String studentId,
                                           String rawTypeText,
                                           String classRank,
                                           String gradeRank,
                                           String supervisor,
                                           String direction,
                                           Integer majorCol,
                                           List<String> row) {
        if (isBlank(studentId)) {
            return;
        }
        String typeLabel = normalizeImportedText(rawTypeText);
        if (isBlank(typeLabel)) {
            if (!isBlank(supervisor) || !isBlank(direction)) {
                typeLabel = "研究生";
            } else if (!isBlank(classRank) || !isBlank(gradeRank) || isBlank(getCellByIndex(row, majorCol))) {
                typeLabel = "高中生";
            }
        }
        if (isBlank(typeLabel)) {
            return;
        }
        Map<String, String> values = importExtraValuesByStudentId.computeIfAbsent(studentId, k -> new LinkedHashMap<>());
        values.put("__导入类型", typeLabel);
    }

    private String resolveDisplayType(Student student) {
        if (student == null) {
            return "";
        }
        Map<String, String> extras = importExtraValuesByStudentId.get(student.getStudentId());
        if (extras != null) {
            String importedType = normalizeImportedText(extras.get("__导入类型"));
            if (!importedType.isEmpty()) {
                return importedType;
            }
        }
        return student.getStudentType().toString();
    }

    private boolean isMetaHeader(String header) {
        String normalized = normalizeHeader(header);
        Set<String> fixed = new HashSet<>(Arrays.asList(
                "学号", "考号", "准考证号", "id", "studentid",
                "姓名", "学生姓名", "name",
                "类型", "学生类型", "类别", "身份",
                "年龄", "年级", "age",
                "班级", "行政班", "班别", "class",
                "省份", "省", "城市", "市", "街道", "详细地址", "地址", "门牌号", "门牌",
                "总分", "排名", "年级排名", "校排名", "班级排名", "班排名", "班内排名",
                "专业", "学科", "导师", "指导教师", "班主任", "研究方向", "方向"
        ));
        return fixed.contains(normalized);
    }

    private boolean isCoreMetaHeader(String normalized) {
        Set<String> core = new HashSet<>(Arrays.asList(
                "学号", "考号", "准考证号", "id", "studentid",
                "姓名", "学生姓名", "name", "名字",
                "类型", "学生类型", "类别", "身份",
                "年龄", "年级", "age",
                "班级", "行政班", "班别", "class", "专业", "学科", "学院",
                "省份", "省", "城市", "市", "街道", "详细地址", "地址", "门牌号", "门牌",
                "导师", "指导教师", "班主任", "研究方向", "方向"
        ));
        return core.contains(normalized);
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.trim().toLowerCase(Locale.ROOT).replace(" ", "");
    }

    private boolean hasAnyColumn(Map<String, Integer> index, String... aliases) {
        return findColumn(index, aliases) != null;
    }

    private Integer findColumn(Map<String, Integer> index, String... aliases) {
        for (String alias : aliases) {
            if (alias == null) {
                continue;
            }
            Integer pos = index.get(alias);
            if (pos != null) {
                return pos;
            }
            pos = index.get(normalizeHeader(alias));
            if (pos != null) {
                return pos;
            }
        }
        return null;
    }

    private String cellByAliases(List<String> row, Map<String, Integer> index, String... aliases) {
        Integer pos = findColumn(index, aliases);
        if (pos == null) {
            return "";
        }
        return getCell(row, pos).trim();
    }

    private int parseIntegerOrDefault(String text, int defaultValue) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String fallbackIfBlank(String text, String fallback) {
        return isBlank(text) ? fallback : text.trim();
    }

    private StudentType resolveType(String typeText, String supervisor, String direction) {
        String type = typeText == null ? "" : typeText.trim();
        if (type.contains("研") || !isBlank(supervisor) || !isBlank(direction)) {
            return StudentType.POSTGRADUATE;
        }
        return StudentType.UNDERGRADUATE;
    }

    private ScoreParseResult parseFlexibleScore(String scoreText, String subject) {
        String raw = normalizeImportedText(scoreText);
        if (raw.isEmpty()) {
            return ScoreParseResult.warning(0.0, "空值已按 0 处理");
        }

        String normalized = normalizeScoreText(raw);
        if (normalized.isEmpty()) {
            return ScoreParseResult.warning(0.0, "空值已按 0 处理");
        }
        if (isMissingScoreText(normalized)) {
            return ScoreParseResult.warning(0.0, "文本成绩“" + raw + "”已按 0 处理");
        }

        Double levelScore = mapGradeLevelScore(normalized);
        if (levelScore != null) {
            return ScoreParseResult.warning(levelScore, "等级“" + raw + "”已换算为 " + formatScore(levelScore));
        }

        if (normalized.matches("-?\\d+(\\.\\d+)?%")) {
            String pure = normalized.substring(0, normalized.length() - 1);
            try {
                return ScoreParseResult.ok(Double.parseDouble(pure));
            } catch (NumberFormatException ignore) {
                // fall through to generic numeric extraction
            }
        }

        try {
            return ScoreParseResult.ok(Double.parseDouble(normalized));
        } catch (NumberFormatException ignore) {
            // Continue with extraction.
        }

        if (normalized.contains("排名") || normalized.contains("名次")
                || (normalized.startsWith("第") && normalized.contains("名"))) {
            return ScoreParseResult.warning(0.0, "检测到排名文本“" + raw + "”，已按 0 处理");
        }

        String numeric = extractFirstNumber(normalized);
        if (!numeric.isEmpty()) {
            try {
                return ScoreParseResult.warning(Double.parseDouble(numeric), "文本成绩“" + raw + "”已提取数字 " + numeric);
            } catch (NumberFormatException ignore) {
                // fall through
            }
        }

        return ScoreParseResult.warning(0.0, "无法解析“" + raw + "”，已按 0 处理");
    }

    private String normalizeScoreText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = normalizeImportedText(text)
                .replace('，', ',')
                .replace(",", "")
                .replace('。', '.')
                .replace('％', '%')
                .replace("分", "");
        StringBuilder builder = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c >= '０' && c <= '９') {
                builder.append((char) ('0' + (c - '０')));
            } else if (c == '－') {
                builder.append('-');
            } else {
                builder.append(c);
            }
        }
        return builder.toString().trim();
    }

    private boolean isMissingScoreText(String text) {
        String value = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        return value.isEmpty()
                || "-".equals(value)
                || "--".equals(value)
                || "null".equals(value)
                || value.contains("缺考")
                || value.contains("弃考")
                || value.contains("缺测")
                || value.contains("免考")
                || value.contains("请假")
                || value.contains("未考")
                || value.contains("缺")
                || value.contains("absent");
    }

    private Double mapGradeLevelScore(String normalized) {
        String value = normalized == null ? "" : normalized.trim().toUpperCase(Locale.ROOT);
        if (value.isEmpty()) {
            return null;
        }
        if ("A+".equals(value) || "A".equals(value) || "优秀".equals(value) || "优".equals(value)) {
            return 95.0;
        }
        if ("B+".equals(value) || "B".equals(value) || "良好".equals(value) || "良".equals(value)) {
            return 85.0;
        }
        if ("C+".equals(value) || "C".equals(value) || "中等".equals(value) || "中".equals(value)) {
            return 75.0;
        }
        if ("D+".equals(value) || "D".equals(value) || "及格".equals(value)) {
            return 65.0;
        }
        if ("E".equals(value) || "F".equals(value) || "不及格".equals(value) || "差".equals(value)) {
            return 50.0;
        }
        return null;
    }

    private String extractFirstNumber(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }

    private String formatScore(double score) {
        return String.format(Locale.ROOT, "%.2f", score);
    }

    private String cell(List<String> row, Map<String, Integer> index, String key) {
        Integer pos = index.get(key);
        if (pos == null) {
            return "";
        }
        return getCell(row, pos).trim();
    }

    private String getCell(List<String> row, int idx) {
        if (row == null || idx < 0 || idx >= row.size()) {
            return "";
        }
        String value = row.get(idx);
        return value == null ? "" : value;
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private void saveAndExit() {
        if (!hasUnsavedChanges) {
            Platform.exit();
            return;
        }
        if (saveStudentsToFile(false)) {
            Platform.exit();
        }
    }

    private void setUnsavedChanges(boolean unsaved) {
        hasUnsavedChanges = unsaved;
        quickSaveExitButton.setVisible(unsaved);
        quickSaveExitButton.setManaged(unsaved);
    }

    private boolean saveStudentsToFile(boolean forceChoosePath) {
        List<Student> students = controller.browseAllStudents();
        if (students.isEmpty()) {
            showError("保存失败", "当前没有学生数据可保存。");
            return false;
        }

        List<String> exportSubjects = subjectNames.isEmpty()
                ? new ArrayList<>(students.get(0).getSubjectScores().keySet())
                : new ArrayList<>(subjectNames);
        if (exportSubjects.isEmpty()) {
            showError("保存失败", "当前没有可导出的科目。");
            return false;
        }

        File target = null;
        if (!forceChoosePath) {
            String lastSavedPath = preferences.get(PREF_LAST_SAVED_FILE, "");
            if (lastSavedPath != null && !lastSavedPath.trim().isEmpty()) {
                target = new File(lastSavedPath.trim());
            }
        }

        if (target == null) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("保存学生信息到文件");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel 文件 (*.xlsx)", "*.xlsx"));
            String fileName = "students-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".xlsx";
            chooser.setInitialFileName(fileName);
            target = chooser.showSaveDialog(table.getScene() == null ? null : table.getScene().getWindow());
            if (target == null) {
                setActionMessage("已取消保存操作。", true);
                return false;
            }
        }

        if (!target.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            target = new File(target.getParentFile(), target.getName() + ".xlsx");
        }

        List<String> headers = new ArrayList<>();
        headers.add("学号");
        headers.add("姓名");
        headers.add("类型");
        headers.add("年龄");
        headers.add("班级");
        headers.add("省份");
        headers.add("城市");
        headers.add("街道");
        headers.add("门牌号");
        headers.addAll(exportSubjects);
        headers.add("总分");
        headers.add("专业");
        headers.add("导师");
        headers.add("研究方向");

        List<List<Object>> rows = new ArrayList<>();
        for (Student student : students) {
            Address addr = student.getAddress();
            List<Object> row = new ArrayList<>();
            row.add(student.getStudentId());
            row.add(student.getName());
            row.add(student.getStudentType().toString());
            row.add(student.getAge());
            row.add(student.getClassName());
            row.add(addr.getProvince());
            row.add(addr.getCity());
            row.add(addr.getStreet());
            row.add(addr.getHouseNumber());

            for (String subject : exportSubjects) {
                row.add(student.getSubjectScore(subject));
            }

            row.add(student.getTotalScore());
            if (student instanceof Undergraduate) {
                row.add(((Undergraduate) student).getMajor());
                row.add("");
                row.add("");
            } else if (student instanceof Postgraduate) {
                Postgraduate pg = (Postgraduate) student;
                row.add("");
                row.add(pg.getSupervisor());
                row.add(pg.getResearchDirection());
            } else {
                row.add("");
                row.add("");
                row.add("");
            }
            rows.add(row);
        }

        try {
            SimpleXlsxWriter.write(target, "学生信息", headers, rows);
        } catch (IOException ex) {
            showError("保存失败", "写入文件失败：" + ex.getMessage());
            return false;
        }

        preferences.put(PREF_LAST_SAVED_FILE, target.getAbsolutePath());
        setUnsavedChanges(false);

        setActionMessage("保存成功：" + target.getAbsolutePath(), true);
        showInfo("保存成功", "学生信息已保存到：\n" + target.getAbsolutePath());
        return true;
    }

    private void updateRanking(List<Student> students) {
        rankByStudentId.clear();
        if (students == null || students.isEmpty()) {
            return;
        }
        List<Student> rankingSource = new ArrayList<>(students);
        rankingSource.sort(Comparator.comparingDouble(Student::getTotalScore).reversed());

        int rank = 0;
        double previousScore = Double.NaN;
        for (int i = 0; i < rankingSource.size(); i++) {
            Student student = rankingSource.get(i);
            double score = student.getTotalScore();
            if (i == 0 || Double.compare(score, previousScore) != 0) {
                rank = i + 1;
                previousScore = score;
            }
            rankByStudentId.put(student.getStudentId(), rank);
        }
    }


    private void setActionMessage(String message, boolean success) {
        actionLabel.setText("提示：" + message);
        actionLabel.getStyleClass().remove("status-error");
        actionLabel.getStyleClass().remove("status-success");
        actionLabel.getStyleClass().add(success ? "status-success" : "status-error");
    }

    private MenuController.StudentFormData toControllerData(FormData data) {
        return new MenuController.StudentFormData(
                data.type,
                data.id,
                data.name,
                data.age,
                data.className,
                data.address,
                data.scores,
                data.major,
                data.supervisor,
                data.direction
        );
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        setActionMessage(message, false);
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setWindowIcon(Stage stage) {
        String iconPath = resolveImagePath("deepin.png");
        if (iconPath != null) {
            stage.getIcons().add(new Image(iconPath));
        }
    }

    private String resolveStyleSheet() {
        URL resource = getClass().getResource("/style.css");
        if (resource != null) {
            return resource.toExternalForm();
        }

        File resourcesStyle = new File("resources/style.css");
        if (resourcesStyle.exists()) {
            return resourcesStyle.toURI().toString();
        }

        File srcStyle = new File("src/style.css");
        if (srcStyle.exists()) {
            return srcStyle.toURI().toString();
        }

        File rootStyle = new File("style.css");
        if (rootStyle.exists()) {
            return rootStyle.toURI().toString();
        }

        return null;
    }

    private static class FormData {
        private StudentType type;
        private String id;
        private String name;
        private int age;
        private String className;
        private Address address;
        private Map<String, Double> scores;
        private List<String> subjectsToInitialize;
        private String major;
        private String supervisor;
        private String direction;
    }

    private static class ScoreInput {
        private final Map<String, Double> scores;
        private final List<String> initialSubjects;

        private ScoreInput(Map<String, Double> scores, List<String> initialSubjects) {
            this.scores = scores;
            this.initialSubjects = initialSubjects;
        }
    }

    private static class ScoreParseResult {
        private final double value;
        private final boolean warning;
        private final String message;

        private ScoreParseResult(double value, boolean warning, String message) {
            this.value = value;
            this.warning = warning;
            this.message = message;
        }

        private static ScoreParseResult ok(double value) {
            return new ScoreParseResult(value, false, "");
        }

        private static ScoreParseResult warning(double value, String message) {
            return new ScoreParseResult(value, true, message);
        }
    }
}


