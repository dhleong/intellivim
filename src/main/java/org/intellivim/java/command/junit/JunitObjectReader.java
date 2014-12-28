package org.intellivim.java.command.junit;

import com.intellij.rt.execution.junit.segments.PoolOfDelimiters;

/**
* @author dhleong
*/
public class JunitObjectReader {
    final String in;
    final int len;
    int position = 0;

    JunitObjectReader(String in, final int initialPosition) {
        this.in = in;
        len = in.length();
        position = initialPosition;
    }

    JunitObjectReader(String in, final String objectPrefix) {
        this(in, objectPrefix.length());
    }

    public String nextReference() {
        return upTo(PoolOfDelimiters.REFERENCE_END);
    }

    public String readLimitedString() {
        final int symbolCount = readInt();
        return advanceTo(position + symbolCount);
    }

    public int readInt() {
        return Integer.parseInt(upTo(PoolOfDelimiters.INTEGER_DELIMITER));
    }

    String advanceTo(int end) {
        final String read = in.substring(position, end);
        position = end;
        return read;
    }

    String upTo(char delimiter) {
        for (int i=position; i < len; i++) {
            if (in.charAt(i) == delimiter) {
                final String read = advanceTo(i);
                position++;
                return read;
            }
        }

        throw new IllegalArgumentException(
                "Couldn't find delimiter `" + delimiter + "'");
    }

}
