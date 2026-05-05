package ev;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sim.Simulation;
import supm.base.SimulationParameters;
import supm.base.Supermarket;
import supm.parts.Checkout;
import supm.parts.Customer;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceEndEventTest {

    private LocalDateTime baseTime;
    private FakeCheckout checkout;
    private FakeCustomer customer;
    private FakeSupermarket market;
    private FakeSimulation sim;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.of(2025, 1, 1, 10, 0);
        checkout = new FakeCheckout();
        customer = new FakeCustomer("Bob");
        market = new FakeSupermarket();
        sim = new FakeSimulation();
    }

    @Test
    void testCheckoutBecomesIdleWhenQueueEmpty() {
        checkout.busy = true;
        ServiceEndEvent event = new ServiceEndEvent(baseTime, market, checkout, customer);

        event.process(sim);

        assertFalse(checkout.busy, "Checkout should become idle when no queue left");
        assertTrue(market.loggedMessage.contains("finished at Checkout"), "Should log service end");
        assertTrue(sim.addedEvents.isEmpty(), "No new ServiceEndEvent should be scheduled");
    }


    @Test
    void testNextCustomerStartsServiceWhenQueueNotEmpty() {
        FakeCustomer nextCustomer = new FakeCustomer("Alice");
        checkout.queue.add(nextCustomer);
        checkout.busy = true;

        ServiceEndEvent event = new ServiceEndEvent(baseTime, market, checkout, customer);
        event.process(sim);

        assertNotNull(nextCustomer.startServiceT, "Next customer should have start time set");
        assertTrue(sim.addedEvents.stream().anyMatch(e -> e instanceof ServiceEndEvent),
                "Next ServiceEndEvent should be scheduled");
        assertTrue(market.loggedMessage.contains("starts service"), "Should log next service start");
        assertTrue(checkout.busy, "Checkout should remain busy");
    }

    private static class FakeSimulation extends Simulation {
        List<Event> addedEvents = new LinkedList<>();
        @Override public void addEvent(Event e) { addedEvents.add(e); }
    }

    private static class FakeCheckout extends Checkout {
        boolean busy = false;
        LocalDateTime busySince;
        LinkedList<Customer> queue = new LinkedList<>();

        public FakeCheckout() { super(1); }
        @Override public boolean isBusy() { return busy; }
        @Override public void setBusy(boolean val) { busy = val; }
        @Override public void startBusy(LocalDateTime t) { busySince = t; }
        @Override public Queue<Customer> getQueue() { return queue; }
        @Override public int queueLength() { return queue.size(); }
        @Override public LocalDateTime getStartBusy() { return busySince; }
        @Override public String toString() { return "Checkout#" + getId(); }
    }

    private static class FakeCustomer extends Customer {
        final String name;
        LocalDateTime startServiceT;

        public FakeCustomer(String name) {
            super(LocalDateTime.of(2025, 1, 1, 9, 0)); // arrival time
            this.name = name;
        }

        @Override public void setStartServiceT(LocalDateTime t) { this.startServiceT = t; }
        @Override public LocalDateTime getStartServiceT() { return startServiceT; }
        @Override public String toString() { return name; }
    }

    private static class FakeSupermarket extends Supermarket {
        String loggedMessage = "";

        public FakeSupermarket() {
            super(new SimulationParameters(1,
                    1.0,
                    1.0,
                    0.1,
                    10.0,
                    600,
                    false));
        }

        @Override
        public void log(String msg) { this.loggedMessage = msg; }

        @Override
        public double nextServiceTime() { return 1.0; } // deterministic
    }
}
