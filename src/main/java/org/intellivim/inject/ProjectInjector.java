package org.intellivim.inject;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import org.intellivim.ICommand;
import org.intellivim.core.util.ProjectUtil;

import java.lang.reflect.Field;

/**
 * @author dhleong
 */
public class ProjectInjector implements Injector<Project> {

    @Override
    public int getPriority() {
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean canInject(final Field field, final ICommand command) {
        return field.getType().isAssignableFrom(Project.class);
    }

    @Override
    public boolean inject(final Gson gson, final Field field, final ICommand command,
            final JsonObject json) throws IllegalAccessException {

        if (!json.has(field.getName()))
            return false;

        final JsonElement el = json.get(field.getName());
        if (!(el.isJsonPrimitive() && el.getAsJsonPrimitive().isString())) {
//            throw new IllegalArgumentException("Project argument should be a string");
            return false;
        }

        final String path = el.getAsString();
        final Project project = ProjectUtil.ensureProject(path);
        field.set(command, project);
        return true;
    }
}
