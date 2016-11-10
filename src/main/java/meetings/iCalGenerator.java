package meetings;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.component.VVenue;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.util.UidGenerator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

public class iCalGenerator {
    static TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
    static TimeZone timeZone = registry.getTimeZone(TimeZone.getDefault().getID());

    static public void generate (Participant p, List<ScheduleEntry> list) {
        Path path = Paths.get(Main.exportDir + "/cals/" + Export.sanitizeName(p.getName()) + ".ics");
        try {
            Files.createDirectories(path.getParent());

            Calendar calendar = new Calendar();
            calendar.getProperties().add(new ProdId("-//SFF Meetings Scheduler//iCal4j 1.0//EN"));
            calendar.getProperties().add(Version.VERSION_2_0);
            calendar.getProperties().add(CalScale.GREGORIAN);
            UidGenerator ug = new UidGenerator("uidGen");
            for (ScheduleEntry event : list) {
                Instant startInstant = event.timeSlot.start.atZone(ZoneId.systemDefault()).toInstant();
                DateTime start = new DateTime(Date.from(startInstant));
                //start.setTimeZone(timeZone);
                Instant endInstant = event.timeSlot.end.atZone(ZoneId.systemDefault()).toInstant();
                DateTime end = new DateTime(Date.from(endInstant));
                //end.setTimeZone(timeZone);
                VEvent meeting = new VEvent(start, end, event.activity);
                meeting.getProperties().add(ug.generateUid());
                meeting.getProperties().add(new Location(event.location));
                meeting.getProperties().add(new Description(event.info));
                calendar.getComponents().add(meeting);
            }

            new CalendarOutputter().output(calendar, Files.newBufferedWriter(path));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
