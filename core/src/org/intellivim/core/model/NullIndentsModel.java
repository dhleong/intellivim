package org.intellivim.core.model;

import com.intellij.openapi.editor.IndentGuideDescriptor;
import com.intellij.openapi.editor.IndentsModel;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created by dhleong on 11/14/14.
 */
public class NullIndentsModel implements IndentsModel {
    @Nullable
    @Override
    public IndentGuideDescriptor getCaretIndentGuide() {
        return null;
    }

    @Nullable
    @Override
    public IndentGuideDescriptor getDescriptor(int startLine, int endLine) {
        return null;
    }

    @Override
    public void assumeIndents(List<IndentGuideDescriptor> descriptors) {

    }
}
