package meetings;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.StageStyle;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class Participants {

    public static Node getNode() {
        HBox box = new HBox(getProducersNode(), getProjectsNode());
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #ED0000;"); //#336699
        return box;
    }

    private static Node getProducersNode() {
        Label label = new Label("Participants");
        label.setFont(Font.font("calibry", FontWeight.BOLD, 20));
        label.setTextFill(Color.WHITE);

        TableView<Producer> table = getTable();
        table.setPlaceholder(new Label("No Participants"));

        FilteredSortedList<Producer> list = new FilteredSortedList<>(DB.producers);
        list.bind(table.comparatorProperty());
        table.setItems(list.sortedData);

        Button add = new Button("Add...");
        add.setOnAction(new EventHandler<ActionEvent>() {
            int number = 0;

            @Override
            public void handle(ActionEvent e) {
                number++;
                TextInputDialog dialog = new TextInputDialog("Producer " + number);
                dialog.initStyle(StageStyle.UTILITY);
                dialog.setHeaderText("");
                dialog.setTitle("New producer");
                dialog.setContentText("Name: ");
                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()) {
                    Producer p = new Producer();
                    p.setName(result.get());
                    DB.producers.add(p);
                    table.getSelectionModel().select(p);
                }
            }
        });

        Button delete = new Button("Remove");
        delete.setOnAction(e -> {
            Producer selected = table.getSelectionModel().getSelectedItem();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText("");
            alert.setContentText("Are you sure you want to remove participant " + selected.getName() + "?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
                Main.closeTab(selected);
                DB.requests.removeIf(request -> request.has(selected));
                DB.meetings.removeIf(meeting -> meeting.has(selected));
                DB.producers.remove(selected);
                DB.events.forEach(event -> event.participants.remove(selected));
                DB.producers.forEach(Participant::count);
                DB.projects.forEach(Participant::count);
            }
        });

        Button importButton = new Button("Import...");
        importButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.initStyle(StageStyle.UTILITY);
            dialog.setHeaderText("");
            dialog.setTitle("Import");
            dialog.setContentText("JSON: ");
            String json = dialog.showAndWait().orElse("[]");
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                try {
                    JSONObject obj = array.getJSONObject(i);
                    boolean found = false;
                    if (obj.has("id") && obj.getInt("id") != -1)
                    for (Producer existing : DB.producers) {
                        if (existing.id.get().equals(obj.getString("id"))) { //TODO fix types
                            if (obj.has("arrivalimport")) {
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy H:mm");
                                if (!obj.getString("arrivalimport").isEmpty()) {
                                    existing.arrival.set(LocalDateTime.parse(obj.getString("arrivalimport") + " 6:00", formatter));
                                    existing.departure.set(LocalDateTime.parse(obj.getString("departureimport") + " 23:00", formatter));
                                }
                            }
                            if (obj.has("email")) existing.email.set(obj.getString("email"));
                            if (obj.has("company")) existing.setCompany(obj.getString("company"));
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        System.out.println("didnt find" + obj.optString("id"));
                        DB.producers.add(new Producer(obj));
                    }
                } catch (JSONException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        });

        HBox buttons = new HBox(10, add, delete, importButton);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().addAll(label, table, list.getField(), buttons);

        table.getSelectionModel().select(0);
        return vbox;
    }

    private static Node getProjectsNode() {
        Label label = new Label("Projects");
        label.setFont(Font.font("calibry", FontWeight.BOLD, 20));
        label.setTextFill(Color.WHITE);

        TableView<Project> table = getTable();
        table.setPlaceholder(new Label("No Projects"));

        FilteredSortedList<Project> list = new FilteredSortedList<>(DB.projects);
        list.bind(table.comparatorProperty());
        table.setItems(list.sortedData);

        Button add = new Button("Add...");
        add.setOnAction(new EventHandler<ActionEvent>() {
            int number = 0;

            @Override
            public void handle(ActionEvent e) {
                number++;
                TextInputDialog dialog = new TextInputDialog("Project " + number);
                dialog.initStyle(StageStyle.UTILITY);
                dialog.setHeaderText("");
                dialog.setTitle("New Project");
                dialog.setContentText("Name: ");
                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()) {
                    Project p = new Project();
                    p.setName(result.get());
                    DB.projects.add(p);
                    table.getSelectionModel().select(p);
                }
            }
        });

        Button importButton = new Button("Import...");
        importButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.initStyle(StageStyle.UTILITY);
            dialog.setHeaderText("");
            dialog.setTitle("Import");
            dialog.setContentText("JSON: ");
            String json = dialog.showAndWait().orElse("[]");
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                try {
                    Project p = new Project(array.getJSONObject(i));
                    DB.projects.add(p);
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }
        });

        HBox buttons = new HBox(10, add, importButton);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().addAll(label, table, list.getField(), buttons);

        table.getSelectionModel().select(0);
        return vbox;
    }

    private static <T extends Participant> TableView<T> getTable() {
        TableView<T> table = new TableView<>();
        table.setStyle("-fx-focus-color: transparent;");
        table.setRowFactory(t -> {
            TableRow<T> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Main.open(row.getItem());
                }
            });
            return row;
        });

        TableColumn<T, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        //nameCol.setSortable(false);
        nameCol.setPrefWidth(150);
        nameCol.setResizable(false);

        TableColumn<T, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        //statusCol.setSortable(false);
        statusCol.setPrefWidth(50);
        nameCol.setResizable(false);

        table.getColumns().add(nameCol);
        table.getColumns().add(statusCol);
        table.setPrefWidth(210);
        table.setPrefHeight(1000);
        return table;
    }

}

