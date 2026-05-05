package stats;

import supm.parts.Checkout;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class SupermarketStats {

    private final List<Checkout> checkouts;
    private final double intervalLength; // in min
    private final Map<Checkout, Integer> servedCustomers = new HashMap<>();
    private final Map<Checkout, Integer> maxQueueLength = new HashMap<>();
    private int abandonedCustomers = 0;
    private double totalWaitTime = 0.0;
    private int numWaitedCustomers = 0;

    // busy time per interval
    private final Map<Checkout, List<Double>> busyTimePerInterval = new HashMap<>();
    private final int numIntervals;
    private final double maxWaitTime;
    private final LocalDateTime simStart;

    public SupermarketStats(List<Checkout> checkouts, double simulationDuration, double intervalLength,double maxWaitTime, LocalDateTime simStart) {
        this.checkouts = checkouts;
        this.intervalLength = intervalLength;
        this.numIntervals = (int) Math.ceil(simulationDuration / intervalLength);
        this.maxWaitTime = maxWaitTime;
        this.simStart = simStart;

        for (Checkout c : checkouts) {
            servedCustomers.put(c, 0);
            maxQueueLength.put(c, 0);
            busyTimePerInterval.put(c, new ArrayList<>(Collections.nCopies(numIntervals, 0.0)));
        }
    }


    // update stats during simulation
    public void customerServed(Checkout c) {
        servedCustomers.put(c, servedCustomers.get(c) + 1);
    }

    public void addWaitTime(double waitTime) {
        totalWaitTime += waitTime;
        numWaitedCustomers++;
    }

    public void customerQueued(Checkout c) {
        int len = c.queueLength();
        maxQueueLength.put(c, Math.max(maxQueueLength.get(c), len));
    }

    public void customerAbandoned() {
        abandonedCustomers++;
        numWaitedCustomers++;
        totalWaitTime += maxWaitTime;
    }

    public void recordBusyTime(Checkout c, LocalDateTime startTime, LocalDateTime endTime) {
        double startMinutes = Duration.between(simStart, startTime).toMinutes();
        double endMinutes   = Duration.between(simStart, endTime).toMinutes();

        int startIdx = Math.max(0, (int) (startMinutes / intervalLength));
        int endIdx   = Math.min(numIntervals - 1, (int) (endMinutes / intervalLength));

        for (int i = startIdx; i <= endIdx; i++) {
            double intervalStart = i * intervalLength;
            double intervalEnd   = intervalStart + intervalLength;

            // calculates overlaps
            double overlap = Math.min(endMinutes, intervalEnd) - Math.max(startMinutes, intervalStart);
            overlap = Math.max(0, overlap); // ensures no negative possible
            double prev = busyTimePerInterval.get(c).get(i);
            busyTimePerInterval.get(c).set(i, prev + overlap);
        }
    }

    // checks if checkout is still occupied at the end
    public void finalizeBusyTimes(LocalDateTime simEnd) {
        for (Checkout c : checkouts) {
            LocalDateTime start = c.getStartBusy();
            if (c.isBusy() && start != null) {
                // record busy time from start until simEnd
                recordBusyTime(c, start, simEnd);
            }
        }
    }

    // calculates stats
    public double getAverageWaitTime() {
        return numWaitedCustomers > 0 ? totalWaitTime / numWaitedCustomers : 0.0;
    }

    public List<List<Double>> getUtilizationTable() {
        List<List<Double>> table = new ArrayList<>();
        for (int i = 0; i < numIntervals; i++) {
            List<Double> row = new ArrayList<>();
            double sum = 0.0;
            for (Checkout c : checkouts) {
                double busy = busyTimePerInterval.get(c).get(i);
                double util = Math.min(1.0, busy / intervalLength); // percental (0..1)
                row.add(util);
                sum += util;
            }
            row.add(sum / checkouts.size()); // average utilization
            table.add(row);
        }
        return table;
    }

    public void printSummary() {
        System.out.println("=== Simulation Summary ===");
        for (Checkout c : checkouts) {
            System.out.printf("Checkout %d: served=%d, maxQueue=%d%n",
                    c.getId(),
                    servedCustomers.get(c),
                    maxQueueLength.get(c));
        }
        System.out.println("Total abandoned customers: " + abandonedCustomers);
        System.out.printf("Average wait time: %.2f min%n", getAverageWaitTime());
    }

    public void printUtilizationTable() {
        System.out.println("\n=== Checkout Utilization Table ===");
        System.out.print("Interval        | ");
        for (Checkout c : checkouts) {
            System.out.printf("C%-8d| ", c.getId());
        }
        System.out.println("Avg Util");

        List<List<Double>> table = getUtilizationTable();
        for (int i = 0; i < table.size(); i++) {
            double start = i * intervalLength;
            double end = start + intervalLength;
            System.out.printf("[%6.1f - %6.1f] | ", start, end);
            for (int j = 0; j < checkouts.size(); j++) {
                System.out.printf("%6.1f%% | ", table.get(i).get(j) * 100);
            }
            System.out.printf("%6.1f%%\n", table.get(i).get(checkouts.size()) * 100);
        }
    }
}
