package org.intellivim.core.command.find;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;

/**
 * @author dhleong
 */
public class LocationResult {
    public final String file;
    public final int offset;

    LocationResult(PsiElement element) {
        final VirtualFile virtualFile = element.getContainingFile()
                .getVirtualFile();

        file = virtualFile.getCanonicalPath();
        offset = element.getTextOffset();
    }
}
