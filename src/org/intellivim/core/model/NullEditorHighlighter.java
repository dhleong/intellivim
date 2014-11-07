package org.intellivim.core.model;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterClient;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * Created by dhleong on 11/6/14.
 */
public class NullEditorHighlighter implements EditorHighlighter {
    @NotNull
    @Override
    public HighlighterIterator createIterator(int startOffset) {
        return new HighlighterIterator() {
            @Override
            public TextAttributes getTextAttributes() {
                return null;
            }

            @Override
            public int getStart() {
                return 0;
            }

            @Override
            public int getEnd() {
                return 0;
            }

            @Override
            public IElementType getTokenType() {
                return null;
            }

            @Override
            public void advance() {

            }

            @Override
            public void retreat() {

            }

            @Override
            public boolean atEnd() {
                return true;
            }

            @Override
            public Document getDocument() {
                return null;
            }
        };
    }

    @Override
    public void setText(@NotNull CharSequence text) {

    }

    @Override
    public void setEditor(@NotNull HighlighterClient editor) {

    }

    @Override
    public void setColorScheme(@NotNull EditorColorsScheme scheme) {

    }

    @Override
    public void beforeDocumentChange(DocumentEvent event) {

    }

    @Override
    public void documentChanged(DocumentEvent event) {

    }
}
