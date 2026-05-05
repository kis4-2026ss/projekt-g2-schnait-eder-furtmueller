package profile;

import java.time.Duration;
import java.time.LocalDateTime;

public class PeriodicArrivalProfile implements ArrivalProfile {
    private final double[] rates;     // rates for intervals
    private final Duration period;    // duration of a single period
    private final Duration interval;  // duration of an interval within the period
    private final LocalDateTime start; // reference time (e.g. Simulation start)

    public PeriodicArrivalProfile(LocalDateTime start, Duration period, double[] rates) {
        this.start = start;
        this.period = period;
        this.rates = rates;
        if (rates[0] <= 0.0) {
            throw new IllegalStateException("First rate needs to be larger than 0");
        }
        this.interval = period.dividedBy(rates.length);
    }

    @Override
    public double getArrivalRate(LocalDateTime time) {
        Duration sinceStart = Duration.between(start, time);
        long modMinutes = sinceStart.toMinutes() % period.toMinutes();
        int index = (int) (modMinutes / interval.toMinutes());
        return rates[index];
    }

    public LocalDateTime nextOpenTime(LocalDateTime current) {
        LocalDateTime t = current;
        double rate = getArrivalRate(t);

        while (rate <= 0) {
            Duration sinceStart = Duration.between(start, t);
            long minutesIntoPeriod = sinceStart.toMinutes() % period.toMinutes();
            int currentIndex = (int) (minutesIntoPeriod / interval.toMinutes());
            int nextIndex = (currentIndex + 1) % rates.length;

            long minutesToNextInterval = nextIndex * interval.toMinutes() - minutesIntoPeriod;
            if (minutesToNextInterval <= 0) {
                minutesToNextInterval += period.toMinutes();
            }

            t = t.plusMinutes(minutesToNextInterval);
            rate = getArrivalRate(t);
        }

        return t;
    }
}
