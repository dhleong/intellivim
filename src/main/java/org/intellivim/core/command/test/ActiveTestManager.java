package org.intellivim.core.command.test;

import com.intellij.openapi.project.Project;

/**
 * Perhaps we should support running multiple JUnit tests at
 *  a time, but seems like a pretty unusual situation....
 * @author dhleong
 */
public class ActiveTestManager {

//    private static Map<String, TestNode> activeTestRoots =
//            new HashMap<String, TestNode>();
//    private static Map<String, TestNode> lastTestRoots =
//            new HashMap<String, TestNode>();

    private static TestNode activeTestRoot, lastTestRoot;

    public static TestNode getActiveTestRoot(Project project) {
//        return activeTestRoots.get(key(project));
        return activeTestRoot;
    }

    public static void setActiveTestRoot(Project project, TestNode root) {
//        final String key = key(project);
//        activeTestRoots.put(key, root);
        activeTestRoot = root;

        if (root != null) {
//            lastTestRoots.put(key, root);
            lastTestRoot = root;
        }
    }

    public static TestNode getLastTestRoot(Project project) {
//        return lastTestRoots.get(key(project));
        return lastTestRoot;
    }

//    private static final String key(Project project) {
//        return project.getProjectFilePath();
//    }
}
