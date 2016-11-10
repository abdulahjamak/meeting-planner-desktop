package meetings;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Pair;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MeetingPlanner {
    private final Participant requesting;
    private final Participant other;
    private final Activity activity;
    private Meeting selected;
    private String status;
    private final Label label = new Label(status);
    private final ToggleGroup group = new ToggleGroup();
    private final Meeting registeredMeeting;
    private final VBox root = new VBox(10);

    public MeetingPlanner(Participant requesting, Participant other, Activity activity) {
        this.requesting = requesting;
        this.other = other;
        this.activity = activity;
        registeredMeeting = DB.getMeeting(requesting, other, activity);
    }

    public Node getNode() {
        Button customButton;
        Node panes;
        boolean hasCustom = false;
        if (registeredMeeting != null) {
            if (!activity.timeSlots.contains(registeredMeeting.timeSlot)) {
                hasCustom = true;
            }
        }

        if (hasCustom) {
            panes = new Label("Custom time slot: " + registeredMeeting.timeSlot.toString());
            customButton = new Button("Remove");
            customButton.setOnAction(e -> {
                DB.meetings.remove(registeredMeeting);
                root.getChildren().setAll(new MeetingPlanner(requesting, other, activity).getNode());
            });
        } else {
            panes = getButtonPane();
            customButton = new Button("Custom Time Slot");
            customButton.setOnAction(e -> {
                getCustomTimeSlot();
            });
        }

        ScrollPane scrollPane = new ScrollPane(panes);
        scrollPane.setPrefHeight(2000);
        scrollPane.setPrefWidth(700);
        //scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color:transparent;");
        label.setMinHeight(50);
        root.getChildren().addAll(scrollPane, customButton, label);
        //root.setPrefWidth(700);
        //root.setPrefHeight(2000);
        return root;
    }

    private void getCustomTimeSlot() {
        final TextField duration = new TextField("30");
        duration.setPrefWidth(50);
        DateTimePicker time = new DateTimePicker();
        Stage stage = new Stage();
        Button add = new Button("Set");
        add.setOnAction(e -> {
            TimeSlot ts = new TimeSlot(time.get(),
                    time.get().plusMinutes(Integer.parseInt(duration.getText())), activity);
            stage.close();
            if (new Verify().verify(requesting, other, ts).result) {
                DB.setMeeting(requesting, other, ts);
                root.getChildren().setAll(new MeetingPlanner(requesting, other, activity).getNode());
            } else {
                Alert a = new Alert(Alert.AlertType.WARNING);
                a.setHeaderText("Time slot intersects existing meetings");
                Verify verify = new Verify();
                verify.verify(requesting, other, ts);
                a.setContentText(verify.reasons);
                a.show();
            }
            stage.close();
        });

        TableView<Pair<String[], TimeSlot>> table = new TableView<>();
        List<Pair<String[], TimeSlot>> list = DB.meetings.stream()
                .filter(m -> m.has(requesting) || m.has(other))
                .map(meeting -> new Pair<>(new String[]{
                        meeting.producer.getName(),
                        meeting.project.getName(),
                        meeting.timeSlot.activity.getName()},
                        meeting.timeSlot))
                .collect(Collectors.toList());
        list.addAll(DB.events.stream()
                .filter(m -> m.has(requesting))
                .map(event -> new Pair<>(
                        new String[]{requesting.getName(), event.getName(), "Event"}, event.timeSlot))
                .collect(Collectors.toList()));
        list.addAll(DB.events.stream()
                .filter(m -> m.has(other))
                .map(event -> new Pair<>(
                        new String[]{other.getName(), event.getName(), "Event"}, event.timeSlot))
                .collect(Collectors.toList()));
        list.sort((o1, o2) -> o1.getValue().start.compareTo(o2.getValue().start));


        for (int i = 0; i < 3; i++) {
            TableColumn<Pair<String[], TimeSlot>, String> tc = new TableColumn<>();
            final int column = i;
            tc.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getKey()[column]));
            table.getColumns().add(tc);
        }

        TableColumn<Pair<String[], TimeSlot>, String> timeslots = new TableColumn<>();
        timeslots.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().toString()));
        table.getColumns().add(timeslots);

        table.setItems(FXCollections.observableArrayList(list));

        HBox toolbar = new HBox(5, time.getNode(), new Label(" Duration: "), duration);
        VBox root = new VBox(10, table, toolbar, add);
        root.setPadding(new Insets(20));

        stage.setTitle("Custom Time Slot");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UTILITY);
        stage.setScene(new Scene(root));
        stage.show();
    }

    private Node getButtonPane() {
        Set<LocalDate> dates = new LinkedHashSet<>();
        for (TimeSlot ts : activity.timeSlots) {
            dates.add(ts.date());
        }

        status = requesting.getName() + " requesting " + other.getName();

        HBox panes = new HBox();
        for (LocalDate date : dates) {
            FlowPane pane = new FlowPane(5, 5);
            pane.setPrefWidth(200);
            activity.timeSlots.stream()
                    .filter(ts -> ts.date().equals(date))
                    .forEach(ts -> pane.getChildren().add(getButton(ts)));
            VBox container = new VBox();
            container.getChildren().addAll(new Label(date.format(DateTimeFormatter.ofPattern("E, d/L"))), pane);
            panes.getChildren().add(container);
        }

        if (selected != null)
            for (Toggle toggle : group.getToggles()) {
                if (toggle.getUserData().equals(selected.timeSlot)) {
                    group.selectToggle(toggle);
                }
            }

        group.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                TimeSlot ts = (TimeSlot) newValue.getUserData();
                if (!(new Verify().verify(requesting, other, ts).result)) {
                    Alert a = new Alert(Alert.AlertType.ERROR);
                    a.setHeaderText("Could not register meeting");
                    a.setContentText(new Verify().verify(requesting, other, ts).reasons);
                    a.show();
                    return;
                }
                status = "Selected meeting: " + ts.getButtonText();
                label.setText(status);
                DB.setMeeting(requesting, other, ts);
                ((ToggleButton) newValue).setStyle("-fx-base: #57b757;");
                if (oldValue != null) ((ToggleButton) oldValue).setStyle("-fx-base: #ffffff;");
            } else {
                DB.meetings.remove(new Meeting(requesting, other, (TimeSlot) oldValue.getUserData()));
                ((ToggleButton) oldValue).setStyle("-fx-base: #ffffff;");
                status = requesting.getName() + " requesting " + other.getName();
                label.setText(status);
                selected = null;
            }
            requesting.count();
            other.count();
        });
        return panes;
    }

    private Node getButton(TimeSlot ts) {
        if (registeredMeeting != null) {
            if (registeredMeeting.timeSlot.equals(ts)) {
                ToggleButton button = new ToggleButton(ts.getButtonText());
                button.setUserData(ts);
                button.setToggleGroup(group);
                selected = registeredMeeting;
                status = "Selected meeting: " + ts.getButtonText();
                label.setText(status);
                button.setStyle("-fx-base: #57b757;");      //green
                return button;
            }
        }

        Verify verify = new Verify();
        verify.generateColors = true;
        if (!verify.verify(requesting, other, ts).result) {
            Button button = new Button(ts.getButtonText());
            List<Stop> stops = new ArrayList<>();
            List<Color> colors = new ArrayList<>(verify.color);
            for (int i = 0; i < colors.size(); i++) {
                stops.add(new Stop(i / (double) colors.size(), colors.get(i)));
                stops.add(new Stop((i + 1) / (double) colors.size(), colors.get(i)));
            }
            LinearGradient gradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.REPEAT, stops);
            button.setBackground(new Background(new BackgroundFill(gradient, null, null)));
            button.setOnMouseEntered(e -> label.setText(verify.reasons));
            button.setOnMouseExited(e -> label.setText(status));
            button.setOnAction(e -> {
                Verify window = new Verify();
                window.window(requesting, other, ts);
            });
            return button;
        }

        ToggleButton button = new ToggleButton(ts.getButtonText());
        button.setUserData(ts);
        button.setToggleGroup(group);
        //button.setPrefWidth(150);
        return button;
    }

    private void reset() {

    }

}
