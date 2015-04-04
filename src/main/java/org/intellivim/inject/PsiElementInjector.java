package org.intellivim.inject;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.intellivim.ICommand;
import org.intellivim.ProjectCommand;
import org.intellivim.core.util.ProjectUtil;

import java.lang.reflect.Field;

/**
 * This attempts to inject the PsiElement from
 *  the field `file` at the offset `offset`.
 *  These must be the exact names of the fields,
 *  but they should be anyway to match with
 *  existing commands, so...
 *
 * @author dhleong
 */
public class PsiElementInjector implements Injector<PsiFile> {

    static final int PRIORITY = PsiFileInjector.PRIORITY + 1;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean canInject(final Field field, final ICommand command) {
        return PsiElement.class.isAssignableFrom(field.getType())
                && !PsiFile.class.isAssignableFrom(field.getType())
                && command instanceof ProjectCommand;
    }

    @Override
    public boolean inject(final Gson gson, final Field field, final ICommand command,
            final JsonObject json) throws IllegalAccessException {
        final int offset = extractOffset(json);
        final PsiFile file = extractFile(command, json);
        if (file == null)
            return false;

        final PsiElement element = file.findElementAt(offset);
        if (element == null) {
            throw new IllegalArgumentException("No element found under cursor");
        }

        final Class<?> fieldType = field.getType();
        final Class<?> actualType = element.getClass();
        if (!fieldType.isAssignableFrom(actualType)) {
            throw new IllegalArgumentException("Expected " + field.getName()
                    + " to be " + fieldType + " but was " + actualType);
        }

        // finally, inject
        field.set(command, element);
        return false;
    }

    private static PsiFile extractFile(final ICommand command,
            final JsonObject json) throws IllegalAccessException {
        if (!json.has("file")) {
            throw new IllegalArgumentException("PsiElement injection requires `file`");
        }

        try {
            final Field fileField = command.getClass().getDeclaredField("file");

            if (!PsiFile.class.isAssignableFrom(fileField.getType())) {
                throw new IllegalStateException("The `file` field must be @Inject PsiFile");
            }

            fileField.setAccessible(true);
            return (PsiFile) fileField.get(command);
        } catch (NoSuchFieldException e) {
            // the `file` field isn't declared in the class; try to extract
            //  it from the json

            final String path = json.get("file").getAsString();
            final Project project = ((ProjectCommand) command).getProject();
            if (project != null) {
                return ProjectUtil.getPsiFile(project, path);
            } else {
                throw new IllegalArgumentException("PsiElement injection requires `project`");
            }
        }
    }

    private static int extractOffset(final JsonObject json) {
        // we'll just take from the json
        if (!json.has("offset")) {
            throw new IllegalArgumentException("PsiElement injection requires `offset`");
        }
        return json.get("offset").getAsInt();
    }
}
