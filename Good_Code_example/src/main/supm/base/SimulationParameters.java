package supm.base;

public class SimulationParameters {
    public int numCheckouts;
    public double meanArrivalRate;
    public double meanServiceTime;
    public double serviceTimeStdDev;
    public double maxWaitTime;
    public int simulationDurationMinutes;
    public boolean protocol;

    public SimulationParameters(int numCheckouts, double meanArrivalRate,
                                double meanServiceTime, double serviceTimeStdDev,
                                double maxWaitTime, int simulationDurationMinutes,
                                boolean protocol) {
        this.numCheckouts = numCheckouts;
        this.meanArrivalRate = meanArrivalRate;             //average customers per minute
        this.meanServiceTime = meanServiceTime;             //average service time in min
        this.serviceTimeStdDev = serviceTimeStdDev;         //Std-Deviation in min of service time
        this.maxWaitTime = maxWaitTime;                     //max wait time in min
        this.simulationDurationMinutes = simulationDurationMinutes;
        this.protocol = protocol;                           //print protocol: yes(true) or no(false)
    }
}
