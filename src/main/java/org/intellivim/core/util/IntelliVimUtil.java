package org.intellivim.core.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.command.CommandProcessor;

/**
 * @author dhleong
 */
public class IntelliVimUtil {

    /**
     * Sometimes a command will not cooperate unless it thinks
     *  we're in unit test mode. Let's be in unit test mode
     * @param runnable
     */
    public static void runInUnitTestMode(Runnable runnable) {
        ApplicationImpl app = (ApplicationImpl) ApplicationManager.getApplication();
        final boolean wasUnitTest = app.isUnitTestMode();

        app.setUnitTestMode(true);
        runnable.run();
        app.setUnitTestMode(wasUnitTest);
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
}
