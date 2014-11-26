package org.intellivim.javaproject;

import org.intellivim.javaproject.subpackage.NotImported;

/**
 * Class with intentional problems for testing
 */
public class Problematic {

    public void foo() {
        NotImported obj;
    }

}
