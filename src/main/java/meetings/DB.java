package meetings;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DB {
    private DB() {
    }

    public static final ArrayList<Meeting> meetings = new ArrayList<>();
    public static final ArrayList<Request> requests = new ArrayList<>();
    public static final ObservableList<Event> events = FXCollections.observableArrayList();
    public static final ObservableList<Producer> producers = FXCollections.observableArrayList();
    public static final ObservableList<Project> projects = FXCollections.observableArrayList();
    public static final ObservableList<Activity> activities = FXCollections.observableArrayList();

    public static void clear() {
        Main.closeTabs();
        producers.clear();
        projects.clear();
        activities.clear();
        meetings.clear();
        requests.clear();
        events.clear();
    }

    public static void saveNew(Path path) {
        try (BufferedWriter out = Files.newBufferedWriter(path)) {
            List<TimeSlot> allTimeSlots = getAllTimeSlots();
            for (Activity activity : activities) {
                out.write("activity:" + activity.toJSON());
                out.newLine();
                out.write("timeslots:");
                out.newLine();
                for (TimeSlot ts : activity.timeSlots) {
                    out.write(Integer.toString(allTimeSlots.indexOf(ts)));
                    out.write(":");
                    out.write(ts.toJSON().toString());
                    out.newLine();
                }
            }
            out.write("producers:");
            out.newLine();
            for (Producer p : producers) {
                out.write(p.toJSON().toString());
                out.newLine();
            }
            out.write("projects:");
            out.newLine();
            for (Project p : projects) {
                out.write(p.toJSON().toString());
                out.newLine();
            }
            out.write("events:");
            out.newLine();
            for (Event p : events) {
                out.write(p.toJSON().toString());
                out.newLine();
            }
            out.write("requests:");
            out.newLine();
            for (Request r : requests) {
                out.write(Integer.toString(producers.indexOf(r.producer)));
                out.write(":");
                out.write(Integer.toString(projects.indexOf(r.project)));
                out.write(":");
                out.write(Integer.toString(activities.indexOf(r.activity)));
                out.newLine();
            }
            out.write("meetings:");
            out.newLine();
            for (Meeting m : meetings) {
                out.write(Integer.toString(producers.indexOf(m.producer)));
                out.write(":");
                out.write(Integer.toString(projects.indexOf(m.project)));
                out.write(":");
                if (allTimeSlots.contains(m.timeSlot)) {
                    out.write(Integer.toString(allTimeSlots.indexOf(m.timeSlot)));
                } else {
                    out.write(m.timeSlot.toJSON().toString());
                }
                out.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void openNew(Path path) {
        try (BufferedReader in = Files.newBufferedReader(path)) {
            Map<Integer, TimeSlot> timeSlots = new HashMap<>();

            clear();
            String line;
            line = in.readLine();
            activityLoop:
            while (line.startsWith("activity:")) {
                Activity activity = new Activity(new JSONObject(line.substring("activity:".length(), line.length())));
                activities.add(activity);
                in.readLine();
                while (true) {
                    line = in.readLine();
                    if (line.equals("producers:")) break activityLoop;
                    if (line.startsWith("activity:")) break;
                    int separator = line.indexOf(':');
                    TimeSlot timeSlot = new TimeSlot(new JSONObject(line.substring(separator + 1, line.length())));
                    timeSlots.put(Integer.parseInt(line.substring(0, separator)), timeSlot);
                    activity.timeSlots.add(timeSlot);
                }
            }
            while (!(line = in.readLine()).equals("projects:")) {
                producers.add(new Producer(new JSONObject(line)));
            }
            while (!(line = in.readLine()).equals("events:")) {
                projects.add(new Project(new JSONObject(line)));
            }
            while (!(line = in.readLine()).equals("requests:")) {
                events.add(new Event(new JSONObject(line)));
            }
            while (!(line = in.readLine()).equals("meetings:")) {
                String[] strings = line.split(":");
                Producer producer = producers.get(Integer.parseInt(strings[0]));
                Project project = projects.get(Integer.parseInt(strings[1]));
                Activity activity = activities.get(Integer.parseInt(strings[2]));
                requests.add(new Request(producer, project, activity));
            }
            while ((line = in.readLine()) != null) {
                String[] strings = line.split(":");
                Producer producer = producers.get(Integer.parseInt(strings[0]));
                Project project = projects.get(Integer.parseInt(strings[1]));
                TimeSlot timeSlot;
                if (!strings[2].startsWith("{")) {
                    timeSlot = timeSlots.get(Integer.parseInt(strings[2]));
                } else {
                    int start = (strings[0] + ":" + strings[1] + ":").length();
                    timeSlot = new TimeSlot(new JSONObject(line.substring(start, line.length())));
                }
                meetings.add(new Meeting(producer, project, timeSlot));
            }
            DB.producers.forEach(Producer::count);
            DB.projects.forEach(Project::count);
        } catch (IOException e) {
            error(e.getMessage());
            e.printStackTrace();
        }
    }

    private static void error(String error) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText("Database Error");
        a.setContentText(error);
        a.show();
    }

    public static Meeting getMeeting(Participant first, Participant second, Activity activity) {
        for (Meeting meeting : meetings) {
            if (activity.equals(meeting.timeSlot.activity)
                    && (first.equals(meeting.producer) && second.equals(meeting.project)
                    || first.equals(meeting.project) && second.equals(meeting.producer)))
                return meeting;
        }
        return null;
    }

    public static void setMeeting(Participant first, Participant second, TimeSlot timeSlot) {
        meetings.remove(getMeeting(first, second, timeSlot.activity));
        if (first instanceof Producer) {
            meetings.add(new Meeting((Producer) first, (Project) second, timeSlot));
        } else {
            meetings.add(new Meeting((Producer) second, (Project) first, timeSlot));
        }
        first.count();
        second.count();
    }

    private static List<TimeSlot> getAllTimeSlots() {
        List<TimeSlot> list = new ArrayList<>();
        for (Activity a : activities) {
            list.addAll(a.timeSlots);
        }
        return list;
    }

    public static boolean isRequested(Participant first, Participant second, Activity activity) {
        Request request = new Request(first, second, activity);
        for (Request r : requests) {
            if (request.equals(r)) return true;
        }
        return false;
    }

    public static void setRequested(Participant first, Participant second, Activity activity, boolean requested) {
        Request request = new Request(first, second, activity);
        if (requested) {
            requests.add(request);
        } else {
            requests.remove(request);
        }
        first.count();
        second.count();
    }
}