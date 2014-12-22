package org.intellivim.inject;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.util.Condition;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.apache.commons.lang.StringUtils;
import org.intellivim.ICommand;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Set;

/**
 * Client name constants holder. Also the Injector
 *  for @ClientSpecific things
 * @author dhleong
 */
public class Client implements Injector<Object> {

    public static final String VIM = "vim";

    /**
     * @see @ClientSpecific
     */
    public static final String DEFAULT = "_";

    static final String SPEC = "";

    static Reflections reflector;

    static Reflections reflections() {
        final Reflections cached = reflector;
        if (cached != null)
            return cached;

        final Reflections newInstance = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("org.intellivim"))
                .setScanners(
                        new SubTypesScanner(),
                        new TypeAnnotationsScanner())
                .filterInputsBy(new FilterBuilder().include("org.intellivim.*"))
        );
        reflector = newInstance;
        return newInstance;
    }

    public static <T> Collection<Class<? extends T>> candidates(final Class<T> parentType) {
        final Set<Class<? extends T>> base = reflections().getSubTypesOf(parentType);
        return ContainerUtil.filter(base, new Condition<Class<? extends T>>() {
            @Override
            public boolean value(final Class<? extends T> aClass) {
                final Class<?>[] interfaces = aClass.getInterfaces();
                return ArrayUtil.contains(parentType, interfaces)
                    || aClass.getSuperclass() == parentType;
            }
        });
    }

    @Override
    public boolean canInject(Field field, ICommand command) {
        return isPossiblyClientSpecific(field.getType());
    }

    @Override
    public boolean inject(Gson gson, Field field, ICommand command, JsonObject json) throws IllegalAccessException {
        Object inflated = inflateFor(gson, field.getType(), json);
        if (inflated != null)
            field.set(command, inflated);
        return false;
    }

    static boolean isPossiblyClientSpecific(Class<?> klass) {
        return klass.isInterface() || Modifier.isAbstract(klass.getModifiers());
    }

    @SuppressWarnings("unchecked")
    static <T> T inflateFor(Gson gson, Class<T> klass, JsonObject json) {
        String clientName = json.has("client") ? json.get("client").getAsString() : null;
        if (StringUtils.isEmpty(clientName)) {
            T def = inflateDefaultFor(gson, klass, json);
            if (def != null)
                return def;

            throw new IllegalArgumentException("Client name must not be null or empty");
        }

        final Class<?> implementation = pickClientImplementation(klass, clientName);
        if (implementation != null) {
            return (T) inflate(gson, implementation, json);
        }

        return inflateDefaultFor(gson, klass, json);
    }

    private static String getHandledClient(Class<?> implementation) {
        // TODO caching?
        final ClientSpecific meta = implementation.getAnnotation(ClientSpecific.class);
        if (meta == null)
            return null; // for example, mocks or dummies for testing

        return meta.value();
    }

    private static Class<?> pickClientImplementation(Class<?> parentType, String clientName) {
        // TODO caching?
        for (Class<?> implementation : candidates(parentType)) {
            String implClient = getHandledClient(implementation);
            if (SPEC.equals(implClient)) {
                throw new IllegalArgumentException(implementation +
                        " must provide a valid client name for its ClientSpecific annotation");
            } else if (clientName.equals(implClient)) {
                return implementation;
            }
        }

        return null;
    }

    private static Object inflate(Gson gson, Class<?> implementation, JsonObject json) {
        return gson.fromJson(json, implementation);
    }

    @SuppressWarnings("unchecked")
    private static <T> T inflateDefaultFor(Gson gson, Class<T> klass, JsonObject json) {
        for (Class<?> implementation : candidates(klass)) {
            if (DEFAULT.equals(getHandledClient(implementation)))
                return (T) inflate(gson, implementation, json);
        }

        // nope :(
        return null;
    }

}
