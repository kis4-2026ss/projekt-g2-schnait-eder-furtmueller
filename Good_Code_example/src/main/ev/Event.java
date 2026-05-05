package ev;

import sim.Simulation;

import java.time.LocalDateTime;

public abstract class Event implements Comparable<Event> {
    protected final LocalDateTime time;

    protected Event(LocalDateTime time) {
        this.time = time;
    }

    public LocalDateTime getTime() { return time; }

    public abstract void process(Simulation sim);

    @Override
    public int compareTo(Event other) { return this.time.compareTo(other.time); }
}

