package meetings;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import org.json.JSONObject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Producer extends Participant {
    private final SimpleStringProperty company = new SimpleStringProperty("");
    public final SimpleStringProperty id = new SimpleStringProperty("");
    public final DateTimePicker arrival = new DateTimePicker();
    public final DateTimePicker departure = new DateTimePicker();

    public Producer() {
        arrival.set(LocalDateTime.of(LocalDate.now().minusMonths(1), LocalTime.of(9, 0)));
        departure.set(LocalDateTime.of(LocalDate.now().plusMonths(1), LocalTime.of(9, 0)));
    }

    public Producer(JSONObject obj) {
        name.set(obj.getString("name"));
        email.set(obj.optString("email"));
        company.set(obj.optString("company"));
        id.set(obj.optString("id"));
        arrival.set(LocalDateTime.of(LocalDate.now().minusMonths(1), LocalTime.of(9, 0)));
        departure.set(LocalDateTime.of(LocalDate.now().plusMonths(1), LocalTime.of(9, 0)));
        if (obj.has("arrival")) {
            arrival.set(LocalDateTime.parse(obj.getString("arrival")));
            departure.set(LocalDateTime.parse(obj.getString("departure")));
        } else if (obj.has("arrivalimport")) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy H:mm");
            if (!obj.getString("arrivalimport").isEmpty()) {
                arrival.set(LocalDateTime.parse(obj.getString("arrivalimport") + " 6:00", formatter));
                departure.set(LocalDateTime.parse(obj.getString("departureimport") + " 23:00", formatter));
            }

        } else if (obj.has("oldone")) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/dd/yyyy H:mm");
            if (!obj.getString("arrivalimport").isEmpty()) {
                arrival.set(LocalDateTime.parse("" + obj.getString("arrivalimport"), formatter));
                departure.set(LocalDateTime.parse("" + obj.getString("departureimport"), formatter));
            }
        } else if (obj.has("arrivald")) {
            if (!obj.getString("arrivald").isEmpty()) {
                arrival.set(LocalDateTime.of(
                        LocalDate.parse(obj.getString("arrivald"), DateTimeFormatter.ofPattern("M/d/yyyy")),
                        LocalTime.parse(obj.getString("arrivalt"), DateTimeFormatter.ofPattern("H:mm"))));
                departure.set(LocalDateTime.of(
                        LocalDate.parse(obj.getString("departured"), DateTimeFormatter.ofPattern("M/d/yyyy")),
                        LocalTime.parse(obj.getString("departuret"), DateTimeFormatter.ofPattern("H:mm"))));
            }
        }
        if (obj.has("events")) {
            for (Event event : DB.events) {
                if (obj.getString("events").contains(event.importName.get())) {
                    event.participants.add(this);
                }
            }

        }
        if (obj.has("projects")) {
            for (Project project : DB.projects) {
                if (obj.getString("projects").contains(project.getImportID())) {
                    DB.requests.add(new Request(this, project, DB.activities.get(0)));
                    project.count();
                }
            }
            this.count();
        }
    }

    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("name", name.get());
        obj.put("id", id.get());
        obj.put("email", email.get());
        obj.put("company", company.get());
        obj.put("arrival", arrival.get().toString());
        obj.put("departure", departure.get().toString());
        return obj;
    }

    @Override
    public ArrayList<Participant> getMeetables(Activity activity) {
        return new ArrayList<>(DB.projects);
    }

    @Override
    String generateInfo(Meeting meeting) {
        return meeting.project.getName();
    }

    @Override
    ArrayList<Property> getProperties() {
        ArrayList<Property> list = new ArrayList<>();
        list.add(new Property("Company", company));
        list.add(new Property("ID", id));
        return list;
    }

    @Override
    void additionalItems(GridPane grid, int position) {
        grid.add(new Label("Arrival: "), 0, position);
        grid.add(arrival.getNode(), 1, position);
        position++;
        grid.add(new Label("Departure: "), 0, position);
        grid.add(departure.getNode(), 1, position);
    }

    @Override
    public Participant other(Meeting meeting) {
        return null;
    }

    @Override
    public Path generate() {
        Path file = Paths.get(Main.exportDir + "/participants/" + Export.sanitizeName(getName()) + ".pdf");
        new PDFGenerator(file).generate(this);
        return file;
    }

    public String getCompany() {
        return company.get();
    }

    public void setCompany(String company) {
        this.company.set(company);
    }

    public SimpleStringProperty companyProperty() {
        return company;
    }


}
