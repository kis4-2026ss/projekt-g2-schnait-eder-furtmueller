package ev;

import sim.Simulation;
import supm.parts.Checkout;
import supm.parts.Customer;
import supm.base.Supermarket;
import supm.statis.SupermarketStatistic;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServiceEndEvent extends Event {
    private final Supermarket market;
    private final Checkout checkout;
    private final Customer customer;

    public ServiceEndEvent(LocalDateTime time, Supermarket market, Checkout checkout, Customer customer) {
        super(time);
        this.market = market;
        this.checkout = checkout;
        this.customer = customer;
    }

    @Override
    public void process(Simulation sim) {
        market.log(time.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")) + ": " + customer + " finished at " + checkout);

        if (!checkout.getQueue().isEmpty()) {
            Customer next = checkout.getQueue().poll();

            if (next != null) {
                next.setStartServiceT(time);
            }

            market.log(next + " starts service at " + checkout);
            LocalDateTime end = time.plusSeconds(((long) market.nextServiceTime())*60);
            sim.addEvent(new ServiceEndEvent(end, market, checkout, next));
        } else {
            checkout.setBusy(false);
        }

        if (market instanceof SupermarketStatistic statMarket) {                    //allows class to work with Supermarket
            statMarket.customerServed(checkout);                                    //and StatisticSupermarket
            statMarket.addWaitTime(customer.getArrivalTime(), customer.getStartServiceT());

            if (!checkout.isBusy()) {                                               //determines busy time once checkout no
                statMarket.recordBusyTime(checkout, checkout.getStartBusy(), time); //longer busy
            }
        }
    }
}
