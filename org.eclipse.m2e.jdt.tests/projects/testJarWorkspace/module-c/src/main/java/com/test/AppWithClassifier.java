package com.test;

public class AppWithClassifier {
  public static void main(String[] args) {
    // This should compile with classifier="tests" dependency
    System.out.println(TestUtil.getTestMessage());
  }
}
