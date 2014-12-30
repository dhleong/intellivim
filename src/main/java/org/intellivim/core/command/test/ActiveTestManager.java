package org.intellivim.core.command.test;

import com.intellij.openapi.project.Project;

import java.util.HashMap;
import java.util.Map;

/**
 * @author dhleong
 */
public class ActiveTestManager {

    private static Map<String, TestNode> activeTestRoots =
            new HashMap<String, TestNode>();

    public static TestNode getActiveTestRoot(final Project project) {
        return activeTestRoots.get(project.getProjectFilePath());
    }

    public static void setActiveTestRoot(Project project, TestNode root) {
        activeTestRoots.put(project.getProjectFilePath(), root);
    }
}
