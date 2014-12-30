package org.intellivim;

import org.intellivim.core.command.test.TestNode;
import org.intellivim.core.command.test.TestNodeAssert;
import org.intellivim.core.util.ExternalRunner;
import org.intellivim.core.util.ExternalRunnerAssert;

/**
 * @author dhleong
 */
public class IVAssertions {

    public static ExternalRunnerAssert assertThat(ExternalRunner actual) {
        return new ExternalRunnerAssert(actual);
    }

    public static TestNodeAssert assertThat(TestNode actual) {
        return new TestNodeAssert(actual);
    }
}
