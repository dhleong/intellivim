package org.intellivim;

import com.intellij.openapi.project.Project;

/**
 * Created by dhleong on 11/16/14.
 */
public abstract class ProjectCommand implements ICommand {

    @Required protected Project project;

    public Project getProject() {
        return project;
    }
}
