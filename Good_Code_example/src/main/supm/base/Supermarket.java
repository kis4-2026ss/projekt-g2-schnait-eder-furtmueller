package supm.base;

import sim.Simulation;
import supm.parts.Checkout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Supermarket extends Simulation {
    private final SimulationParameters params;
    private final List<Checkout> checkouts;
    private final Random random = new Random();

    public Supermarket(SimulationParameters params) {
        this.params = params;
        this.checkouts = new ArrayList<>();

        //generates checkouts
        for (int i = 0; i < params.numCheckouts; i++) {
            checkouts.add(new Checkout(i + 1));
        }
        if (params.maxWaitTime < 0) {
            throw new IllegalStateException("Max wait time cant be negative!");
        }
    }

    public SimulationParameters getParams() { return params; }

    public Checkout findShortestQueue() {
        if (checkouts.isEmpty()) {
            throw new IllegalStateException("Error: No checkouts in store");   //in case no checkouts generated
        }

        //goes to empty checkout if available
        for (Checkout c : checkouts) {
            if (!c.isBusy()) {
                return c;
            }
        }

        //finds shortest queue if no empty checkout
        Checkout shortest = checkouts.getFirst();
        int minLength = shortest.queueLength();

        for (int i = 1; i < checkouts.size(); i++) {
            Checkout c = checkouts.get(i);
            int len = c.queueLength();
            if (len < minLength) {
                shortest = c;
                minLength = len;
            }
        }

        return shortest;
    }

    public void log(String msg) { if (params.protocol) System.out.println(msg); }

    public double nextInterarrivalTime() {
        if (!(params.meanArrivalRate > 0)) {
            throw new IllegalStateException("Constant customer arrival rate needs to be larger than 0");
        }

        double u = random.nextDouble();
        return -Math.log(1 - u) / params.meanArrivalRate;
    }

    public double nextServiceTime() {
        return Math.max(0.1, params.meanServiceTime +               //prevents service time from being
                random.nextGaussian() * params.serviceTimeStdDev);  //negative through Std-Deviations
    }

    public Random getRandom() { return random; }

    public List<Checkout> getCheckout() { return checkouts; }

}
