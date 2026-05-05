package supm.parts;

import java.time.LocalDateTime;

public class Customer {
    private static int nextId = 1;
    private final int id;
    private final LocalDateTime arrivalTime;
    private LocalDateTime startServiceT = null;

    public Customer(LocalDateTime arrivalTime) {
        this.id = nextId++;                     //new id for every customer
        this.arrivalTime = arrivalTime;
    }

    public void setStartServiceT (LocalDateTime start) {
        this.startServiceT = start;
    }

    public LocalDateTime getStartServiceT () {
        assert startServiceT != null : "No service start defined";
        return startServiceT;
    }

    public LocalDateTime getArrivalTime() { return arrivalTime; }

    public int getId() { return id; }

    @Override
    public String toString() { return "Customer " + id;}
}
