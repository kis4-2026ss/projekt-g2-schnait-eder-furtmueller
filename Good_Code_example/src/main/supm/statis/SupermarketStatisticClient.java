package supm.statis;

import ev.CustomerArrivalEvent;
import supm.base.SimulationParameters;


public class SupermarketStatisticClient {
    static void main() {

        SimulationParameters params = new SimulationParameters(
                3,     //checkouts
                1.5,    //average customers per minute
                3.0,    //average service time in min
                1.0,    //Std-Deviation in min of service time
                10.0,   //max wait time in min
                (60*2),    //simulation duration in min
                false    //print protocol: yes(true) or no(false)
        );

        int intervalLength = (30); //interval in minutes
        SupermarketStatistic market = new SupermarketStatistic(params, intervalLength);

        //create first arrival event to start
        market.addEvent(new CustomerArrivalEvent(market.now(), market));



        //startSimulation
        market.run(market.now().plusMinutes(params.simulationDurationMinutes));

        //print out statistics
        market.getStats().finalizeBusyTimes(market.now());
        market.getStats().printSummary();
        market.getStats().printUtilizationTable();
    }
}
