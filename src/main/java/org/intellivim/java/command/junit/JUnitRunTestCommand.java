package org.intellivim.java.command.junit;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.junit.JUnitProcessHandler;
import com.intellij.execution.junit2.segments.DeferredActionsQueueImpl;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import com.intellij.rt.execution.junit.segments.PacketProcessor;
import com.intellij.rt.execution.junit.segments.PoolOfDelimiters;
import org.intellivim.Command;
import org.intellivim.core.command.run.AsyncRunner;
import org.intellivim.core.command.test.AbstractRunTestCommand;
import org.intellivim.core.command.test.AsyncTestRunner;
import org.intellivim.core.command.test.TestNode;
import org.intellivim.core.command.test.TestObjectManager;
import org.intellivim.core.command.test.TestState;
import org.intellivim.core.util.BuildUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author dhleong
 */
@Command("junit")
public class JUnitRunTestCommand extends AbstractRunTestCommand {

    private static final boolean DEBUG = false;

    static final String JUNIT_CONFIG_CLASSNAME =
            "com.intellij.execution.junit.JUnitConfiguration";
    static final String JUNIT_CONSOLE_PROPS_CLASSNAME =
            "com.intellij.execution.junit2.ui.properties.JUnitConsoleProperties";

    static Constructor<? extends TestConsoleProperties> sJunitConsolePropertiesCtor;

    private TestObjectManager mObjects = new TestObjectManager();
    private ClassLoader mClassLoader;

    public JUnitRunTestCommand(final Project project,
           AsyncTestRunner runner, PsiFile file, int offset) {
        super(project, runner, file, offset);
    }

    @Override
    protected String getTestFrameworkName() {
        return "JUnit";
    }

    @Override
    protected TestConsoleProperties createProperties(final Project project,
            final Executor executor) {

        final RunConfiguration configuration =
                BuildUtil.createConfiguration(project, file, offset);

        // instanceof and klass.instanceOf both fail (WTF?)
        if (configuration == null
                || !JUNIT_CONFIG_CLASSNAME.equals(configuration.getClass().getName())) {
            return null;
        }

//        if (!(configuration instanceof JUnitConfiguration)) {
//            System.out.println("Got: " + configuration);
//            return null;
//        }

        // TODO cache class for faster acceptance; pre-fill cache

//        JUnitConfiguration config = (JUnitConfiguration) configuration;
//        return new JUnitConsoleProperties(config, executor);
        mClassLoader = configuration.getClass().getClassLoader();
        return newJUnitConsoleProperties(configuration, executor);
    }

    @Override
    protected void handleProcessStarted(final RunContentDescriptor descriptor,
            final ProcessHandler handler,
            final TestConsoleProperties properties,
            final AsyncTestRunner asyncRunner) {

        if (!(handler instanceof JUnitProcessHandler)) {
            super.handleProcessStarted(descriptor, handler, properties, asyncRunner);
            return;
        }

        // NB It is absolutely impossible to do anything simple with this.
        //  They have a nice JUnitListenersNotifier that they attach
        //  to the Extractors, but the Extractor wraps the Queue that holds
        //  the notifier in an Anonymous class, and does not keep a reference
        //  to the original. So, no way to get at the Notifier, or the
        //  JUnitRunningModel that the original holds :(
        final JUnitProcessHandler junitHandler = (JUnitProcessHandler) handler;

        if (mObjects == null) {
            // no idea how this could be null, but...
            mObjects = new TestObjectManager();
        }

        // sigh. replace the packet dispatcher and handle everything by hand.
        // see above for why we can't just attach a listener, even if
        //  we use all kinds of reflection.
        junitHandler.getOut().setPacketDispatcher(new PacketProcessor() {
            @Override
            public void processPacket(final String packet) {
                try {
                    JUnitRunTestCommand.this.processPacket(packet, asyncRunner);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

        }, new DeferredActionsQueueImpl());

        // also attach a ProcessListener since we're overriding
        //  the default behavior
        handler.addProcessListener(new ProcessAdapter() {

            @Override
            public void onTextAvailable(final ProcessEvent event, final Key outputType) {
                System.out.println("TEXT: " + event.getText());
            }

            @Override
            public void processTerminated(final ProcessEvent event) {
                asyncRunner.terminate();
            }
        });
    }

    /**
     * NB Public for testing
     * @see com.intellij.execution.junit2.ui.TestsPacketsReceiver
     */
    @SuppressWarnings("JavadocReference")
    public final void processPacket(final String packet, final AsyncTestRunner runner)
            throws Exception {
        System.out.println("<<" + packet + "((END))");

        if (packet.startsWith(PoolOfDelimiters.TREE_PREFIX)) {
            runner.onStartTesting(readNode(
                    new JunitObjectReader(packet, PoolOfDelimiters.TREE_PREFIX)));

        }  else if (packet.startsWith(PoolOfDelimiters.INPUT_COSUMER)) {
            notifyTestStart(new JunitObjectReader(packet,
                    PoolOfDelimiters.INPUT_COSUMER));

        }  else if (packet.startsWith(PoolOfDelimiters.CHANGE_STATE)) {
            changeNodeState(new JunitObjectReader(packet,
                    PoolOfDelimiters.CHANGE_STATE),
                runner);

        }  else if (packet.startsWith(PoolOfDelimiters.TESTS_DONE)) {
//            notifyFinish(new JunitObjectReader(packet,
//                    PoolOfDelimiters.TESTS_DONE));
            runner.onFinishTesting();

        } else if (packet.startsWith(PoolOfDelimiters.OBJECT_PREFIX)) {
            readObject(new JunitObjectReader(packet, PoolOfDelimiters.OBJECT_PREFIX));

        }

    }

    void notifyTestStart(JunitObjectReader in) {
        TestNode node = readNodeReference(in);

        if (DEBUG) {
            // I think the state change is sufficient?
            System.out.println(" * notifyTestStart:" + node);
        }
    }

    void changeNodeState(JunitObjectReader in, final AsyncTestRunner runner) {
        TestNode node = readNodeReference(in);
        final int state = in.readInt();
        TestState newState = JunitUtil.getStateFromIndex(state);

        if (DEBUG) {
            System.out.println(" * * TestState: " + node + " -> " + newState);
            System.out.println("      [" + in.in + "]");
        }
        switch (newState) {
        case FAILED:
        case ERROR:
            String message = in.readLimitedString();
            String stack = in.readLimitedString();
            runner.onTestOutput(node, message, AsyncRunner.OutputType.STDERR);
            runner.onTestOutput(node, stack, AsyncRunner.OutputType.STDERR);

            if (DEBUG) {
                System.out.println("    " + message);
                System.out.println(stack);
                System.out.println();
            }

            // We could just fall through and let it get set in the default block,
            //  but that's just a little too hard to follow, so we'll be explicit
            node.setState(newState);
            break;

        case PASSED:
            if (node.state == TestState.FAILED || node.state == TestState.ERROR) {
                // it's already set to failed or error; this problem occurs because
                // IntelliJ decided that "passed" and "completed" were the same,
                //  and that "failed" or "error" don't imply "completed" :/
                break;
            }

            // NB fall through
        default:

            // in most cases, just go ahead and set
            node.setState(newState);
        }

        runner.onTestStateChanged(node);
    }

    void notifyFinish(JunitObjectReader in) {
        final int duration = in.readInt();
        System.out.println(" * * * TestFinished in: " + duration);
    }

    private void readObject(final JunitObjectReader in) {
        final JunitTestInfo info = JunitTestInfo.read(in);
        mObjects.register(info.toTestNode());
    }

    private TestNode readNode(final JunitObjectReader in) {
        final TestNode first = readNodeReference(in);
        final int kids = in.readInt();
        for (int i=0; i < kids; i++) {
            first.addChild(readNode(in));
        }
        return first;
    }

    private TestNode readNodeReference(JunitObjectReader in) {
        return mObjects.getById(in.nextReference());
    }



    static TestConsoleProperties newJUnitConsoleProperties(RunConfiguration config,
            Executor executor) {

        final Constructor<? extends TestConsoleProperties> ctor = getCtor(
                config.getClass());
        if (ctor == null)
            return null;

        try {
            return ctor.newInstance(config, executor);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    static Constructor<? extends TestConsoleProperties> getCtor(Class<?> configClass) {
        final Constructor<? extends TestConsoleProperties> cached =
                sJunitConsolePropertiesCtor;
        if (cached != null)
            return cached;

        try {

            final Class<? extends TestConsoleProperties> klass =
                    (Class<? extends TestConsoleProperties>)
                            configClass.getClassLoader().loadClass(
                                    JUNIT_CONSOLE_PROPS_CLASSNAME);

            final Constructor<? extends TestConsoleProperties> ctor =
                    klass.getConstructor(configClass, Executor.class);

            sJunitConsolePropertiesCtor = ctor;
            return ctor;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return null;
    }

}
