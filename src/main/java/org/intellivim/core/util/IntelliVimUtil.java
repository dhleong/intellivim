package org.intellivim.core.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.command.CommandProcessor;

/**
 * @author dhleong
 */
public class IntelliVimUtil {

    private static boolean sTemporarilyUnitTest = false;

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

        if (sTemporarilyUnitTest)
            app.setUnitTestMode(true);
    }

    public static void unsetUnitTestMode() {
        ApplicationImpl app = (ApplicationImpl) ApplicationManager.getApplication();
        if (sTemporarilyUnitTest)
            app.setUnitTestMode(false);
        sTemporarilyUnitTest = false;
    }

}
