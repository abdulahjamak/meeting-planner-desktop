package meetings;

public class ScheduleEntry {
    final String activity;
    final String info;
    final String location;
    final TimeSlot timeSlot;

    ScheduleEntry(String activity, String info, String location, TimeSlot timeSlot) {
        this.activity = activity;
        this.info = info;
        this.location = location;
        this.timeSlot = timeSlot;
    }
}
