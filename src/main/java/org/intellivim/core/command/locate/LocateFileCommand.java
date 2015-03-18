package org.intellivim.core.command.locate;

import com.intellij.ide.util.gotoByName.ChooseByNameBase;
import com.intellij.ide.util.gotoByName.ChooseByNameModel;
import com.intellij.ide.util.gotoByName.ChooseByNamePopup;
import com.intellij.ide.util.gotoByName.DefaultChooseByNameItemProvider;
import com.intellij.ide.util.gotoByName.GotoClassModel2;
import com.intellij.ide.util.gotoByName.GotoFileModel;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiFile;
import com.intellij.util.CommonProcessors;
import com.intellij.util.containers.ContainerUtil;
import org.intellivim.Command;
import org.intellivim.ProjectCommand;
import org.intellivim.Required;
import org.intellivim.Result;
import org.intellivim.SimpleResult;
import org.intellivim.core.util.IntelliVimUtil;
import org.intellivim.core.util.ProjectUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * NB We may be able to make improvements by studying SearchEverywhereAction
 * @author dhleong
 */
@Command("locate")
public class LocateFileCommand extends ProjectCommand {

    public enum LocateType {
        FILE,
        CLASS
        // TODO symbol?
    }

    @Required LocateType type;
    @Required String pattern;

    /* optional */String file;
    /* optional */boolean fuzzy = true;

    public LocateFileCommand(Project project, LocateType type, String fileContext, String pattern) {
        super(project);

        this.type = type;
        this.pattern = pattern;

        file = fileContext;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result execute() {
        System.out.println("Execute Locate:" + pattern);

        // be unchecked for testing convenience
        final List results = new ArrayList<LocatedFile>();

        final PsiFile context = file == null
            ? null
            : ProjectUtil.getPsiFile(project, file);

        final ChooseByNameModel model = pickModelInUnitTestMode();
        if (model == null) {
            System.out.println("Invalid!: " + type);
            return SimpleResult.error("Invalid locate file type");
        }

        final String pattern = preparePattern(model);

        final ChooseByNameBase chooser = new NullChooseByNameBase(project, model, pattern, context);
        new DefaultChooseByNameItemProvider(context).filterElements(
                chooser, pattern,
                /* everywhere */false,
                new ProgressIndicatorBase(),
                new CommonProcessors.CollectProcessor<Object>(results) {

            @Override
            public boolean process(Object o) {
                return super.process(LocatedFile.from(model, o));
            }
        });

        System.out.println("Results: " + results);
        return SimpleResult.success(ContainerUtil.filter(results, new Condition() {
            @Override
            public boolean value(Object o) {
                return null != o;
            }
        }));
    }

    private String preparePattern(ChooseByNameModel model) {
        String pattern = this.pattern;
        pattern = ChooseByNamePopup.getTransformedPattern(pattern, model);
        pattern = DefaultChooseByNameItemProvider.getNamePattern(model, pattern);

        if (fuzzy) {
            pattern = pattern.replaceAll("(.)", "$1*");
        }

        if (!pattern.startsWith("*") && pattern.length() > 1) {
            pattern = "*" + pattern;
        }

        return pattern;
    }

    /**
     * We have to do this in UnitTestMode to avoid
     *  an NPE when the constructors try to access
     *  a Window
     */
    private ChooseByNameModel pickModelInUnitTestMode() {
        final ChooseByNameModel chosen;
        IntelliVimUtil.setUnitTestMode();
        chosen = pickModel();
        IntelliVimUtil.unsetUnitTestMode();
        return chosen;
    }

    private ChooseByNameModel pickModel() {
        if (type == null)
            return null;

        switch (type) {
        case FILE:
            return new GotoFileModel(project);
        case CLASS:
            return new GotoClassModel2(project);
        }

        return null;
    }

    /** UI-less implementation */
    private static class NullChooseByNameBase extends ChooseByNameBase {
        public NullChooseByNameBase(Project project, ChooseByNameModel model,
                String pattern, PsiFile context) {
            super(project, model, pattern, context);
        }

        @Override
        protected boolean isCheckboxVisible() {
            return false;
        }

        @Override
        protected boolean isShowListForEmptyPattern() {
            return false;
        }

        @Override
        protected boolean isCloseByFocusLost() {
            return false;
        }

        @Override
        protected void showList() {

        }

        @Override
        protected void hideList() {

        }

        @Override
        protected void close(boolean isOk) {

        }
    }
}
