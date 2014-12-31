package org.intellivim.core.command.test;

import com.intellij.openapi.application.ApplicationManager;
import org.intellivim.core.command.run.VimAsyncRunner;
import org.intellivim.inject.Client;
import org.intellivim.inject.ClientSpecific;

/**
 * @author dhleong
 */
@ClientSpecific(Client.VIM)
public class VimAsyncTestRunner extends VimAsyncRunner implements AsyncTestRunner {

    private static final String PREPARE_COMMAND = "intellivim#core#test#onPrepareOutput";
    private static final String OUTPUT_COMMAND = "intellivim#core#test#onOutput";
    private static final String CANCEL_COMMAND =  "intellivim#core#test#onCancelled";
    private static final String TERMINATE_COMMAND =  "intellivim#core#test#onTerminated";

    static final String TEST_START_COMMAND =  "intellivim#core#test#onStartTesting";
    static final String TEST_OUTPUT_COMMAND =  "intellivim#core#test#onTestOutput";
    static final String TEST_STATE_COMMAND =  "intellivim#core#test#onTestStateChange";

    public VimAsyncTestRunner() {
        super(PREPARE_COMMAND, OUTPUT_COMMAND, CANCEL_COMMAND, TERMINATE_COMMAND);
    }

    @Override
    public void onStartTesting(TestNode node) {
//        ApplicationManager.getApplication().invokeLater(new Runnable() {
//
//            @Override
//            public void run() {
                System.out.println("VIM: startTesting");
                remoteFunctionExpr(TEST_START_COMMAND, bufNo);
                System.out.println("VIM: Started");
//
//            }
//        });
    }

    @Override
    public void onTestOutput(final TestNode owner, final String output,
            final OutputType type) {
        final String clean = cleanLines(output);

        // functionExpr is safer, in case they're in input mode
        remoteFunctionExpr(TEST_OUTPUT_COMMAND,
                bufNo,
                owner.id,
                type.name().toLowerCase(),
                clean);
    }

    @Override
    public void onTestStateChanged(final TestNode node) {

        remoteFunctionExpr(TEST_STATE_COMMAND,
                bufNo,
                node.id,
                node.state.name());
    }


    @Override
    public void onFinishTesting() {
        // TODO do something here?
        System.out.println("Stop testing!");
    }
}
