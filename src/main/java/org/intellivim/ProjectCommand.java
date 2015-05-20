package org.intellivim;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.intellivim.core.model.VimEditor;
import org.intellivim.inject.Inject;

/**
 * @author dhleong
 */
public abstract class ProjectCommand implements ICommand, Disposable {

    @Required @Inject protected final Project project;

    public ProjectCommand(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    @Override
    public void dispose() {
        // nop by default
    }

    /**
     * Convenience to create a new EditorEx. It will
     *  be disposed automatically for you in the scope
     *  of this command.
     *
     * @see VimEditor#from(Disposable, PsiFile, int)
     */
    public EditorEx createEditor(PsiFile file, int offset) {
        return VimEditor.from(this, file, offset);
    }
}
