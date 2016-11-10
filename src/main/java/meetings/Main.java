package meetings;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main extends Application {
    private static Stage stage;
    private static TabPane tabPane;
    public static Path exportDir = Paths.get(System.getProperty("user.dir") + "/export/");
    public static Path file;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        final Tab activities = new Tab("Activities");
        activities.setContent(Activities.getNode());
        activities.setClosable(false);

        final Tab events = new Tab("Events");
        events.setContent(Events.getNode());
        events.setClosable(false);

        final Tab export = new Tab("Export");
        export.setContent(Export.getNode());
        export.setClosable(false);

        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        tabPane.getTabs().addAll(activities, events, export);
        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue.getUserData() instanceof Participant) oldValue.setContent(new Label("hello"));
            if (newValue.getUserData() instanceof Participant) {
                newValue.setContent(((Participant) newValue.getUserData()).getNode());
            }
        });

        BorderPane root = new BorderPane();
        root.setLeft(Participants.getNode());
        root.setCenter(tabPane);
        root.setTop(getMenuBar());
        System.out.println("up and running");
        Scene scene = new Scene(root, 1280, 720);

        stage = primaryStage;
        stage.setOnCloseRequest(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Quit?");
            alert.setHeaderText("");
            alert.setContentText("Anything not saved will be lost.");
            if (alert.showAndWait().get() == ButtonType.OK) {
                Platform.exit();
            } else {
                e.consume();
            }
        });

        stage.setScene(scene);
        stage.setTitle("Meeting Planner");
        stage.show();
        stage.setMaximized(true);
        /*
        final Parameters params = getParameters();
        final List<String> parameters = params.getRaw();
        if (!parameters.isEmpty()) {
            File file = new File(parameters.get(0));
            setFile(file);
        }
        */
    }

    public static void open(Participant t) {
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() == t) {
                tabPane.getSelectionModel().select(tab);
                return;
            }
        }
        Tab tab = new Tab();
        tab.textProperty().bind(t.name);
        tab.setUserData(t);
        if (t instanceof Producer) {
            tab.setStyle("-fx-background-color: #BCE1EC;");
        } else {
            tab.setStyle("-fx-background-color: #fba71b;");
        }
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    public static void closeTabs() {
        tabPane.getTabs().removeIf(tab -> tab.getUserData() != null);
    }

    public static void closeTab(Participant p) {
        tabPane.getTabs().removeIf(tab -> tab.getUserData() == p);
    }

    private MenuBar getMenuBar() {
        Menu mFile = new Menu("File");
        MenuItem mNew = new MenuItem("New");
        MenuItem mOpen = new MenuItem("Open...");
        MenuItem mSave = new MenuItem("Save");
        MenuItem mSaveAs = new MenuItem("Save As...");
        MenuItem mReport = new MenuItem("Report...");
        MenuItem mExit = new MenuItem("Exit");

        Menu mHelp = new Menu("Help");
        MenuItem mAbout = new MenuItem("About");

        final FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters()
                .setAll(new FileChooser.ExtensionFilter("Meetings Database", "*.meetdb"));

        mNew.setOnAction(event -> {
            DB.clear();
            file = null;
            exportDir = Paths.get(System.getProperty("user.dir") + "/export/");
            stage.setTitle("Meeting Planner");
        });

        mOpen.setOnAction(event -> {
            File newFile = fileChooser.showOpenDialog(stage);
            if (newFile != null) {
                Path newPath = newFile.toPath();
                setFile(newPath);
                DB.openNew(file);
                Export.load();
            }
        });

        mSave.setOnAction(event -> {
            if (file != null) {
                String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH-mm"));
                Path backup = Paths.get(file.toString().replaceFirst(".meetdb", Export.sanitizeName("-backup-" + time + ".meetdb")));
                DB.saveNew(backup);
                try {
                    Files.copy(backup, file, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Export.save();
            } else {
                mSaveAs.fire();
            }
        });

        mSaveAs.setOnAction(event -> {
            File newFile = fileChooser.showSaveDialog(stage);
            if (newFile != null) {
                Path newPath = newFile.toPath();
                if (newPath.toString().endsWith(".meetdb")) {
                    setFile(newPath);
                } else {
                    setFile(Paths.get(newFile + ".meetdb"));
                }
                DB.saveNew(file);
                Export.save();
            }
        });

        mExit.setOnAction(event -> stage.close());

        mReport.setOnAction(e -> SpreadSheetGenerator.generate());

        mAbout.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initStyle(StageStyle.UTILITY);
            alert.setHeaderText("");
            alert.setContentText("Ali Dlakic\nali@pro.ba\nCopyright 2016");
            alert.show();
        });
        mFile.getItems().addAll(mNew, mOpen, mSave, mSaveAs, mReport, mExit);
        mHelp.getItems().addAll(mAbout);

        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(mFile, mHelp);
        //menuBar.setUseSystemMenuBar(true);
        return menuBar;
    }

    private void setFile(Path newLocation) {
        file = newLocation;
        stage.setTitle("Meeting Planner - " + file);
        exportDir = Paths.get(file.getParent() + "/export/");
    }

}


