package org.intellivim.core.command.params;

import com.intellij.lang.parameterInfo.ParameterInfoUIContextEx;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;

import java.awt.Color;
import java.util.EnumSet;

/**
 * @author dhleong
 */
public class DummyParameterInfoUIContext implements ParameterInfoUIContextEx {

    private final PsiElement owner;
    private final int index;

    private boolean enabled = true;

    private String presentedString;
//    private boolean isDisabled;
//    private boolean strikeout;

    public DummyParameterInfoUIContext(PsiElement owner, int index) {
        this.owner = owner;
        this.index = index;
    }

    @Override
    public String setupUIComponentPresentation(String[] texts, EnumSet<Flag>[] flags, Color background) {
        // is this enough?
        presentedString = StringUtil.join(texts, "\n");
        return presentedString;
    }

    @Override
    public void setEscapeFunction(final Function<String, String> function) {

    }

    @Override
    public String setupUIComponentPresentation(String text, int highlightStartOffset, int highlightEndOffset,
           boolean isDisabled, boolean strikeout, boolean isDisabledBeforeHighlight, Color background) {
//        this.isDisabled = isDisabled;
//        this.strikeout = strikeout;
        presentedString = text;
        return presentedString;
    }

    @Override
    public boolean isUIComponentEnabled() {
        return enabled;
    }

    @Override
    public void setUIComponentEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public int getCurrentParameterIndex() {
        return index;
    }

    @Override
    public PsiElement getParameterOwner() {
        return owner;
    }

    @Override
    public Color getDefaultParameterColor() {
        return null;
    }

    public String getPresentedString() {
        return presentedString;
    }

    public boolean isPresentationValid() {
        // it might just be the unit test environment,
        //  but disabled seems to always be true
//        return !(isDisabled || strikeout);
        return enabled;
    }

}
