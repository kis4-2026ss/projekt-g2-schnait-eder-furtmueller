package supm.profiled;

import ev.CustomerArrivalEvent;
import profile.ArrivalProfile;
import profile.PeriodicArrivalProfile;
import supm.base.SimulationParameters;

import java.time.Duration;
import java.time.LocalDateTime;

public class ProfiledSupermarketStatisticClient {

    static void main() {

        SimulationParameters params = new SimulationParameters(
                3,      // number of checkouts
                0.5,    // mean arrival rate (used only if no profile)
                3.0,    // mean service time in minutes
                2.0,    // Std-Dev of service time
                15.0,   // max wait time in minutes
                (60*24*5),   // simulation duration in minutes
                false   // print protocol
        );

        int intervalLength = (60*4); // interval length in minutes


        double[] dayProfile = {(100.0/4.0/60.0), (220.0/4.0/60), (150.0/4.0/60), 0.0, 0.0, 0.0};
        LocalDateTime simStart = LocalDateTime.now();

        ArrivalProfile profile = new PeriodicArrivalProfile(
                simStart,
                Duration.ofMinutes(60*24),
                dayProfile
        );

        ProfiledSupermarketStatistic market = new ProfiledSupermarketStatistic(params, intervalLength, profile);

        market.addEvent(new CustomerArrivalEvent(market.now(), market));

        market.run(market.now().plusMinutes(params.simulationDurationMinutes));

        market.getStats().finalizeBusyTimes(market.now());
        market.getStats().printSummary();
        market.getStats().printUtilizationTable();
    }
}
