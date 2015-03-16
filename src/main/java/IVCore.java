import com.google.gson.Gson;
import com.intellij.openapi.components.ApplicationComponent;
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

import java.io.IOException;
import java.io.OutputStreamWriter;
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
    private Logger logger = Logger.getLogger("IntelliVim:IVCore");

    private static final int PORT = 4846;
    private HttpServer server;

    public IVCore() {
    }

    @Override
    public void initComponent() {

        wrapAppIcon();

        try {
            // TODO bind on any port, save to a file?
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/command", new CommandHandler());
            server.start();

            logger.info("IntelliVim server listening on port " + server.getAddress().getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void disposeComponent() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "IVCore";
    }

    static class CommandHandler implements HttpHandler {
        final Gson gson = IVGson.newInstance();
        final CommandExecutor executor = new CommandExecutor(gson);

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

            System.out.println(json);

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
}
