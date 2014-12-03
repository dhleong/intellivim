package org.intellivim.core.command.run;

import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.util.Key;
import org.intellivim.inject.UnsupportedClientException;

import java.util.HashMap;

/**
 * ClientSpecific async output support
 *
 * @author dhleong
 */
public interface AsyncRunner {

    public enum OutputType {
        STDOUT,
        STDERR,
        SYSTEM;

        static HashMap<Key, OutputType> map = new HashMap<Key, OutputType>();
        static {
            map.put(ProcessOutputTypes.STDOUT, STDOUT);
            map.put(ProcessOutputTypes.STDERR, STDERR);
            map.put(ProcessOutputTypes.SYSTEM, SYSTEM);
        }

        static OutputType from(Key key) {
            return map.get(key);
        }
    }


    void prepare(String launchId) throws UnsupportedClientException;

    void sendLine(OutputType type, String line);

    void terminate();
}
