package meetings;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Verify {
    private final StringBuilder reasonText = new StringBuilder();
    private final List<Node> nodes = new ArrayList<>();
    public final Set<Color> color = new HashSet<>();
    public String reasons;
    public boolean generateColors = false;
    private boolean generateHyperlinks = false;
    public boolean result = true;
    private Stage stage;

    public Verify verify(Participant participant, TimeSlot timeSlot, boolean isRequesting) {
        if (participant instanceof Producer) {
            Producer prod = (Producer) participant;
            if (timeSlot.start.isBefore(prod.arrival.get())) {
                if (generateColors) color.add(Color.valueOf("888")); //gray
                String text = participant.getName() + " has not yet arrived";
                reasonText.append(text).append("\n");
                if (generateHyperlinks) nodes.add(new Label(text));
            }
            if (timeSlot.end.isAfter(prod.departure.get())) {
                if (generateColors) color.add(Color.valueOf("888")); //gray
                String text = participant.getName() + " has left";
                reasonText.append(text).append("\n");
                if (generateHyperlinks) nodes.add(new Label(text));
            }
        }

        DB.meetings.stream()
                .filter(meeting -> timeSlot.isOverlapping(meeting.timeSlot))
                .filter(meeting -> meeting.has(participant))
                .forEach(meeting -> {
                    String text = meeting.other(participant).getName() + " already has meeting with " +
                            participant.getName();
                    if (generateColors) color.add(Color.valueOf(isRequesting ? "fba71b" : "f3622d")); //red and yellow
                    reasonText.append(text).append("\n");
                    if (generateHyperlinks) {
                        Hyperlink link = new Hyperlink(text);
                        link.setOnAction(e -> {
                            meeting.other(participant).selectedActivity = meeting.timeSlot.activity;
                            meeting.other(participant).selectedParticipant.put(meeting.timeSlot.activity,
                                    new WeakReference<>(participant));
                            stage.close();
                            Main.open(meeting.other(participant));
                        });
                        nodes.add(link);
                    }
                });
        DB.events.stream()
                .filter(event -> timeSlot.isOverlapping(event.timeSlot))
                .filter(event -> event.has(participant))
                .forEach(event -> {
                    if (generateColors)
                        color.add(Color.valueOf(isRequesting ? "BCE1EC" : "41a9c9")); //blue and light blue
                    String text = participant.getName() + " is attending event " + event.getName();
                    reasonText.append(text).append("\n");
                    if (generateHyperlinks) nodes.add(new Label(text));
                });
        if (reasonText.length() != 0) result = false;
        reasons = reasonText.toString().trim();
        return this;
    }

    public Verify verify(Participant requesting, Participant other, TimeSlot ts) {
        verify(requesting, ts, false);
        verify(other, ts, true);
        return this;
    }

    public Verify window(Participant requesting, Participant other, TimeSlot ts) {
        stage = new Stage();
        generateHyperlinks = true;
        verify(requesting, ts, false);
        verify(other, ts, true);
        stage.setTitle("Meeting");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UTILITY);
        VBox root = new VBox(5, nodes.toArray(new Node[0]));
        root.setPadding(new Insets(10));
        stage.setScene(new Scene(root));
        stage.show();
        return this;
    }

}
