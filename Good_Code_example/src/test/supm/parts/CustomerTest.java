package supm.parts;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotEquals;


class CustomerTest {

    @Test
    void toString_Works() {
        Customer c = new Customer(LocalDateTime.now());
        Assertions.assertEquals("Customer 1", c.toString());
    }

    @Test
    void testCustomerIdsUnique() {
        Customer c1 = new Customer(LocalDateTime.now());
        Customer c2 = new Customer(LocalDateTime.now());
        assertNotEquals(c1.getId(), c2.getId());
    }

}