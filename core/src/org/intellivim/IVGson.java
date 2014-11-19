package org.intellivim;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.project.Project;
import org.intellivim.core.util.ProjectUtil;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.reflections.ReflectionUtils.withAnnotation;

/**
 * Created by dhleong on 11/9/14.
 */
public class IVGson {

    static Map<String, Class<?>> commandMap;

    public static Gson newInstance() {
        if (commandMap == null) {
            commandMap = initCommands();
        }

        // FIXME booleans may need to be serialized as 1/0 since vim
        //  doesn't understand true/false....
        return new GsonBuilder()
                .registerTypeAdapter(Project.class, new ProjectTypeAdapter())
                .registerTypeAdapter(HighlightSeverity.class, new SeverityTypeAdapter())
                .registerTypeAdapterFactory(new CommandTypeAdapterFactory())
                .create();
    }

    private static Map<String, Class<?>> initCommands() {
        final HashMap<String, Class<?>> map = new HashMap<String, Class<?>>();

        final Reflections ref = new Reflections(new ConfigurationBuilder()
            .setUrls(ClasspathHelper.forPackage("org.intellivim"))
            .setScanners(
                    new SubTypesScanner(),
                    new TypeAnnotationsScanner())
            .addClassLoader(ICommand.class.getClassLoader())
            .filterInputsBy(new FilterBuilder().include("org.intellivim.*"))
        );

        final Set<Class<?>> commands = ref.getTypesAnnotatedWith(Command.class);
        for (Class<?> commandClass : commands) {
            final Command meta = commandClass.getAnnotation(Command.class);
            final String name = meta.value();
            map.put(name, commandClass);
        }

        return map;
    }

    private static Class<?> getCommandClass(String commandName) {
        final Class<?> klass = commandMap.get(commandName);
        if (klass != null)
            return klass;

        throw new RuntimeException("Unknown command `" + commandName + "`");
    }

    private static class CommandTypeAdapterFactory implements TypeAdapterFactory {

        @Override
        public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> typeToken) {
            if (!ICommand.class.isAssignableFrom(typeToken.getRawType()))
                return null;

            final TypeAdapter<T> defaultAdapter = gson.getDelegateAdapter(this, typeToken);
            final TypeAdapter<JsonObject> jsonAdapter = gson.getAdapter(JsonObject.class);

            return new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter jsonWriter, T t) throws IOException {
                    // probably shouldn't need to serialize a command, but....
                    defaultAdapter.write(jsonWriter, t);
                }

                @Override
                public T read(JsonReader jsonReader) throws IOException {
                    final JsonObject obj = jsonAdapter.read(jsonReader);
                    final String commandName = obj.get("command").getAsString();
                    final Class<?> commandClass = getCommandClass(commandName);
                    T result = (T) gson.getDelegateAdapter(CommandTypeAdapterFactory.this,
                            TypeToken.get(commandClass)).fromJsonTree(obj);

                    // TODO it'd be great if we could @Inject types like VirtualFile
                    //  which depend on the Project, for example

                    final Set<Field> requiredFields = ReflectionUtils
                            .getAllFields(commandClass, withAnnotation(Required.class));
                    for (Field f : requiredFields) {
                        ensureRequiredField(f, obj, result);
                    }

                    return result;
                }
            };
        }

        static void ensureRequiredField(Field f, JsonObject obj, Object result) {
            if (!obj.has(f.getName())) {
                throw new IllegalStateException("The `"
                        + f.getName() + "` field is required");
            }
        }
    }

    private static class ProjectTypeAdapter extends TypeAdapter<Project> {
        @Override
        public void write(JsonWriter jsonWriter, Project project) throws IOException {
            // NB project objects shouldn't be serialized
            jsonWriter.nullValue();
        }

        @Override
        public Project read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.STRING) {
                String path = jsonReader.nextString();
                return ProjectUtil.ensureProject(path);
            }

            throw new IllegalArgumentException("Project argument should be a string");
        }
    }

    private static class SeverityTypeAdapter implements JsonSerializer<HighlightSeverity> {
        @Override
        public JsonElement serialize(HighlightSeverity highlightSeverity,
                 Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(highlightSeverity.getName());
        }
    }
}
