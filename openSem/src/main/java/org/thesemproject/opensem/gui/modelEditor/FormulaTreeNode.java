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
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.jdom2.Element;

/**
 * Rappresenta il nodo di gestione di una formula
 *
 * @since 1.3
 *
 */
public class FormulaTreeNode extends ModelTreeNode {

    private final Map<String, String[]> captures;
    private String formatPattern;
    private boolean actBeforeEnrichment;

    /**
     * Crea un nodo formula
     *
     * @param nodeName nome della formula (e della cattura generata dalla
     * formula stessa)
     */
    public FormulaTreeNode(String nodeName) {
        super(nodeName, TYPE_FORMULA_DEFINITION);
        formatPattern = "";
        captures = new LinkedHashMap<>();
        actBeforeEnrichment = false;
    }

    /**
     * Crea un nodo formula
     *
     * @param nodeName nome del nodo
     * @param mNode nodo da clonare
     */
    public FormulaTreeNode(String nodeName, FormulaTreeNode mNode) {
        super(nodeName, TYPE_FORMULA_DEFINITION);
        captures = mNode.captures;
        formatPattern = mNode.formatPattern;
        actBeforeEnrichment = mNode.actBeforeEnrichment;
    }

    /**
     * Imposta il pattern di formattazione
     *
     * @param formatPattern pattern di formattazione
     */
    public void setFormatPattern(String formatPattern) {
        if (formatPattern == null) {
            formatPattern = "";
        }
        this.formatPattern = formatPattern;
    }

    /**
     * Aggiunge una cattura alla formula
     *
     * @param value cattura da aggiungere
     */
    public void addCapture(String value) {
        String[] row = new String[2];
        String key = String.valueOf(value.hashCode());
        row[0] = key;
        row[1] = (String) intern.intern(value);
        captures.put(key, row);
    }

    /**
     * Ritornano la lista delle catture che definiscono la formula
     *
     * @return lista delle catture (visualizzabili in JTable)
     */
    public List<String[]> getCaptures() {
        return new ArrayList(captures.values());
    }

    /**
     * Rimuove la cattura
     *
     * @param id id della cattura
     */
    public void removeCapture(String id) {
        captures.remove(id);
    }

    /**
     * Aggiorna la cattura
     *
     * @param key chiave della cattura
     * @param value valore della cattura
     */
    public void updateCapture(String key, String value) {
        String[] row = new String[2];
        row[0] = key;
        row[1] = (String) intern.intern(value.toLowerCase());
        captures.put(key, row);
    }

    /**
     * Costruisce la rappresentazione XML del nodo
     *
     * @return rappresentazione XML del nodo
     */
    public Element getXmlElement() {
        Element formula = new Element("f");
        formula.setAttribute("n", getNodeName());
        formula.setAttribute("f", getFormatPattern());
        formula.setAttribute("b", String.valueOf(isActBeforeEnrichment()));
        captures.values().stream().forEach((row) -> {
            Element pattern = new Element("c");
            pattern.addContent(row[1]);
            formula.addContent(pattern);
        });
        return formula;
    }

    /**
     * Aggiorna l'ordine delle catture secondo quanto rappresentato graficamente
     *
     * @param capuresTable tabella dei pattern da cui riprendere l'ordine
     */
    public void updateCapturesFromTable(JTable capuresTable) {
        Map<String, String[]> tmp = new LinkedHashMap<>(captures);
        captures.clear();
        DefaultTableModel model = (DefaultTableModel) capuresTable.getModel();
        int rows = model.getRowCount();
        for (int i = 0; i < rows; i++) {
            String id = (String) model.getValueAt(i, 0);
            captures.put(id, tmp.get(id));
        }

    }

    /**
     * Ritorna il pattern della formula
     *
     * @return pattern della formula
     */
    public String getFormatPattern() {
        return formatPattern;
    }

    /**
     * Ritorna true se la formula deve agire prima dell'arricchimento
     *
     * @return true se agisce prima dell'arricchimento
     */
    public boolean isActBeforeEnrichment() {
        return actBeforeEnrichment;
    }

    /**
     * Imposta se la formula può agire prima o dopo l'arricchimento
     *
     * @param actBeforeEnrichment true se deve agire prima dell'arricchimento
     *
     */
    public void setActBeforeEnrichment(boolean actBeforeEnrichment) {
        this.actBeforeEnrichment = actBeforeEnrichment;
    }

}
