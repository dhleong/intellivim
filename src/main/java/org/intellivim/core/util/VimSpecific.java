package org.intellivim.core.util;

import org.apache.commons.lang.StringUtils;
import org.intellivim.inject.UnsupportedClientException;

/**
 * Base for Vim-specific things utils
 *
 * @author dhleong
 */
public abstract class VimSpecific {

    private static final boolean DEBUG = false;

    /** basically, never timeout */
    static final long TIMEOUT = 10000;

    /** full path to Vim executable */
    String exe;

    /** Vim instance name */
    String instance;

    public String getExe() {
        return StringUtils.isEmpty(exe)
            ? "vim" // I guess?
            : exe;
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

    /** Execute a --remote-expr command */
    protected String remoteExpr(String expr) {
        final ExternalRunner exec = ExternalRunner.run(TIMEOUT,
                getExe(),
                "--servername", instance,
                "--remote-expr", expr
        );

        if (DEBUG) System.out.println("VIM> --remote-expr " + expr);

        if (!exec.isSuccess()) {
            // tell me why
            System.err.println("Error running " + expr);
            System.err.println(" -stderr: " + exec.getStdErr());
            System.err.println(" -stdout: " + exec.getStdOut());
            exec.getError().printStackTrace();
        }

        return exec.getStdOut();
    }

    /** Call a function remotely using --remote-expr to get the result */
    protected String remoteFunctionExpr(String function, String...args) {
        final StringBuilder call = new StringBuilder()
                .append(function).append('(');
        for (int i = 0; i < args.length; i++){
            final String arg = args[i] == null
                    ? "" // ???
                    : args[i].replace("\"", "\\\"");
            call.append('"')
                .append(arg)
                .append('"');
            if (i < args.length - 1){
                call.append(',');
            }
        }
        call.append(')')
            .append(" | redraw!"); // special for func calls
        return remoteExpr(call.toString());
    }
}
