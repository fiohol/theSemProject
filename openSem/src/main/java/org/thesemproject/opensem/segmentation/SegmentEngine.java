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
import org.thesemproject.opensem.classification.MulticlassEngine;
import org.thesemproject.opensem.gui.utils.GuiUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.thesemproject.opensem.gui.LogGui;
import org.thesemproject.opensem.gui.modelEditor.CaptureTreeNode;
import org.thesemproject.opensem.gui.modelEditor.DataProviderTreeNode;
import org.thesemproject.opensem.gui.modelEditor.DataProviderTreeRelationshipNode;
import org.thesemproject.opensem.gui.modelEditor.DictionaryTreeNode;
import org.thesemproject.opensem.gui.modelEditor.ModelTreeNode;
import org.thesemproject.opensem.gui.modelEditor.SegmentTreeNode;
import org.thesemproject.opensem.gui.modelEditor.TableTreeNode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import org.thesemproject.opensem.classification.Tokenizer;
import org.thesemproject.opensem.gui.modelEditor.CapturesGroupTreeNode;
import org.thesemproject.opensem.gui.modelEditor.FormulaTreeNode;

/**
 * Motore di segmentazione. Applicando la configurazione del patternMatrix ad un
 * testo lo segmenta secondo segmenti, catture, dizionari e tabelle
 */
public class SegmentEngine {

    /**
     * Gestisce il ritorno a capo
     */
    public static final String CR = "\n";

    /**
     * Gestisce il tab
     */
    public static final String TAB = "\t";
    private final Map<String, Pattern> bigRegexPattern;
    private DefaultTreeModel visualStructure;
    private final DictionaryTreeNode dictionaryNode;
    private final ModelTreeNode segmentsNode;
    private final ModelTreeNode tablesNode;
    private final ModelTreeNode globalCapturesTreeNode;
    private final ModelTreeNode dataprovidersNode;
    private final List<SegmentConfiguration> patternMatrix;
    private final Map<String, Pattern> dictionary;
    private final Map<String, Pattern> tables;
    private final Map<String, DataProviderConfiguration> providers;
    private final Map<String, Set<String>> tablesValues;

    /**
     * Crea il segmentEngine vuoto. Il segmentEngine va poi inizializzato
     * attraverso una configurazione
     */
    public SegmentEngine() {
        patternMatrix = new ArrayList<>();
        dictionary = new LinkedHashMap<>();
        tables = new LinkedHashMap<>();
        providers = new LinkedHashMap<>();
        bigRegexPattern = new ConcurrentHashMap<>();
        dictionaryNode = new DictionaryTreeNode("Dizionario");
        segmentsNode = new ModelTreeNode("Segmenti", ModelTreeNode.TYPE_SEGMENT);
        tablesNode = new ModelTreeNode("Tabelle", ModelTreeNode.TYPE_TABLE);
        globalCapturesTreeNode = new ModelTreeNode("Catture", ModelTreeNode.TYPE_CAPTURE);
        dataprovidersNode = new ModelTreeNode("Data Providers", ModelTreeNode.TYPE_DATA_PROVIDERS);
        visualStructure = new DefaultTreeModel(getCleansedModel());
        tablesValues = new HashMap<>();
    }

    /**
     * Inizializza il segmentEngine con una configurazione XML
     *
     * @param model percorso del file di configurazione
     * @return true se il sistema è inizializzato
     */
    public boolean init(String model) {
        try {
            buildPatternMatrix(new File(model));
            return true;
        } catch (Exception e) {
            LogGui.printException(e);
        }
        return false;
    }

    /**
     * Inizializza il segmentEngine con un DOM XML e il relativo Path
     *
     * @param model DOM del file di configurazione
     * @param path percorso della struttura
     * @return true se inizializzato
     */
    public boolean init(Document model, String path) {
        try {
            buildPatternMatrix(model, path);
            return true;
        } catch (JDOMException | IOException e) {
            LogGui.printException(e);
        }
        return false;
    }

    /**
     * Ritorna la struttura visuale ovvero la rappresentazione grafica del
     * modello
     *
     * @return rappresentazione grafica del modello
     */
    public DefaultTreeModel getVisualStructure() {
        return visualStructure;
    }

    /**
     * Segmenta un testo senza classificare i singoli segmenti
     *
     * @param text testo da segmentare
     * @param language lingua del testo
     * @return risultato della segmentazione sottoforma di mappa
     * {configurazioneSegmento, Lista di segmentResult} la mappa è gerchica in
     * quanto segmentConfiguration lo può essere. Il risultato va quindi letto
     * gerarchicamente
     */
    public Map<SegmentConfiguration, List<SegmentationResults>> getSegments(String text, String language) {
        if (text == null) {
            return null;
        }
        text = text.toLowerCase();
        return getSegments(Arrays.asList(text.split(CR)), language);
    }

    /**
     * Segmenta un testo, classificando i segmenti marcati come classificabili
     *
     * @param text testo da segmentare
     * @param me motore di classificazione (deve essere inizializzato)
     * @param threshold soglia di classificazione
     * @param language lingua del testo
     * @return risultato della segmentazione sottoforma di mappa
     * {configurazioneSegmento, Lista di segmentResult} la mappa è gerchica in
     * quanto segmentConfiguration lo può essere. Il risultato va quindi letto
     * gerarchicamente
     */
    public Map<SegmentConfiguration, List<SegmentationResults>> getSegments(String text, MulticlassEngine me, double threshold, String language) {
        if (text == null) {
            return null;
        }
        text = text.toLowerCase();
        return startProcess(Arrays.asList(text.split(CR)), me, threshold, language);
    }

    private Map<SegmentConfiguration, List<SegmentationResults>> getSegments(List<String> lines, String language) {
        return startProcess(lines, null, -1, language);
    }

    private Map<SegmentConfiguration, List<SegmentationResults>> startProcess(List<String> lines, MulticlassEngine me, double threshold, String language) {
        //Apre tutti gli indici dei dataproviders...

        String line;
        String previousLines = "";
        String lastLine = "";
        SegmentConfiguration currentSegment = null;
        for (SegmentConfiguration segmentConfiguration : patternMatrix) {
            if (segmentConfiguration.isDefault()) {
                currentSegment = segmentConfiguration;
            }
        }
        if (currentSegment == null) {
            currentSegment = new SegmentConfiguration("Not identified", false, false);
        }
        Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments = new LinkedHashMap<>();
        for (String currLine : lines) {
            line = currLine;
            line = line.replaceAll(TAB, "").replaceAll("(\\s+)", " ").trim().toLowerCase();
            if (line.length() != 0) {
                boolean match = false;
                for (SegmentConfiguration section : patternMatrix) {
                    List<Pattern> patterns = section.getPatternList();
                    for (Pattern pattern : patterns) {
                        if (pattern.matcher(line).find()) { //ha matchato la sezione
                            //A questo punto la riga va da sola e la previousline va nella sezione precedente
                            if (previousLines.length() > 0) {
                                addSentenceToResult(previousLines, currentSegment, identifiedSegments);
                                previousLines = "";
                                lastLine = "";
                            }
                            if (!section.getName().equals(currentSegment.getName())) {
                                currentSegment = section; //Ho una nuova sezione
                            }
                            if (identifiedSegments.containsKey(currentSegment) && currentSegment.isMultiple()) { //Ho un cambio di sezione ma � multipla
                                identifiedSegments.get(currentSegment).add(new SegmentationResults()); // Aggiungo un nuovo segmentresult ala lsita dei risultati
                            }
                            match = true;
                            break;
                        }
                    }
                    if (match) {
                        break;
                    } else //Provo a vedere se c'è un match con linea precedente + \n linea 
                     if (lastLine.length() != 0) {
                            String newLine = lastLine + "@" + line;
                            for (Pattern pattern : patterns) {
                                if (pattern.matcher(newLine).find()) { //ha matchato la sezione
                                    //A questo punto la riga va da sola e la previousline va nella sezione precedente
                                    line = lastLine + " " + line;
                                    if (previousLines.length() > 0) {
                                        int pos = previousLines.lastIndexOf(" " + lastLine);
                                        if (pos != -1) {
                                            previousLines = previousLines.substring(0, pos);
                                        } else if (previousLines.equals(lastLine)) {
                                            previousLines = "";
                                        }
                                        if (previousLines.length() > 0) {
                                            addSentenceToResult(previousLines, currentSegment, identifiedSegments);
                                        }
                                        previousLines = "";
                                        lastLine = "";
                                    }
                                    if (!section.getName().equals(currentSegment.getName())) {
                                        currentSegment = section; //Ho una nuova sezione
                                    }
                                    if (identifiedSegments.containsKey(currentSegment) && currentSegment.isMultiple()) { //Ho un cambio di sezione ma � multipla
                                        identifiedSegments.get(currentSegment).add(new SegmentationResults()); // Aggiungo un nuovo segmentresult ala lsita dei risultati
                                    }
                                    match = true;
                                    break;
                                }
                            }
                        }
                }
                if (previousLines.length() != 0) {
                    previousLines = previousLines + " " + line;
                } else {
                    previousLines = line;
                }
                if (!match) {
                    lastLine = line;
                } else {
                    lastLine = "";
                }
            } else if (previousLines.length() > 0) {
                addSentenceToResult(previousLines, currentSegment, identifiedSegments);
                previousLines = line;
            }
            addLineToResult(line, currentSegment, identifiedSegments);
        }
        if (previousLines.length() > 0) {
            addSentenceToResult(previousLines, currentSegment, identifiedSegments);
        }
        //Fa le sottosezioni e le catture
        for (SegmentConfiguration segmentBean : patternMatrix) { //Verifico se qualche sergment ha figli
            processSegment(segmentBean, identifiedSegments, me, threshold, language);
        }
        //Chiude tutti gli indici dei dataproviders...asdas

        return identifiedSegments;
    }

    private void buildPatternMatrix(File fXmlFile) throws JDOMException, Exception {
        if (fXmlFile.exists()) {
            LogGui.info("Read XML File...");
            Document document = GuiUtils.readXml(fXmlFile.getAbsolutePath());
            LogGui.info("Build pattern matrix...");
            buildPatternMatrix(document, fXmlFile.getParent());
        } else {
            buildPatternMatrix(null, fXmlFile.getParent());
        }
    }

    private DefaultMutableTreeNode getCleansedModel() {
        dictionaryNode.removeAllChildren();
        globalCapturesTreeNode.removeAllChildren();
        dataprovidersNode.removeAllChildren();
        segmentsNode.removeAllChildren();
        tablesNode.removeAllChildren();
        DefaultMutableTreeNode model = new DefaultMutableTreeNode(new ModelTreeNode("Modello", ModelTreeNode.TYPE_ROOT));
        model.add(dictionaryNode);
        model.add(globalCapturesTreeNode);
        model.add(segmentsNode);
        model.add(tablesNode);
        model.add(dataprovidersNode);
        return model;
    }

    private void buildPatternMatrix(Document document, String storageFolder) throws JDOMException, IOException {
        dictionaryNode.removeAllChildren();
        segmentsNode.removeAllChildren();
        globalCapturesTreeNode.removeAllChildren();
        dataprovidersNode.removeAllChildren();
        tablesNode.removeAllChildren();
        patternMatrix.clear();
        bigRegexPattern.clear();
        dictionary.clear();
        tables.clear();
        closeAllReaders();
        providers.clear();
        tablesValues.clear();
        visualStructure = new DefaultTreeModel(getCleansedModel());
        List<CaptureConfiguration> globalLinesCaptureConfigurations = new ArrayList<>();
        List<CaptureConfiguration> globalSentenciesCaptureConfigurations = new ArrayList<>();
        Map<String, List<DataProviderRelationship>> enrichment = new HashMap<>();
        if (document != null) {
            Element rootNode = document.getRootElement();
            if (rootNode.getName().equals("model") || rootNode.getName().equals("M")) {
                LogGui.info("Read dictionary...");
                List<Element> dictionaryList = rootNode.getChildren("dictionary");
                if (dictionaryList.isEmpty()) {
                    dictionaryList = rootNode.getChildren("D");
                }
                dictionaryList.stream().map((element) -> element.getChildren()).forEach((definitions) -> {
                    definitions.stream().filter((definition) -> (definition.getName().equals("definition") || definition.getName().equals("d"))).forEach((definition) -> {
                        String name = definition.getAttributeValue("name");
                        if (name == null) {
                            name = definition.getAttributeValue("n");
                        }
                        if (name != null) {
                            if (!dictionary.containsKey(name)) {
                                try {
                                    dictionaryNode.addDefinition(name, definition.getValue());
                                    Pattern pattern = Pattern.compile(definition.getValue());
                                    dictionary.put(name, pattern);
                                } catch (Exception e) {
                                    LogGui.info("Invalid dictionary pattern on " + name + " " + definition.getValue());
                                }
                            } else {
                                LogGui.info("Warning: a definition with name '" + name + "' already exists!");
                            }
                        }
                    });
                });

                LogGui.info("Read dataproviders...");
                Element dataProviders = rootNode.getChild("DPS");
                if (dataProviders != null) {
                    List<Element> providers = dataProviders.getChildren("dp");
                    providers.stream().map((provider) -> {
                        String name = provider.getAttributeValue("n");
                        String type = provider.getAttributeValue("t");
                        Map<String, String> fields = new HashMap<>();
                        Map<String, String> fieldsPositions = new HashMap<>();
                        Map<String, String> fieldsTable = new HashMap<>();
                        Map<String, String> cfgs = new HashMap<>();
                        Element fieldsElement = provider.getChild("fs");
                        if (fieldsElement != null) {
                            List<Element> fieldsElements = fieldsElement.getChildren("f");
                            fieldsElements.stream().forEach((field) -> {
                                String fieldName = field.getAttributeValue("n");
                                String fieldType = field.getAttributeValue("t");
                                String fieldTable = field.getAttributeValue("tbl");
                                String pos = field.getAttributeValue("p");
                                if (fieldName != null) {
                                    if (fieldType != null) {
                                        fields.put(fieldName, fieldType);
                                    }
                                    if (pos != null) {
                                        fieldsPositions.put(fieldName, pos);
                                    }
                                    if (fieldTable != null) {
                                        fieldsTable.put(fieldName, fieldTable);
                                    }
                                }
                            });
                        }
                        Element configurationFields = provider.getChild("c");
                        if (configurationFields != null) {
                            List<Element> configurations = configurationFields.getChildren("cp");
                            for (Element cp : configurations) {
                                String cfgParam = cp.getAttributeValue("n");
                                String cfgValue = cp.getAttributeValue("v");
                                if (cfgParam != null && cfgValue != null) {
                                    cfgs.put(cfgParam, cfgValue);
                                }
                            }
                        }
                        DataProviderConfiguration dpc = new DataProviderConfiguration(name, type, fields, fieldsPositions, fieldsTable, cfgs, storageFolder);
                        dpc.openIndex();
                        this.providers.put(name, dpc);
                        DataProviderTreeNode dptn = new DataProviderTreeNode(name, dpc);
                        List<Element> dprs = provider.getChildren("dpr");
                        if (dprs != null) {
                            for (Element dpr : dprs) {
                                List<Element> fs = dpr.getChildren("f");
                                String dprName = dpr.getAttributeValue("n");
                                String sName = dpr.getAttributeValue("s");
                                String priority = dpr.getAttributeValue("p");
                                DataProviderTreeRelationshipNode dptrn = new DataProviderTreeRelationshipNode(dprName, dptn);
                                dptrn.setPriority("true".equalsIgnoreCase(priority));
                                dptrn.setSegmentName(sName);
                                DataProviderRelationship dRel = new DataProviderRelationship(sName, dpc, "true".equalsIgnoreCase(priority));
                                for (Element f : fs) {
                                    String field = f.getAttributeValue("n");
                                    String capture = f.getAttributeValue("c");
                                    boolean key = "true".equals(f.getAttributeValue("k"));
                                    boolean toInsert = "true".equals(f.getAttributeValue("i"));
                                    dptrn.setMapping(field, capture, key, toInsert);
                                    dRel.setMapping(field, capture, key, toInsert);
                                    if (toInsert) {
                                        if (capture == null || capture.length() == 0) {
                                            CaptureConfiguration cc = new CaptureConfiguration(field, dpc.getFields().get(field), "", false, false, false, false);
                                            cc.addEnabledSegment(sName);
                                            globalLinesCaptureConfigurations.add(cc); //E' una cattura virtuale che non fa nulla ma appare nei report 
                                        }
                                    }
                                }
                                dptn.add(dptrn);
                                List<DataProviderRelationship> lRel = enrichment.get(sName);
                                if (lRel == null) {
                                    lRel = new ArrayList<>();
                                }
                                lRel.add(dRel);
                                enrichment.put(sName, lRel);
                            }
                        }
                        return dptn;
                    }).forEach((dptn) -> {
                        dataprovidersNode.add(dptn);
                    });
                }

                LogGui.info("Read tables");
                List<Element> tableNode = rootNode.getChildren("tables");
                if (tableNode.isEmpty()) {
                    tableNode = rootNode.getChildren("T");
                }
                for (Element element : tableNode) {
                    List<Element> tableList = element.getChildren();
                    for (Element table : tableList) {
                        String name = table.getAttributeValue("name");
                        if (name == null) {
                            name = table.getAttributeValue("n");
                        }
                        if (name != null) {
                            name = name.toLowerCase();
                            if (!tables.containsKey(name)) {
                                String dp = table.getAttributeValue("dp");
                                String dpn = table.getAttributeValue("dpn");
                                TableTreeNode tableModelTreeNode = new TableTreeNode(name, "true".equalsIgnoreCase(dp), dpn);
                                List<Element> records = table.getChildren("record");
                                if (records.isEmpty()) {
                                    records = table.getChildren("r");
                                }

                                StringBuilder tablePattern = new StringBuilder();
                                List<String> sortedRecord = new ArrayList<>();
                                if (dpn != null) {
                                    LogGui.info("Init table: " + name + " from dataprovider: " + dpn);
                                    DataProviderConfiguration dpc = providers.get(dpn);
                                    sortedRecord.addAll(dpc.getValuesForTable(name));
                                } else {
                                    LogGui.info("Init table: " + name + "(" + records.size() + ") records...");
                                    records.stream().map((record) -> record.getValue()).filter((value) -> (value != null)).map((value) -> value.trim().toLowerCase()).forEach((value) -> {
                                        sortedRecord.add(value);
                                    });
                                }
                                LogGui.info("Table has been readed... Resort value by length");
                                Collections.sort(sortedRecord, (String s1, String s2) -> {
                                    int len1 = s1.length();
                                    int len2 = s2.length();
                                    if (len1 != len2) {
                                        return s2.length() - s1.length();
                                    }
                                    return s2.compareTo(s1);
                                });
                                LogGui.info("Create the big regex...");
                                Set<String> tValues = new HashSet<>();
                                sortedRecord.stream().forEach((value) -> {
                                    tValues.add(value.intern());
                                    if (value.contains(" ")) {
                                        value = value.replace(" ", "(\\s*)");
                                    }
                                    tableModelTreeNode.addRecord(value);
                                    if (tablePattern.length() == 0) {
                                        //Modifica per matchare anche in preseza di spazi
                                        tablePattern.append(value);
                                    } else {
                                        tablePattern.append("|").append(value);
                                    }
                                });
                                LogGui.info("Compile the regex...");
                                try {
                                    Pattern pattern = Pattern.compile(tablePattern.toString());
                                    tables.put(name, pattern);
                                    tablesValues.put(name, tValues);
                                } catch (Exception e) {
                                    LogGui.info("Invalid table! " + name);
                                }
                                tablesNode.add(tableModelTreeNode);
                                LogGui.info("Finish to init table..");
                            } else {
                                LogGui.info("Warning: a table with name '" + name + "' already exists!");
                            }
                        }
                    }
                }
                LogGui.info("Read global captures");
                Element globalCaptures = rootNode.getChild("globalCaptures");
                if (globalCaptures == null) {
                    globalCaptures = rootNode.getChild("GC");
                }
                if (globalCaptures != null) {
                    List<Element> children = globalCaptures.getChildren("c");
                    processGlobalCapture(globalCapturesTreeNode, children, globalSentenciesCaptureConfigurations, globalLinesCaptureConfigurations);
                    List<Element> groups = globalCaptures.getChildren("cg"); //Catture nel gruppo
                    for (Element group : groups) {
                        List<Element> gchildren = group.getChildren("c");
                        CapturesGroupTreeNode groupNode = new CapturesGroupTreeNode(group.getAttributeValue("n"));
                        globalCapturesTreeNode.add(groupNode);
                        processGlobalCapture(groupNode, gchildren, globalSentenciesCaptureConfigurations, globalLinesCaptureConfigurations);
                    }

                }

                LogGui.info("Read segments...");
                List<Element> segmentList = rootNode.getChildren("segments");
                if (segmentList.isEmpty()) {
                    segmentList = rootNode.getChildren("S");
                }
                segmentList.stream().map((element) -> element.getChildren()).forEach((List<Element> segments) -> {
                    segments.stream().map((segment) -> getSegmentBean(segment, dictionary, tables, null, globalLinesCaptureConfigurations, globalSentenciesCaptureConfigurations, enrichment)).forEach((sb) -> {
                        patternMatrix.add(sb);
                    });
                });
            }
        }
    }

    private void processGlobalCapture(ModelTreeNode captureContainer, List<Element> children, List<CaptureConfiguration> globalSentenciesCaptureConfigurations, List<CaptureConfiguration> globalLinesCaptureConfigurations) {
        children.stream().forEach((child) -> {
            if (null != child.getName()) {
                String captureName = child.getAttributeValue("name");
                if (captureName == null) {
                    captureName = child.getAttributeValue("n");
                }
                if (captureName != null) {
                    CaptureTreeNode capture = new CaptureTreeNode(captureName);
                    captureContainer.add(capture);
                    if ("sentence".equals(child.getAttributeValue("scope")) || "s".equals(child.getAttributeValue("s"))) {
                        capture.setScope("sentence");
                        globalSentenciesCaptureConfigurations.add(getCaptureConfiguration(child, captureName, dictionary, tables, capture));
                    } else {
                        globalLinesCaptureConfigurations.add(getCaptureConfiguration(child, captureName, dictionary, tables, capture));
                    }
                }
            }
        });
    }

    private SegmentConfiguration getSegmentBean(Element segment, Map<String, Pattern> dictionary, Map<String, Pattern> tables, SegmentTreeNode parentNode, List<CaptureConfiguration> globalLinesCaptureConfigurations, List<CaptureConfiguration> globalSentenciesCaptureConfigurations, Map<String, List<DataProviderRelationship>> enrichment) {
        String name = segment.getAttributeValue("name");
        if (name == null) {
            name = segment.getAttributeValue("n");
        }
        String multiple = segment.getAttributeValue("multiple");
        if (multiple == null) {
            multiple = segment.getAttributeValue("m");
        }
        String classify = segment.getAttributeValue("classify");
        if (classify == null) {
            classify = segment.getAttributeValue("cl");
        }
        String defaultSection = segment.getAttributeValue("default");
        if (defaultSection == null) {
            defaultSection = segment.getAttributeValue("d");
        }
        if (multiple == null) {
            multiple = "no";
        }
        if (defaultSection == null) {
            defaultSection = "no";
        }
        if (classify == null) {
            classify = "no";
        }
        SegmentConfiguration sb = new SegmentConfiguration(name, "yes".equals(multiple), "yes".equals(defaultSection), "yes".equals(classify));
        SegmentTreeNode segmentModelTreeNode = new SegmentTreeNode(name);
        segmentModelTreeNode.setSegmentConfiguration(sb);
        ModelTreeNode capturesTreeNode = new ModelTreeNode("Catture", ModelTreeNode.TYPE_CAPTURE);
        segmentModelTreeNode.add(capturesTreeNode);
        ModelTreeNode formulasTreeNode = new ModelTreeNode("Formule", ModelTreeNode.TYPE_FORMULA);
        segmentModelTreeNode.add(formulasTreeNode);

        if (parentNode == null) {
            segmentsNode.add(segmentModelTreeNode);
        } else {
            parentNode.add(segmentModelTreeNode);
        }
        globalLinesCaptureConfigurations.stream().filter((cc) -> (cc.isSegmentEnabled(sb.getName()))).forEach((cc) -> {
            sb.addCapture(cc);
        });

        globalSentenciesCaptureConfigurations.stream().filter((cc) -> (cc.isSegmentEnabled(sb.getName()))).forEach((cc) -> {
            sb.addSentenceCapture(cc);
        });

        sb.addRelationships(enrichment.get(sb.getName()));

        List<Element> children = segment.getChildren();
        children.stream().forEach((child) -> {
            if (null != child.getName()) {
                switch (child.getName()) {
                    case "pattern":
                    case "p":
                        String value = child.getValue();
                        if (value != null) {
                            segmentModelTreeNode.addPattern(value);
                            Pattern pattern = getPattern(dictionary, tables, value);
                            if (pattern != null) {
                                sb.addPattern(pattern);
                            } else {
                                LogGui.info("Warning: invalid pattern into element: " + segment.getName() + ": " + value);
                            }
                        }
                        break;
                    case "s":
                    case "segment":
                        sb.addSegment(getSegmentBean(child, dictionary, tables, segmentModelTreeNode, globalLinesCaptureConfigurations, globalSentenciesCaptureConfigurations, enrichment));
                        break;
                    case "c":
                    case "capture":
                        processCapture(sb, capturesTreeNode, child);
                        break;
                    case "cg":
                        List<Element> gchildren = child.getChildren("c");
                        CapturesGroupTreeNode groupNode = new CapturesGroupTreeNode(child.getAttributeValue("n"));
                        capturesTreeNode.add(groupNode);
                        gchildren.stream().forEach((gChild) -> {
                            processCapture(sb, groupNode, gChild);
                        });
                        break;
                    case "f":
                        processFormulas(sb, formulasTreeNode, child);
                        break;
                    default:
                        break;
                }
            }
        });

        return sb;
    }

    private void processCapture(SegmentConfiguration sb, ModelTreeNode capturesTreeNode, Element child) {
        String captureName = child.getAttributeValue("name");
        if (captureName == null) {
            captureName = child.getAttributeValue("n");
        }
        if (captureName != null) {
            CaptureTreeNode capture = new CaptureTreeNode(captureName);
            capturesTreeNode.add(capture);
            if ("sentence".equals(child.getAttributeValue("scope")) || "s".equals(child.getAttributeValue("s"))) {
                capture.setScope("sentence");
                sb.addSentenceCapture(getCaptureConfiguration(child, captureName, dictionary, tables, capture));
            } else {
                sb.addCapture(getCaptureConfiguration(child, captureName, dictionary, tables, capture));
            }
        }
    }

    private void processFormulas(SegmentConfiguration sb, ModelTreeNode formulasTreeNode, Element child) {
        String formulaName = child.getAttributeValue("name");
        if (formulaName == null) {
            formulaName = child.getAttributeValue("n");
        }
        if (formulaName != null) {
            FormulaTreeNode formula = new FormulaTreeNode(formulaName);
            formulasTreeNode.add(formula);
            sb.addFormula(getFormulaConfiguration(child, formulaName, formula));
        }
    }

    /**
     * Ritorna il dizionario
     *
     * @return dizionario come mappa {nome definizione, pattern}
     */
    public Map<String, Pattern> getDictionary() {
        return dictionary;
    }

    /**
     * Ritorna la lista delle tabelle
     *
     * @return lista delle tabelle come mappa {nome della tabella, pattern
     * ottenuto dalla concatenazione di tutti i valori della tabella}
     */
    public Map<String, Pattern> getTables() {
        return tables;
    }

    /**
     * Ritorna un pattern a partire da una regex scritta in formato testo
     * andando a sostituire tutte le notazioni # con i relativi pattern di
     * dizionario o di tabella. Il segmentEngine deve essere inizializzato
     * (ovviamente)
     *
     * Una definizione può essere riferita con la sintassi #nome_definizione .
     *
     * Una tabella può essere riferita con la sintassi #nome_tabella .
     *
     * @param value regex da cui costruire il pattern
     * @return pattern con tutti i riferimenti risolti
     */
    public Pattern getPattern(String value) {
        return getPattern(dictionary, tables, value);
    }

    private Pattern getPattern(Map<String, Pattern> dictionary, Map<String, Pattern> tables, String value) {
        Pattern pattern;
        if (value.contains("#")) {
            if (value.startsWith("#") && (value.indexOf("#", 1) == -1)) {
                String definitionName = value.substring(1);
                pattern = dictionary.get(definitionName);
            } else {
                pattern = bigRegexPattern.get(value);
                if (pattern == null) {
                    int pos = value.indexOf("#");
                    String bigRegex = value;
                    while (pos != -1) {
                        int blank = value.indexOf(" ", pos + 1);
                        if (blank == -1) {
                            blank = value.indexOf("(", pos + 1);
                        }
                        if (blank == -1) {
                            String term = value.substring(pos + 1);
                            Pattern p = dictionary.get(term);
                            if (p == null) {
                                p = tables.get(term);
                            }
                            if (p != null) {
                                bigRegex = bigRegex.replace("#" + term, p.toString());
                            }
                            pos = -1;
                        } else {
                            String term = value.substring(pos + 1, blank);
                            Pattern p = dictionary.get(term.trim());
                            if (p == null) {
                                p = tables.get(term);
                            }
                            if (p != null) {
                                bigRegex = bigRegex.replace("#" + term + " ", p.toString());
                            }
                            pos = value.indexOf("#", blank + 1);
                        }
                    }
                    pattern = Pattern.compile(bigRegex);
                    bigRegexPattern.put(value, pattern);
                }
            }
        } else {
            try {
                pattern = Pattern.compile(value);
            } catch (Exception e) {
                LogGui.info("Error: invalid pattern: " + value);
                return null;
            }
        }
        return pattern;
    }

    private CaptureConfiguration getCaptureConfiguration(Element child, String captureName, Map<String, Pattern> dictionary, Map<String, Pattern> tables, CaptureTreeNode captureTreeNode) {
        String captureType = child.getAttributeValue("type");
        if (captureType == null) {
            captureType = child.getAttributeValue("t");
        }
        if (captureType == null) {
            captureType = "text";
        }
        String captureFormat = child.getAttributeValue("format");
        if (captureFormat == null) {
            captureFormat = child.getAttributeValue("f");
        }
        if (captureFormat == null) {
            captureFormat = "";
        }
        String temp = child.getAttributeValue("tmp");
        if (temp == null) {
            temp = child.getAttributeValue("t");
        }
        if (temp == null) {
            temp = "false";
        }

        String start = child.getAttributeValue("start");
        if (start == null) {
            start = child.getAttributeValue("s");
        }
        if (start == null) {
            start = "false";
        }

        String end = child.getAttributeValue("end");
        if (end == null) {
            end = child.getAttributeValue("e");
        }
        if (end == null) {
            end = "false";
        }

        String sub = child.getAttributeValue("sub");
        if (sub == null) {
            sub = "false";
        }

        CaptureConfiguration cc = new CaptureConfiguration(captureName, captureType, captureFormat, "true".equals(temp), "true".equals(start), "true".equals(end), "true".equals(sub));
        captureTreeNode.setConfiguration(cc);
        List<Element> patterns = child.getChildren();
        patterns.stream().forEach((captureChildren) -> {
            if ("pattern".equalsIgnoreCase(captureChildren.getName()) || "p".equalsIgnoreCase(captureChildren.getName())) {
                String sPosition = captureChildren.getAttributeValue("position");
                if (sPosition == null) {
                    sPosition = captureChildren.getAttributeValue("p");
                }
                if (sPosition == null) {
                    sPosition = "0";
                }
                int position = 0;
                try {
                    position = Integer.parseInt(sPosition);
                } catch (NumberFormatException ignored) {
                }
                String fixValue = captureChildren.getAttributeValue("fixValue");
                if (fixValue == null) {
                    fixValue = captureChildren.getAttributeValue("f");
                }
                if (fixValue == null) {
                    fixValue = "";
                }
                String value = captureChildren.getValue();
                if (value != null) {
                    Pattern pattern = getPattern(dictionary, tables, value);
                    if (pattern != null) {
                        cc.addCapturePattern(new CapturePattern(position, pattern, fixValue));
                        captureTreeNode.addPattern(position, value, fixValue);
                    } else {
                        LogGui.info("Invalid pattern: " + value);
                    }
                }
            } else if ("match".equalsIgnoreCase(captureChildren.getName()) || "m".equalsIgnoreCase(captureChildren.getName())) {
                int position = 0;
                String table = captureChildren.getAttributeValue("table");
                if (table == null) {
                    table = captureChildren.getAttributeValue("t");
                }
                if (table != null) {
                    Pattern pattern = tables.get(table);
                    if (pattern != null) {
                        cc.addCapturePattern(new CapturePattern(position, pattern, null));
                    } else {
                        LogGui.info("Invalid table: " + table);
                    }
                }
            } else if ("capture".equalsIgnoreCase(captureChildren.getName()) || "c".equalsIgnoreCase(captureChildren.getName())) {
                String subCaptureName = captureChildren.getAttributeValue("name");
                if (subCaptureName == null) {
                    subCaptureName = captureChildren.getAttributeValue("n");
                }
                CaptureTreeNode subCapt = new CaptureTreeNode(subCaptureName);
                subCapt.setScope("parent");
                captureTreeNode.add(subCapt);
                cc.addSubCapture(getCaptureConfiguration(captureChildren, subCaptureName, dictionary, tables, subCapt));
            } else if ("segment".equalsIgnoreCase(captureChildren.getName()) || "s".equalsIgnoreCase(captureChildren.getName())) {
                String segName = captureChildren.getValue();
                captureTreeNode.addEnabledSegment(segName);
                cc.addEnabledSegment(segName);
            } else if ("bl".equalsIgnoreCase(captureChildren.getName())) {
                String blocked = captureChildren.getValue();
                captureTreeNode.addBlockedCapture(blocked);
                cc.addBlockedCapture(blocked);
            } else if ("classification".equalsIgnoreCase(captureChildren.getName()) || "cl".equalsIgnoreCase(captureChildren.getName())) {
                String path = captureChildren.getValue();
                ClassificationPath cp = GuiUtils.getClassificationPath(path);
                captureTreeNode.setClassificationPath(cp);
                cc.setClassificationPath(cp);
            }
        });
        return cc;
    }

    private FormulaConfiguration getFormulaConfiguration(Element child, String formulaName, FormulaTreeNode formula) {
        formula.setActBeforeEnrichment("true".equalsIgnoreCase(child.getAttributeValue("b")));
        formula.setFormatPattern(child.getAttributeValue("f"));
        FormulaConfiguration cc = new FormulaConfiguration(formulaName, formula.getFormatPattern(), formula.isActBeforeEnrichment());
        List<Element> captures = child.getChildren();
        captures.stream().forEach((captureChildren) -> {
            if ("c".equalsIgnoreCase(captureChildren.getName())) {
                formula.addCapture(captureChildren.getValue());
                cc.addCapture(captureChildren.getValue());
            }
        });
        return cc;
    }

    private void addSentenceToResult(String previousLine, SegmentConfiguration currentSegment, Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments) {
        List<SegmentationResults> sent = identifiedSegments.get(currentSegment);
        if (sent == null) {
            sent = new ArrayList<>();
            sent.add(new SegmentationResults());
        }
        sent.get(sent.size() - 1).addSentence(previousLine);
        identifiedSegments.put(currentSegment, sent);
    }

    private void addLineToResult(String line, SegmentConfiguration currentSegment, Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments) {
        List<SegmentationResults> sent = identifiedSegments.get(currentSegment);
        if (sent == null) {
            sent = new ArrayList<>();
            sent.add(new SegmentationResults());
        }
        sent.get(sent.size() - 1).addLine(line);

        identifiedSegments.put(currentSegment, sent);
    }

    private Map<SegmentConfiguration, List<SegmentationResults>> getSegments(List<SegmentConfiguration> patternMatrix, List<String> lines, MulticlassEngine me, double threshold, String language) {
        String line;
        String previousLine = "";
        SegmentConfiguration currentSegment = null;
        for (SegmentConfiguration segmentConfiguration : patternMatrix) {
            if (segmentConfiguration.isDefault()) {
                currentSegment = segmentConfiguration;
            }
        }
        if (currentSegment == null) {
            currentSegment = new SegmentConfiguration("Not identified", false, false);
        }
        Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments = new LinkedHashMap<>();
        for (String currLine : lines) {
            line = currLine;
            line = line.replaceAll(TAB, "").replaceAll("(\\s+)", " ").trim().toLowerCase();
            if (line.length() != 0) {
                boolean match = false;
                for (SegmentConfiguration section : patternMatrix) {
                    List<Pattern> patterns = section.getPatternList();
                    for (Pattern pattern : patterns) {
                        if (pattern.matcher(line).find()) { //ha matchato la sezione
                            //A questo punto la riga va da sola e la previousline va nella sezione precedente
                            if (previousLine.length() > 0) {
                                addSentenceToResult(previousLine, currentSegment, identifiedSegments);
                                previousLine = "";
                            }
                            if (!section.getName().equals(currentSegment.getName())) {
                                currentSegment = section; //Ho una nuova sezione
                            }
                            if (identifiedSegments.containsKey(currentSegment) && currentSegment.isMultiple()) { //Ho un cambio di sezione ma � multipla
                                identifiedSegments.get(currentSegment).add(new SegmentationResults()); // Aggiungo un nuovo segmentresult ala lsita dei risultati
                            }
                            match = true;
                            break;
                        }
                    }
                    if (match) {
                        break;
                    }
                }
                if (previousLine.length() != 0) {
                    previousLine = previousLine + " " + line;
                } else {
                    previousLine = line;
                }
            } else if (previousLine.length() > 0) {
                addSentenceToResult(previousLine, currentSegment, identifiedSegments);
                previousLine = line;
            }
            addLineToResult(line, currentSegment, identifiedSegments);
        }
        if (previousLine.length() > 0) {
            addSentenceToResult(previousLine, currentSegment, identifiedSegments);
        }
        //Fa le sottosezioni e le catture
        patternMatrix.stream().forEach((segmentBean) -> {
            processSegment(segmentBean, identifiedSegments, me, threshold, language);
        });
        if (me != null) {
            //Fa le pulizie di primavera ovvero toglie tutti i segmenti con testo tokenizzato non significativo
            Set<SegmentConfiguration> toRemove = new HashSet<>();
            for (SegmentConfiguration segConf : identifiedSegments.keySet()) {
                List<SegmentationResults> resList = identifiedSegments.get(segConf);
                for (int i = resList.size() - 1; i >= 0; i--) {
                    SegmentationResults sr = resList.get(i);
                    String text = sr.getText();
                    text = me.tokenize(text, language);
                    if (text.isEmpty()) {
                        resList.remove(i);
                    }
                }
                if (resList.isEmpty()) {
                    toRemove.add(segConf);
                }
            }
            toRemove.stream().forEach((sc) -> {
                identifiedSegments.remove(sc);
            });
        }
        return identifiedSegments;
    }

    private void addCaptureToIndex(CaptureConfiguration c, Map<String, CaptureConfiguration> capturesIndex) {
        capturesIndex.put(c.getName(), c);
        List<CaptureConfiguration> subCaptures = c.getSubCaptures();
        subCaptures.stream().forEach((sc) -> {
            addCaptureToIndex(sc, capturesIndex);
        });
    }

    private void processSegment(SegmentConfiguration segmentBean, Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments, MulticlassEngine me, double threshold, String language) {
        List<CaptureConfiguration> captureConfigurations = segmentBean.getCaptureConfigurations();
        List<CaptureConfiguration> sCaptureConfigurations = segmentBean.getSentenceCaptureConfigurations();
        List<FormulaConfiguration> formulasAfter = segmentBean.getFormulasAfterEnrich();
        List<FormulaConfiguration> formulasBefore = segmentBean.getFormulasBeforeEnrich();
        Map<String, CaptureConfiguration> capturesIndex = new HashMap<>();
        captureConfigurations.stream().forEach((c) -> {
            addCaptureToIndex(c, capturesIndex);
        });
        sCaptureConfigurations.stream().forEach((c) -> {
            addCaptureToIndex(c, capturesIndex);
        });
        List<SegmentConfiguration> segmentConfigurations = segmentBean.getSegments();
        List<SegmentationResults> segmentResults = identifiedSegments.get(segmentBean);
        List<DataProviderRelationship> relationships = segmentBean.getRelationships();
        if (!(captureConfigurations.isEmpty() && segmentConfigurations.isEmpty())) {
            if (segmentResults == null) {
                return;
            }
            segmentResults.stream().map((sr) -> {
                if (!segmentConfigurations.isEmpty()) {
                    Map<SegmentConfiguration, List<SegmentationResults>> subSegments = getSegments(segmentConfigurations, sr.getLines(), me, threshold, language);
                    sr.setSubsentencies(subSegments);
                }
                return sr;
            }).map((sr) -> {
                if (!captureConfigurations.isEmpty()) {
                    List<String> srLines = sr.getLines();
                    extractCaptures(captureConfigurations, sr, srLines);
                }
                return sr;
            }).filter((sr) -> (!sCaptureConfigurations.isEmpty())).forEach((sr) -> {
                List<String> srLines = sr.getSentencies();
                extractCaptures(sCaptureConfigurations, sr, srLines);
            });
            //Formule pre arricchimento
            if (!formulasBefore.isEmpty()) {
                segmentResults.stream().forEach((sr) -> {
                    formulasBefore.stream().forEach((formula) -> {
                        formula.applyFormula(sr, capturesIndex);
                    });
                });

            }
            if (!relationships.isEmpty()) {
                segmentResults.stream().forEach((sr) -> {
                    relationships.stream().forEach((dpr) -> {
                        dpr.enrich(sr, capturesIndex);
                    });
                });
            }

            //Formule post arricchimento
            if (!formulasAfter.isEmpty()) {
                segmentResults.stream().forEach((sr) -> {
                    formulasAfter.stream().forEach((formula) -> {
                        formula.applyFormula(sr, capturesIndex);
                    });
                });
            }
        }
        if (segmentBean.isClassify()) {
            if (segmentResults == null) {
                return;
            }
            if (me != null) {
                if (me.isIsInit()) {
                    segmentResults.stream().forEach((sr) -> {
                        classify(sr, me, threshold, language);
                    });
                }
            }
        }
    }

    private void extractCaptures(List<CaptureConfiguration> captureConfigurations, SegmentationResults sr, List<String> srLines) {
        Set<String> toRemove = new HashSet<>();
        for (CaptureConfiguration captureConfiguration : captureConfigurations) {
            if (captureConfiguration.isNotSubscribe()) {
                if (sr.getCaptureResults().get(captureConfiguration.getName()) != null)
                    continue;
            }
            List<CapturePattern> patterns = captureConfiguration.getPatterns();
            String capturedValue = null;
            for (CapturePattern pattern : patterns) {
                boolean continueWithNext = true;
                for (String text : srLines) {
                    if (text.trim().length() == 0) {
                        continue;
                    }
                    Pattern p = pattern.getPattern();
                    String fv = pattern.getFixValue();
                    if (fv == null) {
                        fv = "";
                    }
                    Matcher match = p.matcher(text);
                    if (match.find()) {
                        try {
                            boolean isTableNormalized = fv.startsWith("#");
                            String value = (fv.isEmpty() || isTableNormalized) ? match.group(pattern.getPosition()) : fv;
                            if (isTableNormalized) {
                                value = searchSimilar(value, fv.substring(1));
                            }
                            sr.addCaptureResult(captureConfiguration, value);
                            ClassificationPath cp = captureConfiguration.getClassificationPath();
                            if (cp != null) {
                                List<ClassificationPath> cps = new ArrayList<>();
                                cps.add(cp);
                                sr.addClassificationPath(cps);
                                sr.setClassifyByCapture(true);
                            }
                            continueWithNext = false;
                            capturedValue = value;
                            toRemove.addAll(captureConfiguration.getBlockedCaptures());
                            break;
                        } catch (Exception e) {
                            LogGui.info("Exception during capture: " + captureConfiguration.getName() + " " + captureConfiguration.getType());
                            LogGui.printException(e);
                        }
                    }
                }
                if (!continueWithNext) {
                    break;
                }

            }
            if (capturedValue != null) {
                if (captureConfiguration.getSubCaptures().size() > 0) {
                    List<String> list = new ArrayList<>();
                    list.add(sr.getCaptureConfigurationResults().get(captureConfiguration));
                    if (list.size() > 0) {
                        extractCaptures(captureConfiguration.getSubCaptures(), sr, list);
                    }
                    if (captureConfiguration.isTemporary()) {
                        sr.removeCaptureConfigurationResults(captureConfiguration);
                    }
                }
            }
        }
        for (String block : toRemove) {
            sr.removeCaptureConfigurationResults(block);
        }
    }

    private void classify(SegmentationResults sr, MulticlassEngine me, double threshold, String language) {
        if (me != null) {
            if (!sr.isClassifyByCapture()) {
                List<String> lines = sr.getLines();
                StringBuilder text = new StringBuilder();
                lines.stream().forEach((line) -> {
                    text.append(line).append(CR);
                });
                List<ClassificationPath> path = me.bayesClassify(text.toString(), threshold, language);
                sr.addClassificationPath(path);
            }
        }
    }

    /**
     * Ritorna la matrice inizializzata
     *
     * @return configurazione
     */
    public Iterable<SegmentConfiguration> getPatternMatrix() {
        return patternMatrix;
    }

    /**
     * Chiude tutti i readers dei data providers
     */
    public void closeAllReaders() {
        providers.values().stream().forEach((dpc) -> {
            dpc.closeIndex();
        });
    }

    private String searchSimilar(String value, String tableName) {
        final AtomicInteger distance = new AtomicInteger(Integer.MAX_VALUE);
        final FinalString ret = new FinalString(value);
        Set<String> tableValues = tablesValues.get(tableName);
        if (tableValues == null) {
            return value;
        }
        tableValues.stream().forEach((tvalue) -> {
            final int d2 = Tokenizer.distance(value, tvalue);
            if (d2 < distance.get()) {
                distance.set(d2);
                ret.set(tvalue);
            }
        });

        return ret.toString();
    }

    class FinalString {

        private String s;

        public FinalString(String s) {
            this.s = s;
        }

        public void set(String s) {
            this.s = s;
        }

        public String toString() {
            return s;
        }
    }

}
