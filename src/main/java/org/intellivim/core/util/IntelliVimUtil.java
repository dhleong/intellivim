package org.intellivim.core.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.util.ui.UIUtil;

/**
 * @author dhleong
 */
public class IntelliVimUtil {

    private static boolean sTemporarilyUnitTest = false;
    private static boolean sTemporarilyNotUnitTest = false;

    /**
     * Sometimes a command will not cooperate unless it thinks
     *  we're in unit test mode. Let's be in unit test mode
     * @param runnable
     */
    public static void runInUnitTestMode(Runnable runnable) {
        setUnitTestMode();
        runnable.run();
        unsetUnitTestMode();
    }

    /**
     * Execute some code as an "undo-transparent" write action
     *
     * @param runnable
     */
    public static void runWriteCommand(final Runnable runnable) {
        CommandProcessor.getInstance().runUndoTransparentAction(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(runnable);
            }
        });
    }

    /** Convenience */
    public static boolean isUnitTestMode() {
        return ApplicationManager.getApplication().isUnitTestMode();
    }

    public static void setUnitTestMode() {
        ApplicationImpl app = (ApplicationImpl) ApplicationManager.getApplication();
        sTemporarilyUnitTest = !app.isUnitTestMode();

        // FIXME hopefully this isn't needed anymore...
//        if (sTemporarilyUnitTest)
//            app.setUnitTestMode(true);
    }

    public static void unsetUnitTestMode() {
        ApplicationImpl app = (ApplicationImpl) ApplicationManager.getApplication();
//        if (sTemporarilyUnitTest)
//            app.setUnitTestMode(false);
        sTemporarilyUnitTest = false;
    }

    /**
     * Wrap the runnable to ensure it's run on the
     *  Swing dispatch thread
     */
    public static Runnable onSwingThread(final Runnable runnable) {
        return new Runnable() {

            @Override
            public void run() {
                UIUtil.invokeAndWaitIfNeeded(runnable);
            }
        };
    }
}
