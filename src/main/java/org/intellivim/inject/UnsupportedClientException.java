package org.intellivim.inject;

/**
 * Thrown if something was attempted that the accessing Client couldn't handle
 * @author dhleong
 */
public class UnsupportedClientException extends Exception {
    public UnsupportedClientException(String message) {
        super(message);
    }
}
