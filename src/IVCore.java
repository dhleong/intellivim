import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import org.intellivim.core.command.complete.CompleteCommand;
import org.jetbrains.annotations.NotNull;

/**
 * Created by dhleong on 11/3/14.
 */
public class IVCore implements ApplicationComponent, Runnable {
    public IVCore() {
    }

    public void initComponent() {
        System.out.println("Hello");
        ApplicationManager.getApplication().invokeLater(this);
    }

    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    @NotNull
    public String getComponentName() {
        return "IVCore";
    }

    @Override
    public void run() {
        final Application app = ApplicationManager.getApplication();
        final boolean isActive = app.isActive();
        if (!isActive) {
            app.invokeLater(this);
            return;
        }

        // active!
        new CompleteCommand().execute("/Users/dhleong/IdeaProjects/DummyProject/DummyProject.iml");
    }
}
