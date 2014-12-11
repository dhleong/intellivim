package org.intellivim.core.util;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.project.Project;
import org.intellivim.core.command.run.RunCommand;
import org.intellivim.UsableSdkTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author dhleong
 */
public class BuildUtilTest extends UsableSdkTestCase {

    static final long TIMEOUT = 10;

    @Override
    protected void invokeTestRunnable(final Runnable runnable) throws Exception {
        // DON'T run on Swing dispatch thread; some of the compile
        //  stuff wants to run there, and we'll never get the results
        //  if we do, too
        System.out.println("Invoke: " + runnable);
        runnable.run();
    }

    @Override
    protected boolean runInDispatchThread() {
        return false;
    }

    public void testBuildRunnable() throws Exception {
        Project project = prepareProject(RUNNABLE_PROJECT);
        RunnerAndConfigurationSettings settings =
                RunCommand.pickRunSetting(project, null);

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean status = BuildUtil.compileProject(
                project, settings.getConfiguration(),
                new CompileStatusNotification() {
                    @Override
                    public void finished(final boolean aborted, final int errors,
                            final int warnings,
                            final CompileContext compileContext) {

                        System.out.println("aborted=" + aborted +
                                           "errors=" + errors +
                                           "warnings=" + warnings
                        );
                        for (CompilerMessage msg :  compileContext
                                        .getMessages(CompilerMessageCategory.ERROR)) {

                            System.out.println(msg);
                        }

                        latch.countDown();
                    }
                });

        assertTrue("Expected build to have started", status);
        if (!latch.await(TIMEOUT, TimeUnit.SECONDS)) {
            assertTrue("Timeout on compile :(", latch.await(TIMEOUT, TimeUnit.SECONDS));
        }
    }
}
