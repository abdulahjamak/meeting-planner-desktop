package meetings;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.json.JSONObject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

public class Event {
    public final SimpleStringProperty name = new SimpleStringProperty();
    public final SimpleStringProperty importName = new SimpleStringProperty();
    private final SimpleStringProperty location = new SimpleStringProperty("");
    private final SimpleStringProperty activityName = new SimpleStringProperty("Event");
    public final TimeSlot timeSlot;
    public final Set<Participant> participants = new HashSet<>();

    Event(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
    }

    Event(JSONObject obj) {
        name.set(obj.getString("name"));
        importName.set(obj.optString("importname"));
        activityName.set(obj.getString("activityname"));
        location.set(obj.getString("location"));
        timeSlot = new TimeSlot(LocalDateTime.parse(obj.getString("start")),
                LocalDateTime.parse(obj.getString("end")), null);
        for (String s : obj.getString("producers").split(",")) {
            if (!s.isEmpty()) participants.add(DB.producers.get(Integer.parseInt(s)));
        }
        for (String s : obj.getString("projects").split(",")) {
            if (!s.isEmpty()) participants.add(DB.projects.get(Integer.parseInt(s)));
        }
    }

    JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("name", getName());
        obj.put("importname", importName.get());
        obj.put("location", location.get());
        obj.put("activityname", activityName.get());
        obj.put("start", timeSlot.start.toString());
        obj.put("end", timeSlot.end.toString());
        StringJoiner producers = new StringJoiner(",");
        participants.stream()
                .filter(p -> p instanceof Producer)
                .forEach(p -> producers.add(Integer.toString(DB.producers.indexOf(p))));
        obj.put("producers", producers.toString());
        StringJoiner projects = new StringJoiner(",");
        participants.stream()
                .filter(p -> p instanceof Project)
                .forEach(p -> projects.add(Integer.toString(DB.projects.indexOf(p))));
        obj.put("projects", projects.toString());
        return obj;
    }


    public Node getNode() {
        HBox panels = new HBox(10, getCheckView(DB.producers), getCheckView(DB.projects));
        final TextField nameField = new TextField();
        nameField.textProperty().bindBidirectional(name);
        final TextField importField = new TextField();
        importField.textProperty().bindBidirectional(importName);
        final TextField locField = new TextField();
        locField.textProperty().bindBidirectional(location);

        final TextField activityField = new TextField();
        activityField.textProperty().bindBidirectional(activityName);

        Button report = new Button("Report");
        report.setOnAction(e -> {
            Path file = Paths.get(Main.exportDir + "/events/" + Export.sanitizeName(getName()) + ".pdf");
            new PDFGenerator(file).generate(this);
            Export.openFile(file);
        });

        VBox container = new VBox(10, new HBox(5, new Label("Event name: "), nameField),
                new HBox(5, new Label("Location: "), locField),
                new HBox(5, new Label("Impoort ID: "), importField),
                new HBox(5, new Label("Activity display: "), activityField), panels, report);
        container.setPadding(new Insets(20));
        return container;
    }

    private <T extends Participant> Node getCheckView(ObservableList<T> list) {
        FilteredSortedList<T> sortedList = new FilteredSortedList<>(list);

        ListView<T> checkProducers = new ListView<>(sortedList.sortedData);

        ObservableMap<Participant, SimpleBooleanProperty> localCheckState = FXCollections.observableHashMap();
        checkProducers.setCellFactory(CheckBoxListCell.forListView(p -> {
            if (localCheckState.containsKey(p)) {
                return localCheckState.get(p);
            }
            SimpleBooleanProperty observable = new SimpleBooleanProperty(participants.contains(p));
            observable.addListener((obs, oldValue, newValue) -> {
                if (newValue) {
                    String reason = new Verify().verify(p, timeSlot, false).reasons;
                    if (reason.isEmpty()) {
                        participants.add(p);
                    } else {
                        Alert a = new Alert(Alert.AlertType.WARNING);
                        a.setHeaderText("Participant already has activities");
                        a.setContentText(reason);
                        a.show();
                        a.setOnCloseRequest(e -> observable.setValue(false));
                    }
                } else {
                    participants.remove(p);
                }
            });
            localCheckState.put(p, observable);
            return observable;
        }));

        Button checkAll = new Button("Check all");
        checkAll.setOnAction(e -> list.stream()
                .filter(p -> new Verify().verify(p, timeSlot, false).result)
                .forEach(p -> {
                    if (localCheckState.containsKey(p)) {
                        localCheckState.get(p).set(true);
                        participants.add(p);
                    }
                }));
        Button unCheckAll = new Button("Uncheck all");
        unCheckAll.setOnAction(e -> list.forEach(p -> {
            //localCheckState.get(p).set(false);
            participants.remove(p);
        }));

        HBox toolbar = new HBox(10, checkAll, unCheckAll);
        VBox root = new VBox(5);
        root.getChildren().addAll(checkProducers, sortedList.getField(), toolbar);
        return root;
    }

    public boolean has(Participant participant) {
        return participants.contains(participant);
    }

    public String getName() {
        return name.get();
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getLocation() {
        return location.get();
    }

    public String getActivity() {
        return activityName.get();
    }

    @Override
    public String toString() {
        return name.get() + " " + timeSlot.toString();
    }
}
