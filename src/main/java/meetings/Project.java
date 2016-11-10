package meetings;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.GridPane;
import org.json.JSONObject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Project extends Participant {
    private final SimpleStringProperty importID = new SimpleStringProperty("");

    public Project() {
    }

    public Project(JSONObject obj) {
        name.set(obj.getString("name"));
        email.set(obj.optString("email"));
        if (obj.has("importid")) {
            setImportID(obj.getString("importid"));
        } else {
            setImportID(obj.getString("name"));
        }
    }

    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("name", name.get());
        obj.put("email", email.get());
        if (!importID.get().isEmpty()) {
            obj.put("importid", getImportID());
        }
        return obj;
    }

    @Override
    public ArrayList<Participant> getMeetables(Activity activity) {
        return new ArrayList<>(DB.producers);
    }

    @Override
    String generateInfo(Meeting meeting) {
        return meeting.producer.getName() + "\n" + meeting.producer.getCompany();
    }

    @Override
    ArrayList<Property> getProperties() {
        ArrayList<Property> list = new ArrayList<>();
        list.add(new Property("Import ID: ", importID));
        return list;
    }

    @Override
    void additionalItems(GridPane grid, int position) {

    }

    @Override
    public Participant other(Meeting meeting) {
        return null;
    }

    @Override
    public Path generate() {
        Path file = Paths.get(Main.exportDir + "/projects/" + Export.sanitizeName(getName()) + ".pdf");
        new PDFGenerator(file).generate(this);
        return file;
    }

    public String getImportID() {
        return importID.get();
    }

    private void setImportID(String importID) {
        this.importID.set(importID);
    }
}
