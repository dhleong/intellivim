package org.intellivim.core.util;

import org.apache.commons.lang.StringUtils;
import org.intellivim.inject.UnsupportedClientException;

/**
 * Base for Vim-specific things utils
 *
 * @author dhleong
 */
public abstract class VimSpecific {

    /** full path to Vim executable */
    String exe;

    /** Vim instance name */
    String instance;

    public String getExe() {
        return exe;
    }

    public String getInstanceName() {
        return instance;
    }

    public boolean isRemoteExecutionSupported() {
        return !StringUtils.isEmpty(instance);
    }

    protected void ensureSupportsRemoteExecution() throws UnsupportedClientException {
        if (!isRemoteExecutionSupported())
            throw new UnsupportedClientException("Vim --servername support is required");
    }

}
