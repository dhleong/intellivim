package org.intellivim.runnable;

import java.util.Arrays;

public class RunnableMain {
    public static final void main(String[] args) {
        System.out.println("Standard Output");
        System.err.println("Standard Error");
        Arrays.asList(args);
    }
}
