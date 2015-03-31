package org.intellivim;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.intellivim.core.model.VimDocument;
import org.intellivim.core.model.VimEditor;
import org.intellivim.core.util.FileUtil;
import org.intellivim.core.util.ProjectUtil;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Convenient base test case for commands that
 *  will edit some file. Any changes will be
 *  reverted after each test, and assertions
 *  are provided to make testing easier
 *
 * @author dhleong
 */
public abstract class FileEditingTestCase extends BaseTestCase {

    private byte[] originalContents;

    protected abstract String getFilePath();

    public void setUp() throws Exception {
        super.setUp();

        Project project = getProject();
        VirtualFile file = ProjectUtil.getVirtualFile(project, getFilePath());
        originalContents = file.contentsToByteArray();

//        DaemonCodeAnalyzerImpl daemonCodeAnalyzer =
//                (DaemonCodeAnalyzerImpl) DaemonCodeAnalyzer.getInstance(project);
//        daemonCodeAnalyzer.prepareForTest();
//
//        final StartupManagerImpl startupManager = (StartupManagerImpl) StartupManagerEx.getInstanceEx(project);
//        startupManager.runStartupActivities();
//        startupManager.startCacheUpdate();
//        startupManager.runPostStartupActivities();
    }

    public void tearDown() throws Exception {
        super.tearDown();

        if (fileContentsChanged())
            restoreFile();

//        Project project = ProjectUtil.getProject(getProjectPath());
//        ((StartupManagerImpl) StartupManager.getInstance(project)).checkCleared();
//        DaemonCodeAnalyzerImpl daemonCodeAnalyzer =
//                (DaemonCodeAnalyzerImpl) DaemonCodeAnalyzer.getInstance(project);
//        daemonCodeAnalyzer.cleanupAfterTest();
    }

    /*
     * Methods
     */

    public boolean fileContentsChanged() {
        try {
            VirtualFile file = getFile();
            byte[] updatedContents = file.contentsToByteArray();
            final int len = updatedContents.length;
            if (len != originalContents.length) {
                // definitely not same
                return true;
            } else {
                // is it the same?
                for (int i = 0; i < len; i++) {
                    if (originalContents[i] != updatedContents[i]) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            fail("Unable to read file contents: " + e.getMessage());
        }

        // no change
        return false;
    }

    /*
     * Assertions
     */

    protected void assertFileContentsChanged() {
        assertTrue("Expected file contents to have changed", fileContentsChanged());
    }

    protected void assertFileContentsUnchanged() {
        if (!fileContentsChanged())
            return;

        assertSame(new String(originalContents), getCurrentFileContentsSafely());
    }

    protected void assertFileDoesNotContain(CharSequence expectedContents) {
        String actualContents = getCurrentFileContentsSafely();
//        assertFalse("Expected file to NOT contain `" + expectedContents + "`",
//                actualContents.contains(expectedContents));
        assertThat(actualContents).doesNotContain(expectedContents);
    }

    protected void assertFileContains(CharSequence expectedContents) {
        String actualContents = getCurrentFileContentsSafely();
        assertThat(actualContents).contains(expectedContents);
//        assertTrue("Expected file to contain `" + expectedContents
//                        + "`; found instead:\n" + actualContents,
//                actualContents.contains(expectedContents));
    }

    /**
     * Convenience to assert both that the contents have changed somehow,
     *  and that they've changed in the way you expect---by "now" containing
     *  something they (presumably) didn't before
     */
    protected void assertFileNowContains(CharSequence expectedContents) {
        assertFileContentsChanged();
        assertFileContains(expectedContents);
    }


    /*
     * utils
     */

    /** For testing */
    @SuppressWarnings("unused")
    protected void dumpFileContents() {
        System.out.println(getCurrentFileContentsSafely());
    }

    private VirtualFile getFile() {
        final Project proj = getProject();
        return ProjectUtil.getVirtualFile(proj, getFilePath());
    }

    private String getCurrentFileContentsSafely() {
        try {
            return new String(getFile().contentsToByteArray());
        } catch (IOException e) {
            fail("Unable to read file contents");
            return null; // won't actually get here
        }
    }

    private void restoreFile() throws IOException {
        final VirtualFile file = getFile();

        // NB: At this point, the file on disk is correct,
        //  but the PsiFile is still referencing the modified stuff

        final Project project = getProject();
        final String originalString = new String(originalContents);

        final PsiFile psi =  ApplicationManager.getApplication().runWriteAction(
                new ThrowableComputable<PsiFile, IOException>() {

            @Override
            public PsiFile compute() throws IOException {
                file.setBinaryContent(originalContents);

                final PsiFile psi = ProjectUtil.getPsiFile(project, file);
                final DocumentEx doc = VimDocument.getInstance(psi);

                FileDocumentManager.getInstance().reloadFromDisk(doc);
                assertThat(doc.getText()).isEqualTo(originalString);

                // now, commit changes so the PsiFile is updated
                PsiDocumentManager.getInstance(project).commitDocument(doc);
                return psi;
            }
        });

        assertThat(psi.getText()).isEqualTo(originalString);

        FileUtil.commitChanges(new VimEditor(project, psi, 0));
    }


}
