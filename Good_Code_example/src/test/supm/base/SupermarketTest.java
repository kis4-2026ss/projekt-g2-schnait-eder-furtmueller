package supm.base;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import supm.parts.Checkout;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SupermarketTest {

    Supermarket market;
    SimulationParameters params;

    @BeforeEach
    void setUp() {
        params = new SimulationParameters(
                3, 0.5, 3.0, 1.0, 10.0, 60, false
        );
        market = new Supermarket(params);
        market.getRandom().setSeed(123);
    }

    @Test
    void testFindShortestQueueEmpty() {
        Checkout shortest = market.findShortestQueue();
        assertNotNull(shortest);
        assertEquals(0, shortest.queueLength());
    }

    @Test
    void testNextInterarrivalTimePositive() {
        double t = market.nextInterarrivalTime();
        assertTrue(t > 0);
    }

    @Test
    void testNextServiceTimePositive() {
        double s = market.nextServiceTime();
        assertTrue(s >= 0.1);
    }

    @Test
    void testCheckoutsCreated() {
        List<Checkout> cashiers = market.getCheckout();
        assertEquals(params.numCheckouts, cashiers.size());
    }

    @Test
    void testFindShortestQueueThrowsWhenNoCheckouts() {
        params.numCheckouts = 0;

        Supermarket marketC = new Supermarket(params);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                marketC::findShortestQueue,
                "Expected findShortestQueue() to throw when no checkouts exist"
        );

        assertEquals("Error: No checkouts in store", ex.getMessage());
    }

    @Test
    void testNextInterarrivalTimeThrowsWhenRateInvalid() {
        params.meanArrivalRate = 0;  // invalid for constant

        Supermarket marketA = new Supermarket(params);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                marketA::nextInterarrivalTime,
                "Expected nextInterarrivalTime() to throw when meanArrivalRate <= 0"
        );

        assertEquals("Constant customer arrival rate needs to be larger than 0", ex.getMessage());
    }


}
