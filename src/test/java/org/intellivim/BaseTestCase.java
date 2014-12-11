package org.intellivim;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl;
import org.intellivim.core.util.ProjectUtil;

import java.io.File;

/**
 * Base TestCase for all IntelliVim tests
 * @author dhleong
 */
public abstract class BaseTestCase extends UsefulTestCase {
    private static final String ULTIMATE_MARKER_CLASS = "com.intellij.psi.css.CssFile";

    protected static final String JAVA_PROJECT = "java-project";
    protected static final String RUNNABLE_PROJECT = "runnable-project";
    protected static final String LOOPING_PROJECT = "looping-project";
    protected static final String PROBLEMATIC_FILE_PATH = "src/org/intellivim/javaproject/Problematic.java";

    protected JavaCodeInsightTestFixture myFixture;

    public BaseTestCase() {
        // Only in IntelliJ IDEA Ultimate Edition
//        PlatformTestCase.initPlatformLangPrefix();
        // XXX: IntelliJ IDEA Community and Ultimate 12+
        PlatformTestCase.initPlatformPrefix(ULTIMATE_MARKER_CLASS, "Idea");
        System.setProperty(PathManager.PROPERTY_HOME_PATH, "./");
    }

    /** Which project does this test reference? */
    protected abstract String getProjectPath();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final IdeaTestFixtureFactory factory = IdeaTestFixtureFactory.getFixtureFactory();
//        final LightProjectDescriptor projectDescriptor = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR;
        final LightProjectDescriptor projectDescriptor = new DefaultLightProjectDescriptor();
        final TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor);
//        TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder =
//                JavaTestFixtureFactory.createFixtureBuilder(getName());
//        fixtureBuilder.addModule(JavaModuleFixtureBuilder.class);
        final IdeaProjectTestFixture fixture = fixtureBuilder.getFixture();
        myFixture = JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture,
                new LightTempDirTestFixtureImpl(true));
//        myFixture.setCaresAboutInjection(true);
        myFixture.setUp();

//        String communityPath = PlatformTestUtil.getCommunityPath().replace(File.separatorChar, '/');
//        myFixture.setTestDataPath(communityPath + "/projects");

//        myFixture.addClass(
//                "package org.intellivim.javaproject.subpackage;\n\n" +
//                        "public class NotImported2 {}");

//        for (String s : PsiShortNamesCache.getInstance(myFixture.getProject()).getAllClassNames()) {
//            System.out.println(s);
//        }

    }

    @Override
    protected void tearDown() throws Exception {
        myFixture.tearDown();
        myFixture = null;
        super.tearDown();
    }

    protected static void assertSuccess(SimpleResult result) {
        assertNotNull("Expected a SimpleResult", result);
        if (!result.isSuccess())
            fail("Expected successful result but got: " + result.error);
    }


//    protected static VirtualFile createSource(final String name, final String text) {
//        final VirtualFile[] result = new VirtualFile[1];
//        ApplicationManager.getApplication().runWriteAction(
//            new Runnable() {
//                @Override
//                public void run() {
//                    VirtualFile sourceDir = getSourceRoot();
//                    try {
//                        VirtualFile file = sourceDir.createChildData(null, name);
//                        BufferedWriter out = new BufferedWriter(
//                                new OutputStreamWriter(file.getOutputStream(null)));
//                        out.write(text);
//                        out.close();
//                        System.out.println("inLocal? " + file.isInLocalFileSystem());
//                        System.out.println("class? " + file.getClass());
//                        System.out.println("type? " + file.getFileType());
//
//                        result[0] = file;
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//        });
//
//        return result[0];
//    }

    protected String getProjectPath(String projectName) {
        final File root = new File(PathManager.getHomePath());
        final String pathToIml = "projects/" + projectName + "/" + projectName + ".iml";
        return new File(root, pathToIml).getAbsolutePath();
    }

    protected Project getProject() {
        final String path = getProjectPath();
        if (path != null)
            return ProjectUtil.ensureProject(path);

        return myFixture.getProject();
    }
}
