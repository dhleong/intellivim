package org.intellivim.core.command.complete;

import com.google.common.base.Joiner;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.intellivim.core.BaseTestCase;
import org.intellivim.core.SimpleResult;

import java.util.List;

/**
 * Created by dhleong on 11/7/14.
 */
public class CompletionTest extends BaseTestCase {

    public void testCompletion() {
        String projPath = getProjectPath(JAVA_PROJECT);
        String filePath = "src/org/intellivim/javaproject/Dummy.java";
        int offset = 243;

        SimpleResult result = (SimpleResult) new CompleteCommand().execute(projPath, filePath, offset);
        assertSuccess(result);

        List<CompletionInfo> infoList = (List<CompletionInfo>) result.result;
        assertTrue("Expected results", infoList.size() > 0);
    }

}
