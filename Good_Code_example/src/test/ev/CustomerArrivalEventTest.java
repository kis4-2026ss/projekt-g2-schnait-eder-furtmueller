package ev;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import supm.base.Supermarket;
import supm.base.SimulationParameters;
import supm.parts.Checkout;
import supm.parts.Customer;
import sim.Simulation;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerArrivalEventTest {

    private FakeSupermarket market;
    private FakeSimulation sim;
    private FakeCheckout checkout;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        market = new FakeSupermarket();
        sim = new FakeSimulation();
        checkout = new FakeCheckout();
        baseTime = LocalDateTime.of(2025, 1, 1, 10, 0);

        market.setCheckout(checkout);
    }

    @Test
    void testCustomerStartsServiceImmediately() {
        checkout.busy = false; // free

        CustomerArrivalEvent event = new CustomerArrivalEvent(baseTime, market);
        event.process(sim);

        assertTrue(checkout.busy, "Checkout should be marked busy");
        assertEquals(baseTime, checkout.busySince, "Checkout start time should match event time");
        assertTrue(sim.addedEvents.stream().anyMatch(e -> e instanceof ServiceEndEvent),
                "ServiceEndEvent should be scheduled");
        assertTrue(sim.addedEvents.stream().anyMatch(e -> e instanceof CustomerArrivalEvent),
                "Next CustomerArrivalEvent should be scheduled");
    }


    @Test
    void testCustomerQueuesWhenCheckoutBusy() {
        checkout.busy = true;

        CustomerArrivalEvent event = new CustomerArrivalEvent(baseTime, market);

        event.process(sim);

        assertEquals(1, checkout.queue.size(), "Customer should be added to queue");
        assertTrue(sim.addedEvents.stream().anyMatch(e -> e instanceof CustomerAbandonEvent),
                "CustomerAbandonEvent should be scheduled");
        assertTrue(sim.addedEvents.stream().anyMatch(e -> e instanceof CustomerArrivalEvent),
                "Next CustomerArrivalEvent should be scheduled");
    }


    private static class FakeSimulation extends Simulation {
        public final List<Event> addedEvents = new ArrayList<>();
        private final LocalDateTime now = LocalDateTime.of(2025, 1, 1, 10, 0);

        @Override
        public void addEvent(Event e) { addedEvents.add(e); }

        @Override
        public LocalDateTime now() { return now; }
    }

    private static class FakeCheckout extends Checkout {
        boolean busy = false;
        LocalDateTime busySince = null;
        LinkedList<Customer> queue = new LinkedList<>();

        public FakeCheckout() { super(1); }

        @Override public boolean isBusy() { return busy; }
        @Override public void setBusy(boolean val) { this.busy = val; }
        @Override public void startBusy(LocalDateTime t) { this.busySince = t; }
        @Override public int queueLength() { return queue.size(); }
        @Override public Queue<Customer> getQueue() { return queue; }
        @Override public String toString() { return "Checkout#" + getId(); }
    }

    private static class FakeSupermarket extends Supermarket {
        boolean throwOnFind = false;
        FakeCheckout checkout;

        public FakeSupermarket() {
            super(new SimulationParameters(1,
                    1.0,
                    1.0,
                    0.1,
                    10.0,
                    600,
                    false));
        }

        public void setCheckout(FakeCheckout c) {
            this.checkout = c;
        }

        @Override
        public Checkout findShortestQueue() {
            if (throwOnFind) throw new IllegalStateException("No checkouts");
            return checkout;
        }

        @Override
        public double nextServiceTime() { return 1.0; } // minutes
        @Override
        public double nextInterarrivalTime() { return 2.0; } // minutes
        @Override
        public void log(String msg) { /* ignore logging */ }
    }
}
