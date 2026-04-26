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
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ButtonType;
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

    private final TextField majorField = new TextField();
    private final TextField supervisorField = new TextField();
    private final TextField directionField = new TextField();
    private final VBox scoreRowsBox = new VBox(8);
    private final Label scoreSectionTip = new Label();
    private final Button addSubjectRowButton = new Button("新增科目");
    private final List<TextField> subjectNameFields = new ArrayList<>();
    private final List<TextField> scoreValueFields = new ArrayList<>();
    private final List<String> subjectNames = new ArrayList<>();

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

    public static void launchApp(StudentService service, String[] args) {
        bootstrapService = service;
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        if (bootstrapService == null) {
            // Allow launching MainApp directly in IDE without going through Main.main(...).
            bootstrapService = new StudentService(new StudentRepository(500));
        }
        service = bootstrapService;
        controller = new MenuController(service);

        BorderPane root = new BorderPane();
        root.setCenter(buildScrollableCenter());
        root.setBottom(buildStatusBar());
        root.getStyleClass().add("app-root");

        refreshTable(controller.browseAllStudents());
        updateStatus();

        Scene scene = new Scene(root, 1380, 760);
        stage.setTitle("学生信息管理系统");
        stage.setScene(scene);
        setWindowIcon(stage);
        String stylesheet = resolveStyleSheet();
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet);
        }
        stage.setOnShown(e -> {
            idField.requestFocus();
            Platform.runLater(this::tryAutoLoadLastSavedFile);
        });
        stage.show();
    }

    private VBox buildHeader() {
        quickSaveExitButton.getStyleClass().addAll("button-primary", "quick-save-button");
        quickSaveExitButton.setVisible(false);
        quickSaveExitButton.setManaged(false);
        quickSaveExitButton.setOnAction(e -> saveAndExit());

        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        ImageView headerLogo = buildHeaderLogo();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);
        HBox actionsRow = new HBox(8, quickSaveExitButton, leftSpacer, rightSpacer, headerLogo);
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
            MenuItem deleteItem = new MenuItem("删除该学生");
            deleteItem.setOnAction(e -> {
                Student selected = row.getItem();
                if (selected != null) {
                    deleteStudentById(selected.getStudentId());
                }
            });
            ContextMenu menu = new ContextMenu(deleteItem);
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
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStudentType().toString()));

        TableColumn<Student, String> classCol = new TableColumn<>("班级");
        classCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getClassName()));

        table.getColumns().addAll(idCol, nameCol, typeCol, classCol);

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
        table.getColumns().addAll(totalCol, rankCol, addrCol);
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
        Button quickAddButton = new Button("新增学生（主按钮）");
        quickAddButton.setMaxWidth(Double.MAX_VALUE);
        quickAddButton.getStyleClass().add("button-primary");
        quickAddButton.setOnAction(e -> addStudent());

        Button clearButton = new Button("清空表单");
        clearButton.setMaxWidth(Double.MAX_VALUE);
        clearButton.getStyleClass().add("button-secondary");
        clearButton.setOnAction(e -> {
            if (confirmClearAllStudents()) {
                clearFormAndAllStudents();
            } else {
                setActionMessage("已取消清空操作。", true);
            }
        });

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
        browseAllButton.getStyleClass().add("button-secondary");
        browseUnderButton.getStyleClass().add("button-secondary");
        browsePostButton.getStyleClass().add("button-secondary");
        statsButton.getStyleClass().add("button-secondary");
        browseAllButton.setOnAction(e -> {
            refreshTable(controller.browseAllStudents());
            setActionMessage("当前显示：全部学生。", true);
        });
        browseUnderButton.setOnAction(e -> {
            refreshTable(controller.browseByType(StudentType.UNDERGRADUATE));
            setActionMessage("当前显示：仅本科生。", true);
        });
        browsePostButton.setOnAction(e -> {
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

        Button importButton = new Button("导入文件");
        importButton.getStyleClass().add("button-secondary");
        importButton.setOnAction(e -> importStudentsFromFile());

        VBox scoreSection = buildScoreSection();
        VBox importContent = new VBox(8, scoreSection, form, addHint, new HBox(8, quickAddButton, clearButton));
        VBox queryContent = new VBox(8, queryRow, queryButtons);
        VBox utilityContent = new VBox(8, browseButtons, sortRow1, sortRow2, new HBox(8, sortButton, avgButton, importButton, saveButton, aboutButton));

        TitledPane importPane = new TitledPane("学生信息导入", importContent);
        TitledPane queryPane = new TitledPane("查询功能", queryContent);
        TitledPane utilityPane = new TitledPane("浏览 / 排序 / 关于", utilityContent);
        importPane.setCollapsible(true);
        queryPane.setCollapsible(true);
        utilityPane.setCollapsible(true);
        importPane.setExpanded(true);
        queryPane.setExpanded(false);
        utilityPane.setExpanded(false);

        Accordion accordion = new Accordion(importPane, queryPane, utilityPane);

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
        return panel;
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

        VBox box = new VBox(8, title, scoreSectionTip, scoreRowsBox, addSubjectRowButton);
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

                row.getChildren().addAll(subjectIndex, subjectNameFields.get(i), scoreLabel, scoreValueFields.get(i));
                HBox.setHgrow(subjectNameFields.get(i), Priority.ALWAYS);
                HBox.setHgrow(scoreValueFields.get(i), Priority.ALWAYS);
                scoreRowsBox.getChildren().add(row);
            }
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
                row.getChildren().addAll(subjectLabel, scoreField);
                HBox.setHgrow(scoreField, Priority.ALWAYS);
                scoreRowsBox.getChildren().add(row);
            }
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

    private void clearScoreValues() {
        if (subjectNames.isEmpty()) {
            for (TextField field : subjectNameFields) {
                field.clear();
            }
        }
        for (TextField field : scoreValueFields) {
            field.clear();
        }
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
        for (int i = 2501; i <= 2510; i++) {
            classes.add(String.valueOf(i));
        }
        classField.setItems(FXCollections.observableArrayList(classes));

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
        refreshTable(result.getStudents());
        setActionMessage("查询完成，共 " + result.getStudents().size() + " 条结果。", true);
        if (result.getMessage() != null && !result.getMessage().isEmpty()) {
            showInfo("查询结果", result.getMessage());
        }
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

        StringBuilder builder = new StringBuilder();
        builder.append("统计范围：").append(rangeBox.getValue()).append("\n\n");
        for (String subject : subjects) {
            double sum = 0.0;
            for (Student student : source) {
                sum += student.getSubjectScore(subject);
            }
            double avg = sum / source.size();
            builder.append(subject)
                    .append(" 平均分：")
                    .append(String.format(Locale.ROOT, "%.2f", avg))
                    .append("\n");
        }

        double totalAvg = 0.0;
        for (Student student : source) {
            totalAvg += student.getTotalScore();
        }
        totalAvg = totalAvg / source.size();
        builder.append("\n总分平均分：")
                .append(String.format(Locale.ROOT, "%.2f", totalAvg));

        showInfo("平均分统计", builder.toString());
        setActionMessage("已完成平均分统计。", true);
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
            data.major = mustNotBlank(majorField.getText(), "本科生专业不能为空。");
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
            majorField.setText(((Undergraduate) student).getMajor());
            supervisorField.clear();
            directionField.clear();
            profileLabel.setText("专业信息：本科生 - " + ((Undergraduate) student).getMajor());
        } else if (student instanceof Postgraduate) {
            supervisorField.setText(((Postgraduate) student).getSupervisor());
            directionField.setText(((Postgraduate) student).getResearchDirection());
            majorField.clear();
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

        majorField.clear();
        supervisorField.clear();
        directionField.clear();

        clearScoreValues();
        table.getSelectionModel().clearSelection();
        idField.requestFocus();
        profileLabel.setText("专业信息：未选择学生");
        setActionMessage("表单已清空，请从学号开始录入。", true);
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

        List<String> headers = rows.get(0);
        Map<String, Integer> index = buildHeaderIndex(headers);
        if (!hasAnyColumn(index, "学号", "考号", "准考证号", "id", "studentid")) {
            showError("导入失败", "文件缺少必要列：学号/考号/准考证号");
            return;
        }
        if (!hasAnyColumn(index, "姓名", "学生姓名", "name")) {
            showError("导入失败", "文件缺少必要列：姓名");
            return;
        }
        if (!hasAnyColumn(index, "班级", "行政班", "班别", "class")) {
            showError("导入失败", "文件缺少必要列：班级");
            return;
        }

        List<String> importedSubjects = detectSubjectHeaders(headers);
        if (importedSubjects.isEmpty()) {
            showError("导入失败", "文件中没有科目列。");
            return;
        }
        List<String> extraHeaders = detectExtraHeaders(headers, importedSubjects);
        dynamicExtraColumns.addAll(extraHeaders);

        int added = 0;
        int updated = 0;
        int failed = 0;
        int rankingDetected = 0;

        for (int r = 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            String id = cellByAliases(row, index, "学号", "考号", "准考证号", "id", "studentid");
            if (isBlank(id)) {
                continue;
            }
            try {
                String name = cellByAliases(row, index, "姓名", "学生姓名", "name");
                String typeText = cellByAliases(row, index, "类型", "学生类型", "类别", "身份");
                String supervisor = cellByAliases(row, index, "导师", "指导教师", "班主任");
                String direction = cellByAliases(row, index, "研究方向", "方向");
                StudentType type = resolveType(typeText, supervisor, direction);
                int age = parseIntegerOrDefault(cellByAliases(row, index, "年龄", "年级", "age"), 16);
                String className = cellByAliases(row, index, "班级", "行政班", "班别", "class");
                String classRank = cellByAliases(row, index, "班级排名", "班排名", "班内排名");
                String gradeRank = cellByAliases(row, index, "年级排名", "校排名");
                if (!isBlank(classRank) || !isBlank(gradeRank)) {
                    rankingDetected++;
                }
                Address address = new Address(
                        fallbackIfBlank(cellByAliases(row, index, "省份", "省"), "未知省份"),
                        fallbackIfBlank(cellByAliases(row, index, "城市", "市"), "未知城市"),
                        fallbackIfBlank(cellByAliases(row, index, "街道", "详细地址", "地址"), "未知街道"),
                        fallbackIfBlank(cellByAliases(row, index, "门牌号", "门牌"), "0")
                );

                LinkedHashMap<String, Double> scores = new LinkedHashMap<>();
                for (String subject : importedSubjects) {
                    String scoreText = cell(row, index, subject);
                    double score = parseFlexibleScore(scoreText, subject);
                    scores.put(subject, score);
                }

                rememberExtraValues(id, extraHeaders, row, index);

                String major = cellByAliases(row, index, "专业", "学科", "方向");
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
            }
        }

        refreshTable(controller.browseAllStudents());
        updateStatus();
        if (markUnsavedAfterImport && (added > 0 || updated > 0)) {
            setUnsavedChanges(true);
        }
        String rankNote = rankingDetected > 0 ? "，识别班级/年级排名 " + rankingDetected + " 行" : "";
        setActionMessage("导入完成：新增 " + added + "，更新 " + updated + "，失败 " + failed + rankNote, failed == 0);
        if (silentOnSuccess) {
            if (failed == 0) {
                setActionMessage("已自动读取最近保存文件：" + source.getAbsolutePath(), true);
            } else {
                showInfo("自动读取完成", "最近保存文件读取完成。\n新增：" + added + "\n更新：" + updated + "\n失败：" + failed);
            }
        } else {
            showInfo("导入完成", "新增：" + added + "\n更新：" + updated + "\n失败：" + failed + "\n识别排名：" + rankingDetected);
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
            if ("班级排名".equals(normalized) || "班排名".equals(normalized) || "班内排名".equals(normalized)
                    || "年级排名".equals(normalized) || "校排名".equals(normalized)) {
                extras.add(h);
            }
        }
        return extras;
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

    private double parseFlexibleScore(String scoreText, String subject) {
        String value = scoreText == null ? "" : scoreText.trim();
        if (value.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignore) {
            String numeric = value.replaceAll("[^0-9.\\-]", "");
            if (numeric.isEmpty() || "-".equals(numeric) || ".".equals(numeric)) {
                throw new IllegalArgumentException("科目【" + subject + "】成绩不是数字：" + value);
            }
            try {
                return Double.parseDouble(numeric);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("科目【" + subject + "】成绩不是数字：" + value);
            }
        }
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
}


