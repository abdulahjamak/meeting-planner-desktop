package meetings;

import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

import java.util.Optional;

public class Activities {
    private static final TabPane tabPane = new TabPane();

    public static Node getNode() {
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Button newActivity = new Button("Add Activity");
        newActivity.setOnAction(new EventHandler<ActionEvent>() {
            int number = 0;

            @Override
            public void handle(ActionEvent e) {
                number++;
                TextInputDialog dialog = new TextInputDialog("Activity " + number);
                dialog.initStyle(StageStyle.UTILITY);
                dialog.setHeaderText("");
                dialog.setTitle("New Activity");
                dialog.setContentText("Name: ");
                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()) {
                    Activity a = new Activity();
                    a.setName(result.get());
                    DB.activities.add(a);
                }
            }

        });

        DB.activities.addListener((ListChangeListener<Activity>) c -> {
            while (c.next()) {
                for (Activity activity : c.getAddedSubList()) {
                    addActivityTab(activity);
                }
                for (Activity activity : c.getRemoved()) {
                    tabPane.getTabs().remove(tabPane.getTabs().stream()
                            .filter(tab -> tab.getUserData().equals(activity))
                            .findFirst().get());
                }
            }
        });

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(newActivity, tabPane);

        return root;
    }

    private static void addActivityTab(Activity activity) {
        Tab newTab = new Tab();
        newTab.textProperty().bind(activity.nameProperty());
        newTab.setUserData(activity);
        newTab.setContent(activity.getNode());
        tabPane.getTabs().add(newTab);
    }
}
