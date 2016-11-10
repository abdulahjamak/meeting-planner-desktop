package meetings;

import java.util.Objects;

public class Request {
    final Producer producer;
    final Project project;
    final Activity activity;

    public Request(Participant first, Participant second, Activity activity) {
        this.activity = activity;
        if (first instanceof Producer) {
            this.producer = (Producer) first;
            this.project = (Project) second;
        } else {
            this.producer = (Producer) second;
            this.project = (Project) first;
        }
    }

    public boolean has(Participant participant) {
        return producer.equals(participant) || project.equals(participant);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Request other = (Request) obj;
        return this.activity.equals(other.activity) &&
                this.project.equals(other.project) &&
                this.producer.equals(other.producer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.project, this.producer, this.activity);
    }

    public boolean equalsMeeting(Meeting meeting) {
        return meeting.project.equals(project) &&
                meeting.producer.equals(producer) &&
                meeting.timeSlot.activity.equals(activity);
    }
}
