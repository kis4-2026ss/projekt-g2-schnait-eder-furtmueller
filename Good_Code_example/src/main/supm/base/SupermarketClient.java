package supm.base;

import ev.CustomerArrivalEvent;

import java.time.LocalDateTime;

public class SupermarketClient {
    static void main() {
        SimulationParameters params = new SimulationParameters(
                2,      //checkouts
                1.5,    //average customers per minute
                3.0,    //average service time in min
                1.0,    //Std-Deviation in min of service time
                3.0,   //max wait time in min
                15,    //simulation duration in min
                true    //print protocol: yes(true) or no(false)
        );

        Supermarket market = new Supermarket(params);

        //create first arrival event to start
        market.addEvent(new CustomerArrivalEvent(market.now(), market));

        //startSimulation
        market.run(LocalDateTime.now().plusMinutes(params.simulationDurationMinutes));
    }
}
