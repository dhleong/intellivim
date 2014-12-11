package org.intellivim.core.model;

import com.intellij.execution.Location;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

/**
* @author dhleong
*/
public class VimDataContext
        implements DataContext {
    private final Project project;
    private Editor editor;
    private final Location<?> loc;

    public VimDataContext(
            final Project project, Editor editor,
            final Location<?> loc) {
        this.project = project;
        this.editor = editor;
        this.loc = loc;
    }

    @Nullable
    @Override
    public Object getData(@NonNls final String dataId) {
        System.out.println("getData(" + dataId);

        if (CommonDataKeys.PROJECT.getName().equals(dataId)) {
            return project;
        } else if (CommonDataKeys.EDITOR.getName().equals(dataId)) {
            return editor;
        } else if (Location.DATA_KEY.getName().equals(dataId)) {
            return loc;
        }
        return null;
    }
}
