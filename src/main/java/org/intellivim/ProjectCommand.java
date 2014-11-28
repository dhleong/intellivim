package org.intellivim;

import com.intellij.openapi.project.Project;

/**
 * Created by dhleong on 11/16/14.
 */
public abstract class ProjectCommand implements ICommand {

    @Required protected final Project project;

    public ProjectCommand(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }
}
