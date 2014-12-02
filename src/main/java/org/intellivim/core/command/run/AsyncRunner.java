package org.intellivim.core.command.run;

import org.intellivim.inject.UnsupportedClientException;

/**
 * ClientSpecific async output support
 *
 * @author dhleong
 */
public interface AsyncRunner {

    void prepare() throws UnsupportedClientException;

    void sendOut(String line);

    void sendErr(String line);

    void sendSys(String line);

    void terminate();
}
