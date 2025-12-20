package org.eclipse.m2e.tests.demo.core;

/**
 * A simple greeting service to demonstrate Maven 4 project structure.
 */
public class GreetingService {
    
    private final String prefix;
    
    public GreetingService(String prefix) {
        this.prefix = prefix;
    }
    
    public GreetingService() {
        this("Hello");
    }
    
    public String greet(String name) {
        return prefix + ", " + name + "!";
    }
    
    public String getPrefix() {
        return prefix;
    }
}
