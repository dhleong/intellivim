package org.intellivim.javaproject;

/**
 * Class with intentional problems for testing
 */
public class Problematic {

    public void foo() {
        NotImported obj;
        AlsoNotImported other;
    }

}
