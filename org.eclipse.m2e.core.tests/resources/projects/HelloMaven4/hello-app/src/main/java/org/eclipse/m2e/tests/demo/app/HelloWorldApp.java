package org.eclipse.m2e.tests.demo.app;

import org.eclipse.m2e.tests.demo.core.GreetingService;

/**
 * Simple HelloWorld application demonstrating Maven 4 multi-subproject structure.
 */
public class HelloWorldApp {
    
    public static void main(String[] args) {
        GreetingService service = new GreetingService();
        
        String name = args.length > 0 ? args[0] : "Maven 4 World";
        String greeting = service.greet(name);
        
        System.out.println(greeting);
        System.out.println("Built with Maven 4!");
    }
}
