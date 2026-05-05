package profile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class PeriodicArrivalProfileTest {

    private LocalDateTime start;
    private Duration day;
    private double[] rates;
    private PeriodicArrivalProfile profile;

    @BeforeEach
    void setup() {
        start = LocalDateTime.of(2025, 1, 1, 0, 0);
        day = Duration.ofHours(24);
        rates = new double[]{1.0, 2.0, 0.0, 4.0, 0.0, 3.0};
        profile = new PeriodicArrivalProfile(start, day, rates);
    }

    @Test
    void testGetArrivalRateWithinFirstInterval() {
        LocalDateTime t = start.plusMinutes(30); // within first interval
        assertEquals(1.0, profile.getArrivalRate(t));
    }

    @Test
    void testGetArrivalRateDifferentIntervals() {
        int intervalMinutes = (int) (day.toMinutes() / rates.length);

        assertEquals(1.0, profile.getArrivalRate(start.plusMinutes(intervalMinutes / 2)));
        assertEquals(2.0, profile.getArrivalRate(start.plusMinutes(intervalMinutes + 1)));
        assertEquals(0.0, profile.getArrivalRate(start.plusMinutes(intervalMinutes * 2L + 5)));
        assertEquals(4.0, profile.getArrivalRate(start.plusMinutes(intervalMinutes * 3L + 10)));
        assertEquals(3.0, profile.getArrivalRate(start.plusMinutes(intervalMinutes * 5L + 1)));
    }

    @Test
    void testPeriodicWrapAround() {
        LocalDateTime afterFullDay = start.plusMinutes((int) day.toMinutes() + 5); // after 1 full period + 5 min
        double expectedRate = profile.getArrivalRate(start.plusMinutes(5)); // same as beginning
        assertEquals(expectedRate, profile.getArrivalRate(afterFullDay), 1e-9);
    }

    @Test
    void testNextOpenTimeSkipsClosedIntervals() {
        int intervalMinutes = (int) (day.toMinutes() / rates.length);
        LocalDateTime closedStart = start.plusMinutes(intervalMinutes * 2L + 1); // in 0.0 interval
        LocalDateTime nextOpen = profile.nextOpenTime(closedStart);

        // should skip to interval 3 (rate 4.0)
        LocalDateTime expected = start.plusMinutes(intervalMinutes * 3L);
        assertEquals(expected, nextOpen);
    }

    @Test
    void testNextOpenTimeWrapsToNextDayIfAllClosedLater() {
        double[] allClosedLater = new double[]{1.5, 0.0, 0.0, 0.0, 0.0, 0.0};
        PeriodicArrivalProfile p = new PeriodicArrivalProfile(start, day, allClosedLater);
        int intervalMinutes = (int) (day.toMinutes() / allClosedLater.length);

        // time near end of day (closed intervals)
        LocalDateTime t = start.plusMinutes(intervalMinutes * 5L + 10);
        LocalDateTime next = p.nextOpenTime(t);

        // should wrap to next day to first open interval (index 0)
        LocalDateTime expected = start.plusDays(1); // start of next day's first interval
        assertEquals(expected, next);
    }

    @Test
    void testNextOpenTimeAlreadyOpen() {
        LocalDateTime t = start.plusMinutes(10); // rate = 1.0 > 0
        LocalDateTime result = profile.nextOpenTime(t);
        assertEquals(t, result, "Should return same time if already open");
    }

    @Test
    void testIntervalDivisionAccuracy() {
        Duration interval = day.dividedBy(rates.length);
        assertEquals(4 * 60, interval.toMinutes(), "Interval should be 4 hours long");
    }
}
