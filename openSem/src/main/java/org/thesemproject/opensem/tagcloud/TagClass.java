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

import java.util.HashSet;
import java.util.Set;

/**
 * Gestisce una classe che rappresenta una parola nella nuvoletta
 */
public class TagClass {

    private int count;
    private final Set<String> documentsId;
    private final Set<String> words;

    /**
     * Crea una nuova classe di tag
     */
    public TagClass() {
        this.count = 1;
        this.documentsId = new HashSet<>();
        this.words = new HashSet<>();
    }

    /**
     * Aumenta la freaquenza della classe di tag
     */
    public void increment() {
        this.count++;
    }

    /**
     * Aggiunge un id documento ad una classe di tag
     *
     * @param id id documento
     */
    public void addId(String id) {
        documentsId.add(id);
    }

    /**
     * Aggiunge un termine ad una classe di tag. Ricordo che il tag cloud viene
     * fatto in due passate in modo da aggregare assieme i termini che hanno lo
     * stesso stemming
     *
     * @param term termine
     */
    public void addWord(String term) {
        words.add(term);
    }

    /**
     * Ritorna la frequenza del tag
     *
     * @return frequenza (numero di volte in cui è stato incontrato)
     */
    public int getFrequency() {
        return count;
    }

    /**
     * Ritorna la concatenazione di tutti i termini che appartengono al cloud
     * (per permettere di mettere l'etichetta la mouseover)
     *
     * @return lista delle parole che fanno parte del tag
     */
    public String getWordsString() {
        StringBuffer sb = new StringBuffer();
        for (String w : words) {
            sb.append(w).append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * Ritorna una parola tra tutti i termini per rappresentare la classe di tag
     * cloud
     *
     * @return una parola a caso tra i termini
     */
    public String getLabel() {
        for (String w : words) {
            return w;
        }
        return null;
    }

    /**
     * Ritorna l'elenco dei documenti che appartengon al tag
     *
     * @return insieme di id
     */
    public Set<String> getDocumentsId() {
        return documentsId;
    }

}
