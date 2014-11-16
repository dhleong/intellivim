import com.google.gson.Gson;
import com.intellij.openapi.components.ApplicationComponent;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.intellivim.CommandExecutor;
import org.intellivim.IVGson;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by dhleong on 11/3/14.
 */
public class IVCore implements ApplicationComponent {
    private static final int PORT = 4846;
    private HttpServer server;

    public IVCore() {
    }

    @Override
    public void initComponent() {
//        System.out.println("Hello");
//        ApplicationManager.getApplication().invokeLater(this);
        try {
            // TODO bind on any port, save to a file?
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/command", new CommandHandler());
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
            return;
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

//    @Override
//    public void run() {
//        final Application app = ApplicationManager.getApplication();
//        final boolean isActive = app.isActive();
//        if (!isActive) {
//            app.invokeLater(this);
//            return;
//        }
//
//        // active!
//        new CompleteCommand().execute("/Users/dhleong/IdeaProjects/DummyProject/DummyProject.iml");
//    }

    static class CommandHandler implements HttpHandler {
        final Gson gson = IVGson.newInstance();
        final CommandExecutor executor = new CommandExecutor(gson);

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            final Future<Result> future = executor.execute(httpExchange.getRequestBody());
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
                return SimpleResult.error(e);
            }
        }
    }
}
