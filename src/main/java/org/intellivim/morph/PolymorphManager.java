package org.intellivim.morph;

import com.google.gson.JsonObject;
import org.intellivim.Command;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author dhleong
 */
public class PolymorphManager {

    static final class ImplMeta {
        final Class<?> klass;
        final String[] params;

        ImplMeta(final Class<?> klass, final String[] params) {
            this.klass = klass;
            this.params = params;
        }
    }

    final Map<Class<?>, Polymorpher> morphers;
    final Map<String, List<ImplMeta>> commandImpls;

    public PolymorphManager(final Reflections ref) {
        morphers = initMorphers(ref);
        commandImpls = initImpls(ref);
    }

    public Class<?> getCommandImplementation(final JsonObject obj,
            final Class<?> baseClass, final String command) {
        final Polymorpher morpher = getPolymorpher(obj, baseClass);
        List<ImplMeta> impls = commandImpls.get(command);
        if (impls == null) {
            throw new IllegalStateException("No @CommandImpls found for " + command);
        }

        for (ImplMeta impl : impls) {
            if (morpher.matches(obj, impl.params))
                return impl.klass;
        }

        throw new IllegalStateException("No matching @CommandImpl for " + command + ", " + obj);
    }

    public Polymorpher getPolymorpher(JsonObject obj, Class<?> baseClass) {
        final Command command = baseClass.getAnnotation(Command.class);
        final Class<? extends Polymorpher> morpherClass = command.morpher();
        if (morpherClass == Polymorpher.class) {
            throw new IllegalArgumentException("No morpher registered for abstract " + baseClass);
        }

        final Polymorpher morpher = morphers.get(morpherClass);
        if (morpher == null) {
            throw new IllegalArgumentException("Unknown morpher "
                    + morpherClass + " for command " + baseClass);
        }

        return morpher;
    }

    private static Map<Class<?>, Polymorpher> initMorphers(final Reflections ref) {
        final Map<Class<?>, Polymorpher> map = new HashMap<Class<?>, Polymorpher>();

        final Set<Class<? extends Polymorpher>> types =
                ref.getSubTypesOf(Polymorpher.class);
        for (Class<? extends Polymorpher> type : types) {
            try {
                map.put(type, type.newInstance());
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalStateException("Unable to instantiate polymorpher " + type, e);
            }
        }

        return map;
    }

    private static Map<String, List<ImplMeta>> initImpls(final Reflections ref) {
        final Map<String, List<ImplMeta>> map = new HashMap<String, List<ImplMeta>>();
        final Set<Class<?>> types = ref.getTypesAnnotatedWith(CommandImpl.class);
        for (Class<?> type : types) {
            final CommandImpl impl = type.getAnnotation(CommandImpl.class);
            final String command = impl.of();
            final String[] params = impl.whenParams();

            final List<ImplMeta> existingList = map.get(command);
            final List<ImplMeta> list;
            if (existingList == null) {
                list = new ArrayList<ImplMeta>();
                map.put(command, list);
            } else {
                list = existingList;
            }

            list.add(new ImplMeta(type, params));
        }
        return map;
    }

}
