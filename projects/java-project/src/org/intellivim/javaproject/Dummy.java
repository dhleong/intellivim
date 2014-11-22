package org.intellivim.javaproject;

import java.util.ArrayList;

/**
 * Created by dhleong on 11/5/14.
 */
public class Dummy {
    public void boring() {
        System.out.println("Hi");
        new ArrayList<String>();
        new Dummy().fluid().boring();
        ArrayList<String> list = new ArrayList<String>();
        list.add("hi");
    }

    /** I promise it's not boring */
    public void notBoring(int number) {

    }

    public Dummy fluid() {
        return this;
    }
}
