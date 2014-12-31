package org.intellivim;

import com.intellij.openapi.project.Project;
import org.intellivim.inject.Inject;

/**
 * @author dhleong
 */
public abstract class ProjectCommand implements ICommand {

    @Required @Inject protected final Project project;

    public ProjectCommand(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }
}
