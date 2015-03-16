package org.intellivim.core.command.problems;

/**
 * Indicates that something went wrong when
 *  applying a quickfix, usually due to the
 *  arg provided
 *
 * @author dhleong
 */
public class QuickFixException extends Exception {
    public QuickFixException(String message) {
        super(message);
    }
}
