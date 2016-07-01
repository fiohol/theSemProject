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

import org.thesemproject.opensem.segmentation.SegmentConfiguration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jdom2.Element;

/**
 * Rappresenta il nodo di gestione del segmento
 */
public class SegmentTreeNode extends ModelTreeNode {

    private boolean isClassify;
    private boolean isMultiple;
    private boolean isDefault;
    private final Map<String, String[]> patterns;

    /**
     * Crea un nodo segmento
     *
     * @param nodeName nome del segmento
     */
    public SegmentTreeNode(String nodeName) {
        super(nodeName, TYPE_SEGMENT_DEFINITION);
        isClassify = false;
        isMultiple = false;
        isDefault = false;
        patterns = new LinkedHashMap<>();
    }

    /**
     * Imposta la configurazione del segmento
     *
     * @param sb configurazione del segmento
     */
    public void setSegmentConfiguration(SegmentConfiguration sb) {
        isClassify = sb.isClassify();
        isMultiple = sb.isMultiple();
        isDefault = sb.isDefault();
    }

    /**
     * Aggiunge un pattern al segmento
     *
     * @param value pattern da aggiungere
     */
    public void addPattern(String value) {
        String[] row = new String[2];
        String key = String.valueOf(value.hashCode());
        row[0] = key;
        row[1] = (String) intern.intern(value.toLowerCase());
        patterns.put(key, row);
    }

    /**
     * Verifica se il segmento classifica
     *
     * @return true se classifica
     */
    public boolean isClassify() {
        return isClassify;
    }

    /**
     * Verifica se il segmento è multiplo
     *
     * @return true se il segmento è multiplo
     */
    public boolean isMultiple() {
        return isMultiple;
    }

    /**
     * Verifica se il segmento è di default
     *
     * @return true se il segmento è di default
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Ritornano la lista dei pattern che definiscono il segmento
     *
     * @return lista dei pattern (visualizzabili in JTable)
     */
    public List<String[]> getPatterns() {
        return new ArrayList(patterns.values());
    }

    /**
     * Rimuove il pattern
     *
     * @param id id del pattern
     */
    public void removePattern(String id) {
        patterns.remove(id);
    }

    /**
     * Aggiorna il pattern
     *
     * @param key chiave del pattern
     * @param value valore del pattern
     */
    public void updatePattern(String key, String value) {
        String[] row = new String[2];
        row[0] = key;
        row[1] = (String) intern.intern(value.toLowerCase());
        patterns.put(key, row);
    }

    /**
     * Imposta il segmento come segmento che classifica
     *
     * @param isClassify true se si vuole che una volta individuato il segmento,
     * il segmenter chiami il multiclass eingine
     */
    public void setClassify(boolean isClassify) {
        this.isClassify = isClassify;
    }

    /**
     * Imposta la cartinalità di un segmento
     *
     * @param isMultiple true se il segmento definito può apparire più volte nel
     * documento
     */
    public void setMultiple(boolean isMultiple) {
        this.isMultiple = isMultiple;
    }

    /**
     * Imposta se il segmento è di default
     *
     * @param isDefault true se di default
     */
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    /**
     * Costruisce la rappresentazione XML del nodo
     *
     * @return rappresentazione XML del nodo
     */
    public Element getXmlElement() {
        Element segment = new Element("s");
        segment.setAttribute("n", getNodeName());
        if (isClassify) {
            segment.setAttribute("cl", "yes");
        }
        if (isDefault) {
            segment.setAttribute("d", "yes");
        }
        if (isMultiple) {
            segment.setAttribute("m", "yes");
        }
        patterns.values().stream().forEach((row) -> {
            Element pattern = new Element("p");
            pattern.addContent(row[1]);
            segment.addContent(pattern);
        });
        return segment;
    }

    /**
     * Gestisce la cancellazione di una tabella andando a sistemare i patterns
     * che ne fanno riferimento
     *
     * @param table tabella che si vuole rimuovere
     */
    public void removeTable(String table) {
        patterns.values().stream().forEach((row) -> {
            row[1] = row[1].replace("#" + table + " ", "");
        });
    }

    /**
     * Gestisce la rinomina di una tabella sistemando le definizioni dei pattern
     *
     * @param table tabella che si vuole rinominare
     * @param newName nuovo nome tabella
     */
    public void renameTable(String table, String newName) {
        patterns.values().stream().forEach((row) -> {
            row[1] = row[1].replace("#" + table + " ", "#" + newName + " ");
        });
    }

}
