package meetings;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.awt.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Export {
    public static final SimpleStringProperty header = new SimpleStringProperty("Meeting schedule for #name:");
    public static final SimpleStringProperty footer = new SimpleStringProperty("footer\ntext\nbroj tel.");
    public static final SimpleStringProperty emailSubject = new SimpleStringProperty("Meeting schedule for #name");
    public static final SimpleStringProperty emailTemplate = new SimpleStringProperty("dear #name,\nschedule in attachment");
    public static final SimpleBooleanProperty simpleExport = new SimpleBooleanProperty(false);
    public static final SimpleBooleanProperty calExport = new SimpleBooleanProperty(false);
    public static final SimpleBooleanProperty showFinished = new SimpleBooleanProperty(true);


    public static void load() {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(Main.file.getParent() + "/templates.txt"));
            header.set(props.getProperty("header"));
            footer.set(props.getProperty("footer"));
            emailSubject.set(props.getProperty("emailSubject"));
            emailTemplate.set(props.getProperty("emailTemplate"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        Properties props = new Properties();
        props.setProperty("header", header.get());
        props.setProperty("footer", footer.get());
        props.setProperty("emailSubject", emailSubject.get());
        props.setProperty("emailTemplate", emailTemplate.get());
        try {
            props.store(new FileOutputStream(Main.file.getParent() + "/templates.txt"), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Node getNode() {
        Label label = new Label("Export Setings");
        label.setFont(Font.font(24));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Label headerLabel = new Label("Header:");
        grid.add(headerLabel, 0, 0);
        TextArea headerField = new TextArea();
        headerField.textProperty().bindBidirectional(header);
        headerField.setPrefWidth(300);
        grid.add(headerField, 1, 0);

        Label footerLabel = new Label("Footer:");
        grid.add(footerLabel, 0, 1);
        TextArea footerArea = new TextArea();
        footerArea.textProperty().bindBidirectional(footer);
        footerArea.setPrefWidth(300);
        grid.add(footerArea, 1, 1);

        Label subjectLabel = new Label("Subject:");
        grid.add(subjectLabel, 2, 0);
        TextField subjectField = new TextField();
        subjectField.textProperty().bindBidirectional(emailSubject);
        subjectField.setPrefWidth(300);
        grid.add(subjectField, 3, 0);

        Label emailLabel = new Label("Email:");
        grid.add(emailLabel, 2, 1);
        TextArea emailArea = new TextArea();
        emailArea.textProperty().bindBidirectional(emailTemplate);
        emailArea.setPrefWidth(300);
        grid.add(emailArea, 3, 1);

        CheckBox simple = new CheckBox("Simple Export");
        simple.selectedProperty().bindBidirectional(simpleExport);
        CheckBox cals = new CheckBox("Calendar Export");
        cals.selectedProperty().bindBidirectional(calExport);
        CheckBox finished = new CheckBox("Show Finished");
        finished.selectedProperty().bindBidirectional(showFinished);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        HBox hbox = new HBox(10);
        hbox.getChildren().addAll(participantTable(), projectTable());
        vbox.getChildren().addAll(label, simple, cals, finished, grid, hbox);
        return vbox;
    }

    private static Node participantTable() {
        TableColumn<Producer, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setSortable(false);

        TableColumn<Producer, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setSortable(false);

        TableColumn<Producer, String> company = new TableColumn<>("Company");
        company.setCellValueFactory(new PropertyValueFactory<>("company"));
        company.setSortable(false);

        TableColumn<Producer, String> status = new TableColumn<>("Status");
        status.setCellValueFactory(new PropertyValueFactory<>("status"));
        status.setSortable(true);

        TableColumn<Producer, Boolean> checkedCol = new TableColumn<>("Export/Email");
        checkedCol.setCellValueFactory(new PropertyValueFactory<>("exportChecked"));
        checkedCol.setCellFactory(CheckBoxTableCell.forTableColumn(checkedCol));
        checkedCol.setEditable(true);

        TableView<Producer> table = new TableView<>();
        table.setEditable(true);
        //noinspection unchecked
        table.getColumns().addAll(nameCol, emailCol, company, status, checkedCol);
        table.setPrefHeight(400);
        table.setPrefWidth(600);
        table.setPlaceholder(new Label("No Participants"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(DB.producers);

        Button export = new Button("Export...");
        export.setOnAction(e -> {
            Path file = Paths.get(Main.exportDir + "/participants.pdf");
            new PDFGenerator(file).generate(DB.producers.stream().filter(Producer::getExportChecked)
                    .toArray(Participant[]::new));
            Export.openFile(file);
        });

        Button emailb = new Button("Email(!)");
        emailb.setStyle("-fx-base: #f3622d;");
        emailb.setOnAction(e -> Email.email(
                        DB.producers.stream()
                        .filter(p -> p.getExportChecked() && !p.getEmail().isEmpty())
                        .toArray(Producer[]::new)
        ));

        Button checkAll = new Button("Check all");
        checkAll.setOnAction(e -> DB.producers.forEach(p -> p.exportCheckedProperty().set(true)));
        Button unCheckAll = new Button("Uncheck all");
        unCheckAll.setOnAction(e -> DB.producers.forEach(p -> p.exportCheckedProperty().set(false)));

        Button meetings = new Button("Meetings");
        meetings.setOnAction(e -> DB.producers.forEach(p -> p.exportCheckedProperty().set(
                DB.meetings.stream().anyMatch(m -> m.has(p)))));

        Button events = new Button("Events");
        events.setOnAction(e -> DB.producers.forEach(p -> p.exportCheckedProperty().set(DB.events.stream().anyMatch(ev -> ev.has(p)))));
        HBox buttons = new HBox(10);

        buttons.getChildren().addAll(checkAll, unCheckAll, export, emailb /*, meetings, events */);

        Label label = new Label("Participants");
        label.setFont(Font.font(18));

        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(label, table, buttons);

        return vbox;
    }

    private static Node projectTable() {
        TableColumn<Project, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setSortable(false);

        TableColumn<Project, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setSortable(false);

        TableColumn<Project, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setSortable(false);

        TableColumn<Project, Boolean> checkedCol = new TableColumn<>("Export/Email");
        checkedCol.setCellValueFactory(new PropertyValueFactory<>("exportChecked"));
        checkedCol.setCellFactory(CheckBoxTableCell.forTableColumn(checkedCol));
        checkedCol.setEditable(true);

        TableView<Project> table = new TableView<>();
        table.setEditable(true);
        //noinspection unchecked
        table.getColumns().addAll(nameCol, emailCol, statusCol, checkedCol);
        table.setPrefHeight(400);
        table.setPrefWidth(600);
        table.setPlaceholder(new Label("No Participants"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.setItems(DB.projects);

        Button export = new Button("Export...");
        export.setOnAction(e -> {
            Path file = Paths.get(Main.exportDir + "/projects.pdf");
            new PDFGenerator(file).generate(DB.projects.stream()
                    .filter(Project::getExportChecked).toArray(Project[]::new));
            Export.openFile(file);
        });

        Button emailb = new Button("Email(!)");
        emailb.setStyle("-fx-base: #f3622d;");
        emailb.setOnAction(e -> Email.email(DB.projects.stream()
                .filter(p -> p.getExportChecked() && !p.getEmail().isEmpty()).toArray(Project[]::new)));

        Button checkAll = new Button("Check all");
        checkAll.setOnAction(e -> DB.projects.forEach(p -> p.exportCheckedProperty().set(true)));
        Button unCheckAll = new Button("Uncheck all");
        unCheckAll.setOnAction(e -> DB.projects.forEach(p -> p.exportCheckedProperty().set(false)));

        HBox buttons = new HBox(10);
        buttons.getChildren().addAll(checkAll, unCheckAll, export, emailb);

        Label label = new Label("Projects");
        label.setFont(Font.font(18));

        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(label, table, buttons);

        return vbox;
    }

    public static String sanitizeName(String name) {
        return name.replaceAll("[\u0001-\u001f<>:\"/\\\\|?*\u007f]+", "").trim();
    }

    public static void openFile(Path path) {
        new Thread(() -> {
            try {
                Desktop.getDesktop().open(path.toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}

