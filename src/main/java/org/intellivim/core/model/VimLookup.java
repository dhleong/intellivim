package org.intellivim.core.model;

import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupListener;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by dhleong on 11/5/14.
 */
public class VimLookup implements Lookup {
    private final Project project;
    private final Editor editor;

    public VimLookup(Project project, Editor editor) {
        this.project = project;
        this.editor = editor;
    }

    @Override
    public int getLookupStart() {
        System.out.println(">> VimLookup.getLookupStart");
        return 0;
    }

    @Nullable
    @Override
    public LookupElement getCurrentItem() {
        System.out.println(">> VimLookup.getCurrentItem");
        return null;
    }

    @Override
    public void addLookupListener(LookupListener listener) {

        System.out.println(">> VimLookup.addLookupListener");
    }

    @Override
    public void removeLookupListener(LookupListener listener) {

    }

    @Override
    public Rectangle getBounds() {
        System.out.println(">> VimLookup.getBounds");
        return null;
    }

    @Override
    public Rectangle getCurrentItemBounds() {
        System.out.println(">> VimLookup.getCurrentItemBounds");
        return null;
    }

    @Override
    public boolean isPositionedAboveCaret() {
        return false;
    }

    @Nullable
    @Override
    public PsiElement getPsiElement() {
        System.out.println(">> VimLookup.getPsiElement");
        return null;
    }

    @Override
    public Editor getEditor() {
        return editor;
    }

    @Override
    public PsiFile getPsiFile() {
        System.out.println(">> VimLookup.getPsiFile");
        return null;
    }

    @Override
    public boolean isCompletion() {
        System.out.println(">> VimLookup.isCompletion");
        return false;
    }

    @Override
    public List<LookupElement> getItems() {
        System.out.println(">> VimLookup.getItems");
        return null;
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @NotNull
    @Override
    public String itemPattern(@NotNull LookupElement element) {
        System.out.println(">> VimLookup.itemPattern");
        return null;
    }

    @NotNull
    @Override
    public PrefixMatcher itemMatcher(@NotNull LookupElement item) {
        System.out.println(">> VimLookup.itemMatcher");
        return null;
    }

    @Override
    public boolean isSelectionTouched() {
        return false;
    }

    @Override
    public List<String> getAdvertisements() {
        System.out.println(">> VimLookup.getAdvertisements");
        return null;
    }
}
