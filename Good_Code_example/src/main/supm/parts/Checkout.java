package supm.parts;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Queue;

public class Checkout {
    private final int id;
    private final Queue<Customer> queue = new LinkedList<>();
    private boolean busy = false;
    private LocalDateTime startBusy;

    public Checkout(int id) {
        this.id = id;
    }

    //used to determine busy time
    public void startBusy(LocalDateTime time) { this.startBusy = time; }
    public LocalDateTime getStartBusy() { return startBusy; }

    public boolean isBusy() { return busy; }
    public void setBusy(boolean busy) { this.busy = busy; }

    //customer queue per checkout
    public Queue<Customer> getQueue() { return queue; }

    public int queueLength() { return queue.size(); }

    public int getId() { return id; }

    @Override
    public String toString() { return "Checkout " + id; }
}
