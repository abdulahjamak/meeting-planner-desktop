package meetings;

import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class DateTimePicker {
    private final TextField hours;
    private final TextField minutes;
    private final DatePicker datePicker;

    private DateTimePicker(LocalDateTime init) {
        hours = new TextField(Integer.toString(init.getHour()));
        minutes = new TextField(Integer.toString(init.getMinute()));
        datePicker = new DatePicker(init.toLocalDate());
        hours.setPrefWidth(50);
        minutes.setPrefWidth(50);
        datePicker.setPrefWidth(200);

        hours.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                hours.setText("0");
                return;
            }
            if (newValue.matches("[0-9]*")) {
                int h = Integer.parseInt(newValue);
                h = Integer.max(h, 0);
                h = Integer.min(h, 23);
                hours.setText(Integer.toString(h));
            } else {
                hours.setText(oldValue);
            }
        });
        minutes.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                minutes.setText("0");
                return;
            }
            if (newValue.matches("[0-9]*")) {
                int m = (Integer.parseInt(newValue));
                m = Integer.max(m, 0);
                m = Integer.min(m, 59);
                minutes.setText(Integer.toString(m));
            } else {
                minutes.setText(oldValue);
            }
        });

    }

    public DateTimePicker() {
        this(LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 0)));
    }

    public void bump(long pause) {
        set(get().plusMinutes(pause));
    }

    public Node getNode() {
        return new HBox(5, new Label("Time: "), hours, minutes, datePicker);
    }

    public LocalDateTime get() {
        LocalTime time = LocalTime.of(Integer.parseInt(hours.getText()),
                Integer.parseInt(minutes.getText()));
        return LocalDateTime.of(datePicker.getValue(), time);
    }

    public void set(LocalDateTime newTime) {
        hours.setText(Integer.toString(newTime.getHour()));
        minutes.setText(Integer.toString(newTime.getMinute()));
        datePicker.setValue(newTime.toLocalDate());
    }
}
