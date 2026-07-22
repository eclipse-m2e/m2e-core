package com.test;

public class TestClass {
  public void testSomething() {
    // Test code can use TestUtil from module-a's test-jar (scope=test)
    System.out.println(TestUtil.getTestMessage());
  }
}
