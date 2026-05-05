package sim;

import ev.Event;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import util.AdjustableClock;

import java.time.LocalDateTime;

public class SimulationTest {

    static class TestEvent extends Event {

        TestEvent(LocalDateTime time) {
            super(time);
        }

        @Override
        public void process(Simulation sim) {
            System.out.println("This is a test Event!");
        }
    }

    @Test
    void step_advancesCurrentTime() {
        AdjustableClock clock = new AdjustableClock();
        Simulation sim = new Simulation(clock);

        LocalDateTime start = sim.now();
        LocalDateTime future = start.plusSeconds(10);

        sim.addEvent(new TestEvent(future));
        sim.step();

        Assertions.assertEquals(future, sim.now());
    }


    @Test
    void step_onEmptyQueue_doesNotThrow() {
        Simulation sim = new Simulation(new AdjustableClock());
        Assertions.assertDoesNotThrow(sim::step);
    }

    @Test
    void run_stopsBeforeFutureEvents() {
        AdjustableClock clock = new AdjustableClock();
        Simulation sim = new Simulation(clock);

        LocalDateTime t1 = sim.now().plusSeconds(2);
        LocalDateTime t2 = sim.now().plusSeconds(100); // too far ahead

        sim.addEvent(new TestEvent(t1));
        sim.addEvent(new TestEvent(t2));

        sim.run(sim.now().plusSeconds(10));
    }
}
