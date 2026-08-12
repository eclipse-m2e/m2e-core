package com.test;

public class App {
  public static void main(String[] args) {
    // This should compile only if module-a's test-classes are on the classpath
    System.out.println(TestUtil.getTestMessage());
  }
}
