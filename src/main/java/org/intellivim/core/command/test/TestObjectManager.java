package org.intellivim.core.command.test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author dhleong
 */
public class TestObjectManager {

    Map<String, TestNode> nodes = new HashMap<String, TestNode>();

    public TestNode getById(String id) {
        return nodes.get(id);
    }

    public void register(TestNode node) {
        nodes.put(node.id, node);
    }
}
