package meetings;

import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class TimeSlot {
    public final LocalDateTime start;
    public final LocalDateTime end;
    public final Activity activity;

    public TimeSlot(LocalDateTime start, LocalDateTime end, Activity activity) {
        this.start = start;
        this.end = end;
        this.activity = activity;
    }

    public TimeSlot(JSONObject obj) {
        start = LocalDateTime.parse(obj.getString("start"));
        end = LocalDateTime.parse(obj.getString("end"));
        activity = DB.activities.get(obj.getInt("activity"));
    }

    JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("start", start.toString());
        obj.put("end", end.toString());
        obj.put("activity", DB.activities.indexOf(activity));
        return obj;
    }

    public LocalDate date() {
        return start.toLocalDate();
    }

    public boolean isOverlapping(TimeSlot ts) {
        return start.isBefore(ts.end) && ts.start.isBefore(end);
    }

    public String getButtonText() {
        return start.format(DateTimeFormatter.ofPattern("HH:mm"))
                + " - " + end.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    @Override
    public String toString() {
        return start.format(DateTimeFormatter.ofPattern("E d/M HH:mm"))
                + " - " + end.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TimeSlot other = (TimeSlot) obj;
        return this.start.equals(other.start) &&
                this.end.equals(other.end) &&
                this.activity.equals(other.activity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.start, this.end, this.activity);
    }
}
