package org.intellivim.morph;

import com.google.gson.JsonObject;
import org.intellivim.inject.PsiFileInjector;

/**
 * Pick command implementation based on file extensions
 *
 * @author dhleong
 */
public class FileExtensionsMorpher implements Polymorpher {
    @Override
    public boolean matches(final JsonObject input, final String... params) {
        final String file = input.get(PsiFileInjector.DEFAULT_FIELD_NAME).getAsString();
        final int extPos = file.lastIndexOf('.');
        final String ext = file.substring(extPos + 1);
        for (String param : params) {
            if (ext.equals(param))
                return true;
        }

        return false;
    }
}
