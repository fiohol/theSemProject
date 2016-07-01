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
package org.thesemproject.opensem.tagcloud;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.mcavallo.opencloud.Cloud;
import org.mcavallo.opencloud.Tag;

/**
 * Gestice i risultati di una operazione di tag cloud
 */
public class TagCloudResults {

    private final Map<String, TagClass> wordCloud;

    /**
     * Istanzia l'oggetto
     */
    public TagCloudResults() {
        wordCloud = new ConcurrentHashMap<>();
    }

    /**
     * Aggiunge un risultato
     *
     * @param parsedTerm termine parsato (stemming applicato). Al medesimo
     * termine parsato possono corrispondere più words. Ad esempio amico e amici
     * hanno lo stesso parsedTerm "amic"
     * @param word parola che ha dato vita al termine parsato
     * @param id id del documento
     */
    public void add(String parsedTerm, String word, String id) {
        synchronized (wordCloud) {
            TagClass tag = wordCloud.get(parsedTerm);
            if (tag == null) {
                tag = new TagClass();
            } else {
                tag.increment();
            }
            tag.addId(id);
            tag.addWord(word);
            wordCloud.put(parsedTerm, tag);
        }
    }

    /**
     * Ritorna una TagClass a partire da un oggetto Tag (oggetto di OpenCloud)
     *
     * @param tag oggetto tag
     * @return TagClass risultante
     */
    public TagClass getTagClass(Tag tag) {
        return wordCloud.get(tag.getLink());
    }

    /**
     * Ritorna un oggetto Cloud di OpenCloud che tenga conto dei top 200 termini
     *
     * @return Cloud
     */
    public Cloud getCloud() {
        return getCloud(200);
    }

    /**
     * Ritorna un oggetto Cloud di OpenCloud che tenga conto dei top maxTag
     * termini
     *
     * @param maxTag numero di termini massimo
     * @return Cloud
     */
    public Cloud getCloud(int maxTag) {
        Cloud cloud = new Cloud();
        cloud.setMaxTagsToDisplay(maxTag);
        wordCloud.keySet().stream().forEach((s) -> {
            TagClass tc = wordCloud.get(s);
            cloud.addTag(new Tag(tc.getLabel(), s, tc.getFrequency()));
        });

        return cloud;
    }

}
