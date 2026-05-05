package stats;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import supm.parts.Checkout;
import supm.parts.Customer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class SupermarketStatsTest {

    private SupermarketStats stats;
    private List<Checkout> checkouts;
    private LocalDateTime simStart;

    @BeforeEach
    void setup() {
        simStart = LocalDateTime.of(2025, 1, 1, 10, 0);
        checkouts = new ArrayList<>();
        checkouts.add(new Checkout(1));
        checkouts.add(new Checkout(2));

        stats = new SupermarketStats(checkouts,
                60,    // 60 min total simulation
                10,    // 10-min intervals
                5,     // max wait time
                simStart);
    }

    @Test
    void testCustomerServedIncrementsCount() {
        Checkout c1 = checkouts.getFirst();
        stats.customerServed(c1);
        stats.customerServed(c1);
        stats.customerServed(checkouts.get(1));

        // Verify internal counters
        assertEquals(2, getServedCount(stats, c1));
        assertEquals(1, getServedCount(stats, checkouts.get(1)));
    }

    @Test
    void testAddWaitTimeCalculations() {
        stats.addWaitTime(5);
        stats.addWaitTime(15);

        assertEquals(10.0, stats.getAverageWaitTime());
    }

    @Test
    void testCustomerAbandonedIncreasesWaitTime() {
        stats.customerAbandoned();
        stats.customerAbandoned();

        // 2 abandoned → both counted as waited customers
        assertEquals(5.0, stats.getAverageWaitTime()); // since maxWaitTime = 5
    }

    @Test
    void testCustomerQueuedUpdatesMaxQueueLength() {
        Checkout c1 = checkouts.getFirst();
        // Simulate queue growing
        c1.getQueue().add(new Customer(LocalDateTime.of(2025,1,1,10,0)));
        c1.getQueue().add(new Customer(LocalDateTime.of(2025,1,1,10,3)));
        stats.customerQueued(c1);

        // Shrink queue
        c1.getQueue().remove();
        stats.customerQueued(c1);

        assertEquals(2, getMaxQueue(stats, c1));
    }

    @Test
    void testRecordBusyTimeSingleInterval() {
        Checkout c1 = checkouts.getFirst();
        LocalDateTime start = simStart.plusMinutes(5);
        LocalDateTime end = simStart.plusMinutes(8);

        stats.recordBusyTime(c1, start, end);

        double recorded = getBusyTime(stats, c1, 0);
        assertEquals(3.0, recorded); // 3 minutes busy
    }

    @Test
    void testRecordBusyTimeOverlapsTwoIntervals() {
        Checkout c1 = checkouts.getFirst();
        LocalDateTime start = simStart.plusMinutes(8);
        LocalDateTime end = simStart.plusMinutes(15);

        stats.recordBusyTime(c1, start, end);

        double interval1 = getBusyTime(stats, c1, 0);
        double interval2 = getBusyTime(stats, c1, 1);

        // 8–10 = 2 minutes in interval 0, 10–15 = 5 minutes in interval 1
        assertEquals(2.0, interval1);
        assertEquals(5.0, interval2);
    }

    @Test
    void testFinalizeBusyTimesAddsBusyForActiveCheckouts() {
        Checkout c1 = checkouts.getFirst();
        c1.setBusy(true);
        c1.startBusy(simStart.plusMinutes(20));

        stats.finalizeBusyTimes(simStart.plusMinutes(30));
        assertTrue(getBusyTime(stats, c1, 2) > 0);
    }

    @Test
    void testGetUtilizationTableAverages() {
        Checkout c1 = checkouts.get(0);
        Checkout c2 = checkouts.get(1);

        stats.recordBusyTime(c1, simStart.plusMinutes(0), simStart.plusMinutes(10)); // full interval
        stats.recordBusyTime(c2, simStart.plusMinutes(0), simStart.plusMinutes(5));  // half interval

        List<List<Double>> table = stats.getUtilizationTable();
        List<Double> firstRow = table.getFirst();

        // Expect C1=1.0, C2=0.5, avg=0.75
        assertEquals(1.0, firstRow.get(0));
        assertEquals(0.5, firstRow.get(1));
        assertEquals(0.75, firstRow.get(2));
    }

    @SuppressWarnings("unchecked")
    private int getServedCount(SupermarketStats s, Checkout c) {
        try {
            var field = SupermarketStats.class.getDeclaredField("servedCustomers");
            field.setAccessible(true);
            var map = (java.util.Map<Checkout, Integer>) field.get(s);
            return map.get(c);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private int getMaxQueue(SupermarketStats s, Checkout c) {
        try {
            var field = SupermarketStats.class.getDeclaredField("maxQueueLength");
            field.setAccessible(true);
            var map = (java.util.Map<Checkout, Integer>) field.get(s);
            return map.get(c);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private double getBusyTime(SupermarketStats s, Checkout c, int interval) {
        try {
            var field = SupermarketStats.class.getDeclaredField("busyTimePerInterval");
            field.setAccessible(true);
            var map = (java.util.Map<Checkout, java.util.List<Double>>) field.get(s);
            return map.get(c).get(interval);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
