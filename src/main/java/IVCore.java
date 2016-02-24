import com.google.gson.Gson;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.AppIconScheme;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.ui.AppIcon;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.intellivim.CommandExecutor;
import org.intellivim.IVGson;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.jetbrains.annotations.NotNull;
import org.reflections.ReflectionUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * @author dhleong
 */
public class IVCore implements ApplicationComponent {
    private static final String INTELLIVIM_PLUGIN_ID = "org.intellivim";

    static Logger logger = Logger.getLogger("IntelliVim:IVCore");

    private HttpServer server;
    private InstanceInfo instance;

    public IVCore() {
    }

    @Override
    public void initComponent() {

        wrapAppIcon();

        try {
            server = HttpServer.create(new InetSocketAddress(0), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/command", new CommandHandler());
            server.start();

            final int port = server.getAddress().getPort();
            instance = InstanceInfo.create(port);
            instance.write();

            logger.info("IntelliVim server listening on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
            logger.warning("Unable to start IntelliVim server");
        }
    }


    @Override
    public void disposeComponent() {
        HttpServer server = this.server;
        if (server != null) {
            server.stop(0);
            this.server = null;
        }

        InstanceInfo info = instance;
        if (info != null) {
            info.delete();
            instance = null;
        }
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "IVCore";
    }

    @NotNull
    static File getIvHome() {
        return new File(System.getProperty("user.home"), ".intellivim");
    }

    @NotNull
    public static PluginId getPluginId() {
        return PluginId.getId(INTELLIVIM_PLUGIN_ID);
    }

    @NotNull
    public static String getVersion() {
//        if (!ApplicationManager.getApplication().isInternal()) {
            final IdeaPluginDescriptor plugin = PluginManager.getPlugin(getPluginId());
            return plugin != null ? plugin.getVersion() : "SNAPSHOT";
//        }
//        else {
//            return "INTERNAL";
//        }
    }

    static class CommandHandler implements HttpHandler {
        final Gson gson = IVGson.newInstance();
        final CommandExecutor executor = new CommandExecutor(gson, getVersion());

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {

            final Future<Result> future;
            try {
                future = executor.execute(httpExchange.getRequestBody());
            } catch (Exception e) {
                // catch any troublemakers
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            final Result result = collectResult(future);
            final String json = gson.toJson(result);
            final int code = result.isSuccess() ? 200 : 400;

            // send headers
            httpExchange.sendResponseHeaders(code, json.length());

            // write result
            final OutputStreamWriter out = new OutputStreamWriter(httpExchange.getResponseBody());
            out.write(json);
            out.close();
        }

        static Result collectResult(Future<Result> future) {
            try {
                return future.get();
            } catch (Exception e) {
                e.printStackTrace();
                return SimpleResult.error(e);
            }
        }
    }

    /**
     * Pretty much every action triggers an index update, since
     *  we're editing files outside of IntelliJ's knowledge.
     *  There may be smarter ways around this, but it's what
     *  we have for now.
     * At any rate, when the index is complete, IntelliJ wants
     *  to request the user's attention, which is super
     *  annoying---especially on OSX. So, we'll wrap this up
     *  and prevent that from happening.
     * In the future, we could perhaps forward these hints
     *  to the client.
     */
    @SuppressWarnings("unchecked")
    private void wrapAppIcon() {
        final AppIcon wrapped = AppIcon.getInstance();
        AppIcon wrapper = new AppIcon() {
            @Override
            public boolean setProgress(final Project project, final Object processId,
                    final AppIconScheme.Progress scheme, final double value,
                    final boolean isOk) {
                return wrapped.setProgress(project, processId, scheme, value, isOk);
            }

            @Override
            public boolean hideProgress(final Project project, final Object processId) {
                return wrapped.hideProgress(project, processId);
            }

            @Override
            public void setErrorBadge(final Project project, final String text) {
                wrapped.setErrorBadge(project, text);
            }

            @Override
            public void setOkBadge(final Project project, final boolean visible) {
                wrapped.setOkBadge(project, visible);
            }

            @Override
            public void requestAttention(final Project project, final boolean critical) {
                if (critical) {
                    wrapped.requestAttention(project, true);
                }
            }

            @Override
            public void requestFocus(final IdeFrame frame) {
                // I'd prefer that you didn't
            }
        };

        final Set<Field> iconFields = ReflectionUtils.getAllFields(AppIcon.class,
                ReflectionUtils.withTypeAssignableTo(AppIcon.class));
        for (Field field : iconFields) {
            try {
                field.setAccessible(true);
                field.set(null, wrapper);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private static class InstanceInfo {
        private final long created = System.currentTimeMillis();

        public int port;
        public String version;

        public static @NotNull InstanceInfo create(int port) {
            final InstanceInfo info = new InstanceInfo();
            info.port = port;
            info.version = getVersion();
            return info;
        }

        /**
         * Writes this instance info to disk so clients can find it.
         * See: https://github.com/dhleong/intellivim/issues/33
         */
        public void write() {
            File file = getFile();
            try {
                final OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
                final Writer writer = new OutputStreamWriter(os);
                IVGson.newInstance().toJson(this, writer);
                writer.close();
                os.close();
                logger.info("Created instance file: " + file.getAbsolutePath()
                    + "; port=" + port + "; version=" + version);

                // try to ensure it doesn't stick around, in case our
                //  disposeComponent() method isn't called for whatever reason;
                //  this is NOT reliable---unexpected JVM termination will NOT
                //  respect this request!
                file.deleteOnExit();
            } catch (IOException e) {
                e.printStackTrace();
                logger.warning("Unable to write .instances/ file: " + file.getAbsolutePath());
            }
        }

        public void delete() {
            final File file = getFile();
            if (file.delete()) {
                logger.info("Removed instance file: " + file.getAbsolutePath());
            } else {
                logger.warning("Could not delete " + file.getAbsolutePath());
            }
        }

        private File getFile() {
            return new File(getInstancesDir(), String.valueOf(created));
        }

        private static @NotNull File getInstancesDir() {
            File dir = new File(getIvHome(), ".instances");
            if (!dir.exists() && !dir.mkdirs()) {
                // shouldn't happen...
                logger.warning("Couldn't create .instances dir!");
            }
            return dir;
        }
    }
}
