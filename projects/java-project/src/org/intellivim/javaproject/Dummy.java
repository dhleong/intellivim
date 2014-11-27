package org.intellivim.javaproject;

import java.util.ArrayList;

/**
 * Some dummy class for testing purposes
 * @author dhleong
 */
public class Dummy {
    public void boring() {
        System.out.println("Hi");
        new ArrayList<String>();
        new Dummy().fluid().boring();
        ArrayList<String> list = new ArrayList<String>();
        list.add("hi");
        notBoring(42);
    }

    /** I promise it's not boring */
    public void notBoring(int number) {
        Problematic problem;
    }

    public Dummy fluid() {
        return this;
    }
}
