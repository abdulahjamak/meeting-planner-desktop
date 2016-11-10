package meetings;


import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.lang.ref.WeakReference;


public class ActivitiesView {
    private final Participant participant;

    public ActivitiesView(Participant participant) {
        this.participant = participant;
    }

    public Node getNode() {
        TabPane tabPane = new TabPane();
        tabPane.setPrefHeight(2000);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) oldValue.setContent(new Label("you should not see this"));
            if (newValue != null) newValue.setContent(new ActivityView((Activity) newValue.getUserData()).getNode());
        });

        DB.activities.forEach((activity) -> {
            Tab tab = new Tab();
            tab.setText(activity.getName());
            tab.setUserData(activity);
            tabPane.getTabs().add(tab);
            if (activity == participant.selectedActivity)
                tabPane.getSelectionModel().select(tab);
        });

        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) participant.selectedActivity = (Activity) newValue.getUserData();
        });

        return tabPane;
    }

    private class ActivityView {
        private final Activity activity;

        public ActivityView(Activity activity) {
            this.activity = activity;
        }

        public Node getNode() {
            FilteredSortedList<Participant> list =
                    new FilteredSortedList<>(FXCollections.observableArrayList(participant.getMeetables(activity)));
            ListView<Participant> checkProjects = new ListView<>(list.sortedData);
            checkProjects.setPrefHeight(2000);

            ObservableMap<Participant, ObservableValue<Boolean>> localCheckState = FXCollections.observableHashMap();
            ObservableList<Participant> selected = FXCollections.observableArrayList();
            for (Participant other : participant.getMeetables(activity)) {
                Boolean value = DB.isRequested(participant, other, activity);
                if (value) selected.add(other);
                BooleanProperty observable = new SimpleBooleanProperty(value);
                observable.addListener((obs, oldValue, newValue) -> {
                    if (newValue) {
                        selected.add(other);
                        DB.setRequested(participant, other, activity, true);
                    } else {
                        selected.remove(other);
                        DB.setRequested(participant, other, activity, false);
                        DB.meetings.remove(DB.getMeeting(participant, other, activity));
                    }
                });
                localCheckState.put(other, observable);
            }

            checkProjects.setCellFactory(CheckBoxListCell.forListView(new Callback<Participant, ObservableValue<Boolean>>() {
                @Override
                public ObservableValue<Boolean> call(Participant p) {
                    return localCheckState.get(p);
                }
            }));

            StackPane meetingPicker = new StackPane(new Label("Select a project"));

            ListView<Participant> selectedList = new ListView<>(selected);
            selectedList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (selected.size() == 1) {
                    selectedList.getSelectionModel().select(0);
                }
                if (newValue != null) {
                    participant.selectedParticipant.put(activity, new WeakReference<>(newValue));
                    meetingPicker.getChildren().setAll(new MeetingPlanner(participant, newValue, activity).getNode());
                } else {
                    meetingPicker.getChildren().setAll(new Label("Select a project"));
                }
            });

            if (participant.selectedParticipant.containsKey(activity)) {
                selectedList.scrollTo(participant.selectedParticipant.get(activity).get());
                selectedList.getSelectionModel().select(participant.selectedParticipant.get(activity).get());
            }

            HBox hbox = new HBox(10);
            hbox.setPadding(new Insets(10));
            hbox.getChildren().addAll(new VBox(10, checkProjects, list.getField()), selectedList, meetingPicker);

            return hbox;
        }
    }
}
