package org.intellivim.core.command.test;

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

    public VimAsyncTestRunner() {
        super(PREPARE_COMMAND, OUTPUT_COMMAND, CANCEL_COMMAND, TERMINATE_COMMAND);
    }

    @Override
    public void onStartTesting(TestNode node) {
        // FIXME implement these
        System.out.println("Start testing!");
    }

    @Override
    public void onTestOutput(final TestNode owner, final String output,
            final OutputType type) {

    }

    @Override
    public void onTestStateChanged(final TestNode node) {

    }


    @Override
    public void onFinishTesting() {
        System.out.println("Stop testing!");
    }
}
