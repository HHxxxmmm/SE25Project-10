package com.example.techprototype.Util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class TicketNumberGeneratorTest {

    @Test
    void testGenerateTicketNumber() {
        String ticketNumber = TicketNumberGenerator.generateTicketNumber();
        assertNotNull(ticketNumber);
        assertTrue(ticketNumber.startsWith("T"));
        assertTrue(ticketNumber.length() > 2);
    }

    @Test
    void testGenerateOrderNumber() {
        String orderNumber = TicketNumberGenerator.generateOrderNumber();
        assertNotNull(orderNumber);
        assertFalse(orderNumber.startsWith("T"));
        assertTrue(orderNumber.length() > 2);
    }

    @Test
    void testTicketNumberUniqueness() {
        Set<String> ticketNumbers = new HashSet<>();
        int count = 1000;

        for (int i = 0; i < count; i++) {
            String ticketNumber = TicketNumberGenerator.generateTicketNumber();
            ticketNumbers.add(ticketNumber);
        }

        assertEquals(count, ticketNumbers.size(), "All ticket numbers should be unique");
    }

    @Test
    void testOrderNumberUniqueness() {
        Set<String> orderNumbers = new HashSet<>();
        int count = 1000;

        for (int i = 0; i < count; i++) {
            String orderNumber = TicketNumberGenerator.generateOrderNumber();
            orderNumbers.add(orderNumber);
        }

        assertEquals(count, orderNumbers.size(), "All order numbers should be unique");
    }
}
 
