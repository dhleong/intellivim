package org.intellivim.core.util;


import org.apache.log4j.lf5.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeoutException;

/**
 * For running an external program and gathering its output
 *
 * @author dhleong
 */
public class ExternalRunner {

    private static final long JOIN_TIMEOUT = 1000;
    private static final long DEFAULT_RUN_TIMEOUT = 500;

    private final String[] cmdArray;
    private boolean interrupted;
    private Exception error;

    private ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    private ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    private int exitValue;
    private boolean finished;

    private ExternalRunner(String[] cmdArray) {
        this.cmdArray = cmdArray;
    }

    public boolean isSuccess() {
        return finished && !interrupted && error == null && exitValue == 0;
    }

    public String getStdOut() {
        // we don't expect to call this more than once...
        return stdout.toString();
    }

    public String getStdErr() {
        return stderr.toString();
    }

    public Exception getError() {
        return error;
    }

    public int getExitValue() {
        return exitValue;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    void run() {
        final Runtime runtime = Runtime.getRuntime();
        try {
            Process proc = runtime.exec(cmdArray);

            Thread out = readStreamInto(proc.getInputStream(), stdout);
            Thread err = readStreamInto(proc.getErrorStream(), stderr);

            exitValue = proc.waitFor();

            long now = System.currentTimeMillis();
            System.out.println("Start");
            out.join(JOIN_TIMEOUT);
            err.join(JOIN_TIMEOUT);

            System.out.println("Thread2: " + out.isAlive());
            System.out.println("Thread2: " + err.isAlive());
            System.out.println("Stop: " + (System.currentTimeMillis() - now));
            System.out.println("out=" + stdout.toString());
            System.out.println("err=" + stderr.toString());

            finished = true;

        } catch (IOException e) {
            onError(e);
        } catch (InterruptedException e) {
            onError(e);
        }
    }

    void onError(Exception e) {
        error = e;
    }

    private Thread readStreamInto(final InputStream in,
              final ByteArrayOutputStream out) {
        final Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    StreamUtils.copy(in, out);
                } catch (IOException e) {
                    onError(e);
                }
                System.out.println("Copied: " + isAlive());
            }
        };
        thread.start();
        System.out.println("Thread: " + thread.isAlive());
        return thread;
    }

    ExternalRunner start(long timeout) {
        final Thread thread = new Thread() {
            public void run() {
                ExternalRunner.this.run();
            }
        };

        // go
        thread.start();

        try {
            thread.join(timeout);

            if (!finished) {
                // I guess?
                onError(new TimeoutException());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            interrupted = true;
        }

        return this;
    }

    static ExternalRunner prepare(String... cmdArray) {
        return new ExternalRunner(cmdArray);
    }

    public static ExternalRunner run(String... cmdArray) {
        return run(DEFAULT_RUN_TIMEOUT, cmdArray);
    }

    public static ExternalRunner run(long timeout, String... cmdArray) {
        return prepare(cmdArray).start(timeout);
    }
}
