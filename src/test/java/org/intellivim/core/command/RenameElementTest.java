package org.intellivim.core.command;

import com.intellij.psi.PsiFile;
import org.intellivim.FileEditingTestCase;
import org.intellivim.SimpleResult;

/**
 * @author dhleong
 */
public class RenameElementTest extends FileEditingTestCase {
    @Override
    protected String getFilePath() {
        return DUMMY_FILE_PATH;
    }

    @Override
    protected String getProjectPath() {
        return getProjectPath(JAVA_PROJECT);
    }

    public void testRename() {
        final int offset = 314;
        final PsiFile file = getPsiFile();
//        final PsiElement element = file.findElementAt(offset);

        assertFileContains("list = new ArrayList");
        assertFileContains("list.add(");

        final SimpleResult result = (SimpleResult)
                new RenameElementCommand(getProject(), file, offset, "list2").execute();
        assertSuccess(result);

        assertFileDoesNotContain("list = new ArrayList");
        assertFileDoesNotContain("list.add(");
        assertFileNowContains("list2 = new ArrayList");
        assertFileNowContains("list2.add(");
    }

}
