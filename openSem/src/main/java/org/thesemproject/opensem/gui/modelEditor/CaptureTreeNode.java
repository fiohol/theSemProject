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

import org.thesemproject.opensem.classification.ClassificationPath;
import org.thesemproject.opensem.segmentation.CaptureConfiguration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import org.jdom2.Element;

/**
 *
 * Gestisce un nodo dell'albero del modello per rappresentare una cattura
 */
public class CaptureTreeNode extends ModelTreeNode {

    private String captureType;
    private String captureFormat;
    private String scope;
    private Set<String> enabledSegments;
    private final Map<String, String[]> patterns;
    private ClassificationPath classificationPath;
    private boolean temporary;
    private boolean startPeriod;
    private boolean endPeriod;
    private boolean notSubscribe;
    private Set<String> blockedCaptures;

    /**
     * Istanzia il nodo
     *
     * @param nodeName nome della cattura
     */
    public CaptureTreeNode(String nodeName) {
        super(nodeName, TYPE_CAPTURE_DEFINITION);
        captureType = "text";
        captureFormat = "";
        scope = "local";
        patterns = new LinkedHashMap<>();
        enabledSegments = new HashSet<>();
        blockedCaptures = new HashSet<>();
        classificationPath = null;
        temporary = false;
        startPeriod = false;
        endPeriod = false;
        notSubscribe = false;
    }

    /**
     * Istanzia un nodo a clonando un vecchio nodo
     *
     * @param nodeName nome della cattura
     * @param cloned nodo da clonare
     */
    public CaptureTreeNode(String nodeName, CaptureTreeNode cloned) {
        super(nodeName, TYPE_CAPTURE_DEFINITION);
        captureType = cloned.captureType;
        captureFormat = cloned.captureFormat;
        scope = cloned.scope;
        patterns = cloned.patterns;
        enabledSegments = cloned.enabledSegments;
        temporary = cloned.temporary;
        notSubscribe = cloned.notSubscribe;
        startPeriod = cloned.startPeriod;
        endPeriod = cloned.endPeriod;
        classificationPath = cloned.classificationPath;
        blockedCaptures = cloned.blockedCaptures;
    }

    /**
     * Imposta la configurazione del nodo
     *
     * @param cc configurazione del nodo
     */
    public void setConfiguration(CaptureConfiguration cc) {
        captureType = (String) intern.intern(cc.getType());
        captureFormat = (String) intern.intern(cc.getFormat());
        temporary = cc.isTemporary();
        notSubscribe = cc.isNotSubscribe();
        startPeriod = cc.isStartPeriod();
        endPeriod = cc.isEndPeriod();
    }

    /**
     * Aggiunge un pattern al nodo
     *
     * @param position posizione della cattura
     * @param value valore del pattern
     * @param normalization normalizzazione
     */
    public void addPattern(int position, String value, String normalization) {
        String[] row = new String[4];
        row[0] = String.valueOf(value.hashCode());
        row[1] = (String) intern.intern(value.toLowerCase());
        row[2] = (String) intern.intern(String.valueOf(position));
        row[3] = (String) intern.intern(normalization);
        patterns.put(row[0], row);
    }

    /**
     * rimuove tutti i pattern
     */
    public void resetPatterns() {
        patterns.clear();
    }

    /**
     * Imposta lo scope della cattura
     *
     * @param scope scope della cattura
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * Ritorna lo scope della cattura
     *
     * @return scope della cattura
     */
    public String getScope() {
        return scope;
    }

    /**
     * Ritorna il tipo della cattura
     *
     * @return tipo della cattura
     */
    public String getCaptureType() {
        return captureType;
    }

    /**
     * Ritorna il formatter della cattura
     *
     * @return formatter della cattura
     */
    public String getCaptureFormat() {
        return captureFormat;
    }

    /**
     * Rimuove un pattern
     *
     * @param id id del pattern
     */
    public void removePattern(String id) {
        patterns.remove(id);
    }

    /**
     * Aggiorna il pattern
     *
     * @param id id del pattern
     * @param position posizione
     * @param value valore del pattern
     * @param normalization formatter del pattern
     */
    public void updatePattern(String id, int position, String value, String normalization) {
        String[] row = new String[4];
        row[0] = id;
        row[1] = (String) intern.intern(value.toLowerCase());
        row[2] = (String) intern.intern(String.valueOf(position));
        row[3] = (String) intern.intern(normalization);
        patterns.put(id, row);
    }

    /**
     * Aggiorna il tipo di cattura
     *
     * @param captureType tipo di cattura
     */
    public void setCaptureType(String captureType) {
        this.captureType = captureType;
    }

    /**
     * Aggiorna il formatter della cattura
     *
     * @param captureFormat formatter della cattura
     */
    public void setCaptureFormat(String captureFormat) {
        this.captureFormat = captureFormat;
    }

    /**
     * ritorna l'elenco dei patterns in formato da mostrare in tabella
     *
     * @return lista delle righe che rappresentano il pattern
     */
    public List<String[]> getPatterns() {
        return new ArrayList(patterns.values());
    }

    /**
     * Ritorna la rappresentazione XML della cattura per essere salvata nel file
     * di configurazione segments.xml
     *
     * @return Elemento XML
     */
    public Element getXmlElement() {
        Element capture = new Element("c");
        capture.setAttribute("n", nodeName);
        capture.setAttribute("t", captureType);
        capture.setAttribute("tmp", String.valueOf(temporary));
        capture.setAttribute("sub", String.valueOf(notSubscribe));
        capture.setAttribute("s", String.valueOf(startPeriod));
        capture.setAttribute("e", String.valueOf(endPeriod));
        if (captureFormat.trim().length() > 0) {
            capture.setAttribute("f", captureFormat);
        }
        if (scope.equalsIgnoreCase("sentence")) {
            capture.setAttribute("s", "s");
        }
        patterns.values().stream().forEach((row) -> {
            Element pattern = new Element("p");
            pattern.addContent(row[1]);
            pattern.setAttribute("p", row[2]);
            String fix = row[3];
            if (fix == null) {
                fix = "";
            }
            pattern.setAttribute("f", fix);
            capture.addContent(pattern);
        });
        if (enabledSegments != null) {
            if (enabledSegments.size() > 0) {
                enabledSegments.stream().map((s) -> {
                    Element segment = new Element("s");
                    segment.addContent(s);
                    return segment;
                }).forEach((segment) -> {
                    capture.addContent(segment);
                });
            }
        }
        if (blockedCaptures != null) {
            if (blockedCaptures.size() > 0) {
                blockedCaptures.stream().map((s) -> {
                    Element block = new Element("bl");
                    block.addContent(s);
                    return block;
                }).forEach((blocked) -> {
                    capture.addContent(blocked);
                });
            }
        }
        if (classificationPath != null) {
            Element classfication = new Element("cl");
            classfication.addContent(classificationPath.toSmallClassString());
            capture.addContent(classfication);
        }
        return capture;
    }

    /**
     * Ritorna l'elenco delle catture bloccate
     *
     * @since 1.4
     * @return elenco catture bloccate
     */
    public Set<String> getBlockedCaptures() {
        return blockedCaptures;
    }

    /**
     * Imposta le catture da bloccare
     *
     * @since 1.4
     * @param blockedCaptures catture da bloccare
     */
    public void setBlockedCaptures(Set<String> blockedCaptures) {
        this.blockedCaptures = blockedCaptures;
    }

    /**
     * Aggiunge il nome di una cattura da bloccare
     *
     * @since 1.4
     * @param capture cattura da bloccare
     */
    public void addBlockedCapture(String capture) {
        this.blockedCaptures.add(capture);
    }

    /**
     * Ritorna l'elenco dei segmenti su cui la cattura è attiva
     *
     * @return insieme dei segmenti
     */
    public Set<String> getEnabledSegments() {
        return enabledSegments;
    }

    /**
     * Imposta i segmenti su cui la cattura è attiva
     *
     * @param enabledSegments insieme dei segmenti
     */
    public void setEnabledSegments(Set<String> enabledSegments) {
        this.enabledSegments = enabledSegments;
    }

    /**
     * Aggiunge un segmento su cui abilitare la cattura
     *
     * @param segment nome del segmento
     */
    public void addEnabledSegment(String segment) {
        enabledSegments.add(segment);
    }

    /**
     * Imposta il percorso di classificazione dove si deve classificare il
     * segmento se la cattura ha successo
     *
     * @param cp percorso di classificazione
     */
    public void setClassificationPath(ClassificationPath cp) {
        this.classificationPath = cp;
    }

    /**
     * Ritorna il percorso di classificazione
     *
     * @return percorso di classificazione
     */
    public ClassificationPath getClassificationPath() {
        return classificationPath;
    }

    /**
     * Verifica se la cattura è temporanea
     *
     * @return true se la cattura è temporanea
     */
    public boolean isTemporary() {

        return temporary;
    }

    /**
     * Verifica se la cattura può sovrascrivere valori presenti
     *
     * @since 1.4
     * @return true se non deve sovrascrivere
     */
    public boolean isNotSubscribe() {
        return notSubscribe;
    }

    /**
     * Imposta se la cattura è temporanea
     *
     * @param temporary true se la cattura è temporanea
     */
    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    /**
     * Imposta se la cattura non deve sovrascrivere
     *
     * @since 1.4
     * @param notSubscribe true se non deve sovrascrivere
     */
    public void setNotSubscribe(boolean notSubscribe) {
        this.notSubscribe = notSubscribe;
    }

    /**
     * Imposta se la cattura identifica un fine periodo
     *
     * @param selected true se la cattura è un fine periodo
     */
    public void setAsEndPeriod(boolean selected) {
        this.endPeriod = selected;
    }

    /**
     * Imposta se la cattura identifica un inizio periodo
     *
     * @param selected true se la cattura è un inizio periodo
     */
    public void setAsStartPeriod(boolean selected) {
        this.startPeriod = selected;
    }

    /**
     * Verifica se è inizio periodo
     *
     * @return true se la cattura è un inizio periodo
     */
    public boolean isStartPeriod() {
        return startPeriod;
    }

    /**
     * Verifica se è fine periodo
     *
     * @return true se è fine periodo
     */
    public boolean isEndPeriod() {
        return endPeriod;
    }

    /**
     * Gestisce la cancellazione di una tabella sistemando le definizioni
     *
     * @param table tabella da cancellare
     */
    public void removeTable(String table) {
        patterns.values().stream().forEach((row) -> {
            row[1] = row[1].replace("#" + table + " ", "");
        });
    }

    /**
     * Gestisce la rinomina di una tabella sistemando le definizioni
     *
     * @param table tabella da rinominare
     * @param newName nuovo nome
     */
    public void renameTable(String table, String newName) {
        patterns.values().stream().forEach((row) -> {
            row[1] = row[1].replace("#" + table + " ", "#" + newName + " ");
        });
    }

    /**
     * Aggiorna l'ordine dei pattern secondo quanto rappresentato graficamente
     *
     * @since 1.0.2
     * @param capturePatternTable tabella dei pattern da cui riprendere l'ordine
     */
    public void updatePatternsFromTable(JTable capturePatternTable) {
        Map<String, String[]> tmp = new LinkedHashMap<>(patterns);
        resetPatterns();
        DefaultTableModel model = (DefaultTableModel) capturePatternTable.getModel();
        int rows = model.getRowCount();
        for (int i = 0; i < rows; i++) {
            String id = (String) model.getValueAt(i, 0);
            patterns.put(id, tmp.get(id));
        }

    }

    /**
     * Rimuove una cattura da bloccare
     *
     * @since 1.4
     * @param capture cattura da rimuovere
     */
    public void removeBlockedCapture(String capture) {
        blockedCaptures.remove(capture);
    }

}
