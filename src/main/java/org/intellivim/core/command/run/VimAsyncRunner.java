package org.intellivim.core.command.run;

import org.intellivim.core.util.VimSpecific;
import org.intellivim.inject.Client;
import org.intellivim.inject.ClientSpecific;
import org.intellivim.inject.UnsupportedClientException;

/**
 * @author dhleong
 */
@ClientSpecific(Client.VIM)
public class VimAsyncRunner extends VimSpecific implements AsyncRunner {

    @Override
    public void prepare() throws UnsupportedClientException {
        ensureSupportsRemoteExecution();

        // FIXME
    }

    @Override
    public void sendLine(OutputType type, String line) {
        // FIXME
    }

    @Override
    public void terminate() {
        // FIXME
    }

}
