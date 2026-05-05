package supm.profiled;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import profile.ArrivalProfile;
import profile.PeriodicArrivalProfile;
import supm.base.SimulationParameters;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class ProfiledSupermarketStatisticTest {

    private LocalDateTime start;
    private SimulationParameters params;

    @BeforeEach
    void setup() {
        start = LocalDateTime.of(2025, 1, 1, 8, 0);
        params = new SimulationParameters(
                2,     // checkouts
                1.0,   // avg customers per minute (unused here)
                2.0,   // avg service time
                1.0,   // std dev
                10.0,  // max wait
                120, // sim duration
                false  // silent
        );
    }

    @Test
    void testNextInterarrivalTimeWithChangingRate() {
        double[] rates = {0.5, 0.0, 3.0}; // always open
        ArrivalProfile profile = new PeriodicArrivalProfile(start, Duration.ofHours(3), rates);
        ProfiledSupermarketStatistic market = new ProfiledSupermarketStatistic(params, 30, profile);

        // call nextInterarrivalTime
        double interarrival = market.nextInterarrivalTime();

        // should be > 0 but finite
        assertTrue(interarrival > 0 && interarrival < 30, "Interarrival time should be reasonable for rate=2.0");
    }

    @Test
    void testNextInterarrivalTimeWithConstantRate() {
        double[] rates = {0.5}; // always open
        ArrivalProfile profile = new PeriodicArrivalProfile(start, Duration.ofHours(3), rates);
        ProfiledSupermarketStatistic market = new ProfiledSupermarketStatistic(params, 30, profile);

        // call nextInterarrivalTime
        double interarrival = market.nextInterarrivalTime();

        // should be > 0 but finite
        assertTrue(interarrival > 0 && interarrival < 30, "Interarrival time should be reasonable for rate=2.0");
    }



    @Test
    void testNextInterarrivalTimeChangesWithRandomness() {
        double[] rates = {5.0}; // constant high rate
        ArrivalProfile profile = new PeriodicArrivalProfile(start, Duration.ofHours(1), rates);
        ProfiledSupermarketStatistic market1 = new ProfiledSupermarketStatistic(params, 10, profile);
        ProfiledSupermarketStatistic market2 = new ProfiledSupermarketStatistic(params, 10, profile);

        // Different random draws → different results (high rate → small interarrival)
        double v1 = market1.nextInterarrivalTime();
        double v2 = market2.nextInterarrivalTime();

        assertNotEquals(v1, v2, "Different instances should yield different random interarrivals");
        assertTrue(v1 > 0 && v2 > 0);
    }
}
