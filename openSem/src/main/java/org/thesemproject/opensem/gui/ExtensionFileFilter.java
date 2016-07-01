/* 
 * Copyright 2016 The Sem Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thesemproject.opensem.gui;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 * Gestisce il filtro sul file dialog
 */
public class ExtensionFileFilter extends FileFilter {

    String description;

    String extensions[];

    /**
     * Istanzia il filtro sull'estensione
     *
     * @param description descrizione del filtro
     * @param extension estensione ammessa
     */
    public ExtensionFileFilter(String description, String extension) {
        this(description, new String[]{extension});
    }

    /**
     * Istanzia il filtro su un insieme di estensioni
     *
     * @param description descrizione del filtro
     * @param extensions elenco delle estensioni
     */
    public ExtensionFileFilter(String description, String extensions[]) {
        if (description == null) {
            this.description = extensions[0];
        } else {
            this.description = description;
        }
        this.extensions = (String[]) extensions.clone();
        toLower(this.extensions);
    }

    private void toLower(String array[]) {
        for (int i = 0, n = array.length; i < n; i++) {
            array[i] = array[i].toLowerCase();
        }
    }

    /**
     * Ritorna la descrizione del filtro
     *
     * @return ritorna la descrizione del filtro
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Verifica se il file passato rispetta il filtro
     *
     * @param file file passato dal sistema
     * @return true se il file rispetta il criterio di filtro
     */
    @Override
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        } else {
            String path = file.getAbsolutePath().toLowerCase();
            for (int i = 0, n = extensions.length; i < n; i++) {
                String extension = extensions[i];
                if ((path.endsWith(extension) && (path.charAt(path.length() - extension.length() - 1)) == '.')) {
                    return true;
                }
            }
        }
        return false;
    }
}
