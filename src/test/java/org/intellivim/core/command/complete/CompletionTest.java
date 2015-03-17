package org.intellivim.core.command.complete;

import com.intellij.openapi.project.Project;
import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dhleong
 */
public class CompletionTest extends BaseTestCase {

    @Override
    protected String getProjectPath() {
        return getProjectPath(JAVA_PROJECT);
    }

    public void testCompletion() {
        Project project = getProject();
        String filePath = "src/org/intellivim/javaproject/Dummy.java";
        int offset = 269; // new Dummy().

        SimpleResult result = (SimpleResult) new CompleteCommand(
                project, filePath, offset).execute();
        assertSuccess(result);

        List<CompletionInfo> infoList = result.getResult();
        assertThat(infoList)
                .isNotEmpty()
                .has(sizeAtLeast(3));

        CompletionInfo first = infoList.get(0);
        assertNotNull(first);
        assertEquals("boring()", first.body);
        assertEquals("()-> void", first.detail);
        assertEquals("", first.doc);

        CompletionInfo second = infoList.get(1);
        assertNotNull(second);
        assertEquals("notBoring(", second.body);
        assertEquals("(int number)-> void", second.detail);
        assertEquals("/** I promise it's not boring */", second.doc);

        CompletionInfo last = infoList.get(2);
        assertNotNull(last);
        assertEquals("fluid()", last.body);
        assertEquals("()-> Dummy", last.detail);
        assertEquals("", last.doc);
    }

    private static Condition<? super List<?>> sizeAtLeast(final int minSize) {
        return new Condition<Collection<?>>("size at least " + minSize) {
            @Override
            public boolean matches(final Collection<?> ts) {
                return ts.size() >= minSize;
            }
        };
    }

}
