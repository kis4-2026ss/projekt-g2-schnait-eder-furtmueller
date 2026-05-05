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
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerAbandonEventTest {

    private LocalDateTime baseTime;
    private FakeCheckout checkout;
    private FakeSupermarket market;
    private FakeCustomer customer;
    private FakeSimulation sim;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.of(2025, 1, 1, 10, 0);
        checkout = new FakeCheckout();
        customer = new FakeCustomer("Alice");
        market = new FakeSupermarket();
        sim = new FakeSimulation();
    }

    @Test
    void testCustomerAbandonsSuccessfully() {
        checkout.queue.add(customer);
        CustomerAbandonEvent event = new CustomerAbandonEvent(baseTime, market, checkout, customer);

        event.process(sim);

        assertFalse(checkout.queue.contains(customer), "Customer should have been removed from queue");
        assertTrue(market.loggedMessage.contains("abandons queue"), "Abandon log should be produced");
    }

    @Test
    void testCustomerNotInQueue() {
        CustomerAbandonEvent event = new CustomerAbandonEvent(baseTime, market, checkout, customer);

        event.process(sim);

        assertEquals(0, checkout.queue.size(), "Queue should remain empty");
        assertNull(market.loggedMessage, "No log expected when not in queue");
    }


    private static class FakeSimulation extends Simulation { }

    private static class FakeCheckout extends Checkout {
        LinkedList<Customer> queue = new LinkedList<>();

        public FakeCheckout() { super(1); }
        @Override public Queue<Customer> getQueue() { return queue; }
        @Override public String toString() { return "Checkout#" + getId(); }
    }

    private static class FakeCustomer extends Customer {
        private final String name;
        public FakeCustomer(String name) {
            super(LocalDateTime.now());
            this.name = name;
        }
        @Override public String toString() { return name; }
    }

    /** Minimal fake supermarket that just records log messages. */
    private static class FakeSupermarket extends Supermarket {
        String loggedMessage = null;

        public FakeSupermarket() {
            super(new SimulationParameters(1,
                    1.0,
                    1.0,
                    0.1,
                    10.0,
                    600,
                    false));
        }
        @Override public void log(String msg) { this.loggedMessage = msg; }
    }
}
