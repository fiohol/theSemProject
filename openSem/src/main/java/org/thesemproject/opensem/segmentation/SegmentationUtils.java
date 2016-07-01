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
package org.thesemproject.opensem.segmentation;

import org.thesemproject.opensem.classification.ClassificationPath;
import org.thesemproject.opensem.gui.LogGui;
import org.thesemproject.opensem.segmentation.functions.DurationsMap;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.tree.DefaultMutableTreeNode;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.thesemproject.opensem.utils.BSonUtils;

/**
 * Utility per trattare i segmentationResults e renderli visibili
 */
public class SegmentationUtils {

    /**
     * Ritorna un BSON con i risultati della segmentazione
     *
     * @param document BSON in cui inserire i risultati
     * @param identifiedSegments risultato della segmentazione
     * @return Documento BSON con i risultati della segmentazione
     * @throws Exception Eccezione
     */
    public static Document getDocument(Document document, Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments) throws Exception {
        return writeDocumentSegments(document, identifiedSegments, "");
    }

    /**
     * Ritorna una rappresentazione grafica dei risultati di una segmentazione
     * sottoforma di JTree
     *
     * @param root root del JTree
     * @param identifiedSegments risultato della segmentazione
     * @param language lingua del documento
     * @return rappresentazione JTree del risultato della segmentazione
     * @throws Exception Eccezione
     */
    public static DefaultMutableTreeNode getJTree(DefaultMutableTreeNode root, Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments, String language) throws Exception {
        if (root == null) {
            return null;
        }
        if (language != null) {
            root.add(new DefaultMutableTreeNode("Lingua: " + language));
        }
        return getJTreeSegments(root, identifiedSegments, "");
    }

    /**
     * Ritorna le righe di un segmento come lista di coppie di array di oggetti
     * e liste di classificazione. Gli array di oggetti sono utili poi per
     * caricare i dati nella tabella di segmentazione della gui.
     *
     * @param id id del documento (può essere anche vuoto)
     * @param fileName nome del file analizzato (è solo un dato di
     * arricchimento)
     * @param identifiedSegments risultati della segmentazione
     * @param language lingua del documento
     * @return Righe segmentate
     * @throws Exception Eccezione
     */
    public static List<Pair<Object[], List<ClassificationPath>>> getSegmentsRows(String id, String fileName, Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments, String language) throws Exception {
        return getSegmentsRows(id, fileName, new ArrayList<>(), identifiedSegments, "", language);
    }

    /**
     * Ritorna le statistiche del file
     *
     * @param stats Mappa delle statistiche da popolare
     * @param identifiedSegments risultato della segmentazione
     * @param parentName nome del segmento padre
     * @return mappa delle statistiche
     */
    public static Map<String, Integer> getFileStats(Map<String, Integer> stats, Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments, String parentName) {
        if (identifiedSegments == null) {
            return stats;
        }
        if (stats == null) {
            stats = new HashMap<>();
        }
        Set<SegmentConfiguration> segments = identifiedSegments.keySet();
        for (SegmentConfiguration s : segments) {
            String segmentName = (parentName.length() > 0 ? parentName + "." : "") + s.getName();
            Map<String, Integer> cellIndex = null;
            List<SegmentationResults> srs = identifiedSegments.get(s);
            for (SegmentationResults sr : srs) {
                boolean addToDocument = true;
                if (s.getName().equalsIgnoreCase("Not Identified")) {
                    s.setName("");
                    segmentName = parentName;
                    addToDocument = false;
                }
                if (s.getName().length() != 0) {
                    if (parentName.length() == 0) {
                        updateStats(stats, "Segments", 1);
                    }
                    updateStats(stats, "GlobalSegments", 1);
                    if (s.isClassify()) {
                        updateStats(stats, "ClassSegments", 1);
                    }
                    updateStats(stats, segmentName, 1);
                }
                Map<SegmentConfiguration, List<SegmentationResults>> subSegments = sr.getSubsentencies();
                if (subSegments.size() > 0) {// HO sottosegmenti
                    stats = getFileStats(stats, subSegments, segmentName);
                } else {
                    Map<String, String> captures = sr.getCaptureResults();
                    if (!captures.isEmpty()) {
                        updateStats(stats, "Captures", captures.size());
                        for (String cc : captures.keySet()) {
                            String value = captures.get(cc);
                            if (value.length() > 0) {
                                updateStats(stats, cc, 1);
                            }
                        }
                    }
                    List<String> sent = sr.getSentencies();
                    updateStats(stats, "Sentencies", sent.size());
                    if (s.isClassify()) {
                        List<ClassificationPath> bayes = sr.getClassificationPaths();
                        updateStats(stats, "Classifications", bayes.size());
                    }
                }

            }
        }
        return stats;
    }

    private static DefaultMutableTreeNode getJTreeSegments(DefaultMutableTreeNode document, Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments, String parentName) throws Exception {
        Set<SegmentConfiguration> segments = identifiedSegments.keySet();
        for (SegmentConfiguration s : segments) {
            String segmentName = (parentName.length() > 0 ? parentName + "." : "") + s.getName();
            Map<String, Integer> cellIndex = null;
            List<SegmentationResults> srs = identifiedSegments.get(s);
            for (SegmentationResults sr : srs) {
                DefaultMutableTreeNode treeNode;
                boolean addToDocument = true;
                if (s.getName().equalsIgnoreCase("Not Identified")) {
                    s.setName("");
                    segmentName = parentName;
                }
                if (s.getName().length() != 0) {
                    treeNode = new DefaultMutableTreeNode(s.getName());

                } else {
                    treeNode = document;
                    addToDocument = false;
                }
                double duration = sr.getDurationYears();
                if (duration > 0) {
                    DefaultMutableTreeNode durationsNode = new DefaultMutableTreeNode("Timeline");
                    durationsNode.add(new DefaultMutableTreeNode("Durata anni: " + duration));
                    durationsNode.add(new DefaultMutableTreeNode("Durata anni (assoluta): " + sr.getIntDurationYears()));
                    durationsNode.add(new DefaultMutableTreeNode("Anni: " + Arrays.toString(sr.getYears())));
                    durationsNode.add(new DefaultMutableTreeNode("Mesi: " + Arrays.toString(sr.getMonths())));
                    treeNode.add(durationsNode);
                }
                Map<SegmentConfiguration, List<SegmentationResults>> subSegments = sr.getSubsentencies();
                if (subSegments.size() > 0) {// HO sottosegmenti
                    treeNode = getJTreeSegments(treeNode, subSegments, segmentName);
                } else {
                    Map<String, String> captures = sr.getCaptureResults();
                    if (!captures.isEmpty()) {
                        final DefaultMutableTreeNode capturesTreeNode = new DefaultMutableTreeNode("Catture");
                        treeNode.add(capturesTreeNode);
                        captures.keySet().stream().map((captureConfiguration) -> {
                            return new DefaultMutableTreeNode(captureConfiguration + ": " + captures.get(captureConfiguration));
                        }).forEach((captureTreeNode) -> {

                            capturesTreeNode.add(captureTreeNode);
                        });
                    }
                    List<String> sent = sr.getSentencies();
                    StringBuilder text = new StringBuilder();
                    sent.stream().forEach((sen) -> {
                        text.append(sen).append("\n");
                    });
                    treeNode.add(new DefaultMutableTreeNode(new TextNode(text)));
                    if (s.isClassify()) {
                        List<ClassificationPath> bayes = sr.getClassificationPaths();
                        javax.swing.tree.DefaultMutableTreeNode clResults = new javax.swing.tree.DefaultMutableTreeNode("Classificazione");
                        bayes.stream().forEach((cp) -> {
                            javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode(cp.getTechnology());
                            javax.swing.tree.DefaultMutableTreeNode currentNode = treeNode1;
                            for (int i = 0; i < 4; i++) {
                                String node = cp.getPath()[i];
                                if (node != null) {
                                    String label = node + "(" + ClassificationPath.df.format(cp.getScore()[i]) + ")";
                                    javax.swing.tree.DefaultMutableTreeNode treeNode2 = new javax.swing.tree.DefaultMutableTreeNode(label);
                                    currentNode.add(treeNode2);
                                    currentNode = treeNode2;
                                }
                            }
                            clResults.add(treeNode1);
                        });
                        treeNode.add(clResults);
                    }
                }
                if (addToDocument) {
                    document.add(treeNode);
                }
            }
        }
        return document;
    }

    private static List<Pair<Object[], List<ClassificationPath>>> getSegmentsRows(String id, String fileName, List<Pair<Object[], List<ClassificationPath>>> rows, Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments, String parentName, String language) {
        Set<SegmentConfiguration> segments = identifiedSegments.keySet();
        for (SegmentConfiguration s : segments) {
            String segmentName = (parentName.length() > 0 ? parentName + "." : "") + s.getName();
            Map<String, Integer> cellIndex = null;
            List<SegmentationResults> srs = identifiedSegments.get(s);
            for (SegmentationResults sr : srs) {
                if (s.getName().equalsIgnoreCase("Not Identified")) {
                    s.setName("");
                    segmentName = parentName;
                }
                Map<SegmentConfiguration, List<SegmentationResults>> subSegments = sr.getSubsentencies();
                if (subSegments.size() > 0) {// HO sottosegmenti
                    rows = getSegmentsRows(id, fileName, rows, subSegments, segmentName, language);
                } else {
                    List<String> sent = sr.getSentencies();
                    StringBuilder text = new StringBuilder();
                    sent.stream().forEach((sen) -> {
                        text.append(sen).append("\n");
                    });
                    if (s.isClassify()) { //Segmento che ci interessa
                        Object[] row = new Object[7];
                        row[0] = String.valueOf(id + "." + (rows.size() + 1));
                        row[5] = fileName;
                        row[3] = language;
                        row[1] = "";
                        row[2] = "";
                        row[4] = text.toString();
                        row[6] = "";
                        List<ClassificationPath> bayes = sr.getClassificationPaths();
                        if (bayes.size() == 1) {
                            row[1] = bayes.get(0).toSmallString();
                        } else if (bayes.size() == 2) {
                            row[1] = bayes.get(0).toSmallString();
                            row[2] = bayes.get(1).toSmallString();
                        }
                        rows.add(Pair.of(row, bayes));
                    }
                }
            }
        }
        return rows;
    }

    private static void updateStats(Map<String, Integer> stats, String key, int i) {
        Integer segmentsNo = stats.get(key);
        if (segmentsNo == null) {
            segmentsNo = 0;
        }
        segmentsNo += i;
        stats.put(key, segmentsNo);
    }

    private static void writeHtmlSegments(Writer sdOutSeg, Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments, String parentName) throws Exception {
        Set<SegmentConfiguration> segments = identifiedSegments.keySet();
        for (SegmentConfiguration s : segments) {
            String segmentName = (parentName.length() > 0 ? parentName + "." : "") + s.getName();
            Map<String, Integer> cellIndex = null;
            List<SegmentationResults> srs = identifiedSegments.get(s);
            for (SegmentationResults sr : srs) {
                if (s.getName().equalsIgnoreCase("Not Identified")) {
                    s.setName("");
                    segmentName = parentName;
                }
                if (s.getName().length() != 0) {
                    sdOutSeg.write("<div class=\"" + s.getName() + "\"><hr><b>" + s.getName() + "</b><hr>\n");
                }
                double duration = sr.getDurationYears();
                if (duration > 0) {
                    sdOutSeg.write("<br><b>Durata anni:</b> " + duration);
                    sdOutSeg.write("<br><b>Durata anni (assoluta):</b> " + sr.getIntDurationYears());
                    sdOutSeg.write("<br><b>Anni:</b> " + Arrays.toString(sr.getYears()));
                }
                Map<SegmentConfiguration, List<SegmentationResults>> subSegments = sr.getSubsentencies();
                if (subSegments.size() > 0) {
                    // HO sottosegmenti
                    writeHtmlSegments(sdOutSeg, subSegments, segmentName);
                } else {
                    Map<String, String> captures = sr.getCaptureResults();
                    if (!captures.isEmpty()) {
                        sdOutSeg.write("<div class=\"captures\">\n");
                        sdOutSeg.write("<table>");
                        for (String captureConfiguration : captures.keySet()) {
                            sdOutSeg.write("<tr><td><b>" + captureConfiguration + "</b>:</td><td>" + captures.get(captureConfiguration) + "</td></tr>\n");
                        }
                        sdOutSeg.write("</table></div><br>\n");
                    }
                    List<String> sent = sr.getSentencies();
                    StringBuilder text = new StringBuilder();
                    for (String sen : sent) {
                        sdOutSeg.write(sen + "<br>");
                        text.append(sen).append("\n");
                    }
                    if (s.isClassify()) {
                        List<ClassificationPath> cps = sr.getClassificationPaths();
                        sdOutSeg.write("<br><b>Classification</b><br>");
                        for (ClassificationPath cp : cps) {
                            sdOutSeg.write(cp.toString() + "<br>");
                        }
                    }
                }
                sdOutSeg.write("</div>");
            }
        }
    }

    /**
     * Ritorna una rappresentazione HTML dei risultati della segmentazione
     *
     * @param identifiedSegments risultati della segmentazione
     * @param language lingua
     * @return testo html
     * @throws Exception Eccezione
     */
    public static String getHtml(Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments, String language) throws Exception {
        Writer sdOutSeg = new StringWriter();
        sdOutSeg.write("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">"
                + "<style>"
                + "td, th {\n"
                + "    border: 1px solid black;\n"
                + "}"
                + "table {\n"
                + "    border-collapse: collapse;\n"
                + "}"
                + "</style>"
                + "</head><body>");
        if (language != null) {
            sdOutSeg.write("<b>Language</b> " + language + "<br><br>");
        }
        writeHtmlSegments(sdOutSeg, identifiedSegments, "");
        sdOutSeg.write("<br><br></body></html>");
        sdOutSeg.close();
        return sdOutSeg.toString();
    }

    /**
     * Ritorna una rappresentazione HTML delle durate. Dato che ogni segmento
     * può avere una durata, determinata dalla differenza di tempo tra il valore
     * della cattura che identifica un inizio periodo e il valore di una cattura
     * che identifica un fine periodo, ogni cosa catturata all'interno del
     * segmento ha la stessa durata. Si supponga che si sta analizzando un
     * documento dove vengono descritti fatti temporalmente classificabili. Se
     * la segmentazione avviene per priodo, tutto quanto descritto nel periodo
     * ha esattamente la durata del priodo.
     *
     * @param identifiedSegments risultato della segmentazione
     * @return HTML delle durate sottoforma di tabella pivot, dove nelle righe
     * ci sono le catture del segmento e nelle colonne gli anni
     */
    public static String getHtmlDurations(Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments) {
        DurationsMap durations = DurationsMap.getDurations(identifiedSegments);
        List<Pair<String, String>> keys = durations.keySet();
        Writer sdOutSeg = new StringWriter();
        try {
            sdOutSeg.write("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">"
                    + "<style>"
                    + "body{\n"
                    + "   font-family: Arial; \n"
                    + "}"
                    + "td, th {\n"
                    + "    border: 1px solid black;\n"
                    + "}"
                    + "table {\n"
                    + "    border-collapse: collapse;\n"
                    + "}"
                    + "</style>"
                    + "</head><body>");
            sdOutSeg.write("<tr><td><b>Elemento</b></td><td><b>Valore</b></td><td><b>Durata (Anni)</b></td>");
            List<String> years = durations.getYearsList();
            for (String year : years) {
                sdOutSeg.write("<td><b>" + year + "</b></td>");
            }
            sdOutSeg.write("</tr>");
            for (Pair<String, String> key : keys) {
                sdOutSeg.write("<tr><td>" + key.getLeft() + "</td><td>" + key.getRight() + "</td><td align=\"right\">" + durations.getDurationYears(key) + "</td>");
                Set<String> enabledYears = durations.getYears(key);
                for (String year : years) {
                    sdOutSeg.write((enabledYears.contains(year) ? "<td bgcolor=\"#000080\">&nbsp;" : "<td>") + "</td>");
                }
                sdOutSeg.write("</tr>");
            }
            sdOutSeg.write("</table>");
            sdOutSeg.write("<br><br></body></html>");
            sdOutSeg.close();
        } catch (IOException ex) {
            LogGui.printException(ex);
        }
        return sdOutSeg.toString();
    }

    /**
     * Parametro per identificare "Il più lungo"
     */
    public static int LONGEST = 0;

    /**
     * Parametro per identificare "il più corto"
     */
    public static int SHORTEST = 1;

    /**
     * parametro per identificare "il primo"
     */
    public static int FIRST = 2;

    /**
     * parametro per identificare "l'ultimo"
     */
    public static int LAST = 3;

    /**
     * Ritorna l'esperienza richiesta dal parametro come coppia nome esperienza,
     * valore esperienza. Si supponga che una degli elementi catturati siano le
     * esperienze di una persona in una scheda personale. Si supponga che questa
     * esperienza venga memorizzata in una cattura chiamata
     * "EsperienzaFormativa". Attraverso questo metodo è possibile conoscere la
     * prima, l'ultima, la più lunga, la più corta esperienza formativa della
     * persona
     *
     * @param identifiedSegments risultato della segmentazione
     * @param key esperienza o nome della cattura che si vuole misurare. Se si
     * vuole misurare un parametro basato sulla classificazione la chiave deve
     * essere DurationsMap.CLASSIFICATIONS
     * @param type Tipo di misura (più lunga, più corta etc)
     * @return chiave per accedere alla DurationsMap e farsi dare i valori
     * dell'esperienza.
     */
    public Pair<String, String> getExperience(Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments, String key, int type) {
        DurationsMap durations = DurationsMap.getDurations(identifiedSegments);
        return getExperience(durations, key, type);

    }

    /**
     * Ritorna l'esperienza richiesta dal parametro come coppia nome esperienza,
     * valore esperienza. Si supponga che una degli elementi catturati siano le
     * esperienze di una persona in una scheda personale. Si supponga che questa
     * esperienza venga memorizzata in una cattura chiamata
     * "EsperienzaFormativa". Attraverso questo metodo è possibile conoscere la
     * prima, l'ultima, la più lunga, la più corta esperienza formativa della
     * persona
     *
     * @param durations durata
     * @param key esperienza o nome della cattura che si vuole misurare. Se si
     * vuole misurare un parametro basato sulla classificazione la chiave deve
     * essere DurationsMap.CLASSIFICATIONS
     * @param type Tipo di misura (più lunga, più corta etc)
     * @return chiave per accedere alla DurationsMap e farsi dare i valori
     * dell'esperienza.
     */
    public Pair<String, String> getExperience(DurationsMap durations, String key, int type) {
        Pair<String, String> ret = null;
        double rdYear = 0;
        int rlYear = 3000;
        int rfYear = 0;
        if (durations != null) { //Il calcolo deve esistere
            if (key != null) { //La chiave può essere il nome dell'attributo o la chiave DurationsMap.CLASSIFICATIONS
                //Il valore può essere anche null. In questo caso, indipendentemente dal valore da il risultato.
                List<Pair<String, String>> keys = durations.keySet();
                for (Pair<String, String> k : keys) {
                    if (k.getLeft().equals(key)) { //Siamo in quello che chiedono.

                        double dYear = durations.getDurationYears(k);
                        Set<String> years = durations.getYears(k);
                        int firstYear = 3000;
                        int lastYear = 0;
                        for (String y : years) {
                            int d = Integer.parseInt(y);
                            if (d < firstYear) {
                                firstYear = d;
                            }
                            if (d > lastYear) {
                                lastYear = d;
                            }
                        }
                        boolean change = false;
                        if (ret == null) {
                            change = true;
                        } else if (type == LONGEST) {
                            if (rdYear < dYear) {
                                change = true;
                            }
                        } else if (type == SHORTEST) {
                            if (rdYear > dYear) {
                                change = true;
                            }
                        } else if (type == FIRST) {
                            if (rfYear > firstYear) {
                                change = true;
                            }
                        } else if (type == LAST) {
                            if (rfYear < firstYear || rlYear > lastYear) {
                                change = true;
                            }
                        }
                        if (change) {
                            ret = k;
                            rdYear = dYear;
                            rfYear = firstYear;
                            rlYear = lastYear;
                        }

                    }
                }
            }
        }
        return ret;
    }

    private static Document writeDocumentSegments(Document document, Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments, String parentName) throws Exception {
        Set<SegmentConfiguration> segments = identifiedSegments.keySet();
        for (SegmentConfiguration s : segments) {
            String segmentName = (parentName.length() > 0 ? parentName + "." : "") + s.getName();
            Map<String, Integer> cellIndex = null;
            List<SegmentationResults> srs = identifiedSegments.get(s);
            for (SegmentationResults sr : srs) {
                Document segmentDocument = new Document();
                if (s.getName().equalsIgnoreCase("Not Identified")) {
                    s.setName("");
                    segmentName = parentName;
                }
                if (s.getName().length() != 0) {
                    segmentDocument.put("SegmentName", s.getName());
                }
                double duration = sr.getDurationYears();
                if (duration > 0) {
                    Document timeline = new Document();
                    timeline.put("DurationYears", duration);
                    timeline.put("DurationYearsAbsolute", sr.getIntDurationYears());
                    timeline.put("Years", sr.getYears());
                    timeline.put("Months", sr.getMonths());
                    segmentDocument.put("Timeline", timeline);
                }
                Map<SegmentConfiguration, List<SegmentationResults>> subSegments = sr.getSubsentencies();
                if (subSegments.size() > 0) {
                    // HO sottosegmenti
                    segmentDocument = writeDocumentSegments(segmentDocument, subSegments, segmentName);
                } else {
                    Map<String, String> captures = sr.getCaptureResults();
                    if (!captures.isEmpty()) {
                        for (String captureConfiguration : captures.keySet()) {
                            segmentDocument.put(captureConfiguration, captures.get(captureConfiguration));
                        }
                    }
                    List<String> sent = sr.getSentencies();
                    StringBuilder text = new StringBuilder();
                    sent.stream().forEach((String sen) -> {
                        text.append(sen).append("\n");
                    });
                    segmentDocument.put(BSonUtils.TEXT, text);
                    if (s.isClassify()) {
                        List<ClassificationPath> cps = sr.getClassificationPaths();
                        for (int i = 0; i < cps.size(); i++) {
                            document.put("BayesPath" + (i + 1), cps.get(i).getPath());
                            document.put("BayesScore" + (i + 1), cps.get(i).getScore());
                        }
                    }
                }
                document.append("Segment", segmentDocument);
            }
        }
        return document;
    }

    /**
     * Ritorna le catture sottoforma di righe che possono essere messe in
     * tabella (rappresentazione GUI)
     *
     * @param identifiedSegments risultato della segmentazione
     * @param language lingua
     * @return Lista di array di oggetti. Ogni array di oggetti è una riga di
     * una tabella, ogni elemento dell'array è una colonna.
     */
    public static List<Object[]> getCapturesRows(Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments, String language) {
        return getCapturesRows(new ArrayList<>(), identifiedSegments, "", language);
    }

    private static List<Object[]> getCapturesRows(List<Object[]> rows, Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments, String parentName, String language) {
        Set<SegmentConfiguration> segments = identifiedSegments.keySet();
        for (SegmentConfiguration s : segments) {
            String segmentName = (parentName.length() > 0 ? parentName + "." : "") + s.getName();
            Map<String, Integer> cellIndex = null;
            List<SegmentationResults> srs = identifiedSegments.get(s);
            int countSegment = 0;
            for (SegmentationResults sr : srs) {
                countSegment++;
                if (s.getName().equalsIgnoreCase("Not Identified")) {
                    s.setName("");
                    segmentName = parentName;
                }
                Map<SegmentConfiguration, List<SegmentationResults>> subSegments = sr.getSubsentencies();
                if (subSegments.size() > 0) {// HO sottosegmenti
                    rows = getCapturesRows(rows, subSegments, segmentName, language);
                } else {
                    Map<String, String> map = sr.getCaptureResults();
                    if (map.size() > 0) {
                        for (String cc : map.keySet()) {

                            String value = map.get(cc);
                            Object[] row = new Object[3];
                            if (srs.size() > 1) {
                                row[0] = countSegment + "." + segmentName;
                            } else {
                                row[0] = segmentName;
                            }
                            row[1] = cc;
                            row[2] = value;
                            rows.add(row);
                        }
                    }
                }
            }
        }
        return rows;
    }

    /**
     * Nodo testo per l'albero di rappresentazione del ritultato della
     * segmentazione
     */
    public static class TextNode {

        private String text = "";

        /**
         * Costruisce il nodo testo
         *
         * @param text testo
         */
        public TextNode(StringBuilder text) {
            this.text = text.toString();
        }

        /**
         * Tronca, per la rappresentazione grafica, il testo a 60 caratteri
         *
         * @return testo troncato
         */
        @Override
        public String toString() {
            if (text.length() > 60) {
                return text.substring(0, 60) + "...";
            }
            return text;
        }

        /**
         * Ritorna il testo completo
         *
         * @return testo completo
         */
        public String getText() {
            return text;
        }
    }
}
