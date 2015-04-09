package org.intellivim.core.command.impl;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.intellivim.FileEditingTestCase;
import org.intellivim.SimpleResult;
import org.intellivim.core.util.ProjectUtil;

/**
 * @author dhleong
 */
public class ImplementTest extends FileEditingTestCase {

    final String filePath = "src/org/intellivim/javaproject/SubClass.java";

    @Override
    protected String getFilePath() {
        return filePath;
    }

    @Override
    protected String getProjectPath() {
        return getProjectPath(JAVA_PROJECT);
    }

    public void testSubClassNormal() {
        final String signature = "public void normalMethod()";
        final int offset = 161;

        assertFileDoesNotContain(signature);
        SimpleResult result = implementAt(signature, offset);
        assertSuccess(result);
        assertFileNowContains(signature + " {");

        assertFileFormattedCorrectly();
    }

    public void testSubClassAbstract() {
        final String signature = "public abstract void abstractMethod()";
        final String implemented = "public void abstractMethod() {";
        final int offset = 161;

        assertFileDoesNotContain(signature);
        SimpleResult result = implementAt(signature, offset);
        assertSuccess(result);
        assertFileNowContains(implemented);

        assertFileFormattedCorrectly();
    }

    public void testImplementMultiple() {
        final int offset = 161;
        final String[] signatures = {
            "public abstract void abstractMethod()",
            "public void normalMethod()"
        };
        final String[] expected = {
            "public void abstractMethod() {",
            "public void normalMethod() {"
        };

        for (String signature : signatures) {
            assertFileDoesNotContain(signature);
        }

        final Project project = getProject();
        final PsiFile file = ProjectUtil.getPsiFile(project, filePath);
        SimpleResult result = execute(new ImplementCommand(
                project, file, offset, signatures));
        assertSuccess(result);

        for (String signature : expected) {
            assertFileNowContains(signature);
        }

        assertFileFormattedCorrectly();
    }

    public void testNestedClass() {
        final String signature = "public void boring()";
        final int offset = 153;

        assertFileDoesNotContain(signature);
        SimpleResult result = implementAt(signature, offset);
        assertSuccess(result);
        assertFileNowContains(signature + " {");

        assertFileFormattedCorrectly();
    }

    public void testNestedClassWithJavadoc() {
        final String signature = "public void notBoring(int number)";
        final int offset = 153;

        assertFileDoesNotContain(signature);
        SimpleResult result = implementAt(signature, offset);
        assertSuccess(result);
        assertFileNowContains(signature + " {");

        assertFileFormattedCorrectly();
        dumpFileContents();
    }

    private void assertFileFormattedCorrectly() {

        // make sure there are no wacky formatting issues
        assertFileDoesNotContain("@java.lang");
        assertFileDoesNotContain("}}");

//        dumpFileContents();
    }

    private SimpleResult implementAt(String signature, int offset) {
        final Project project = getProject();
        final PsiFile file = ProjectUtil.getPsiFile(project, filePath);
        return execute(new ImplementCommand(project, file, signature, offset));
    }
}
