package ev;

import sim.Simulation;
import supm.parts.Checkout;
import supm.parts.Customer;
import supm.base.Supermarket;
import supm.statis.SupermarketStatistic;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CustomerAbandonEvent extends Event {
    private final Supermarket market;
    private final Checkout cashier;
    private final Customer customer;

    public CustomerAbandonEvent(LocalDateTime time, Supermarket market, Checkout checkout, Customer customer) {
        super(time);
        this.market = market;
        this.cashier = checkout;
        this.customer = customer;
    }

    @Override
    public void process(Simulation sim) {
        if (cashier.getQueue().remove(customer)) {  //only possible if still in queue

            if (market instanceof SupermarketStatistic statMarket) {    //allows class to work with Supermarket
                statMarket.customerAbandoned();                         //and StatisticSupermarket
            }

            market.log(time.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")) + ": " + customer + " abandons queue at " + cashier + " (waited too long)");
        }
    }
}
