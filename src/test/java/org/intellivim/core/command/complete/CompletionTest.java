package org.intellivim.core.command.complete;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.assertj.core.api.Condition;
import org.intellivim.BaseTestCase;
import org.intellivim.SimpleResult;
import org.intellivim.core.util.ProjectUtil;

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

        final PsiFile file = ProjectUtil.getPsiFile(project, filePath);
        SimpleResult result = execute(new CompleteCommand(project, file, offset));
        assertSuccess(result);

        CompleteCommand.CompletionResultInfo completion = result.getResult();
        List<CompletionInfo<?>> infoList = completion.completions;
        assertThat(infoList)
                .isNotEmpty()
                .has(sizeAtLeast(4));

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

        CompletionInfo third = infoList.get(2);
        assertNotNull(third);
        assertEquals("notBoring(", third.body);
        assertEquals("(int number, String foo)-> void", third.detail);
        assertEquals("", third.doc);

        CompletionInfo last = infoList.get(3);
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
