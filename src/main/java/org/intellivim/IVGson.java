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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.intellivim.core.util.Profiler;
import org.intellivim.core.util.ProjectUtil;
import org.intellivim.inject.Inject;
import org.intellivim.inject.Injector;
import org.intellivim.morph.PolymorphManager;
import org.intellivim.morph.Polymorpher;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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

    /** Wraps the input ICommand for multi-stage parsing */
    @SuppressWarnings("unchecked")
    public static class RawCommand {

        private static Map<Field, Injector<?>> sFieldInjectors = new HashMap<Field, Injector<?>>();

        private Class<?> commandClass;
        final Gson gson;
        final JsonObject obj;
        final ICommand raw;
        final Set<Field> fieldsToInject;

        private RawCommand(final Gson gson, Class<?> commandClass, JsonObject obj,
                ICommand raw) {
            this.gson = gson;
            this.commandClass = commandClass;
            this.obj = obj;
            this.raw = raw;

            fieldsToInject = ReflectionUtils
                    .getAllFields(commandClass, withAnnotation(Inject.class));
            Class<?> superClass = commandClass.getSuperclass();
            while (superClass != null && superClass != Object.class) {

                Set<Field> superFields = ReflectionUtils
                    .getAllFields(superClass, withAnnotation(Inject.class));
                fieldsToInject.addAll(superFields);

                superClass = superClass.getSuperclass();
            }
        }

        RawCommand(Gson gson, final Class<?> commandClass, JsonObject obj, final Object o) {
            this(gson, commandClass, obj, (ICommand) o);
        }

        public boolean needsInitOnDispatch() {
            return !fieldsToInject.isEmpty();
        }

        public ICommand init() throws IllegalAccessException {
            // inject
//            for (Field f : fieldsToInject) {
//                injectField(gson, f, obj, raw);
//            }

            Profiler profiler = Profiler.with(RawCommand.class);

            // iterate over injectors in order
            for (Injector<?> injector : injectors) {
                for (Field f : fieldsToInject) {
                    if (!injector.canInject(f, raw)) {
                        continue;
                    }

                    // got it!
                    profiler.mark("  inject: " + injector);
                    f.setAccessible(true);
                    injector.inject(gson, f, raw, obj);
                    profiler.mark("injected: " + injector);
                }
            }

            profiler.mark("getRequired");
            final Set<Field> requiredFields = ReflectionUtils
                    .getAllFields(commandClass, withAnnotation(Required.class));
            profiler.mark("gotRequired");
            for (Field f : requiredFields) {
                ensureRequiredField(f, obj, raw);
            }
            profiler.mark("ensuredRequired");

            return raw;
        }

        public boolean needsExecuteOnDispatch() {
            // We could distinguish this better in the future
            return needsInitOnDispatch();
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

    static Map<String, Class<?>> commandMap;
    static List<Injector<?>> injectors;
    static PolymorphManager polymorphManager;

    public static Gson newInstance() {
        if (commandMap == null) {
            init();
        }

        // FIXME booleans may need to be serialized as 1/0 since vim
        //  doesn't understand true/false....
        return new GsonBuilder()
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
        polymorphManager = new PolymorphManager(ref);
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

    private static Map<Class<?>, Polymorpher> initMorphers(Reflections ref) {
        final HashMap<Class<?>, Polymorpher> map = new HashMap<Class<?>, Polymorpher>();

        final Set<Class<? extends Polymorpher>> types = ref.getSubTypesOf(Polymorpher.class);
        for (final Class<? extends Polymorpher> type : types) {
            try {
                map.put(type, type.newInstance());
            } catch (Exception e) {
                throw new IllegalStateException("Unable to instantiate Polymorpher " + type);
            }
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
        Collections.sort(list, new Comparator<Injector<?>>() {
            @Override
            public int compare(final Injector<?> o1, final Injector<?> o2) {
                // manual comparison lets us use Integer.MIN_VALUE without
                //  fear of overflow
                final int left = o1.getPriority();
                final int right = o2.getPriority();
                if (left < right)
                    return -1;
                else if (left == right)
                    return 0;
                else
                    return 1;
            }
        });

        return list;
    }

    private static Class<?> getCommandClass(final JsonObject obj, String commandName) {
        final Class<?> klass = commandMap.get(commandName);
        if (klass != null) {
            if ((klass.getModifiers() & Modifier.ABSTRACT) != 0) {
                return polymorphManager.getCommandImplementation(obj, klass, commandName);
            } else {
                return klass;
            }
        }

        throw new IllegalArgumentException("Unknown command `" + commandName + "`");
    }


    private static class CommandTypeAdapterFactory implements TypeAdapterFactory {

        @Override
        public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> typeToken) {
            if (!RawCommand.class.isAssignableFrom(typeToken.getRawType()))
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
                    final Class<?> commandClass = getCommandClass(obj, commandName);
                    try {
                        return (T) new RawCommand(gson,
                                commandClass,
                                obj,
                                gson.getDelegateAdapter(CommandTypeAdapterFactory.this,
                                        TypeToken.get(commandClass)).fromJsonTree(obj));
                    } catch (Exception e) {
                        throw new RuntimeException("Error parsing " + commandClass + ": " + obj, e);
                    }
                }

            };
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
            PsiFile.class,
            PsiElement.class,
            Project.class
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
