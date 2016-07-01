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
package org.thesemproject.opensem.gui.modelEditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jdom2.Element;

/**
 * Gestisce il nodo del dizionario
 */
public class DictionaryTreeNode extends ModelTreeNode {

    private final Map<String, String[]> tableContent;

    /**
     * Nodo del dizionario
     *
     * @param nodeName nome del nodo
     */
    public DictionaryTreeNode(String nodeName) {
        super(nodeName, ModelTreeNode.TYPE_DICTIONARY);
        tableContent = new LinkedHashMap<>();

    }

    /**
     * Verifica se un nodo contiene una definizione
     *
     * @param name nome della definizione
     * @return true se il dizionario contiene la definizione
     */
    public boolean containsDefinition(String name) {
        return tableContent.containsKey(name.toLowerCase());
    }

    /**
     * Aggiunge una definizione al dizionario
     *
     * @param name nome della definizione
     * @param value pattern della definizione
     */
    public void addDefinition(String name, String value) {
        if (name == null || value == null) {
            return;
        }
        if (name.length() == 0) {
            return;
        }
        name = name.toLowerCase();
        value = value.toLowerCase();
        if (tableContent.containsKey(name)) {
            return;
        }

        String[] row = new String[2];
        row[0] = (String) intern.intern(name);
        row[1] = (String) intern.intern(value.toLowerCase());
        tableContent.put(name, row);
    }

    /**
     * Ritorna la lista dei suggerimenti (per l'autocomplete)
     *
     * @return lista dei suggerimenti con le definizioni del dizionario
     */
    public List<String> getSuggestionList() {
        List<String> ret = new ArrayList<>();
        for (String s : tableContent.keySet()) {
            ret.add("#" + s + " (Dizionario)");
        }
        return ret;
    }

    /**
     * Ritorna il dizionario sottoforma di tabella da visualizzare
     *
     * @return dizionario completo da mostrare in JTable
     */
    public List<String[]> getTableContent() {
        return new ArrayList(tableContent.values());
    }

    /**
     * Aggiorna la definizione
     *
     * @param name nome della definizione
     * @param value pattern
     */
    public void updateDefinition(String name, String value) {
        String[] row = tableContent.get(name);
        if (row != null) {
            row[1] = (String) intern.intern(value);
            tableContent.put(name, row);
        }
    }

    /**
     * Elimina una definizione
     *
     * @param name nome della definizione da eliminare
     */
    public void removeDefinition(String name) {
        tableContent.remove(name);
    }

    /**
     * Ritorna la rappresentazione XML del dioznario
     *
     * @param dictionary Elemento dove inserire i dati di dizionario
     */
    public void getXmlElement(Element dictionary) {
        tableContent.values().stream().map((row) -> {
            Element definition = new Element("d");
            definition.setAttribute("n", row[0]);
            definition.addContent(row[1]);
            return definition;
        }).forEach((definition) -> {
            dictionary.addContent(definition);
        });
    }

    /**
     * Ritorna tutte le definizioni
     *
     * @return collection con i nomi delle definizioni
     */
    public Collection<? extends String> getAllDefinitionsName() {
        Set<String> names = new HashSet<>();
        tableContent.values().stream().forEach((row) -> {
            names.add(row[0]);
        });
        return names;
    }

}
