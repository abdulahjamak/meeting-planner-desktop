package meetings;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class TimeSlots {
    private final Activity activity;

    TimeSlots(Activity activity) {
        this.activity = activity;
    }

    public Node getNode() {
        final TextField duration = new TextField("30");
        final TextField pause = new TextField("0");
        DateTimePicker time = new DateTimePicker();
        duration.setPrefWidth(50);
        pause.setPrefWidth(50);
        ListView<TimeSlot> slotList = new ListView<>(activity.timeSlots);

        Button addSlot = new Button("Add Time Slot");
        addSlot.setOnAction(e -> {
            TimeSlot ts = new TimeSlot(time.get(),
                    time.get().plusMinutes(Integer.parseInt(duration.getText())), activity);
            if (!activity.timeSlots.stream().filter(ts::isOverlapping).findAny().isPresent()) {
                activity.timeSlots.add(ts);
                activity.timeSlots.sort((o1, o2) -> o1.start.compareTo(o2.start));
                time.bump(Integer.parseInt(pause.getText()) + Integer.parseInt(duration.getText()));
            } else {
                Alert a = new Alert(Alert.AlertType.WARNING);
                a.setHeaderText("");
                a.setContentText("Time slot intersects existing time slot in activity");
                a.show();
            }
        });

        Button remove = new Button("Remove Time Slot");
        remove.setOnAction(e -> {
            TimeSlot selected = slotList.getSelectionModel().getSelectedItem();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText("Are you sure you want to remove time slot " + selected.toString() + "?");
            alert.setContentText("Existing meetings will be promoted to custom time slots");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
                activity.timeSlots.remove(selected);
            }
        });

        Button export = new Button("Report...");
        export.setOnAction(e -> {
            Path file = Paths.get(Main.exportDir + "/timeslots.pdf");
            new PDFGenerator(file).generate(activity);
            Export.openFile(file);
        });

        VBox toolbar = new VBox(5, new HBox(5, time.getNode(), new Label("Duration:"), duration,
                new Label("Pause:"), pause),
                new HBox(5, addSlot, remove, export));

        return new VBox(20, slotList, toolbar);
    }
}
