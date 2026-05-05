package supm.profiled;

import profile.PeriodicArrivalProfile;
import supm.base.SimulationParameters;
import profile.ArrivalProfile;
import supm.statis.SupermarketStatistic;


import java.time.LocalDateTime;
import java.util.Random;


public class ProfiledSupermarketStatistic extends SupermarketStatistic {

    private final ArrivalProfile profile;
    private final Random random = new Random();

    public ProfiledSupermarketStatistic(SimulationParameters params, int intervalLength, ArrivalProfile profile) {
        super(params, intervalLength);
        this.profile = profile;
    }

    @Override
    public double nextInterarrivalTime() {
        LocalDateTime current = this.now();

        LocalDateTime nextOpen = ((PeriodicArrivalProfile) profile).nextOpenTime(current);

        // If nextOpen > now -> simulation needs to "wait"
        long waitMinutes = java.time.Duration.between(current, nextOpen).toMinutes();

        double rate = profile.getArrivalRate(nextOpen);
        double u = random.nextDouble();
        double interarrival = -Math.log(1 - u) / rate;

        return waitMinutes + interarrival;
    }

}
