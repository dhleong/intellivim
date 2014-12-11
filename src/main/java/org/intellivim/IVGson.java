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
import com.intellij.psi.PsiFile;
import org.intellivim.core.util.ProjectUtil;
import org.intellivim.inject.Inject;
import org.intellivim.inject.Injector;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.reflections.ReflectionUtils.withAnnotation;

/**
 * Factory for Gson instances used to read
 *  Command inputs and serialize Results
 *
 * @author dhleong
 */
public class IVGson {

    static Map<String, Class<?>> commandMap;
    static List<Injector<?>> injectors;

    public static Gson newInstance() {
        if (commandMap == null) {
            init();
        }

        // FIXME booleans may need to be serialized as 1/0 since vim
        //  doesn't understand true/false....
        return new GsonBuilder()
                .registerTypeAdapter(Project.class, new ProjectTypeAdapter())
                .registerTypeAdapter(HighlightSeverity.class, new SeverityTypeAdapter())
                .registerTypeAdapterFactory(new CommandTypeAdapterFactory())
                .registerTypeAdapterFactory(new OnlyInjectableTypesAdapterFactory())
                .create();
    }

    private static void init() {
        final Reflections ref = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("org.intellivim"))
                .setScanners(
                        new SubTypesScanner(),
                        new TypeAnnotationsScanner())
                .addClassLoader(ICommand.class.getClassLoader())
                .filterInputsBy(new FilterBuilder().include("org.intellivim.*"))
        );

        commandMap = initCommands(ref);
        injectors = initInjectors(ref);
    }

    private static Map<String, Class<?>> initCommands(Reflections ref) {
        final HashMap<String, Class<?>> map = new HashMap<String, Class<?>>();

        final Set<Class<?>> commands = ref.getTypesAnnotatedWith(Command.class);
        for (Class<?> commandClass : commands) {
            final Command meta = commandClass.getAnnotation(Command.class);
            final String name = meta.value();
            map.put(name, commandClass);
        }

        return map;
    }

    private static List<Injector<?>> initInjectors(Reflections ref) {
        final Set<Class<? extends Injector>> set = ref.getSubTypesOf(Injector.class);
        final List<Injector<?>> list = new ArrayList<Injector<?>>(set.size());
        for (Class<? extends Injector> type : set) {
            try {
                list.add(type.newInstance());
            } catch (Exception e) {
                throw new IllegalStateException("Could not inflate Injector " + type);
            }
        }

        return list;
    }

    private static Class<?> getCommandClass(String commandName) {
        final Class<?> klass = commandMap.get(commandName);
        if (klass != null)
            return klass;

        throw new IllegalArgumentException("Unknown command `" + commandName + "`");
    }

    private static class CommandTypeAdapterFactory implements TypeAdapterFactory {

        private static Map<Field, Injector<?>> sFieldInjectors = new HashMap<Field, Injector<?>>();

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
                @SuppressWarnings("unchecked")
                public T read(JsonReader jsonReader) throws IOException {
                    final JsonObject obj = jsonAdapter.read(jsonReader);
                    final String commandName = obj.get("command").getAsString();
                    final Class<?> commandClass = getCommandClass(commandName);
                    T result = (T) gson.getDelegateAdapter(CommandTypeAdapterFactory.this,
                            TypeToken.get(commandClass)).fromJsonTree(obj);

                    final Set<Field> injectedFields = ReflectionUtils
                            .getAllFields(commandClass, withAnnotation(Inject.class));
                    for (Field f : injectedFields) {
                        injectField(gson, f, obj, (ICommand) result);
                    }

                    final Set<Field> requiredFields = ReflectionUtils
                            .getAllFields(commandClass, withAnnotation(Required.class));
                    for (Field f : requiredFields) {
                        ensureRequiredField(f, obj, result);
                    }

                    return result;
                }

            };
        }

        static void injectField(Gson gson, Field f, JsonObject obj, ICommand result) {
            try {
                // cached?
                Injector<?> cached = sFieldInjectors.get(f);
                if (cached != null) {
                    f.setAccessible(true);
                    if (cached.inject(gson, f, result, obj)) {
                        return;
                    }
                }

                // no? search
                for (Injector<?> injector : injectors) {
                    if (!injector.canInject(f, result)) {
                        continue;
                    }

                    // got it!
                    f.setAccessible(true);
                    if (injector.inject(gson, f, result, obj)) {
                        // this is THE ONE. cache it
                        sFieldInjectors.put(f, injector);
                        return;
                    }
                }

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        static void ensureRequiredField(Field f, JsonObject obj, Object result) {
            if (!obj.has(f.getName())) {
                try {
                    f.setAccessible(true);
                    if (f.get(result) == null) {
                        throw new IllegalArgumentException("The `"
                                + f.getName() + "` field is required");
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
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

    /** Basically always returns "null" for types that need to be @Injected */
    private static class OnlyInjectableTypesAdapterFactory
            implements TypeAdapterFactory {

        // NB Could/should probably build this with Reflections and
        //  an annotation on the Injector
        HashSet<Class<?>> types = new HashSet<Class<?>>(Arrays.asList(
            PsiFile.class
        ));

        @Override
        public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> typeToken) {
            if (!types.contains(typeToken.getRawType()))
                return null;

            return new TypeAdapter<T>() {
                @Override
                public void write(final JsonWriter jsonWriter, final T t) throws
                        IOException {
                    jsonWriter.value(t.toString()); // ?!
                }

                @Override
                public T read(final JsonReader jsonReader) throws IOException {
                    jsonReader.skipValue();
                    return null; // ALWAYS
                }
            };
        }
    }
}
