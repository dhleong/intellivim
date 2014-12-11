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

    private String bufNo;

    @Override
    public void prepare(String launchId) throws UnsupportedClientException {
        ensureSupportsRemoteExecution();

        final String raw = remoteFunctionExpr("intellivim#core#run#onPrepareOutput",
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
        remoteFunctionExpr("intellivim#core#run#onOutput",
                bufNo,
                type.name().toLowerCase(),
                clean);
    }

    @Override
    public void cancel() {
        remoteFunctionExpr("intellivim#core#run#onCancelled", bufNo);
    }

    @Override
    public void terminate() {
        remoteFunctionExpr("intellivim#core#run#onTerminated", bufNo);
    }

}
