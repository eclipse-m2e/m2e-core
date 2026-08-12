package com.test;

public class RegularApp {
  public static void main(String[] args) {
    // Should be able to use MainUtil from module-a
    System.out.println(MainUtil.getMessage());

    // Should NOT be able to use TestUtil - if uncommented, this would cause compilation error:
    // System.out.println(TestUtil.getTestMessage());
  }
}
