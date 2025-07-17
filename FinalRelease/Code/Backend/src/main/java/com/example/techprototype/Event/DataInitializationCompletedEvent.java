package com.example.techprototype.Event;

import org.springframework.context.ApplicationEvent;

public class DataInitializationCompletedEvent extends ApplicationEvent {
    
    public DataInitializationCompletedEvent() {
        super("DataInitializationCompleted");
    }
    
    public DataInitializationCompletedEvent(Object source) {
        super(source);
    }
} 