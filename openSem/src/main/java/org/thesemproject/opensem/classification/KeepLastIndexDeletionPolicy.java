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
package org.thesemproject.opensem.classification;

import org.thesemproject.opensem.gui.LogGui;
import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexDeletionPolicy;

/**
 * Definisce la policy di cancellazione dell'indice
 */
public class KeepLastIndexDeletionPolicy extends IndexDeletionPolicy {

    /**
     * Cancella tutti i commit ad eccezione dell'ultimo
     *
     * @param commits lista dei commit
     */
    public void onInit(List commits) {
        //System.out.println("onInit -> onCommit");
        // Note that commits.size() should normally be 1:
        try {
            onCommit(commits);
        } catch (IOException e) {
            LogGui.printException(e);
        }
    }

    /**
     * Cancella tutti i commit ad eccezione dell'ultimo
     *
     * @param commits lista dei commit
     * @throws java.io.IOException
     */
    public void onCommit(List commits) throws IOException {
        //System.out.println("onCommit: " + commits);
        // Note that commits.size() should normally be 2 (if not
        // called by onInit above):
        int size = commits.size();
        for (int i = 0; i < size - 1; i++) {
            ((IndexCommit) commits.get(i)).delete();
        }
    }
}
