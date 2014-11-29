package org.intellivim.javaproject;

/**
 * @author dhleong
 */
public class SubClass extends SuperClass {

    static class NestedClass extends Dummy {

    }

    @Override
    public void normalMethod() {
        super.normalMethod();
    }

    @Override
    public void abstractMethod() {
        super.abstractMethod();
    }
}
