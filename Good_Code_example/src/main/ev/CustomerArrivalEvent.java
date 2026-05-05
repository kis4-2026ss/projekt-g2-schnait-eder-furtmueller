package ev;

import sim.Simulation;
import supm.parts.Checkout;
import supm.parts.Customer;
import supm.base.Supermarket;
import supm.profiled.ProfiledSupermarketStatistic;
import supm.statis.SupermarketStatistic;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CustomerArrivalEvent extends Event {
    private final Supermarket market;
    private final Customer customer;

    public CustomerArrivalEvent(LocalDateTime time, Supermarket market) {
        super(time);
        this.market = market;
        this.customer = new Customer(time);

    }

    @Override
    public void process(Simulation sim) {
        market.log(time.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")) + ": " + customer + " arrives");

        Checkout checkout = market.findShortestQueue();

        if (!checkout.isBusy()) {
            checkout.setBusy(true);
            checkout.startBusy(time);

            customer.setStartServiceT(time);

            market.log(customer + " starts service at " + checkout);
            LocalDateTime endTime = time.plusSeconds(((long) market.nextServiceTime())*60);
            sim.addEvent(new ServiceEndEvent(endTime, market, checkout, customer));
        } else {
            checkout.getQueue().add(customer);
            market.log(customer + " queues at " + checkout + " (len=" + checkout.queueLength() + ")");

            if (market instanceof SupermarketStatistic statMarket) {        //allows class to work with Supermarket
                statMarket.customerQueued(checkout);                        //and StatisticSupermarket
            }

            double maxWait = market.getParams().maxWaitTime;
            LocalDateTime abandonTime = time.plusMinutes((long) maxWait);
            sim.addEvent(new CustomerAbandonEvent(abandonTime, market, checkout, customer));
        }



        double nextInterarrival;

        if (market instanceof ProfiledSupermarketStatistic profMarket) {
            nextInterarrival = profMarket.nextInterarrivalTime();
        } else {
            nextInterarrival = market.nextInterarrivalTime();
        }

        LocalDateTime nextTime = time.plusSeconds((long) (nextInterarrival*60));

        if (Duration.between(sim.now(), nextTime).toMinutes() <             //stops generating new arrivals
                market.getParams().simulationDurationMinutes) {             //once simulation Duration is over
            sim.addEvent(new CustomerArrivalEvent(nextTime, market));
        }
    }
}
