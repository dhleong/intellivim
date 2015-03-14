package org.intellivim.core.command.impl;

import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dhleong
 */
public class GetImplementablesTest extends BaseTestCase {

    final String filePath = "src/org/intellivim/javaproject/SubClass.java";

    /**
     * Compares CharSequences by their toString values.
     *  Otherwise, we have to store as a String, and
     *  that's just a bummer
     */
    private Comparator<CharSequence> charSequenceComparator =
            new Comparator<CharSequence>() {
                @Override
                public int compare(CharSequence first, CharSequence second) {
                    return first.toString().compareTo(second.toString());
                }
            };

    @Override
    protected String getProjectPath() {
        return getProjectPath(JAVA_PROJECT);
    }

    public void testOutside() {
        // attempting outside of a context should
        //  fail gracefully
        SimpleResult result = getImplementablesAt(36);
        assertThat(result.error).isEqualTo("No context for implement");
    }

    public void testSubClass() {
        SimpleResult result = getImplementablesAt(161);
        assertSuccess(result);

        final Implementables impl = result.getResult();
        assertThat(impl)
                .extracting("description", CharSequence.class)
                    .usingElementComparator(charSequenceComparator)
                    .contains("public void normalMethod()")
                    .contains("public abstract void abstractMethod()");
    }

    public void testNestedClass() {
        SimpleResult result = getImplementablesAt(153);
        assertSuccess(result);

        // this nested class extends Dummy
        final Implementables impl = result.getResult();
        assertThat(impl)
                .extracting("description", CharSequence.class)
                    .usingElementComparator(charSequenceComparator)
                    .contains("public void boring()")
                    .contains("public Dummy fluid()")
                    .contains("public void notBoring(int number)");
    }

    SimpleResult getImplementablesAt(int offset) {
        return (SimpleResult) new GetImplementablesCommand(
                getProject(), filePath, offset).execute();
    }
}
