package sim;

import ev.Event;
import util.AdjustableClock;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.PriorityQueue;

public class Simulation {
    private final PriorityQueue<Event> queue = new PriorityQueue<>();
    private final Clock clock;
    private LocalDateTime currentTime;


    public Simulation() {
        this(Clock.systemDefaultZone());
    }

    public Simulation(Clock clock) {
        this.clock = clock;
        this.currentTime = LocalDateTime.now(clock);
    }


    public LocalDateTime now() {
        return currentTime;
    }


    public void addEvent(Event e) {
        queue.add(e);
    }


    public void step() {
        Event e = queue.poll();

        if (e == null) return;

        if (e.getTime().isAfter(currentTime)) {
            Duration timePassed = Duration.between(currentTime, e.getTime());
            currentTime = e.getTime();
            if (clock instanceof AdjustableClock adj) {
                adj.incrementTime(timePassed);
            }
        }

        e.process(this);
    }

    public void run(LocalDateTime until) {
        while (!queue.isEmpty()) {
            Event e = queue.peek();
            if (e.getTime().isAfter(until)) {
                break;
            }
            step();
        }
    }
}
