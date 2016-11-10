package meetings;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Events {
    public static Node getNode() {
        final TextField search = new TextField();
        FilteredList<Event> filteredData = new FilteredList<>(DB.events, p -> true);
        search.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(person -> newValue.isEmpty()
                    || person.getName().toLowerCase().contains(newValue.toLowerCase()));
        });
        SortedList<Event> sortedData = new SortedList<>(filteredData);

        TableView<Event> list = new TableView<>(sortedData);
        TableColumn<Event, String> name = new TableColumn<>("Event name");
        name.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getActivity() + " " + e.getValue().name.get()));
        name.setPrefWidth(290);
        TableColumn<Event, String> time = new TableColumn<>("Time");
        time.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().timeSlot.toString()));
        time.setPrefWidth(190);
        sortedData.comparatorProperty().bind(list.comparatorProperty());

        //noinspection unchecked
        list.getColumns().addAll(name, time);
        list.setPrefHeight(4000);
        list.setPrefWidth(500);
        StackPane detail = new StackPane();
        list.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            detail.getChildren().setAll(newValue.getNode());
        });

        Button add = new Button("Add Event");
        add.setOnAction(e -> addEvent());

        Button remove = new Button("Remove Event");
        remove.setOnAction(e -> {
            detail.getChildren().setAll(new Label("Select event"));
            DB.events.remove(list.getSelectionModel().getSelectedItem());
        });

        VBox master = new VBox(20);
        master.setPadding(new Insets(20));
        master.getChildren().addAll(list, search, new HBox(10, add, remove));

        HBox container = new HBox(20);
        container.getChildren().addAll(master, detail);

        return container;
    }

    private static void addEvent() {
        final TextField name = new TextField("");
        final TextField duration = new TextField("30");
        DateTimePicker time = new DateTimePicker();
        duration.setPrefWidth(50);

        Stage stage = new Stage();

        Button add = new Button("Add Event");
        add.setOnAction(e -> {
            TimeSlot ts = new TimeSlot(time.get(), time.get().plusMinutes(Integer.parseInt(duration.getText())), null);
            Event event = new Event(ts);
            DB.events.add(event);
            event.setName(name.getText());
            stage.close();
        });

        HBox toolbar = new HBox(5, time.getNode(), new Label("Duration: "), duration);

        VBox root = new VBox(10, new HBox(5, new Label("Name:"), name), toolbar, add);
        root.setPadding(new Insets(20));

        stage.setTitle("Add Event");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UTILITY);
        stage.setScene(new Scene(root));
        stage.show();
    }

}
