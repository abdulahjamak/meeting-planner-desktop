package meetings;

import java.util.Objects;

public class Meeting {
    public final Producer producer;
    public final Project project;
    public final TimeSlot timeSlot;

    public Meeting(Producer producer, Project project, TimeSlot timeSlot) {
        this.producer = producer;
        this.project = project;
        this.timeSlot = timeSlot;
    }

    public Meeting(Participant first, Participant second, TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
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

    public Participant other(Participant requesting) {
        if (requesting instanceof Producer) {
            return project;
        } else {
            return producer;
        }
    }

    public Participant same(Participant requesting) {
        if (requesting instanceof Producer) {
            return producer;
        } else {
            return project;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Meeting other = (Meeting) obj;
        return this.timeSlot.equals(other.timeSlot) &&
                this.project.equals(other.project) &&
                this.producer.equals(other.producer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.timeSlot, this.project, this.producer);
    }


    @Override
    public String toString() {
        return System.identityHashCode(this) + ": " + timeSlot.toString() + ": " + producer.getName() + " with " + project.getName();
    }

}
