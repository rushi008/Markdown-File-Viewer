package markdownviewer;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.print.Collation;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.PrintColor;
import javafx.print.PrintQuality;
import javafx.print.PrintSides;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Split print dialog with real page breaks. JavaFX WebEngine.print() scales the
 * whole document onto one page, so preview and PDF both paginate manually.
 */
final class PrintPreviewDialog {

    private final Stage owner;
    private final String html;

    private ComboBox<Printer> printerBox;
    private Spinner<Integer> copiesSpinner;
    private CheckBox collateBox;
    private ComboBox<Paper> paperBox;
    private ToggleButton portraitBtn;
    private ToggleButton landscapeBtn;
    private ToggleButton colorBtn;
    private ToggleButton monoBtn;
    private ComboBox<String> marginBox;
    private ComboBox<PrintSides> sidesBox;
    private ComboBox<Integer> pagesPerSheetBox;
    private ComboBox<PrintQuality> qualityBox;
    private ComboBox<Integer> fontSizeBox;
    private RadioButton allPagesRadio;
    private RadioButton currentPageRadio;
    private RadioButton rangeRadio;
    private TextField rangeField;

    private StackPane previewCanvas;
    private StackPane paperPage;
    private WebView previewWeb;
    private GridPane nUpGrid;
    private Label pageLabel;
    private Button prevPageBtn;
    private Button nextPageBtn;
    private Button printBtn;

    private int currentPage = 0;
    private int currentSheet = 0;
    private int pageCount = 1;
    private double pageViewW = 400;
    private double pageViewH = 520;
    private double contentHeightPx = 520;
    private boolean previewLoaded;
    private boolean capturing;
    private boolean relayouting;
    private final List<Image> pageImages = new ArrayList<>();
    private static final int DEFAULT_FONT_SIZE = 11;

    private PrintPreviewDialog(Stage owner, String html) {
        this.owner = owner;
        this.html = html;
    }

    static void open(Stage owner, WebEngine sourceEngine, String html) {
        new PrintPreviewDialog(owner, html).show();
    }

    private void show() {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Print");
        stage.setResizable(true);

        BorderPane root = new BorderPane();
        VBox optionsColumn = wrapOptionsInScroll();
        root.setLeft(optionsColumn);
        root.setCenter(buildPreviewPane());
        root.setBottom(buildButtonBar(stage));
        BorderPane.setAlignment(optionsColumn, Pos.TOP_LEFT);

        Rectangle2D vis = visualBounds();
        double width = Math.min(1080, vis.getWidth() * 0.9);
        double height = Math.min(720, vis.getHeight() * 0.88);
        stage.setMinWidth(Math.min(760, vis.getWidth() * 0.85));
        stage.setMinHeight(Math.min(480, vis.getHeight() * 0.75));

        Scene scene = new Scene(root, width, height);
        stage.setScene(scene);
        stage.setX(vis.getMinX() + (vis.getWidth() - width) / 2);
        stage.setY(vis.getMinY() + (vis.getHeight() - height) / 2);
        stage.show();

        refreshPapers();
        loadPreview();
        Platform.runLater(this::relayoutPaper);
    }

    private VBox wrapOptionsInScroll() {
        VBox options = buildOptionsPane();
        options.setMinHeight(Region.USE_PREF_SIZE);
        options.setMaxWidth(Double.MAX_VALUE);

        ScrollPane scroll = new ScrollPane(options);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setPannable(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scroll.setStyle("-fx-background-color: #f6f8fa; -fx-background: #f6f8fa;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox column = new VBox(scroll);
        column.setPrefWidth(320);
        column.setMinWidth(300);
        column.setMaxWidth(360);
        column.setFillWidth(true);
        column.setStyle("-fx-background-color: #f6f8fa; -fx-border-color: #d0d7de; -fx-border-width: 0 1 0 0;");
        return column;
    }

    private VBox buildOptionsPane() {
        VBox options = new VBox(12);
        options.setPadding(new Insets(16, 16, 24, 16));
        options.setStyle("-fx-background-color: #f6f8fa;");

        Label heading = new Label("Print options");
        heading.setFont(Font.font("Segoe UI", 18));
        heading.setStyle("-fx-font-weight: bold; -fx-text-fill: #24292f;");

        printerBox = new ComboBox<>(FXCollections.observableArrayList(Printer.getAllPrinters()));
        printerBox.setMaxWidth(Double.MAX_VALUE);
        printerBox.setCellFactory(cb -> printerCell());
        printerBox.setButtonCell(printerCell());
        Printer defaultPrinter = Printer.getDefaultPrinter();
        if (defaultPrinter != null) {
            printerBox.getSelectionModel().select(defaultPrinter);
        } else if (!printerBox.getItems().isEmpty()) {
            printerBox.getSelectionModel().selectFirst();
        }
        printerBox.valueProperty().addListener((obs, o, n) -> {
            refreshPapers();
            refreshSides();
            relayoutPaper();
        });

        fontSizeBox = new ComboBox<>(FXCollections.observableArrayList(9, 10, 11, 12, 14, 16, 18));
        fontSizeBox.setMaxWidth(Double.MAX_VALUE);
        fontSizeBox.setCellFactory(cb -> fontSizeCell());
        fontSizeBox.setButtonCell(fontSizeCell());
        fontSizeBox.getSelectionModel().select(Integer.valueOf(DEFAULT_FONT_SIZE));
        fontSizeBox.valueProperty().addListener((obs, o, n) -> {
            if (n != null && previewWeb != null) {
                previewWeb.getEngine().loadContent(previewHtml(), "text/html");
            }
        });

        copiesSpinner = new Spinner<>();
        copiesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1));
        copiesSpinner.setEditable(true);
        copiesSpinner.setMaxWidth(Double.MAX_VALUE);
        collateBox = new CheckBox("Collate");
        collateBox.setSelected(true);
        VBox copiesBox = new VBox(8, copiesSpinner, collateBox);

        ToggleGroup pagesGroup = new ToggleGroup();
        keepOneSelected(pagesGroup);
        allPagesRadio = new RadioButton("All pages");
        currentPageRadio = new RadioButton("Current page (single)");
        rangeRadio = new RadioButton("Pages");
        allPagesRadio.setToggleGroup(pagesGroup);
        currentPageRadio.setToggleGroup(pagesGroup);
        rangeRadio.setToggleGroup(pagesGroup);
        allPagesRadio.setSelected(true);
        rangeField = new TextField();
        rangeField.setPromptText("e.g. 1-3, 5");
        rangeField.setDisable(true);
        rangeRadio.selectedProperty().addListener((obs, o, n) -> {
            rangeField.setDisable(!n);
            updatePageLabel();
        });
        allPagesRadio.setOnAction(e -> updatePageLabel());
        currentPageRadio.setOnAction(e -> updatePageLabel());
        VBox pagesBox = new VBox(6, allPagesRadio, currentPageRadio, rangeRadio, rangeField);

        pagesPerSheetBox = new ComboBox<>(FXCollections.observableArrayList(1, 2, 4, 6, 9));
        pagesPerSheetBox.setMaxWidth(Double.MAX_VALUE);
        pagesPerSheetBox.setCellFactory(cb -> pagesPerSheetCell());
        pagesPerSheetBox.setButtonCell(pagesPerSheetCell());
        pagesPerSheetBox.getSelectionModel().select(Integer.valueOf(1));
        pagesPerSheetBox.valueProperty().addListener((obs, o, n) -> {
            currentSheet = 0;
            if (n != null && n > 1) {
                capturePagesThenRefresh();
            } else {
                refreshSheetPreview();
            }
        });

        paperBox = new ComboBox<>();
        paperBox.setMaxWidth(Double.MAX_VALUE);
        paperBox.setCellFactory(cb -> paperCell());
        paperBox.setButtonCell(paperCell());
        paperBox.valueProperty().addListener((obs, o, n) -> relayoutPaper());

        ToggleGroup orientGroup = new ToggleGroup();
        keepOneSelected(orientGroup);
        portraitBtn = optionToggle("Portrait", orientGroup, true);
        landscapeBtn = optionToggle("Landscape", orientGroup, false);
        portraitBtn.setOnAction(e -> relayoutPaper());
        landscapeBtn.setOnAction(e -> relayoutPaper());
        HBox orientBox = new HBox(8, portraitBtn, landscapeBtn);
        growAll(portraitBtn, landscapeBtn);

        ToggleGroup colorGroup = new ToggleGroup();
        keepOneSelected(colorGroup);
        colorBtn = optionToggle("Color", colorGroup, true);
        monoBtn = optionToggle("Black & white", colorGroup, false);
        colorBtn.setOnAction(e -> applyPreviewFilter());
        monoBtn.setOnAction(e -> applyPreviewFilter());
        HBox colorBox = new HBox(8, colorBtn, monoBtn);
        growAll(colorBtn, monoBtn);

        marginBox = new ComboBox<>(FXCollections.observableArrayList("Normal", "Narrow", "Minimum"));
        marginBox.getSelectionModel().select("Normal");
        marginBox.setMaxWidth(Double.MAX_VALUE);
        marginBox.valueProperty().addListener((obs, o, n) -> relayoutPaper());

        sidesBox = new ComboBox<>();
        sidesBox.setMaxWidth(Double.MAX_VALUE);
        sidesBox.setCellFactory(cb -> sidesCell());
        sidesBox.setButtonCell(sidesCell());
        refreshSides();

        qualityBox = new ComboBox<>(FXCollections.observableArrayList(
                PrintQuality.HIGH, PrintQuality.NORMAL, PrintQuality.DRAFT));
        qualityBox.setMaxWidth(Double.MAX_VALUE);
        qualityBox.setCellFactory(cb -> qualityCell());
        qualityBox.setButtonCell(qualityCell());
        qualityBox.getSelectionModel().select(PrintQuality.NORMAL);

        options.getChildren().addAll(
                heading,
                labeled("Printer", printerBox),
                labeled("Font size", fontSizeBox),
                labeled("Copies", copiesBox),
                labeled("Pages", pagesBox),
                labeled("Pages per sheet", pagesPerSheetBox),
                labeled("Paper", paperBox),
                labeled("Orientation", orientBox),
                labeled("Color", colorBox),
                labeled("Margins", marginBox),
                labeled("Sides", sidesBox),
                labeled("Quality", qualityBox)
        );
        return options;
    }

    private BorderPane buildPreviewPane() {
        BorderPane preview = new BorderPane();
        preview.setStyle("-fx-background-color: #6e7781;");

        paperPage = new StackPane();
        paperPage.setStyle("-fx-background-color: white; -fx-border-color: rgba(0,0,0,0.18); -fx-border-width: 1;");

        previewWeb = new WebView();
        previewWeb.setContextMenuEnabled(false);
        previewWeb.setMinSize(0, 0);
        previewWeb.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        nUpGrid = new GridPane();
        nUpGrid.setHgap(6);
        nUpGrid.setVgap(6);
        nUpGrid.setVisible(false);
        nUpGrid.setManaged(false);

        paperPage.getChildren().addAll(previewWeb, nUpGrid);

        previewCanvas = new StackPane(paperPage);
        previewCanvas.setStyle("-fx-background-color: #6e7781;");
        previewCanvas.setMinSize(0, 0);
        StackPane.setAlignment(paperPage, Pos.CENTER);
        previewCanvas.layoutBoundsProperty().addListener((obs, o, n) -> relayoutPaper());
        previewCanvas.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.getDeltaY() < 0) {
                goToSheet(currentSheet + 1);
            } else if (e.getDeltaY() > 0) {
                goToSheet(currentSheet - 1);
            }
            e.consume();
        });

        prevPageBtn = navButton("◀");
        nextPageBtn = navButton("▶");
        prevPageBtn.setOnAction(e -> goToSheet(currentSheet - 1));
        nextPageBtn.setOnAction(e -> goToSheet(currentSheet + 1));

        pageLabel = new Label("Page 1 of 1");
        pageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");

        HBox nav = new HBox(12, prevPageBtn, pageLabel, nextPageBtn);
        nav.setAlignment(Pos.CENTER);
        nav.setPadding(new Insets(8, 16, 12, 16));
        nav.setMinHeight(44);

        preview.setCenter(previewCanvas);
        preview.setBottom(nav);
        return preview;
    }

    private HBox buildButtonBar(Stage stage) {
        Button close = new Button("Close");
        close.setStyle(secondaryButtonStyle());
        close.setOnAction(e -> stage.close());
        close.setCancelButton(true);

        printBtn = new Button("Print");
        printBtn.setStyle(primaryButtonStyle());
        printBtn.setDefaultButton(true);
        printBtn.setOnAction(e -> startPrint(stage));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, close, spacer, printBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 16, 10, 16));
        bar.setMinHeight(56);
        bar.setPrefHeight(56);
        bar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d0d7de; -fx-border-width: 1 0 0 0;");
        return bar;
    }

    private int selectedFontSize() {
        Integer value = fontSizeBox == null ? null : fontSizeBox.getValue();
        return value == null ? DEFAULT_FONT_SIZE : value;
    }

    private String previewHtml() {
        int fs = selectedFontSize();
        String extra =
                "<style>"
                        + "html,body{overflow:auto !important; height:auto !important;}"
                        + "body{padding:10px 12px !important; max-width:none !important; margin:0 !important;"
                        + " font-size:" + fs + "px !important; line-height:1.45 !important;}"
                        + "h1{font-size:1.45em !important;} h2{font-size:1.22em !important;} h3{font-size:1.08em !important;}"
                        + "code,pre{font-size:0.92em !important;}"
                        + "::-webkit-scrollbar{width:0 !important; height:0 !important; display:none !important;}"
                        + "</style>";
        if (html.contains("</head>")) {
            return html.replace("</head>", extra + "</head>");
        }
        return extra + html;
    }

    private void loadPreview() {
        previewWeb.getEngine().loadContent(previewHtml(), "text/html");
        previewWeb.getEngine().getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                previewLoaded = true;
                applyPreviewFilter();
                currentPage = 0;
                currentSheet = 0;
                Platform.runLater(() -> Platform.runLater(this::remeasureAndShow));
            }
        });
    }

    private void applyPreviewFilter() {
        try {
            boolean mono = monoBtn != null && monoBtn.isSelected();
            previewWeb.getEngine().executeScript(
                    "document.documentElement.style.filter = '" + (mono ? "grayscale(1)" : "none") + "';"
            );
        } catch (Exception ignored) {
            // Preview engine may not be ready yet.
        }
        refreshSheetPreview();
    }

    private void relayoutPaper() {
        if (relayouting || previewCanvas == null || paperPage == null) {
            return;
        }
        if (previewCanvas.getWidth() <= 0 || previewCanvas.getHeight() <= 0) {
            return;
        }
        relayouting = true;
        try {
            layoutPaperNow();
        } finally {
            relayouting = false;
        }
    }

    private void layoutPaperNow() {
        double xPad = 24;
        double yPad = 16;
        double availW = Math.max(80, previewCanvas.getWidth() - xPad * 2);
        double availH = Math.max(80, previewCanvas.getHeight() - yPad * 2);

        double paperW = 595.28;
        double paperH = 841.89;
        double left = 0.06;
        double right = 0.06;
        double top = 0.06;
        double bottom = 0.06;

        PageLayout layout = createPageLayout();
        if (layout != null) {
            paperW = layout.getPaper().getWidth();
            paperH = layout.getPaper().getHeight();
            if (currentOrientation() == PageOrientation.LANDSCAPE) {
                double tmp = paperW;
                paperW = paperH;
                paperH = tmp;
            }
            left = layout.getLeftMargin() / paperW;
            right = layout.getRightMargin() / paperW;
            top = layout.getTopMargin() / paperH;
            bottom = layout.getBottomMargin() / paperH;
        } else if (currentOrientation() == PageOrientation.LANDSCAPE) {
            double tmp = paperW;
            paperW = paperH;
            paperH = tmp;
        }

        double aspect = paperW / paperH;
        double w;
        double h;
        if (availW / availH > aspect) {
            h = availH;
            w = h * aspect;
        } else {
            w = availW;
            h = w / aspect;
        }

        paperPage.setMinSize(w, h);
        paperPage.setPrefSize(w, h);
        paperPage.setMaxSize(w, h);

        pageViewW = Math.max(1, w - w * left - w * right);
        pageViewH = Math.max(1, h - h * top - h * bottom);
        paperPage.setPadding(new Insets(h * top, w * right, h * bottom, w * left));

        if (previewLoaded) {
            Platform.runLater(this::remeasureAndShow);
        }
    }

    private void remeasureAndShow() {
        if (!previewLoaded || previewWeb == null || pageViewH <= 1) {
            return;
        }
        pageCount = countDocumentPages(previewWeb);
        contentHeightPx = pageCount * pageViewH;
        if (currentPage >= pageCount) {
            currentPage = pageCount - 1;
        }
        applyPageOffset();
        updatePageLabel();
        refreshSheetPreview();
    }

    private void applyPageOffset() {
        scrollWebViewToPage(previewWeb, currentPage);
    }

    private static void scrollWebViewToPage(WebView view, int pageIndex) {
        try {
            view.getEngine().executeScript(
                    "window.scrollTo(0," + pageIndex
                            + "*(window.innerHeight||document.documentElement.clientHeight||1));"
            );
        } catch (Exception ignored) {
            // Engine may not be ready.
        }
    }

    private int countDocumentPages(WebView view) {
        double fallback = pageViewH > 1 ? pageViewH : 500;
        try {
            Object result = view.getEngine().executeScript(
                    "(function(chFallback){var h=Math.max(document.body.scrollHeight,document.documentElement.scrollHeight);"
                            + "var ch=window.innerHeight||document.documentElement.clientHeight||0;"
                            + "if(ch<2) ch=chFallback;"
                            + "return Math.max(1,Math.ceil(h/ch));})(" + fallback + ")"
            );
            if (result instanceof Number) {
                return Math.max(1, ((Number) result).intValue());
            }
        } catch (Exception ignored) {
            // Fall through.
        }
        return 1;
    }

    private int pagesPerSheet() {
        Integer value = pagesPerSheetBox == null ? 1 : pagesPerSheetBox.getValue();
        return value == null ? 1 : value;
    }

    private int sheetCount() {
        int nUp = pagesPerSheet();
        return Math.max(1, (int) Math.ceil(selectedPageCount() / (double) nUp));
    }

    private int selectedPageCount() {
        int[] range = peekPageRange();
        if (range == null) {
            return Math.max(1, pageCount);
        }
        return Math.max(1, range[1] - range[0] + 1);
    }

    private void goToSheet(int sheet) {
        int max = sheetCount() - 1;
        if (sheet < 0 || sheet > max) {
            return;
        }
        currentSheet = sheet;
        int nUp = pagesPerSheet();
        int[] range = peekPageRange();
        int start = range == null ? 0 : range[0] - 1;
        currentPage = Math.min(pageCount - 1, start + currentSheet * nUp);
        applyPageOffset();
        refreshSheetPreview();
        updatePageLabel();
    }

    private void updatePageLabel() {
        if (pageLabel == null) {
            return;
        }
        int nUp = pagesPerSheet();
        int sheets = sheetCount();
        if (nUp == 1) {
            pageLabel.setText("Page " + (currentPage + 1) + " of " + pageCount);
        } else {
            pageLabel.setText("Sheet " + (currentSheet + 1) + " of " + sheets
                    + "  ·  " + nUp + " pages/sheet");
        }
        if (prevPageBtn != null) {
            prevPageBtn.setDisable(currentSheet <= 0);
        }
        if (nextPageBtn != null) {
            nextPageBtn.setDisable(currentSheet >= sheets - 1);
        }
    }

    private void capturePagesThenRefresh() {
        if (capturing || !previewLoaded || pageCount < 1) {
            refreshSheetPreview();
            return;
        }
        capturing = true;
        pageImages.clear();
        capturePage(0);
    }

    private void capturePage(int index) {
        if (index >= pageCount) {
            capturing = false;
            currentPage = 0;
            currentSheet = 0;
            applyPageOffset();
            refreshSheetPreview();
            return;
        }
        currentPage = index;
        applyPageOffset();
        Platform.runLater(() -> {
            try {
                pageImages.add(previewWeb.snapshot(null, null));
            } catch (Exception ignored) {
                // Snapshot can fail if the node is not yet shown.
            }
            capturePage(index + 1);
        });
    }

    private void refreshSheetPreview() {
        if (paperPage == null || nUpGrid == null || previewWeb == null) {
            return;
        }
        int nUp = pagesPerSheet();
        boolean multi = nUp > 1 && !pageImages.isEmpty();
        previewWeb.setVisible(!multi);
        previewWeb.setManaged(!multi);
        nUpGrid.setVisible(multi);
        nUpGrid.setManaged(multi);

        if (!multi) {
            applyPageOffset();
            updatePageLabel();
            return;
        }

        nUpGrid.getChildren().clear();
        nUpGrid.getColumnConstraints().clear();
        nUpGrid.getRowConstraints().clear();
        int[] grid = gridFor(nUp);
        int cols = grid[0];
        int rows = grid[1];
        for (int c = 0; c < cols; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / cols);
            cc.setHgrow(Priority.ALWAYS);
            nUpGrid.getColumnConstraints().add(cc);
        }
        for (int r = 0; r < rows; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(100.0 / rows);
            rc.setVgrow(Priority.ALWAYS);
            nUpGrid.getRowConstraints().add(rc);
        }

        int[] range = peekPageRange();
        int startDoc = range == null ? 0 : range[0] - 1;
        int first = startDoc + currentSheet * nUp;
        for (int i = 0; i < nUp; i++) {
            int docIndex = first + i;
            StackPane cell = new StackPane();
            cell.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d0d7de;");
            if (docIndex >= 0 && docIndex < pageImages.size()) {
                ImageView view = new ImageView(pageImages.get(docIndex));
                view.setPreserveRatio(true);
                view.setSmooth(true);
                view.fitWidthProperty().bind(cell.widthProperty().subtract(4));
                view.fitHeightProperty().bind(cell.heightProperty().subtract(4));
                if (monoBtn != null && monoBtn.isSelected()) {
                    view.setEffect(new ColorAdjust(0, -1, 0, 0));
                }
                cell.getChildren().add(view);
            }
            nUpGrid.add(cell, i % cols, i / cols);
            GridPane.setHgrow(cell, Priority.ALWAYS);
            GridPane.setVgrow(cell, Priority.ALWAYS);
        }
        updatePageLabel();
    }

    private static int[] gridFor(int nUp) {
        return switch (nUp) {
            case 2 -> new int[] {1, 2};
            case 4 -> new int[] {2, 2};
            case 6 -> new int[] {2, 3};
            case 9 -> new int[] {3, 3};
            default -> new int[] {1, 1};
        };
    }

    private void startPrint(Stage dialog) {
        int[] range = resolvePageRange();
        if (range == null) {
            return;
        }
        Printer printer = printerBox.getValue();
        if (printer == null) {
            warn("No printer selected", "Choose a printer on the left, or install a printer (including Microsoft Print to PDF).");
            return;
        }
        PageLayout layout = createPageLayout();
        if (layout == null) {
            warn("Could not start print job", "No page layout is available for this printer.");
            return;
        }
        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null) {
            warn("Could not start print job", "The selected printer is not available.");
            return;
        }

        job.getJobSettings().setPageLayout(layout);
        job.getJobSettings().setCopies(copiesSpinner.getValue());
        job.getJobSettings().setCollation(collateBox.isSelected() ? Collation.COLLATED : Collation.UNCOLLATED);
        PrintQuality quality = qualityBox.getValue();
        if (quality != null) {
            Set<PrintQuality> supported = printer.getPrinterAttributes().getSupportedPrintQuality();
            if (supported == null || supported.contains(quality)) {
                job.getJobSettings().setPrintQuality(quality);
            }
        }
        if (monoBtn.isSelected()) {
            Set<PrintColor> colors = printer.getPrinterAttributes().getSupportedPrintColors();
            if (colors != null && colors.contains(PrintColor.MONOCHROME)) {
                job.getJobSettings().setPrintColor(PrintColor.MONOCHROME);
            }
        } else {
            Set<PrintColor> colors = printer.getPrinterAttributes().getSupportedPrintColors();
            if (colors != null && colors.contains(PrintColor.COLOR)) {
                job.getJobSettings().setPrintColor(PrintColor.COLOR);
            }
        }
        PrintSides sides = sidesBox.getValue();
        if (sides != null) {
            job.getJobSettings().setPrintSides(sides);
        }

        printBtn.setDisable(true);
        printPaginated(dialog, job, layout, range);
    }

    /**
     * Prints one physical page at a time. WebEngine.print() would squash the
     * whole markdown file onto a single PDF page.
     */
    private void printPaginated(Stage dialog, PrinterJob job, PageLayout layout, int[] range) {
        double pw = layout.getPrintableWidth();
        double ph = layout.getPrintableHeight();

        WebView printView = new WebView();
        printView.setContextMenuEnabled(false);
        printView.setPrefSize(pw, ph);
        printView.setMinSize(pw, ph);
        printView.setMaxSize(pw, ph);

        StackPane host = new StackPane(printView);
        host.setMinSize(pw, ph);
        host.setPrefSize(pw, ph);
        host.setMaxSize(pw, ph);
        host.setStyle("-fx-background-color: white;");

        Stage hidden = new Stage();
        hidden.initOwner(dialog);
        hidden.initStyle(StageStyle.UTILITY);
        hidden.setScene(new Scene(host, pw, ph));
        hidden.setX(-12000);
        hidden.setY(-12000);
        hidden.show();

        printView.getEngine().getLoadWorker().stateProperty().addListener((obs, o, st) -> {
            if (st != Worker.State.SUCCEEDED) {
                return;
            }
            Platform.runLater(() -> {
                int total = countDocumentPages(printView);
                int from = Math.max(1, range[0]);
                int to = Math.min(total, range[1]);
                int nUp = pagesPerSheet();
                if (nUp <= 1) {
                    printNextPage(dialog, hidden, job, layout, host, printView, from, to, true);
                } else {
                    capturePrintPages(dialog, hidden, job, layout, host, printView, from, to, nUp, new ArrayList<>());
                }
            });
        });
        printView.getEngine().loadContent(previewHtml(), "text/html");
    }

    private void printNextPage(Stage dialog, Stage hidden, PrinterJob job, PageLayout layout,
                               StackPane host, WebView printView,
                               int page, int last, boolean okSoFar) {
        if (!okSoFar || page > last) {
            boolean ended = job.endJob();
            hidden.close();
            printBtn.setDisable(false);
            if (okSoFar && ended) {
                dialog.close();
            } else {
                warn("Print failed", "The printer could not complete the job.");
            }
            return;
        }
        scrollWebViewToPage(printView, page - 1);
        Platform.runLater(() -> {
            boolean ok = job.printPage(layout, host);
            printNextPage(dialog, hidden, job, layout, host, printView, page + 1, last, ok);
        });
    }

    private void capturePrintPages(Stage dialog, Stage hidden, PrinterJob job, PageLayout layout,
                                   StackPane host, WebView printView,
                                   int page, int last, int nUp, List<Image> pages) {
        if (page > last) {
            boolean ok = printNUpFromImages(job, layout, pages, nUp);
            hidden.close();
            printBtn.setDisable(false);
            if (ok) {
                dialog.close();
            } else {
                warn("Print failed", "The printer could not complete the job.");
            }
            return;
        }
        scrollWebViewToPage(printView, page - 1);
        Platform.runLater(() -> {
            pages.add(host.snapshot(null, null));
            capturePrintPages(dialog, hidden, job, layout, host, printView, page + 1, last, nUp, pages);
        });
    }

    private boolean printNUpFromImages(PrinterJob job, PageLayout layout, List<Image> pages, int nUp) {
        int[] grid = gridFor(nUp);
        int cols = grid[0];
        int rows = grid[1];
        double pw = layout.getPrintableWidth();
        double ph = layout.getPrintableHeight();
        boolean success = true;
        for (int sheetStart = 0; sheetStart < pages.size(); sheetStart += nUp) {
            GridPane sheet = new GridPane();
            sheet.setPrefSize(pw, ph);
            sheet.setMinSize(pw, ph);
            sheet.setMaxSize(pw, ph);
            sheet.setHgap(4);
            sheet.setVgap(4);
            sheet.setStyle("-fx-background-color: white;");
            for (int c = 0; c < cols; c++) {
                ColumnConstraints cc = new ColumnConstraints();
                cc.setPercentWidth(100.0 / cols);
                sheet.getColumnConstraints().add(cc);
            }
            for (int r = 0; r < rows; r++) {
                RowConstraints rc = new RowConstraints();
                rc.setPercentHeight(100.0 / rows);
                sheet.getRowConstraints().add(rc);
            }
            for (int i = 0; i < nUp; i++) {
                StackPane cell = new StackPane();
                cell.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb;");
                int docIndex = sheetStart + i;
                if (docIndex < pages.size()) {
                    ImageView view = new ImageView(pages.get(docIndex));
                    view.setPreserveRatio(true);
                    view.setFitWidth(pw / cols - 6);
                    view.setFitHeight(ph / rows - 6);
                    if (monoBtn.isSelected()) {
                        view.setEffect(new ColorAdjust(0, -1, 0, 0));
                    }
                    cell.getChildren().add(view);
                }
                sheet.add(cell, i % cols, i / cols);
            }
            success = job.printPage(layout, sheet) && success;
        }
        return job.endJob() && success;
    }

    private int[] peekPageRange() {
        if (allPagesRadio == null || allPagesRadio.isSelected()) {
            return new int[] {1, Math.max(1, pageCount)};
        }
        if (currentPageRadio != null && currentPageRadio.isSelected()) {
            int page = Math.max(1, currentPage + 1);
            return new int[] {page, page};
        }
        return parseRange(rangeField.getText(), false);
    }

    private int[] resolvePageRange() {
        remeasureAndShow();
        if (allPagesRadio.isSelected()) {
            return new int[] {1, Math.max(1, pageCount)};
        }
        if (currentPageRadio.isSelected()) {
            int page = Math.max(1, currentPage + 1);
            return new int[] {page, page};
        }
        int[] parsed = parseRange(rangeField.getText(), true);
        if (parsed == null) {
            warn("Invalid page range", "Enter pages like 1-3 or 2. Use numbers between 1 and " + pageCount + ".");
        }
        return parsed;
    }

    private int[] parseRange(String text, boolean strict) {
        if (text == null || text.isBlank()) {
            return strict ? null : new int[] {1, Math.max(1, pageCount)};
        }
        String cleaned = text.trim().split(",")[0].trim();
        try {
            int from;
            int to;
            if (cleaned.contains("-")) {
                String[] parts = cleaned.split("-", 2);
                from = Integer.parseInt(parts[0].trim());
                to = Integer.parseInt(parts[1].trim());
            } else {
                from = Integer.parseInt(cleaned);
                to = from;
            }
            if (from < 1 || to < from || from > pageCount) {
                return strict ? null : new int[] {1, Math.max(1, pageCount)};
            }
            to = Math.min(to, pageCount);
            return new int[] {from, to};
        } catch (NumberFormatException ex) {
            return strict ? null : new int[] {1, Math.max(1, pageCount)};
        }
    }

    private void warn(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.initOwner(owner);
        alert.setTitle("Print");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private PageLayout createPageLayout() {
        Printer printer = printerBox.getValue();
        if (printer == null) {
            printer = Printer.getDefaultPrinter();
        }
        if (printer == null) {
            return null;
        }

        Paper paper = paperBox.getValue();
        if (paper == null) {
            paper = printer.getDefaultPageLayout().getPaper();
        }
        PageOrientation orientation = currentOrientation();
        String margin = marginBox.getValue();
        try {
            if ("Minimum".equals(margin)) {
                return printer.createPageLayout(paper, orientation, Printer.MarginType.HARDWARE_MINIMUM);
            }
            if ("Narrow".equals(margin)) {
                return printer.createPageLayout(paper, orientation, 18, 18, 18, 18);
            }
            return printer.createPageLayout(paper, orientation, 36, 36, 36, 36);
        } catch (Exception ex) {
            return printer.getDefaultPageLayout();
        }
    }

    private PageOrientation currentOrientation() {
        return (landscapeBtn != null && landscapeBtn.isSelected())
                ? PageOrientation.LANDSCAPE
                : PageOrientation.PORTRAIT;
    }

    private void refreshPapers() {
        if (paperBox == null) {
            return;
        }
        Printer printer = printerBox.getValue();
        paperBox.getItems().clear();
        if (printer == null) {
            return;
        }
        Set<Paper> papers = printer.getPrinterAttributes().getSupportedPapers();
        if (papers != null && !papers.isEmpty()) {
            paperBox.getItems().addAll(papers);
            Paper current = printer.getDefaultPageLayout().getPaper();
            if (papers.contains(current)) {
                paperBox.getSelectionModel().select(current);
            } else {
                paperBox.getSelectionModel().selectFirst();
            }
        }
    }

    private void refreshSides() {
        if (sidesBox == null) {
            return;
        }
        sidesBox.getItems().clear();
        Printer printer = printerBox == null ? null : printerBox.getValue();
        if (printer == null) {
            sidesBox.getItems().add(PrintSides.ONE_SIDED);
            sidesBox.getSelectionModel().selectFirst();
            return;
        }
        Set<PrintSides> supported = printer.getPrinterAttributes().getSupportedPrintSides();
        if (supported == null || supported.isEmpty()) {
            sidesBox.getItems().add(PrintSides.ONE_SIDED);
        } else {
            sidesBox.getItems().addAll(supported);
        }
        if (sidesBox.getItems().contains(PrintSides.ONE_SIDED)) {
            sidesBox.getSelectionModel().select(PrintSides.ONE_SIDED);
        } else {
            sidesBox.getSelectionModel().selectFirst();
        }
    }

    private Rectangle2D visualBounds() {
        if (owner != null) {
            List<Screen> screens = Screen.getScreensForRectangle(
                    owner.getX(), owner.getY(),
                    Math.max(1, owner.getWidth()), Math.max(1, owner.getHeight()));
            if (!screens.isEmpty()) {
                return screens.get(0).getVisualBounds();
            }
        }
        return Screen.getPrimary().getVisualBounds();
    }

    private static void growAll(ToggleButton... buttons) {
        for (ToggleButton btn : buttons) {
            HBox.setHgrow(btn, Priority.ALWAYS);
            btn.setMaxWidth(Double.MAX_VALUE);
        }
    }

    private static void keepOneSelected(ToggleGroup group) {
        group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                oldToggle.setSelected(true);
            }
        });
    }

    private static VBox labeled(String title, javafx.scene.Node control) {
        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #57606a; -fx-font-size: 12px; -fx-font-weight: bold;");
        return new VBox(6, label, control);
    }

    private static ToggleButton optionToggle(String text, ToggleGroup group, boolean selected) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setSelected(selected);
        btn.setStyle(toggleStyle(selected));
        btn.selectedProperty().addListener((obs, o, n) -> btn.setStyle(toggleStyle(n)));
        return btn;
    }

    private static String toggleStyle(boolean selected) {
        if (selected) {
            return "-fx-background-color: #ddf4ff; -fx-border-color: #54aeff; -fx-border-radius: 6; "
                    + "-fx-background-radius: 6; -fx-padding: 6 8; -fx-cursor: hand; -fx-font-weight: bold;";
        }
        return "-fx-background-color: white; -fx-border-color: #d0d7de; -fx-border-radius: 6; "
                + "-fx-background-radius: 6; -fx-padding: 6 8; -fx-cursor: hand;";
    }

    private static Button navButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.18); -fx-text-fill: white; "
                        + "-fx-background-radius: 16; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 4 12;"
        );
        return btn;
    }

    private static ListCell<Printer> printerCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Printer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        };
    }

    private static ListCell<Paper> paperCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Paper item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        };
    }

    private static ListCell<PrintSides> sidesCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(PrintSides item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item == PrintSides.DUPLEX) {
                    setText("Two-sided (long edge)");
                } else if (item == PrintSides.TUMBLE) {
                    setText("Two-sided (short edge)");
                } else {
                    setText("One-sided");
                }
            }
        };
    }

    private static ListCell<Integer> fontSizeCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item == DEFAULT_FONT_SIZE) {
                    setText(item + " pt — Normal");
                } else if (item < DEFAULT_FONT_SIZE) {
                    setText(item + " pt — Small");
                } else {
                    setText(item + " pt — Large");
                }
            }
        };
    }

    private static ListCell<Integer> pagesPerSheetCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item == 1) {
                    setText("1 — Single page");
                } else {
                    setText(item + " pages per sheet");
                }
            }
        };
    }

    private static ListCell<PrintQuality> qualityCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(PrintQuality item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item == PrintQuality.HIGH) {
                    setText("High");
                } else if (item == PrintQuality.DRAFT) {
                    setText("Draft");
                } else {
                    setText("Normal");
                }
            }
        };
    }

    private static String primaryButtonStyle() {
        return "-fx-background-color: #1f883d; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-padding: 8 22; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 14px;";
    }

    private static String secondaryButtonStyle() {
        return "-fx-background-color: #ffffff; -fx-text-fill: #24292f; -fx-border-color: #d0d7de; "
                + "-fx-padding: 8 18; -fx-background-radius: 6; -fx-border-radius: 6; -fx-cursor: hand;";
    }
}
