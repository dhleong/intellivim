package org.intellivim.core;

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
public class BaseTestCase extends UsefulTestCase {
    private static final String ULTIMATE_MARKER_CLASS = "com.intellij.psi.css.CssFile";

    protected static final String JAVA_PROJECT = "java-project";

    protected CodeInsightTestFixture myFixture;

    public BaseTestCase() {
        // Only in IntelliJ IDEA Ultimate Edition
//        PlatformTestCase.initPlatformLangPrefix();
        // XXX: IntelliJ IDEA Community and Ultimate 12+
        PlatformTestCase.initPlatformPrefix(ULTIMATE_MARKER_CLASS, "PlatformLangXml");
        System.setProperty(PathManager.PROPERTY_HOME_PATH, "./");
    }

    protected static void assertSuccess(SimpleResult result) {
        assertNotNull("Expected a SimpleResult", result);
        if (!result.isSuccess())
            fail("Expected successful result but got: " + result.error);
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
        myFixture.setUp();
//        myFixture.setTestDataPath(getTestDataPath());
//        KeyHandler.getInstance().fullReset(myFixture.getEditor());
//        Options.getInstance().resetAllOptions();
//        VimPlugin.getKey().resetKeyMappings();
    }

    @Override
    protected void tearDown() throws Exception {
        myFixture.tearDown();
        myFixture = null;
        super.tearDown();
    }

    protected String getProjectPath(String projectName) {
        final File root = new File(PathManager.getHomePath());
        final String pathToIml = "projects/" + projectName + "/" + projectName + ".iml";
        return new File(root, pathToIml).getAbsolutePath();
    }
}
