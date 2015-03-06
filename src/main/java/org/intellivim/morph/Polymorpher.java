package org.intellivim.morph;

import com.google.gson.JsonObject;

/**
 * @author dhleong
 */
public interface Polymorpher {

    /**
     *
     * @param input
     * @param params
     * @return True if the CommandImpl with the given params
     *  matches the input
     */
    public boolean matches(JsonObject input, String...params);
}
