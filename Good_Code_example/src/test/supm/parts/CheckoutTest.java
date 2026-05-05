package supm.parts;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutTest {

    @Test
    void testQueueLength() {
        Checkout c = new Checkout(1);
        assertEquals(0, c.queueLength());

        c.getQueue().add(new Customer(LocalDateTime.now()));
        assertEquals(1, c.queueLength());
    }

    @Test
    void testBusyFlag() {
        Checkout c = new Checkout(1);
        assertFalse(c.isBusy());
        c.setBusy(true);
        assertTrue(c.isBusy());
    }

    @Test
    void testIds() {
        Checkout c = new Checkout(1);
        assertEquals(1,c.getId());
    }
}