package org.intellivim;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellivim.core.util.ProjectUtil;

import java.io.IOException;

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

    protected abstract String getProjectPath();

    protected abstract String getFilePath();

    public void setUp() throws Exception {
        super.setUp();

        Project project = ProjectUtil.getProject(getProjectPath());
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
        assertFalse("Expected file to NOT contain `" + expectedContents + "`",
                actualContents.contains(expectedContents));

    }

    protected void assertFileContains(CharSequence expectedContents) {
        String actualContents = getCurrentFileContentsSafely();
        assertTrue("Expected file to contain `" + expectedContents
                        + "`; found instead:\n" + actualContents,
                actualContents.contains(expectedContents));
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

    private VirtualFile getFile() {
        final Project proj = ProjectUtil.getProject(getProjectPath());
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
        getFile().setBinaryContent(originalContents);
    }


}
