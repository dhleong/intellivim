package org.intellivim.core.command.test;

/**
 * @author dhleong
 */
public enum TestState {
    SKIPPED,
    /**
     * NB: There's a separate state for this, but at least in IntelliJ 13
     *  it is the same index as PASSED, so it will never be seen
     */
    COMPLETE,
    NOT_RUN,
    RUNNING,
    TERMINATED,
    IGNORED,
    FAILED,
    COMPARISON_FAILURE,
    ERROR,
    PASSED
}
