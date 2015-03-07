package org.intellivim.core.command.test;

import org.intellivim.inject.Client;
import org.intellivim.inject.ClientSpecific;
import org.intellivim.inject.UnsupportedClientException;

/**
 * Default client that doesn't support async test execution
 *
 * @author dhleong
 */
@ClientSpecific(Client.DEFAULT)
public class DummyTestRunner extends VimAsyncTestRunner {

    @Override
    public void prepare(String launchId) throws UnsupportedClientException {
        throw new UnsupportedClientException("Unknown client does not support test running");
    }
}
