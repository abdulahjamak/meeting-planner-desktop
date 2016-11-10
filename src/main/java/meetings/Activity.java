package meetings;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.json.JSONObject;

public class Activity {
    private final SimpleStringProperty name = new SimpleStringProperty("");
    private final SimpleStringProperty location = new SimpleStringProperty("");
    public final ObservableList<TimeSlot> timeSlots = FXCollections.observableArrayList();

    private TimeSlots timeSlotsView = new TimeSlots(this);

    public Activity() {
    }

    public Activity(JSONObject obj) {
        setName(obj.getString("name"));
        location.set(obj.getString("location"));
    }

    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("name", getName());
        obj.put("location", location.get());
        return obj;
    }

    public Node getNode() {
        TextField nameField = new TextField();
        nameField.textProperty().bindBidirectional(name);
        TextField locField = new TextField();
        locField.textProperty().bindBidirectional(location);
        //todo add filters
        HBox hbox = new HBox(20);
        hbox.getChildren().addAll(timeSlotsView.getNode());

        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(new HBox(new Label("Name: "), nameField),
                new HBox(new Label("Location: "), locField), hbox);
        return root;
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

}
