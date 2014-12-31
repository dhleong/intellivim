package org.intellivim.inject;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.intellivim.ICommand;
import org.intellivim.ProjectCommand;
import org.intellivim.core.util.ProjectUtil;

import java.lang.reflect.Field;

/**
 * @author dhleong
 */
public class PsiFileInjector implements Injector<PsiFile> {

    static final String DEFAULT_FIELD_NAME = "file";

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public boolean canInject(final Field field, final ICommand command) {
        return field.getType().isAssignableFrom(PsiFile.class)
                && command instanceof ProjectCommand;
    }

    @Override
    public boolean inject(final Gson gson, final Field field, final ICommand command,
            final JsonObject json) throws IllegalAccessException {

        final String fieldName = field.getName();
        if (!(json.has(fieldName) || json.has(DEFAULT_FIELD_NAME)))
            return false;

        final String path = json.has(fieldName)
            ? json.get(fieldName).getAsString()
            : json.get(DEFAULT_FIELD_NAME).getAsString();
        final Project project = ((ProjectCommand) command).getProject();
        field.set(command, ProjectUtil.getPsiFile(project, path));
        return true;
    }
}
