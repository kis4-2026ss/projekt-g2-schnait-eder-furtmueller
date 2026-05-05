package supm.statis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import stats.SupermarketStats;
import supm.base.SimulationParameters;
import supm.parts.Checkout;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SupermarketStatisticTest {

    private SupermarketStatistic statMarket;

    @BeforeEach
    void setUp() {
        SimulationParameters params = new SimulationParameters(
                2,
                1.0,
                1.0,
                4,
                3,
                120,
                false
        );


        // interval length = 30 minutes
        statMarket = new SupermarketStatistic(params, 30);
    }

    @Test
    void testStatsInitialization() {
        SupermarketStats stats = statMarket.getStats();
        assertNotNull(stats, "SupermarketStats should be initialized");
        assertEquals(2, getPrivateCheckoutsSize(stats), "Should track 2 checkouts");
    }

    @Test
    void testCustomerQueuedDelegation() {
        Checkout c = statMarket.getCheckout().getFirst();
        statMarket.customerQueued(c);

        int maxQueue = getPrivateMaxQueue(statMarket.getStats(), c);
        assertTrue(maxQueue >= 0, "Should record queue length for checkout");
    }

    @Test
    void testCustomerServedDelegation() {
        Checkout c = statMarket.getCheckout().getFirst();
        statMarket.customerServed(c);

        int served = getPrivateServed(statMarket.getStats(), c);
        assertEquals(1, served, "Should increment served count for checkout");
    }

    @Test
    void testCustomerAbandonedDelegation() {
        double beforeAvg = statMarket.getStats().getAverageWaitTime();
        statMarket.customerAbandoned();
        double afterAvg = statMarket.getStats().getAverageWaitTime();

        assertTrue(afterAvg >= beforeAvg, "Abandon should increase average wait time");
    }

    @Test
    void testAddWaitTimeDelegation() {
        LocalDateTime arrival = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime serviceStart = arrival.plusMinutes(10);

        statMarket.addWaitTime(arrival, serviceStart);

        double avgWait = statMarket.getStats().getAverageWaitTime();
        assertEquals(10.0, avgWait, "Wait time should be correctly added");
    }

    @Test
    void testRecordBusyTimeDelegation() {
        Checkout c = statMarket.getCheckout().getFirst();
        LocalDateTime start = statMarket.now();
        LocalDateTime end = start.plusMinutes(15);

        statMarket.recordBusyTime(c, start, end);

        double busyTime = getPrivateBusy(statMarket.getStats(), c);
        assertTrue(busyTime > 0, "Busy time should be recorded");
    }


    @SuppressWarnings("unchecked")
    private int getPrivateCheckoutsSize(SupermarketStats s) {
        try {
            var f = SupermarketStats.class.getDeclaredField("checkouts");
            f.setAccessible(true);
            List<Checkout> list = (List<Checkout>) f.get(s);
            return list.size();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private int getPrivateServed(SupermarketStats s, Checkout c) {
        try {
            var f = SupermarketStats.class.getDeclaredField("servedCustomers");
            f.setAccessible(true);
            var map = (java.util.Map<Checkout, Integer>) f.get(s);
            return map.get(c);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private int getPrivateMaxQueue(SupermarketStats s, Checkout c) {
        try {
            var f = SupermarketStats.class.getDeclaredField("maxQueueLength");
            f.setAccessible(true);
            var map = (java.util.Map<Checkout, Integer>) f.get(s);
            return map.get(c);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private double getPrivateBusy(SupermarketStats s, Checkout c) {
        try {
            var f = SupermarketStats.class.getDeclaredField("busyTimePerInterval");
            f.setAccessible(true);
            var map = (java.util.Map<Checkout, java.util.List<Double>>) f.get(s);
            return map.get(c).getFirst();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
