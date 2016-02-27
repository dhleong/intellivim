package org.intellivim;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.intellivim.core.model.VimEditor;
import org.intellivim.inject.Inject;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @NotNull
    protected DataContext getDataContext(Editor editor) {
        return new ProjectCommandDataContext(this, editor);
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

    private static class ProjectCommandDataContext implements DataContext {

        @NotNull final ProjectCommand projectCommand;
        @Nullable final Editor editor;

        public ProjectCommandDataContext(@NotNull ProjectCommand projectCommand,
                                         @Nullable Editor editor) {
            this.projectCommand = projectCommand;
            this.editor = editor;
        }

        @Nullable
        @Override
        public Object getData(@NonNls String dataId) {
            if (PlatformDataKeys.EDITOR.getName().equals(dataId)) {
                return editor;
            } else if (PlatformDataKeys.PROJECT.getName().equals(dataId)) {
                return projectCommand.project;
            } else if (PlatformDataKeys.VIRTUAL_FILE.getName().equals(dataId)
                    && editor != null) {
                return VimEditor.getVirtualFile(editor);
            } else if (PlatformDataKeys.PSI_FILE.getName().equals(dataId)
                    && editor != null) {
                return VimEditor.getPsiFile(editor);
            }
            return null;
        }
    }
}
