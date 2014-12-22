package org.intellivim.core.command.run;

import org.apache.commons.lang.StringUtils;
import org.intellivim.core.util.VimSpecific;
import org.intellivim.inject.Client;
import org.intellivim.inject.ClientSpecific;
import org.intellivim.inject.UnsupportedClientException;

/**
 * @author dhleong
 */
@ClientSpecific(Client.VIM)
public class VimAsyncRunner extends VimSpecific implements AsyncRunner {

    private static final String PREPARE_COMMAND = "intellivim#core#run#onPrepareOutput";
    private static final String OUTPUT_COMMAND = "intellivim#core#run#onOutput";
    private static final String CANCEL_COMMAND =  "intellivim#core#run#onCancelled";
    private static final String TERMINATE_COMMAND =  "intellivim#core#run#onTerminated";

    private final String prepareCommand, outputCommand;
    private final String cancelCommand, terminateCommand;

    private String bufNo;

    public VimAsyncRunner() {
        this(PREPARE_COMMAND, OUTPUT_COMMAND, CANCEL_COMMAND, TERMINATE_COMMAND);
    }

    protected VimAsyncRunner(String prepareCommand, String outputCommand,
            String cancelCommand, String terminateCommand) {
        this.prepareCommand = prepareCommand;
        this.outputCommand = outputCommand;
        this.cancelCommand = cancelCommand;
        this.terminateCommand = terminateCommand;
    }

    @Override
    public void prepare(String launchId) throws UnsupportedClientException {
        ensureSupportsRemoteExecution();

        final String raw = remoteFunctionExpr(prepareCommand,
                launchId);
        if (StringUtils.isEmpty(raw))
            throw new RuntimeException("Timeout preparing output");

        bufNo = raw.trim();
    }

    @Override
    public void sendLine(OutputType type, String line) {
        final String clean = line.trim()
                .replaceAll("\n", "\\\\r")
                .replaceAll("\t", "    ");

        // functionExpr is safer, in case they're in input mode
        remoteFunctionExpr(outputCommand,
                bufNo,
                type.name().toLowerCase(),
                clean);
    }

    @Override
    public void cancel() {
        remoteFunctionExpr(cancelCommand, bufNo);
    }

    @Override
    public void terminate() {
        remoteFunctionExpr(terminateCommand, bufNo);
    }

}
