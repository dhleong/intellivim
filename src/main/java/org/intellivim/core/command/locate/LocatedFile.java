package org.intellivim.core.command.locate;

import com.intellij.ide.util.gotoByName.ChooseByNameModel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

/**
 * @author dhleong
 */
public class LocatedFile {

    private final String display;
    private final String path;

    LocatedFile(String display, String path) {
        this.display = display;
        this.path = path;
    }

    LocatedFile(String name, PsiFile file) {
        this(name, file.getVirtualFile().getCanonicalPath());
    }

    /** Testing constructor */
    public LocatedFile(String display) {
        this.display = display;
        this.path = null;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LocatedFile
            && display.equals(((LocatedFile) obj).display);
    }

    @Override
    public String toString() {
        return String.format("[LocatedFile(%s)@(%s)]", display, path);
    }

    public static LocatedFile from(ChooseByNameModel model, Object obj) {
        if (obj instanceof String) {
            System.out.println("It's a string! " + obj);
            return null;
        }
        final String name = model.getFullName(obj);
        if (obj instanceof PsiFile) {
            final PsiFile file = (PsiFile) obj;
            return new LocatedFile(name, file);
        } else if (obj instanceof PsiElement) {
            PsiFile file = ((PsiElement) obj).getContainingFile();
            return new LocatedFile(name, file);
        }

        System.err.println("Unhandled LocatedFile result: " + obj.getClass() + " / " + obj);
        return null;
    }
}
