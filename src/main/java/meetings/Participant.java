package meetings;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;
import javafx.util.Pair;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public abstract class Participant {
    final SimpleStringProperty name = new SimpleStringProperty("");
    final SimpleStringProperty email = new SimpleStringProperty("");
    private final SimpleStringProperty status = new SimpleStringProperty("0/0");
    private final SimpleBooleanProperty exportChecked = new SimpleBooleanProperty(true);
    public final HashMap<Activity, WeakReference<Participant>> selectedParticipant = new HashMap<>();
    public Activity selectedActivity;

    public Node getNode() {
        Label titleLabel = new Label();
        titleLabel.textProperty().bindBidirectional(name);
        titleLabel.setFont(Font.font("helvetica", FontWeight.BOLD, 20));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ArrayList<Property> properties = new ArrayList<>();
        properties.add(new Property("Name: ", name));
        properties.add(new Property("Email: ", email));
        properties.addAll(getProperties());
        int position = 0;
        for (Property property : properties) {
            Label label = new Label(property.name);
            grid.add(label, 0, position);
            TextField field = new TextField();
            field.textProperty().bindBidirectional(property.property);
            grid.add(field, 1, position);
            position++;
        }

        additionalItems(grid, position);

        Button timeSheet = new Button("Get Time Sheet...");
        timeSheet.setOnAction(e -> {
            Export.openFile(generate());
        });
        grid.add(timeSheet, 2, 0);

        ListView<EventGroup> eventView = new ListView<>(FXCollections.observableList(getEvents()));

        eventView.setCellFactory(p -> {
            CheckBoxListCell<EventGroup> cell = new CheckBoxListCell<>();
            cell.setSelectedStateCallback(param -> {
                cell.setDisable(param.checked > 0 && param.checked < param.eventList.size());
                SimpleBooleanProperty prop = new SimpleBooleanProperty(param.checked == param.eventList.size());
                prop.addListener((observable, oldValue, newValue) -> {
                    if (newValue) {
                        param.eventList.forEach(event -> event.participants.add(this));
                    } else {
                        param.eventList.forEach(event -> event.participants.remove(this));
                    }
                });
                return prop;
            });
            return cell;
        });

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().addAll(new HBox(10, new VBox(5, titleLabel, grid), eventView), new ActivitiesView(this).getNode());

        return vbox;
    }

    private class EventGroup {
        String name = "";
        List<Event> eventList = new ArrayList<>();
        int checked = 0;

        @Override
        public String toString() {
            return name;
        }
    }

    private List<EventGroup> getEvents() {
        Map<String, EventGroup> visited = new HashMap<>();
        for (Event event : DB.events) {
            String name = event.importName.get();
            if (name.isEmpty()) continue;
            if (!visited.containsKey(name)) {
                EventGroup eventGroup = new EventGroup();
                eventGroup.name = name;
                visited.put(name, eventGroup);
            }
            EventGroup found = visited.get(name);
            found.eventList.add(event);
            if (event.has(this)) found.checked++;
        }

        return new ArrayList<>(visited.values());
    }

    abstract String generateInfo(Meeting meeting);

    abstract ArrayList<Property> getProperties();

    abstract void additionalItems(GridPane grid, int position);

    class Property {
        final String name;
        final SimpleStringProperty property;

        Property(String name, SimpleStringProperty property) {
            this.name = name;
            this.property = property;
        }
    }

    public void count() {
        int total = 0, set = 0;
        for (Request request : DB.requests) {
            if (request.has(this)) {
                total++;
                for (Meeting meeting : DB.meetings) {
                    if (request.equalsMeeting(meeting)) {
                        set++;
                        break;
                    }
                }
            }
        }
        status.setValue(set + "/" + total);
    }

    public List<ScheduleEntry> getSchedule() {
        List<ScheduleEntry> list = DB.meetings.stream()
                .filter(m -> m.has(this))
                .map(meeting -> new ScheduleEntry(
                        meeting.timeSlot.activity.getName(),
                        generateInfo(meeting),
                        meeting.timeSlot.activity.getLocation(),
                        meeting.timeSlot))
                .collect(Collectors.toList());
        list.addAll(DB.events.stream()
                .filter(m -> m.has(this))
                .map(event -> new ScheduleEntry(
                        event.getActivity(),
                        event.getName(),
                        event.getLocation(),
                        event.timeSlot))
                .collect(Collectors.toList()));
        if (!Export.showFinished.get()) list = list.stream()
                .filter(entry -> entry.timeSlot.end.isAfter(LocalDateTime.now()))
                .collect(Collectors.toCollection(ArrayList::new));
        list.sort((o1, o2) -> o1.timeSlot.start.compareTo(o2.timeSlot.start));
        if (Export.calExport.get()) iCalGenerator.generate(this, list);
        return list;
    }

    public abstract Participant other(Meeting meeting);

    public abstract Path generate();

    abstract public JSONObject toJSON();

    abstract public ArrayList<Participant> getMeetables(Activity activity);

    public String getName() {
        return name.get();
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public SimpleStringProperty emailProperty() {
        return email;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public SimpleStringProperty statusProperty() {
        return status;
    }

    public boolean getExportChecked() {
        return exportChecked.get();
    }

    public void setExportChecked(boolean checked) {
        exportChecked.set(checked);
    }

    public SimpleBooleanProperty exportCheckedProperty() {
        return exportChecked;
    }

    public String getEmail() {
        return email.get();
    }

    @Override
    public String toString() {
        return name.get();
    }
}
