package org.intellivim.java.command.junit;

import com.intellij.rt.execution.junit.states.PoolOfTestStates;
import org.intellivim.core.command.test.TestState;

import java.util.HashMap;
import java.util.Map;

/**
 * @author dhleong
 */
public class JunitUtil {

    static Map<Integer, TestState> BY_INDEX = new HashMap<Integer, TestState>();
    static {
        BY_INDEX.put(PoolOfTestStates.SKIPPED_INDEX, TestState.SKIPPED);
        BY_INDEX.put(PoolOfTestStates.COMPLETE_INDEX, TestState.COMPLETE);
        BY_INDEX.put(PoolOfTestStates.NOT_RUN_INDEX, TestState.NOT_RUN);
        BY_INDEX.put(PoolOfTestStates.RUNNING_INDEX, TestState.RUNNING);
        BY_INDEX.put(PoolOfTestStates.TERMINATED_INDEX, TestState.TERMINATED);
        BY_INDEX.put(PoolOfTestStates.IGNORED_INDEX, TestState.IGNORED);
        BY_INDEX.put(PoolOfTestStates.FAILED_INDEX, TestState.FAILED);
        BY_INDEX.put(PoolOfTestStates.COMPARISON_FAILURE, TestState.COMPARISON_FAILURE);
        BY_INDEX.put(PoolOfTestStates.ERROR_INDEX, TestState.ERROR);
        BY_INDEX.put(PoolOfTestStates.PASSED_INDEX, TestState.PASSED);
    }

    public static TestState getStateFromIndex(int stateIndex) {
        return BY_INDEX.get(stateIndex);
    }
}
