package org.intellivim;

import org.intellivim.core.util.ExternalRunner;
import org.intellivim.core.util.ExternalRunnerAssert;

/**
 * @author dhleong
 */
public class IVAssertions {

    public static ExternalRunnerAssert assertThat(ExternalRunner actual) {
        return new ExternalRunnerAssert(actual);
    }

}
