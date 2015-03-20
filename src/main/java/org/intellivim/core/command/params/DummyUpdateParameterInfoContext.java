package org.intellivim.core.command.params;

import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dhleong
 */
public class DummyUpdateParameterInfoContext implements UpdateParameterInfoContext {

    private List<DummyParameterInfoUIContext> contexts = new ArrayList<DummyParameterInfoUIContext>();

    private Project project;
    private PsiFile file;
    private Editor editor;
    private Object[] objects;
    private int parameterListStart;

    private PsiElement owner;
    private int currentIndex;
    private Object highlighted;

    public DummyUpdateParameterInfoContext(Project project, PsiFile file, Editor editor, Object[] itemsToShow) {
        this.project = project;
        this.file = file;
        this.editor = editor;
        objects = itemsToShow;

        parameterListStart = editor.getCaretModel().getOffset() - 1;
    }

    @Override
    public void removeHint() {
        // ???
    }

    @Override
    public void setParameterOwner(PsiElement o) {
        owner = o;
    }

    @Override
    public PsiElement getParameterOwner() {
        return owner;
    }

    public Object getHighlightedParameter() {
        return highlighted;
    }

    @Override
    public void setHighlightedParameter(Object parameter) {
        this.highlighted = parameter;
    }

    @Override
    public void setCurrentParameter(int index) {
        currentIndex = index;
    }

    @Override
    public boolean isUIComponentEnabled(int index) {
        return contexts.get(index).isUIComponentEnabled();
    }

    @Override
    public void setUIComponentEnabled(int index, boolean enabled) {
        contexts.get(index).setUIComponentEnabled(enabled);
    }

    @Override
    public int getParameterListStart() {
        return parameterListStart; // FIXME
    }

    @Override
    public Object[] getObjectsToView() {
        return objects;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public PsiFile getFile() {
        return file;
    }

    @Override
    public int getOffset() {
        return editor.getCaretModel().getOffset();
    }

    @NotNull
    @Override
    public Editor getEditor() {
        return editor;
    }

    public void add(DummyParameterInfoUIContext infoContext) {
        contexts.add(infoContext);
    }

    public String getPresentedString(int i) {
        return contexts.get(i).getPresentedString();
    }
}
