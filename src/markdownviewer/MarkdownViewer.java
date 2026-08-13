package markdownviewer;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.collections.transformation.FilteredList;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.Extension;
import org.commonmark.node.Node;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class MarkdownViewer extends Application {

    private Stage primaryStage;
    private Parser parser;
    private HtmlRenderer renderer;

    private ObservableList<File> openFiles;
    private ListView<File> fileListView;
    private BorderPane mainLayout;
    private StackPane centerContent;
    
    private Preferences prefs;
    private static final String PREF_OPEN_FILES = "open_files";
    private static final String PREF_LAST_SELECTED = "last_selected";

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Markdown Viewer");
        
        prefs = Preferences.userNodeForPackage(MarkdownViewer.class);

        // Set up CommonMark with Tables Extension
        List<Extension> extensions = Arrays.asList(TablesExtension.create());
        parser = Parser.builder().extensions(extensions).build();
        renderer = HtmlRenderer.builder().extensions(extensions).build();

        openFiles = FXCollections.observableArrayList();
        mainLayout = new BorderPane();

        // Setup Sidebar (Left)
        VBox sidebar = buildSidebar();
        mainLayout.setLeft(sidebar);

        // Setup Center Content
        centerContent = new StackPane();
        centerContent.setStyle("-fx-background-color: #ffffff;");
        mainLayout.setCenter(centerContent);
        
        // Restore previous session
        restoreSession();

        Scene scene = new Scene(mainLayout, 1100, 700);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }
    
    @Override
    public void stop() {
        // Save current session on exit
        String paths = openFiles.stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.joining("|||"));
        prefs.put(PREF_OPEN_FILES, paths);
        
        File selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            prefs.put(PREF_LAST_SELECTED, selected.getAbsolutePath());
        } else {
            prefs.remove(PREF_LAST_SELECTED);
        }
    }
    
    private void restoreSession() {
        String savedPaths = prefs.get(PREF_OPEN_FILES, "");
        if (!savedPaths.isEmpty()) {
            String[] paths = savedPaths.split("\\|\\|\\|");
            for (String path : paths) {
                File f = new File(path);
                if (f.exists() && f.isFile()) {
                    openFiles.add(f);
                }
            }
        }
        
        if (openFiles.isEmpty()) {
            showWelcomeScreen();
        } else {
            String lastSelected = prefs.get(PREF_LAST_SELECTED, "");
            File toSelect = openFiles.get(0);
            for (File f : openFiles) {
                if (f.getAbsolutePath().equals(lastSelected)) {
                    toSelect = f;
                    break;
                }
            }
            fileListView.getSelectionModel().select(toSelect);
        }
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(10));
        sidebar.setStyle("-fx-background-color: #1e1e2e;");
        sidebar.setPrefWidth(280);

        Label sidebarTitle = new Label("Open Files");
        sidebarTitle.setFont(Font.font("Segoe UI", 16));
        sidebarTitle.setStyle("-fx-text-fill: #cdd6f4; -fx-font-weight: bold;");

        TextField searchField = new TextField();
        searchField.setPromptText("\uD83D\uDD0D Search files...");
        searchField.setStyle(
                "-fx-background-color: #181825; " +
                "-fx-text-fill: #cdd6f4; " +
                "-fx-prompt-text-fill: #a6adc8; " +
                "-fx-border-color: #45475a; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-padding: 4 24 4 8;"
        );
        
        Button clearSearchBtn = new Button("✕");
        clearSearchBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a6adc8; -fx-cursor: hand; -fx-padding: 4 8 4 4; -fx-font-size: 10px;");
        clearSearchBtn.setVisible(false);
        
        clearSearchBtn.setOnAction(e -> {
            searchField.clear();
            searchField.requestFocus();
        });

        StackPane searchPane = new StackPane(searchField, clearSearchBtn);
        StackPane.setAlignment(clearSearchBtn, Pos.CENTER_RIGHT);

        FilteredList<File> filteredFiles = new FilteredList<>(openFiles, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearSearchBtn.setVisible(newValue != null && !newValue.isEmpty());
            filteredFiles.setPredicate(file -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return file.getName().toLowerCase().contains(lowerCaseFilter);
            });
        });

        Button openButton = new Button("\uD83D\uDCC2 Open Files...");
        openButton.setMaxWidth(Double.MAX_VALUE);
        openButton.setStyle(
                "-fx-background-color: #89b4fa;" +
                "-fx-text-fill: #1e1e2e;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8;" +
                "-fx-background-radius: 6;" +
                "-fx-cursor: hand;"
        );
        openButton.setOnAction(e -> openFiles());

        fileListView = new ListView<>(filteredFiles);
        VBox.setVgrow(fileListView, Priority.ALWAYS);
        fileListView.setStyle(
                "-fx-background-color: #1e1e2e; " +
                "-fx-control-inner-background: #1e1e2e; " +
                "-fx-border-color: #45475a; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4;"
        );
        
        fileListView.setCellFactory(param -> new ListCell<File>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #1e1e2e;");
                } else {
                    setText(null);
                    
                    HBox cellBox = new HBox();
                    cellBox.setAlignment(Pos.CENTER_LEFT);
                    
                    Label nameLabel = new Label("\uD83D\uDCC4 " + file.getName());
                    if (isSelected()) {
                        nameLabel.setStyle("-fx-text-fill: #cdd6f4; -fx-font-weight: bold; -fx-cursor: hand;");
                        setStyle("-fx-background-color: #45475a;");
                    } else {
                        nameLabel.setStyle("-fx-text-fill: #a6adc8; -fx-cursor: hand;");
                        setStyle("-fx-background-color: #1e1e2e;");
                    }
                    
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    
                    Button closeBtn = new Button("✕");
                    closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f38ba8; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 2 6;");
                    closeBtn.setOnAction(e -> {
                        openFiles.remove(file);
                        if (openFiles.isEmpty()) {
                            // Ensure the welcome screen is shown and preferences updated
                            fileListView.getSelectionModel().clearSelection();
                            stop(); // Save state immediately on close to reflect in preferences
                        }
                    });
                    
                    cellBox.getChildren().addAll(nameLabel, spacer, closeBtn);
                    setGraphic(cellBox);
                }
            }
        });

        fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                renderAndShowFile(newSelection);
            } else {
                showWelcomeScreen();
            }
        });

        sidebar.getChildren().addAll(sidebarTitle, searchPane, openButton, fileListView);
        return sidebar;
    }

    private void showWelcomeScreen() {
        centerContent.getChildren().clear();
        
        VBox welcome = new VBox(20);
        welcome.setAlignment(Pos.CENTER);
        
        Label title = new Label("Markdown Viewer");
        title.setFont(Font.font("Segoe UI", 32));
        title.setStyle("-fx-text-fill: #45475a; -fx-font-weight: bold;");
        
        Label desc = new Label("A fast and beautiful way to read your markdown notes.");
        desc.setFont(Font.font("Segoe UI", 16));
        desc.setStyle("-fx-text-fill: #7f849c;");
        
        Button openBtn = new Button("\uD83D\uDCC2  Open Markdown Files...");
        openBtn.setStyle(
                "-fx-background-color: #89b4fa;" +
                "-fx-text-fill: #1e1e2e;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;" +
                "-fx-padding: 12 24;" +
                "-fx-background-radius: 6;" +
                "-fx-cursor: hand;"
        );
        openBtn.setOnAction(e -> openFiles());
        
        welcome.getChildren().addAll(title, desc, openBtn);
        centerContent.getChildren().add(welcome);
    }

    private void openFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Markdown Files");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Markdown Files", "*.md", "*.markdown"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(primaryStage);
        if (selectedFiles != null) {
            for (File file : selectedFiles) {
                if (!openFiles.contains(file)) {
                    openFiles.add(file);
                }
            }
            // Auto-select the first newly opened file
            if (!selectedFiles.isEmpty()) {
                fileListView.getSelectionModel().select(selectedFiles.get(0));
            }
        }
    }

    private void renderAndShowFile(File file) {
        try {
            String content = Files.readString(file.toPath());
            Node document = parser.parse(content);
            String html = renderer.render(document);

            String styledHtml = "<!DOCTYPE html><html><head><meta charset='utf-8'><title>" + file.getName() + "</title><style>" +
                    "body { font-family: -apple-system,BlinkMacSystemFont,'Segoe UI','Segoe UI Emoji','Apple Color Emoji','Noto Color Emoji',Helvetica,Arial,sans-serif; line-height: 1.6; color: #24292e; padding: 32px 40px; max-width: 900px; margin: 0 auto; background-color: #ffffff; }" +
                    "h1, h2, h3 { border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; margin-top: 24px; margin-bottom: 16px; font-weight: 600; }" +
                    "h1 { font-size: 2em; } h2 { font-size: 1.5em; } h3 { font-size: 1.25em; }" +
                    "p { margin-top: 0; margin-bottom: 16px; }" +
                    "ul, ol { padding-left: 2em; margin-bottom: 16px; }" +
                    "li { margin-bottom: 4px; }" +
                    "table { border-collapse: collapse; width: 100%; margin-bottom: 16px; }" +
                    "table, th, td { border: 1px solid #dfe2e5; }" +
                    "th, td { padding: 6px 13px; }" +
                    "th { background-color: #f6f8fa; font-weight: 600; }" +
                    "tr:nth-child(2n) { background-color: #f6f8fa; }" +
                    "code { font-family: SFMono-Regular,Consolas,'Liberation Mono',Menlo,monospace; background-color: rgba(27,31,35,0.05); padding: 0.2em 0.4em; border-radius: 3px; font-size: 85%; }" +
                    "pre { background-color: #f6f8fa; padding: 16px; overflow: auto; border-radius: 6px; line-height: 1.45; }" +
                    "pre code { background-color: transparent; padding: 0; }" +
                    "blockquote { padding: 0 1em; color: #6a737d; border-left: 0.25em solid #dfe2e5; margin: 0 0 16px 0; }" +
                    "a { color: #0366d6; text-decoration: none; }" +
                    "a:hover { text-decoration: underline; }" +
                    "img { max-width: 100%; }" +
                    "hr { height: 0.25em; padding: 0; margin: 24px 0; background-color: #e1e4e8; border: 0; border-radius: 3px; }" +
                    "</style></head><body>" + html + "</body></html>";

            WebView webView = new WebView();
            WebEngine engine = webView.getEngine();
            engine.loadContent(styledHtml, "text/html");

            HBox searchBox = new HBox(2);
            searchBox.setAlignment(Pos.CENTER);
            searchBox.setStyle("-fx-background-color: #ffffff; -fx-padding: 4 8; -fx-background-radius: 8; -fx-border-color: #d0d7de; -fx-border-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 3);");
            searchBox.setMaxWidth(Region.USE_PREF_SIZE);
            searchBox.setMaxHeight(Region.USE_PREF_SIZE);
            searchBox.setPickOnBounds(false);
            
            TextField docSearchField = new TextField();
            docSearchField.setPromptText("\uD83D\uDD0D Search in document...");
            docSearchField.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 4; -fx-pref-width: 200;");
            
            Button prevBtn = new Button("▲");
            prevBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #57606a; -fx-padding: 4;");
            Button nextBtn = new Button("▼");
            nextBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #57606a; -fx-padding: 4;");
            
            Button openSearchBtn = new Button("\uD83D\uDD0D");
            openSearchBtn.setStyle("-fx-background-color: #ffffff; -fx-padding: 8; -fx-background-radius: 20; -fx-border-color: #d0d7de; -fx-border-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 3); -fx-cursor: hand; -fx-text-fill: #57606a;");
            StackPane.setAlignment(openSearchBtn, Pos.TOP_RIGHT);
            StackPane.setMargin(openSearchBtn, new Insets(20, 40, 0, 0));
            openSearchBtn.setVisible(false);
            
            openSearchBtn.setOnAction(e -> {
                openSearchBtn.setVisible(false);
                searchBox.setVisible(true);
                docSearchField.requestFocus();
            });

            Button closeBtn = new Button("✕");
            closeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #57606a; -fx-padding: 4; -fx-font-weight: bold;");
            closeBtn.setOnAction(e -> {
                searchBox.setVisible(false);
                openSearchBtn.setVisible(true);
            });
            
            Runnable doSearch = () -> {
                String text = docSearchField.getText();
                if (text != null && !text.isEmpty()) {
                    String escapedText = text.replace("\\", "\\\\").replace("'", "\\'");
                    engine.executeScript("window.find('" + escapedText + "', false, false, true, false, true, false);");
                }
            };
            
            Runnable doSearchBackwards = () -> {
                String text = docSearchField.getText();
                if (text != null && !text.isEmpty()) {
                    String escapedText = text.replace("\\", "\\\\").replace("'", "\\'");
                    engine.executeScript("window.find('" + escapedText + "', false, true, true, false, true, false);");
                }
            };
            
            docSearchField.setOnAction(e -> doSearch.run());
            nextBtn.setOnAction(e -> doSearch.run());
            prevBtn.setOnAction(e -> doSearchBackwards.run());
            
            searchBox.getChildren().addAll(docSearchField, prevBtn, nextBtn, closeBtn);
            StackPane.setAlignment(searchBox, Pos.TOP_RIGHT);
            StackPane.setMargin(searchBox, new Insets(20, 40, 0, 0));
            
            webView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.isControlDown() && event.getCode() == KeyCode.F) {
                    searchBox.setVisible(true);
                    openSearchBtn.setVisible(false);
                    docSearchField.requestFocus();
                    event.consume();
                }
            });

            centerContent.getChildren().clear();
            centerContent.getChildren().addAll(webView, searchBox, openSearchBtn);

        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could not render file");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}

