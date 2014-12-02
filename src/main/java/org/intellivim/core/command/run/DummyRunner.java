package org.intellivim.core.command.run;

import org.intellivim.inject.Client;
import org.intellivim.inject.ClientSpecific;
import org.intellivim.inject.UnsupportedClientException;

/**
 * Default client that doesn't support async execution
 *
 * @author dhleong
 */
@ClientSpecific(Client.DEFAULT)
public class DummyRunner extends VimAsyncRunner {

    @Override
    public void prepare() throws UnsupportedClientException {
        throw new UnsupportedClientException("Unknown client does not support project running");
    }

}
