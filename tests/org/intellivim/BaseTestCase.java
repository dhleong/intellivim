package org.intellivim;

import com.intellij.openapi.application.PathManager;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl;

import java.io.File;

/**
 * Created by dhleong on 11/7/14.
 */
public abstract class BaseTestCase extends UsefulTestCase {
    private static final String ULTIMATE_MARKER_CLASS = "com.intellij.psi.css.CssFile";

    protected static final String JAVA_PROJECT = "java-project";
    protected static final String PROBLEMATIC_FILE_PATH = "src/org/intellivim/javaproject/Problematic.java";

    protected CodeInsightTestFixture myFixture;

    public BaseTestCase() {
        // Only in IntelliJ IDEA Ultimate Edition
//        PlatformTestCase.initPlatformLangPrefix();
        // XXX: IntelliJ IDEA Community and Ultimate 12+
        PlatformTestCase.initPlatformPrefix(ULTIMATE_MARKER_CLASS, "Idea");
        System.setProperty(PathManager.PROPERTY_HOME_PATH, "./");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final IdeaTestFixtureFactory factory = IdeaTestFixtureFactory.getFixtureFactory();
        final LightProjectDescriptor projectDescriptor = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR;
        final TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor);
        final IdeaProjectTestFixture fixture = fixtureBuilder.getFixture();
        myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture,
                new LightTempDirTestFixtureImpl(true));
//        myFixture.setCaresAboutInjection(true);
        myFixture.setUp();

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
}
