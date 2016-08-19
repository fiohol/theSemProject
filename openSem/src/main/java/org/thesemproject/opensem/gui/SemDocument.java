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

import org.thesemproject.opensem.classification.ClassificationPath;
import org.thesemproject.opensem.segmentation.SegmentConfiguration;
import org.thesemproject.opensem.segmentation.SegmentationResults;
import org.thesemproject.opensem.segmentation.SegmentationUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.jdom2.Element;

/**
 * Gestisce l'oggetto documento costruito in memoria a partire da un file. Il
 * documento ha una sua rappresentazione nella tabella dei file e n
 * rappresentazioni (tanti quanti sono i segmenti) nel pannello dei segmenti. La
 * classe contiene anche il risultato della segmentazione applicata al documento
 * Questo oggetto permette, dato un documento di costruire le sue viste
 * grafiche.
 *
 */
public class SemDocument implements Serializable {

    /**
     * Ritorna le righe dei segmenti
     *
     * @return righe per popolare la tabella dei segmenti. ogni elemento della
     * lista è una riga ogni elemento dell'array è una colonna
     */
    public List<Object[]> getSegmentRows() {
        return segmentRows;
    }

    /**
     * Imnposta le righe per la tabella dei segmenti associati
     *
     * @param segmentRows righe per la tabella dei segmenti
     */
    public void setSegmentRows(List<Object[]> segmentRows) {
        this.segmentRows = segmentRows;
    }

    /**
     * Ritorna il nome del file
     *
     * @return nome del file
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Imposta il nome del file
     *
     * @param fileName nome del file
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Ritorna l'id del documento
     *
     * @return id del documento
     */
    public String getId() {
        return id;
    }

    /**
     * Imposta l'id del documento
     *
     * @param id id del documento
     */
    public void setId(String id) {
        this.id = id;
    }

    private Object[] row;
    private List<Object[]> segmentRows = new ArrayList<>();
    private List<Object[]> capturesRows = new ArrayList<>();
    private Map<String, List<ClassificationPath>> classRows = new HashMap<>();
    private Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments;
    private Map<String, Integer> stats;
    private String language;
    private String fileName;
    private String id;

    /**
     * Costruisce la rappresentazione XML di un document e di tutte le sue
     * caratteristiche
     *
     * @since 1.3.2
     *
     * @return Elemento che rappresenta il document
     */
    public Element getXmlElement() {
        Element doc = new Element("sd");
        doc.addContent(getObjectArray(row));
        Element sr = new Element("sr");
        doc.addContent(sr);
        for (Object[] segmentRow : segmentRows) {
            sr.addContent(getObjectArray(segmentRow));

        }
        Element cr = new Element("cr");
        doc.addContent(cr);
        for (Object[] captureRow : capturesRows) {
            sr.addContent(getObjectArray(captureRow));

        }
        doc.setAttribute("i", id);
        doc.setAttribute("l", language);
        doc.setAttribute("f", fileName);
        return doc;
    }

    /**
     * Imposta la lingua del documento
     *
     * @param language lingua del documento
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Ritorna la riga che rappresenta il documento nella tabella dei files
     *
     * @return array che rappresenta le colonne della tabella dei files
     */
    public Object[] getRow() {
        return row;
    }

    /**
     * Imposta le colonne della riga del pannello dei files.
     *
     * @param row riga della tabella
     */
    public void setRow(Object[] row) {
        this.row = row;
        this.fileName = ((String) row[1]).intern();
        this.id = String.valueOf(row[0]).intern();

    }

    /**
     * Ritorna il risultato della segmentazione per un file.
     *
     * @return risultato della segmentazione
     */
    public Map<SegmentConfiguration, List<SegmentationResults>> getIdentifiedSegments() {
        return identifiedSegments;
    }

    /**
     * Imposta i risultati del processo di segmentazione
     *
     * @param identifiedSegments risultato del processo di segmentazione
     * @throws Exception Eccezione
     */
    public void setIdentifiedSegments(Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments) throws Exception {
        this.identifiedSegments = identifiedSegments;
        List<Pair<Object[], List<ClassificationPath>>> ret = SegmentationUtils.getSegmentsRows(id, fileName, identifiedSegments, language);
        capturesRows = SegmentationUtils.getCapturesRows(identifiedSegments, language);
        segmentRows = new ArrayList<>();
        classRows.clear();

        for (Pair<Object[], List<ClassificationPath>> r : ret) {
            Object[] r1 = r.getLeft();
            List<ClassificationPath> cp = r.getRight();
            segmentRows.add(r1);
            classRows.put((String) r1[0], cp);
        }
        if (stats != null) {
            stats.clear();
        }
        SegmentationUtils.getFileStats(stats, identifiedSegments, "");
    }

    /**
     * Ritorna la lista delle righe da inserire nella tabella delle catture per
     * il documento
     *
     * @return righe per la tabella delle catture
     */
    public List<Object[]> getCapturesRows() {
        return capturesRows;
    }

    /**
     * Ritorna la lingua del documento
     *
     * @return lingua del documento
     */
    public String getLanguage() {
        return this.language;
    }

    /**
     * Ritorna la classificazione del documento
     *
     * @param segmentId del segmento (ogni segmento ha la sua classificazione,
     * ogni file ha più di una classificazione)
     * @return Lista dei percorsi di classificazione
     */
    public List<ClassificationPath> getClassPath(String segmentId) {
        return classRows.get(segmentId);
    }

    /**
     * Imposta la classificazione per un segmento
     *
     * @param segmentId del segmento
     * @param cp percorso di classificazione
     */
    public void setClassPath(String segmentId, List<ClassificationPath> cp) {
        classRows.put(segmentId, cp);
    }

    /**
     * Imposta il segment come marcato. Un segment marcato ha una X nell'ultima
     * colonna. Inoltre a fronte di una riclassificazione se un segmento marcato
     * cambia classificazione scatena un'allerta
     *
     * @param segmentId id del segmento
     */
    public void udpateSegmentsRows(String segmentId) {
        for (Object[] row : segmentRows) {
            String id = (String) row[0];
            if (id.equals(segmentId)) {
                row[6] = "X";
            }
        }
    }

    /**
     * Clona un documento
     *
     * @return Documento clonato
     */
    @Override
    public SemDocument clone() {
        SemDocument cloned = new SemDocument();
        if (row != null) {
            cloned.row = new Object[row.length];
            for (int i = 0; i < row.length; i++) {
                cloned.row[i] = row[i];
            }
        }
        cloned.segmentRows.addAll(segmentRows);
        cloned.capturesRows.addAll(capturesRows);
        cloned.classRows.putAll(classRows);
        if (identifiedSegments != null) {
            cloned.identifiedSegments = new HashMap<>();
            cloned.identifiedSegments.putAll(identifiedSegments);
        }
        if (language != null) {
            cloned.language = String.valueOf(language);
        }
        if (fileName != null) {
            cloned.fileName = String.valueOf(fileName);
        }
        cloned.id = String.valueOf(id);
        return cloned;
    }

    /**
     * Confronta due documenti. Può essere utilizzato per comparare due
     * documenti oppure per capire i cambiamenti effettuati su un documento
     *
     * @param semDocument documento con cui comparare il documento corrente
     * @return differenze tra due documenti.
     */
    public List<Object[]> compareWith(SemDocument semDocument) {
        ArrayList<Object[]> compare = new ArrayList<>();
        if (id == semDocument.id) {
            //Statistiche
            compareStats(compare, 3, semDocument, "Segments");
            compareStats(compare, 4, semDocument, "Captures");
            compareStats(compare, 5, semDocument, "Sentencies");
            compareStats(compare, 6, semDocument, "Classifications");
            //Verifico i cambi delle catture
            Map<String, Object> oldValues = new HashMap<>();
            int doppi = 0;
            for (Object[] oldValue : semDocument.capturesRows) {
                String key = oldValue[0] + "." + oldValue[1];
                if (oldValues.containsKey(key)) {
                    doppi++;
                    key = key + "." + doppi;
                }
                oldValues.put(key, oldValue[2]);
            }
            Map<String, Object> newValues = new HashMap<>();
            doppi = 0;
            for (Object[] newValue : capturesRows) {
                String key = newValue[0] + "." + newValue[1];
                if (newValues.containsKey(key)) {
                    doppi++;
                    key = key + "." + doppi;
                }
                newValues.put(key, newValue[2]);
                Object oldValue = oldValues.get(key);
                if (oldValue == null) {
                    oldValue = "";
                }
                if (!newValue[2].equals(oldValue)) {
                    String group = key;
                    if (group.indexOf(".") < 3) {
                        group = group.substring(group.indexOf(".") + 1);
                    }
                    compare.add(getCompareRow(key, oldValue, newValue[2], group, ""));
                }
            }

            for (String key : oldValues.keySet()) {
                if (!newValues.containsKey(key)) {
                    String group = key;
                    if (group.indexOf(".") < 3) {
                        group = group.substring(group.indexOf(".") + 1);
                    }
                    compare.add(getCompareRow(key, oldValues.get(key), "", group, ""));
                }
            }
            //Verifico i cambi sui segmenti di classificazione

            Map<String, Object> oldClasses = new HashMap<>();
            Map<String, String> oldChecked = new HashMap<>();
            for (Object[] oldValue : semDocument.segmentRows) {
                List<ClassificationPath> cp = semDocument.classRows.get(oldValue[0]);
                if (cp != null) {
                    int size = cp.size();
                    if (size > 0) {
                        oldClasses.put(oldValue[0] + ".Bayes1", cp.get(0).toSmallClassString());
                    }
                    if (size > 1) {
                        oldClasses.put(oldValue[0] + ".Bayes2", cp.get(1).toSmallClassString());
                    }
                }
                oldChecked.put((String) oldValue[0], (String) oldValue[6]);
            }
            boolean classificationChanged = false;
            Map<String, Object> newClasses = new HashMap<>();
            for (Object[] newValue : segmentRows) {
                boolean changed = false;
                List<ClassificationPath> cp = classRows.get(newValue[0]);
                if (cp != null) {
                    int size = cp.size();
                    if (size > 0) {
                        String key1 = newValue[0] + ".Bayes1";
                        newClasses.put(key1, cp.get(0).toSmallClassString());
                        Object oldValue1 = oldClasses.get(key1);
                        if (oldValue1 == null) {
                            oldValue1 = "";
                        }
                        Object newValue1 = newClasses.get(key1);
                        if (newValue1 == null) {
                            newValue1 = "";
                        }
                        if (!newValue1.equals(oldValue1)) {
                            compare.add(getCompareRow(key1, oldValue1, newValue1, "Classificazione", (String) newValue[4]));
                            changed = true;
                        }
                    }
                    if (size > 1) {
                        String key2 = newValue[0] + ".Bayes2";
                        newClasses.put(key2, cp.get(1).toSmallClassString());
                        Object oldValue1 = oldClasses.get(key2);
                        if (oldValue1 == null) {
                            oldValue1 = "";
                        }
                        Object newValue1 = newClasses.get(key2);
                        if (newValue1 == null) {
                            newValue1 = "";
                        }
                        if (!newValue1.equals(oldValue1)) {
                            compare.add(getCompareRow(key2, oldValue1, newValue1, "Classificazione", (String) newValue[4]));
                            changed = true;
                        }
                    }
                }
                if ("X".equalsIgnoreCase(oldChecked.get(newValue[0]))) {
                    newValue[6] = "X";
                    if (changed) {
                        newValue[6] = "A";

                    }
                } else if (changed) {
                    newValue[6] = "C";
                }
            }

            for (String key : oldClasses.keySet()) {
                if (!newClasses.containsKey(key)) {
                    compare.add(getCompareRow(key, oldClasses.get(key), "", "Classificazione", ""));

                }
            }

        }
        return compare;
    }

    private void compareStats(List<Object[]> compare, int i, SemDocument old, String kpi) {
        Integer newValue = (Integer) row[i];
        Integer oldValue = (Integer) old.row[i];
        if (newValue == null) {
            newValue = 0;
        }
        if (oldValue == null) {
            oldValue = 0;
        }
        if (newValue.intValue() != oldValue.intValue()) {
            compare.add(getCompareRow(kpi, oldValue, newValue, "Statistiche", ""));
        }
    }

    private Object[] getCompareRow(String kpi, Object oldValue, Object newValue, String classe, String text) {
        Object[] crow = new Object[6];
        crow[0] = this.id;
        crow[1] = kpi;
        String txt = String.valueOf(text);
        if (txt == null) {
            txt = "";
        }
        if (txt.length() > 120) {
            txt = txt.substring(0, 117) + "...";
        }
        crow[2] = txt;
        crow[3] = String.valueOf(oldValue);
        crow[4] = String.valueOf(newValue);
        crow[5] = classe;
        return crow;
    }

    /**
     * Ritorna le statistiche di un documento (segmenti, catture etc)
     *
     * @return mappa {indicatore, valore}
     */
    public Map<String, Integer> getStats() {
        if (stats == null) {
            stats = new HashMap<>();
        }
        if (stats.isEmpty()) {
            stats = SegmentationUtils.getFileStats(stats, identifiedSegments, "");
        }
        return stats;
    }

    private Element getObjectArray(Object[] row) {
        Element objArray = new Element("oa");
        objArray.setAttribute("s", String.valueOf(row.length));
        for (int i = 0; i < row.length; i++) {
            Element col = new Element("e");
            col.addContent(String.valueOf(row[i]));
            objArray.addContent(col);
        }
        return objArray;
    }

}
