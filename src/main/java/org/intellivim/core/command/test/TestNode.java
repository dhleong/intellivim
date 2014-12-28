package org.intellivim.core.command.test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dhleong
 */
public class TestNode {

    /* Public fields because the interfaces is final for json-ifying */

    public final String id;
    public final String name;

    public final List<TestNode> kids = new ArrayList<TestNode>();

    public TestState state;

    public TestNode(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addChild(TestNode node) {
        kids.add(node);
    }

    public void setState(TestState state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return id + ":" + name + "(" + state + ")"
                + (kids.isEmpty() ? "" : "+" + kids);
    }
}
