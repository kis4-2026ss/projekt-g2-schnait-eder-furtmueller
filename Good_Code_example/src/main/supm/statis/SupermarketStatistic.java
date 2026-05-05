package supm.statis;

import stats.SupermarketStats;
import supm.base.SimulationParameters;
import supm.base.Supermarket;
import supm.parts.Checkout;

import java.time.Duration;
import java.time.LocalDateTime;

public class SupermarketStatistic extends Supermarket {

    private final SupermarketStats stats;

    public SupermarketStatistic(SimulationParameters params, int intervalLength) {
        super(params);

        double simDuration = params.simulationDurationMinutes;
        this.stats = new SupermarketStats(getCheckout(), simDuration, intervalLength, params.maxWaitTime, this.now());
    }

    public SupermarketStats getStats() { return stats; }

    public void customerQueued(Checkout c) { stats.customerQueued(c); }

    public void customerAbandoned() { stats.customerAbandoned(); }

    public void customerServed(Checkout c) {
        stats.customerServed(c);
    }

    public void addWaitTime(LocalDateTime arrivalTime, LocalDateTime serviceStartTime) {
        double waitTime = Duration.between(arrivalTime, serviceStartTime).toMinutes();
        stats.addWaitTime(waitTime);
    }

    public void recordBusyTime(Checkout c, LocalDateTime startTimeMinutes, LocalDateTime endTimeMinutes) {
        stats.recordBusyTime(c, startTimeMinutes, endTimeMinutes);
    }

}

