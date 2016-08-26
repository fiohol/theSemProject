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

import org.thesemproject.opensem.gui.utils.GuiUtils;
import org.thesemproject.opensem.gui.LogGui;
import org.thesemproject.opensem.gui.SemGui;
import org.thesemproject.opensem.gui.utils.CapturesUtils;
import org.thesemproject.opensem.gui.utils.DataProvidersUtils;
import org.thesemproject.opensem.gui.utils.DictionaryUtils;
import org.thesemproject.opensem.gui.utils.SegmentsUtils;
import org.thesemproject.opensem.gui.utils.TablesUtils;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.jdom2.Document;
import org.jdom2.Element;

/**
 * Gestisce l'editor del modello di segmentazione
 */
public class ModelEditor {

    private final JTabbedPane modelElements;
    private final JSplitPane dictionarySplit;
    private final JSplitPane segmentsSplit;
    private final JSplitPane captureSplit;
    private final JSplitPane dataproviderSplit;
    private final JSplitPane dataproviderRelatioshipSplit;
    private final JPanel forumlaSplitPanel;
    private final JPanel tablePanel;
    private final JPanel helpPanel;
    private final SemGui sem;

    private ModelTreeNode currentNode;
    private TreePath currentPath;

    /**
     * Istanzia l'editor
     *
     * @param sem ambiente grafico
     * @param modelElements pannello dove ci sono i pannelli che rappresentano
     * l'input per i vari tipi di nodi
     */
    public ModelEditor(SemGui sem, JTabbedPane modelElements) {
        this.sem = sem;
        this.modelElements = modelElements;
        dictionarySplit = (JSplitPane) this.modelElements.getComponentAt(0);
        segmentsSplit = (JSplitPane) this.modelElements.getComponentAt(1);
        captureSplit = (JSplitPane) this.modelElements.getComponentAt(3);
        tablePanel = (JPanel) this.modelElements.getComponentAt(2);
        helpPanel = (JPanel) this.modelElements.getComponentAt(4);
        dataproviderSplit = (JSplitPane) this.modelElements.getComponentAt(5);
        dataproviderRelatioshipSplit = (JSplitPane) this.modelElements.getComponentAt(6);
        forumlaSplitPanel = (JPanel) this.modelElements.getComponentAt(7);
        modelElements.removeAll();
        modelElements.addTab("Model Editor", helpPanel);
    }

    private int processNode(ModelTreeNode node) {
        TreeNode[] tns = node.getPath();
        int lenPath = tns.length;
        setCorrectPanel(node);
        return lenPath;
    }

    /**
     * Verifica se un nodo è il dizionario
     *
     * @param node nodo
     * @return true se è dizionario
     */
    public boolean isDictionary(ModelTreeNode node) {
        return node.getClass().equals(DictionaryTreeNode.class);
    }

    /**
     * Verifica se un nodo è la definizione del segmento
     *
     * @param node nodo
     * @return true se è una definizione di segmento (o sottosegmento)
     */
    public boolean isSegmentChild(ModelTreeNode node) {
        return node.getClass().equals(SegmentTreeNode.class);
    }

    /**
     * Verifica se un nodo è un contenitore di tabelle
     *
     * @param node nodo
     * @return true se è un contenitore di tabelle
     */
    public boolean isTable(ModelTreeNode node) {
        return (node.getNodeType() == ModelTreeNode.TYPE_TABLE);
    }

    /**
     * Verifica se un nodo è il nodo dataprovider
     *
     * @param node nodo
     * @return true se è un dataprovider
     */
    public boolean isDataProviders(ModelTreeNode node) {
        return (node.getNodeType() == ModelTreeNode.TYPE_DATA_PROVIDERS);
    }

    /**
     * Verifica se un nodo è un dataprovider
     *
     * @param node nodo
     * @return true se è un dataprovider
     */
    public boolean isDataProvidersChild(ModelTreeNode node) {
        return node.getClass().equals(DataProviderTreeNode.class);
    }

    /**
     * Verifica se un nodo è il nodo Formule
     *
     * @since 1.3
     *
     * @param node nodo
     * @return true se è forumla
     */
    public boolean isForumlas(ModelTreeNode node) {
        return (node.getNodeType() == ModelTreeNode.TYPE_FORMULA);
    }

    /**
     * Verifica se un nodo è una forumla
     *
     * @since 1.3
     *
     * @param node nodo
     * @return true se è un dataprovider
     */
    public boolean isForumlaDefinition(ModelTreeNode node) {
        return node.getClass().equals(FormulaTreeNode.class);
    }

    /**
     * Verifica se un nodo è una relazione del dataprovider
     *
     * @param node nodo
     * @return true se è una relazione del dataprovider
     */
    public boolean isDataProvidersRelationship(ModelTreeNode node) {
        return node.getClass().equals(DataProviderTreeRelationshipNode.class);
    }

    /**
     * Verifica se il nodo è una tabella
     *
     * @param node nodo
     * @return true se è una tabella
     */
    public boolean isTableChild(ModelTreeNode node) {
        return node.getClass().equals(TableTreeNode.class);
    }

    /**
     * Verifica se un nodo è un contenitore di segmenti
     *
     * @param node nodo
     * @return true se è un contenitore di segmenti
     */
    public boolean isSegment(ModelTreeNode node) {
        return node.getNodeType() == ModelTreeNode.TYPE_SEGMENT;
    }

    /**
     * Verifica se un nodo è un contenitore di catture o gruppi di catture
     *
     * @param node nodo
     * @return true se è un contenitore di catture
     */
    public boolean isCapture(ModelTreeNode node) {
        return node.getNodeType() == ModelTreeNode.TYPE_CAPTURE;
    }

    /**
     * Verifica se un nodo è un gruppo di catture
     *
     * @since 1.2
     * @param node nodo
     * @return true se è un gruppo di catture
     */
    public boolean isCaptureGroup(ModelTreeNode node) {
        return node.getNodeType() == ModelTreeNode.TYPE_CAPTURE_GROUP;
    }

    /**
     * Verifica se un nodo è una cattura
     *
     * @param node nodo
     * @return true se è una cattura
     */
    public boolean isCaptureChild(ModelTreeNode node) {
        return node.getClass().equals(CaptureTreeNode.class);
    }

    private void setCorrectPanel(ModelTreeNode node) {
        modelElements.removeAll();
        if (isTableChild(node)) {
            setCurrentNode(node);
            TablesUtils.populateTablePanel((TableTreeNode) node, sem);
            modelElements.addTab(node.toString(), tablePanel);
        } else if (isSegmentChild(node)) {
            setCurrentNode(node);
            SegmentsUtils.populateSegmentSplit((SegmentTreeNode) node, sem);
            modelElements.addTab(node.toString(), segmentsSplit);
        } else if (isCaptureChild(node)) {
            setCurrentNode(node);
            CapturesUtils.populateCaptureSplit((CaptureTreeNode) node, sem);
            modelElements.addTab(node.toString(), captureSplit);
        } else if (isDataProvidersChild(node)) {
            setCurrentNode(node);
            DataProvidersUtils.populateDataProviderSplit((DataProviderTreeNode) node, sem);
            modelElements.addTab(node.toString(), dataproviderSplit);
        } else if (isDataProvidersRelationship(node)) {
            setCurrentNode(node);
            DataProvidersUtils.populateDataProviderRelationship((DataProviderTreeRelationshipNode) node, sem);
            modelElements.addTab(node.toString(), dataproviderRelatioshipSplit);
        } else if (isDictionary(node)) {
            setCurrentNode(node);
            DictionaryUtils.populateDictionarySplit((DictionaryTreeNode) node, sem);
            modelElements.addTab("Dizionario", dictionarySplit);

        } else if (isForumlaDefinition(node)) {
            setCurrentNode(node);
            CapturesUtils.populateForumlaSplit((FormulaTreeNode) node, sem);
            modelElements.addTab(node.toString(), forumlaSplitPanel);
        } else {
            setCurrentNode(null);
            modelElements.addTab("Model Editor", helpPanel);
        }
    }

    /**
     * Gestisce l'azione su un nodo segmento
     *
     * @param tree albero che rappresenta il modello
     * @param evt evento
     */
    public void segmentsActionPerformed(JTree tree, MouseEvent evt) {
        int selRow = tree.getRowForLocation(evt.getX(), evt.getY());
        TreePath selPath = tree.getPathForLocation(evt.getX(), evt.getY());
        tree.setSelectionPath(selPath);
        segmentsActionPerformed(tree, evt, selRow);
    }

    /**
     * Gestisce l'azione su un nodo segmento
     *
     * @param tree albero che rappresenta il modello
     * @param evt evento
     * @param selRow riga selezionata
     */
    public void segmentsActionPerformed(JTree tree, MouseEvent evt, int selRow) {

        if (selRow != -1) {
            try {
                final DefaultMutableTreeNode nx = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (nx instanceof ModelTreeNode) {
                    ModelTreeNode node = (ModelTreeNode) nx;
                    ActionListener menuListener = new MenuActionListner(tree, node);

                    int lenPath = processNode(node);
                    if (lenPath > 1) {

                        if (!isDictionary(node)) {
                            if (evt != null) {
                                if (javax.swing.SwingUtilities.isRightMouseButton(evt)) {
                                    JPopupMenu popup = new JPopupMenu();
                                    if (isTable(node)) {
                                        addMenuVoice(popup, "Aggiungi tabella", "/org/thesemproject/opensem/gui/icons16/table.png", menuListener);
                                    }
                                    if (isDataProviders(node)) {
                                        addMenuVoice(popup, "Aggiungi provider", "/org/thesemproject/opensem/gui/icons16/plug.png", menuListener);
                                    }
                                    if (isDataProvidersChild(node)) {
                                        addMenuVoice(popup, "Aggiungi relazione", "/org/thesemproject/opensem/gui/icons16/connect.png", menuListener);
                                    }
                                    if (isSegment(node) || isSegmentChild(node)) {
                                        addMenuVoice(popup, "Aggiungi segmento", "/org/thesemproject/opensem/gui/icons16/script.png", menuListener);

                                    }
                                    if (isCapture(node) || isCaptureChild(node) || isCaptureGroup(node)) {
                                        addMenuVoice(popup, "Aggiungi cattura", "/org/thesemproject/opensem/gui/icons16/doc_tag.png", menuListener);

                                        if (copyNode != null || cutNode != null) {
                                            addMenuVoice(popup, "Incolla cattura", "/org/thesemproject/opensem/gui/icons16/page_paste.png", menuListener);
                                        }
                                    }
                                    if (isCapture(node)) {
                                        addMenuVoice(popup, "Comprimi catture", "/org/thesemproject/opensem/gui/icons16/compress.png", menuListener);
                                        addMenuVoice(popup, "Aggiungi gruppo catture", "/org/thesemproject/opensem/gui/icons16/package.png", menuListener);
                                    }
                                    if (isCaptureGroup(node)) {
                                        addMenuVoice(popup, "Comprimi catture", "/org/thesemproject/opensem/gui/icons16/compress.png", menuListener);
                                        addMenuVoice(popup, "Rinomina gruppo catture", "/org/thesemproject/opensem/gui/icons16/application_edit.png", menuListener);
                                    }
                                    if (isCaptureChild(node)) {
                                        addMenuVoice(popup, "Copia cattura", "/org/thesemproject/opensem/gui/icons16/page_copy.png", menuListener);
                                        addMenuVoice(popup, "Taglia cattura", "/org/thesemproject/opensem/gui/icons16/cut.png", menuListener);
                                        addMenuVoice(popup, "Rinomina cattura", "/org/thesemproject/opensem/gui/icons16/application_edit.png", menuListener);
                                    }
                                    if (isForumlas(node)) {
                                        addMenuVoice(popup, "Aggiungi Formula", "/org/thesemproject/opensem/gui/icons16/doc_tag.png", menuListener);
                                    }
                                    if (isForumlaDefinition(node)) {
                                        addMenuVoice(popup, "Rinomina Formula", "/org/thesemproject/opensem/gui/icons16/application_edit.png", menuListener);
                                    }
                                    if (isTableChild(node)) {
                                        addMenuVoice(popup, "Rinomina tabella", "/org/thesemproject/opensem/gui/icons16/application_edit.png", menuListener);
                                    }
                                    if (isForumlaDefinition(node) || isTableChild(node) || isSegmentChild(node) || (isCaptureGroup(node)) || (isCaptureChild(node)) || (isDataProvidersChild(node)) || (isDataProvidersRelationship(node))) {
                                        addMenuVoice(popup, "Elimina", "/org/thesemproject/opensem/gui/icons16/cross.png", menuListener);
                                    }
                                    popup.show(tree, evt.getX(), evt.getY());
                                }
                            }

                        }
                    }

                }
            } catch (Exception e) {
                LogGui.printException(e);
            }
        }
    }

    private void addMenuVoice(JPopupMenu popup, String label, String icon, ActionListener menuListener) {
        JMenuItem item;
        popup.add(item = new JMenuItem(label, new javax.swing.ImageIcon(getClass().getResource(icon))));
        item.setHorizontalTextPosition(JMenuItem.RIGHT);
        item.addActionListener(menuListener);
    }

    /**
     * Ritorna i suggerimenti
     *
     * @param root radice
     * @return lista dei suggerimenti
     */
    public List<String> getSuggestions(DefaultMutableTreeNode root) {
        int childrenSize = root.getChildCount();
        List<String> ret = new ArrayList<>();
        for (int i = 0; i < childrenSize; i++) {
            DefaultMutableTreeNode mx = (DefaultMutableTreeNode) root.getChildAt(i);
            if (mx instanceof ModelTreeNode) {
                ModelTreeNode node = (ModelTreeNode) mx;
                if (isDictionary(node)) {
                    DictionaryTreeNode dtn = (DictionaryTreeNode) node;
                    ret.addAll(dtn.getSuggestionList());
                } else if (isTable(node)) {
                    int tableNumber = node.getChildCount();
                    for (int k = 0; k < tableNumber; k++) {
                        ret.add("#" + node.getChildAt(k).toString() + " (tabella)");
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Ritorna la rappresentazione XML del modello
     *
     * @param root root del modello
     * @return rappresentazione xml
     */
    public Document getXml(DefaultMutableTreeNode root) {
        Document doc = new Document();
        Element model = new Element("M");
        doc.addContent(model);
        Element dictionary = new Element("D");
        Element segments = new Element("S");
        Element tables = new Element("T");
        Element globalCaptures = new Element("GC");
        Element dataproviders = new Element("DPS");
        model.addContent(dictionary);
        model.addContent(segments);
        model.addContent(tables);
        model.addContent(globalCaptures);
        model.addContent(dataproviders);
        int childrenSize = root.getChildCount();
        for (int i = 0; i < childrenSize; i++) {
            ModelTreeNode node = (ModelTreeNode) root.getChildAt(i);
            if (isDictionary(node)) {
                DictionaryTreeNode dtn = (DictionaryTreeNode) node;
                dtn.getXmlElement(dictionary);
            } else if (isSegment(node)) {
                processSegmentsSubtree(node, segments);
            } else if (isTable(node)) {
                processTablesSubtree(node, tables);
            } else if (isCapture(node)) {
                processCaptures(node, globalCaptures);
            } else if (isDataProviders(node)) {
                processDataProviders(node, dataproviders);
            }
        }
        return doc;
    }

    private void processSegmentsSubtree(ModelTreeNode node, Element segments) {
        int childrenSize = node.getChildCount();
        for (int i = 0; i < childrenSize; i++) {
            ModelTreeNode child = (ModelTreeNode) node.getChildAt(i);
            if (isSegmentChild(child)) {
                Element segment = ((SegmentTreeNode) child).getXmlElement();
                segments.addContent(segment);
                processSegment((SegmentTreeNode) child, segment);
            }
        }
    }

    private void processTablesSubtree(ModelTreeNode node, Element tables) {
        int childrenSize = node.getChildCount();
        for (int i = 0; i < childrenSize; i++) {
            TableTreeNode table = (TableTreeNode) node.getChildAt(i);
            tables.addContent(table.getXmlElement());
        }
    }

    private void processSegment(SegmentTreeNode node, Element segment) {
        int childrenSize = node.getChildCount();
        for (int i = 0; i < childrenSize; i++) {
            ModelTreeNode child = (ModelTreeNode) node.getChildAt(i);
            if (isSegmentChild(child)) { //E' un figlio
                Element subSegment = ((SegmentTreeNode) child).getXmlElement();
                segment.addContent(subSegment);
                processSegment((SegmentTreeNode) child, subSegment);
            } else if (isCapture(child)) {
                processCaptures(child, segment);
            } else if (isForumlas(child)) {
                processForumlas(child, segment);
            }
        }
    }

    private void processForumlas(ModelTreeNode node, Element segment) {
        int childrenSize = node.getChildCount();
        for (int i = 0; i < childrenSize; i++) {
            ModelTreeNode child = (ModelTreeNode) node.getChildAt(i);
            if (isForumlaDefinition(child)) { //E' un figlio
                Element formula = ((FormulaTreeNode) child).getXmlElement();
                segment.addContent(formula);
            }
        }
    }

    private void processCaptures(ModelTreeNode node, Element segment) {
        int childrenSize = node.getChildCount();
        for (int i = 0; i < childrenSize; i++) {
            ModelTreeNode child = (ModelTreeNode) node.getChildAt(i);
            if (isCaptureChild(child)) { //E' un figlio
                Element capture = ((CaptureTreeNode) child).getXmlElement();
                segment.addContent(capture);
                processCaptures(child, capture);
            } else if (isCaptureGroup(child)) { //E' un gruppo
                Element captureGroup = ((CapturesGroupTreeNode) child).getXmlElement();
                segment.addContent(captureGroup);
                processCaptures(child, captureGroup);
            }
        }
    }

    /**
     * Ritorna i segmenti
     *
     * @param root root dell'albero del modello
     * @return lista dei segmenti
     */
    public List<String> getSegmentsNames(DefaultMutableTreeNode root) {
        List<String> names = new ArrayList<>();
        Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = e.nextElement();
            if (node instanceof SegmentTreeNode) {
                names.add(((SegmentTreeNode) node).getNodeName());
            }
        }
        Collections.sort(names);
        return names;
    }

    /**
     * Ritorna le tabelle che non sono ancora state associate ad un dataprovider
     *
     * @param root root dell'albero di modello
     * @return lista delle tabelle
     */
    public List<String> getFreeDpTablesNames(DefaultMutableTreeNode root) {
        List<String> names = new ArrayList<>();
        names.add("");
        Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = e.nextElement();
            if (node instanceof TableTreeNode) {
                TableTreeNode tt = (TableTreeNode) node;
                if (tt.isPopulatedFromDp() && !tt.isDpLinked()) {
                    names.add(tt.getNodeName());
                }
            }
        }
        Collections.sort(names);
        return names;
    }

    /**
     * Ritorna la lista delle definizioni di dizionario e dei nomi delle
     * tabelle. Usato per il suggeritore
     *
     * @param root root dell'albero di modello
     * @return insieme dei nomi
     */
    public Set<String> getDictionaryAndTablesNames(DefaultMutableTreeNode root) {
        Set<String> names = new HashSet<>();
        names.add("");
        Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = e.nextElement();
            if (node instanceof TableTreeNode) {
                TableTreeNode tt = (TableTreeNode) node;
                names.add(tt.getNodeName());
            }
            if (node instanceof DictionaryTreeNode) {
                DictionaryTreeNode dtn = (DictionaryTreeNode) node;
                names.addAll(dtn.getAllDefinitionsName());
            }
        }
        return names;
    }

    /**
     * Ritorna la lista delle catture e delle formule del segmento
     *
     * @param root root dell'albero del modello
     * @param segmentName nome del segmento
     * @return lista dei nomi delle catture
     */
    public List<String> getSegmentsCaptures(DefaultMutableTreeNode root, String segmentName) {
        return getSegmentsCaptures(root, segmentName, true);
    }

    /**
     * Ritorna la lista delle catture del segmento
     *
     * @since 1.3
     *
     * @param root root dell'albero del modello
     * @param segmentName nome del segmento
     * @param includeFormulas true se deve includere nella lista anche le
     * formule (che sono comunque catture)
     * @return lista dei nomi delle catture
     */
    public List<String> getSegmentsCaptures(DefaultMutableTreeNode root, String segmentName, boolean includeFormulas) {
        List<String> names = new ArrayList<>();
        Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = e.nextElement();
            if (node instanceof SegmentTreeNode) {
                SegmentTreeNode snode = ((SegmentTreeNode) node);
                if ((segmentName == null) || (snode.getNodeName().equals(segmentName))) {
                    Enumeration<DefaultMutableTreeNode> ex = snode.depthFirstEnumeration();
                    while (ex.hasMoreElements()) {
                        DefaultMutableTreeNode xnode = ex.nextElement();
                        if (xnode instanceof CaptureTreeNode) {
                            String cname = ((CaptureTreeNode) xnode).getNodeName();
                            if (!names.contains(cname)) {
                                names.add(cname);
                            }
                        }
                        if (includeFormulas) {
                            if (xnode instanceof FormulaTreeNode) {
                                String cname = ((FormulaTreeNode) xnode).getNodeName();
                                if (!names.contains(cname)) {
                                    names.add(cname);
                                }
                            }
                        }
                    }
                }
            } else if (node instanceof CaptureTreeNode) {
                CaptureTreeNode ctn = (CaptureTreeNode) node;
                if (ctn.getEnabledSegments() != null) {
                    if ((segmentName == null) || ctn.getEnabledSegments().contains(segmentName)) {
                        if (!names.contains(ctn.getNodeName())) {
                            names.add(ctn.getNodeName());
                        }
                    }
                } else if ((segmentName == null)) {
                    if (!names.contains(ctn.getNodeName())) {
                        names.add(ctn.getNodeName());
                    }
                }
            }
        }
        Collections.sort(names);
        return names;
    }

    /**
     * Ritorna i nodi di cattura 
     * @param root nodo root
     * @param onlyClass se a true estrae le sole catture che classificano
     * @return lista dei nodi catturati
     */
    public List<CaptureTreeNode> getSegmentsCapturesNodes(DefaultMutableTreeNode root, boolean onlyClass) {
        List<CaptureTreeNode> captures = new ArrayList<>();
        Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = e.nextElement();
            if (node instanceof SegmentTreeNode) {
                SegmentTreeNode snode = ((SegmentTreeNode) node);
                Enumeration<DefaultMutableTreeNode> ex = snode.depthFirstEnumeration();
                while (ex.hasMoreElements()) {
                    DefaultMutableTreeNode xnode = ex.nextElement();
                    if (xnode instanceof CaptureTreeNode) {
                        CaptureTreeNode ctn = (CaptureTreeNode) xnode;
                        String cname = ctn.getNodeName();
                        if (!captures.contains(ctn)) {
                            if (onlyClass) {
                                if (ctn.getClassificationPath() != null) {
                                    captures.add(ctn);
                                }
                            } else {
                                captures.add(ctn);
                            }
                        }
                    }
                }
            } else if (node instanceof CaptureTreeNode) {
                CaptureTreeNode ctn = (CaptureTreeNode) node;
                if (!captures.contains(ctn)) {
                    if (onlyClass) {
                        if (ctn.getClassificationPath() != null) {
                            captures.add(ctn);
                        }
                    } else {
                        captures.add(ctn);
                    }
                }
            }
        }
        return captures;
    }

    private CaptureTreeNode copyNode = null;
    private CaptureTreeNode cutNode = null;

    private void processDataProviders(ModelTreeNode node, Element dataproviders) {
        int childrenSize = node.getChildCount();
        for (int i = 0; i < childrenSize; i++) {
            ModelTreeNode child = (ModelTreeNode) node.getChildAt(i);
            if (isDataProvidersChild(child)) { //E' un figlio
                Element dataprovider = ((DataProviderTreeNode) child).getXmlElement();
                dataproviders.addContent(dataprovider);
                processDataproviderRelationship(child, dataprovider);
            }
        }
    }

    private void processDataproviderRelationship(ModelTreeNode node, Element dataprovider) {
        int childrenSize = node.getChildCount();
        for (int i = 0; i < childrenSize; i++) {
            ModelTreeNode child = (ModelTreeNode) node.getChildAt(i);
            if (isDataProvidersRelationship(child)) { //E' un figlio
                Element dataproviderRelationship = ((DataProviderTreeRelationshipNode) child).getXmlElement();
                dataprovider.addContent(dataproviderRelationship);
            }
        }
    }

    /**
     * Modifica la relazione tra dataprovider e tabella. Utilizzato quando si
     * rinomina una tabelle
     *
     * @param root root dell'albero del modello
     * @param oldTableName vecchio nome tabella
     * @param newTableName nuovo nome tabella
     * @param dpName nome dataprovider
     */
    public void modifyTableRelationship(DefaultMutableTreeNode root, String oldTableName, String newTableName, String dpName) {
        Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = e.nextElement();
            if (node instanceof TableTreeNode) {
                TableTreeNode tt = (TableTreeNode) node;
                if (tt.isPopulatedFromDp()) {
                    if (tt.getNodeName().equals(oldTableName)) {
                        tt.setDpName(null);
                    }
                    if (tt.getNodeName().equals(newTableName)) {
                        tt.setDpName(dpName);
                    }
                }
            }
        }
    }

    /**
     * Rimuove una tabella
     *
     * @param root root dell'albero del modello
     * @param table nome tabella
     */
    public void removeTable(DefaultMutableTreeNode root, String table) {
        Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = e.nextElement();
            if (node instanceof SegmentTreeNode) {
                SegmentTreeNode st = (SegmentTreeNode) node;
                st.removeTable(table);
            } else if (node instanceof CaptureTreeNode) {
                CaptureTreeNode st = (CaptureTreeNode) node;
                st.removeTable(table);
            } else if (node instanceof DataProviderTreeNode) {
                DataProviderTreeNode st = (DataProviderTreeNode) node;
                st.removeTable(table);
            }
        }
    }

    /**
     * Gestisce la rinomina della tabella
     *
     * @param root root dell'albero del modello
     * @param table nome vecchio
     * @param newName nome nuovo
     */
    public void renameTable(DefaultMutableTreeNode root, String table, String newName) {
        Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = e.nextElement();
            if (node instanceof SegmentTreeNode) {
                SegmentTreeNode st = (SegmentTreeNode) node;
                st.renameTable(table, newName);
            } else if (node instanceof CaptureTreeNode) {
                CaptureTreeNode st = (CaptureTreeNode) node;
                st.renameTable(table, newName);
            } else if (node instanceof DataProviderTreeNode) {
                DataProviderTreeNode st = (DataProviderTreeNode) node;
                st.renameTable(table, newName);
            }
        }
    }

    private class MenuActionListner implements ActionListener {

        private final JTree tree;
        private final DefaultMutableTreeNode node;

        public MenuActionListner(JTree tree, DefaultMutableTreeNode node) {
            this.tree = tree;
            this.node = node;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            TreePath tp = tree.getLeadSelectionPath();
            DefaultTreeModel model = (DefaultTreeModel) (tree.getModel());
            try {
                if (event.getActionCommand().startsWith("Aggiungi segmento")) {
                    String name = JOptionPane.showInputDialog(null, "Nome del segmento");
                    if (name != null) {
                        //Controllare doppioni

                        SegmentTreeNode newNode = new SegmentTreeNode(name);
                        newNode.add(new ModelTreeNode("Catture", ModelTreeNode.TYPE_CAPTURE));
                        node.add(newNode);
                        model.reload();
                        GuiUtils.scrollToNode(tree, name);
                        processNode(newNode);
                    }
                } else if (event.getActionCommand().startsWith("Aggiungi tabella")) {
                    Set<String> names = getDictionaryAndTablesNames((DefaultMutableTreeNode) model.getRoot());

                    String name = JOptionPane.showInputDialog(null, "Nome della tabella");
                    if (name != null) {
                        if (names.contains(name)) {
                            GuiUtils.showErrorDialog("Il nome della tabella non deve esistere nel dizionario e deve essere univoco", "Errore");
                            return;

                        }
                        //Controllare doppioni
                        TableTreeNode newNode = new TableTreeNode(name);
                        node.add(newNode);
                        model.reload();
                        GuiUtils.scrollToNode(tree, name);
                        processNode(newNode);
                    }
                } else if (event.getActionCommand().startsWith("Aggiungi provider")) {
                    String name = JOptionPane.showInputDialog(null, "Nome del provider");
                    if (name != null) {
                        //Controllare doppioni
                        DataProviderTreeNode newNode = new DataProviderTreeNode(name, sem.getPercorsoIndice().getText());
                        node.add(newNode);
                        model.reload();
                        GuiUtils.scrollToNode(tree, name);
                        processNode(newNode);
                    }
                } else if (event.getActionCommand().startsWith("Aggiungi relazione")) {
                    String name = JOptionPane.showInputDialog(null, "Nome della relazione");
                    if (name != null) {
                        //Controllare doppioni
                        if (isValidFileName(name)) {
                            if (GuiUtils.find((DefaultMutableTreeNode) node, name, true).size() == 0) {
                                DataProviderTreeRelationshipNode newNode = new DataProviderTreeRelationshipNode(name, (DataProviderTreeNode) node);
                                node.add(newNode);
                                model.reload();
                                GuiUtils.scrollToNode(tree, name);
                                processNode(newNode);
                            } else {
                                GuiUtils.showErrorDialog("Il nome di un dataprovider deve essere univoco.", "Nome errato");
                            }
                        } else {
                            GuiUtils.showErrorDialog("Il nome di un dataprovider deve essere valido anche come\nnome di un file.", "Nome errato");
                        }
                    }
                } else if (event.getActionCommand().startsWith("Aggiungi gruppo catture")) {
                    String name = JOptionPane.showInputDialog(null, "Nome del gruppo");
                    if (name != null) {
                        name = "{" + name + "}";
                        CapturesGroupTreeNode newNode = new CapturesGroupTreeNode(name);
                        node.add(newNode);
                        model.reload();
                        GuiUtils.scrollToNode(tree, name);
                        processNode(newNode);
                    }
                } else if (event.getActionCommand().startsWith("Aggiungi cattura")) {
                    String name = JOptionPane.showInputDialog(null, "Nome della cattura");
                    if (name != null) {
                        CaptureTreeNode newNode = new CaptureTreeNode(name);
                        node.add(newNode);
                        model.reload();
                        GuiUtils.scrollToNode(tree, name);
                        processNode(newNode);
                    }
                } else if (event.getActionCommand().startsWith("Copia cattura")) {
                    CaptureTreeNode mNode = (CaptureTreeNode) node;
                    copyNode = new CaptureTreeNode("copia", mNode);
                } else if (event.getActionCommand().startsWith("Taglia cattura")) {
                    cutNode = (CaptureTreeNode) node;
                } else if (event.getActionCommand().startsWith("Incolla cattura")) {
                    if (copyNode != null) {
                        String name = JOptionPane.showInputDialog(null, "Nome della cattura");
                        if (name != null) {
                            CaptureTreeNode newNode = new CaptureTreeNode(name, copyNode);
                            node.add(newNode);
                            model.reload();
                            GuiUtils.scrollToNode(tree, name);
                            processNode(newNode);
                        }
                        copyNode = null;
                    }
                    if (cutNode != null) {
                        CaptureTreeNode newNode = new CaptureTreeNode(cutNode.getNodeName(), cutNode);
                        model.removeNodeFromParent(cutNode);
                        node.add(newNode);
                        model.reload();
                        GuiUtils.scrollToNode(tree, newNode.getNodeName());
                        processNode(newNode);
                        cutNode = null;
                    }
                } else if (event.getActionCommand().startsWith("Aggiungi Formula")) {
                    String name = JOptionPane.showInputDialog(null, "Nome della formula (cattura)");
                    if (name != null) {
                        FormulaTreeNode newNode = new FormulaTreeNode(name);
                        node.add(newNode);
                        model.reload();
                        GuiUtils.scrollToNode(tree, name);
                        processNode(newNode);
                    }
                } else if (event.getActionCommand().startsWith("Elimina")) {
                    if (GuiUtils.showConfirmDialog("Confermi l'eliminazione del sottoramo selezionato?", "Conferma cancellazione")) {
                        if (node instanceof DataProviderTreeNode) {
                            DataProviderTreeNode dptn = (DataProviderTreeNode) node;
                            DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
                            Set<String> tables = dptn.getTables();
                            for (String table : tables) {
                                sem.getModelEditor().modifyTableRelationship(root, table, null, null);
                            }
                        }
                        if (node instanceof TableTreeNode) {
                            TableTreeNode tab = (TableTreeNode) node;
                            DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
                            String dp = tab.getDpName();
                            if (dp != null) { //Cancella tutti i riferimenti alla tabella

                            }

                        }
                        model.removeNodeFromParent(node);
                    }
                } else if (event.getActionCommand().startsWith("Rinomina c")) {
                    String name = JOptionPane.showInputDialog(null, "Nome della cattura", node.toString());
                    if (name != null) {
                        CaptureTreeNode mNode = (CaptureTreeNode) node;
                        CaptureTreeNode newNode = new CaptureTreeNode(name, mNode);
                        int count = mNode.getChildCount();
                        for (int i = count - 1; i >= 0; i--) {
                            CaptureTreeNode cnode = (CaptureTreeNode) mNode.getChildAt(i);
                            mNode.remove(i);
                            newNode.add(cnode);
                        }
                        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                        model.removeNodeFromParent(node);
                        parent.add(newNode);
                        model.reload();
                        GuiUtils.scrollToNode(tree, name);
                        processNode(newNode);
                    }
                } else if (event.getActionCommand().startsWith("Rinomina g")) {
                    String n = node.toString();
                    if (n.startsWith("{")) {
                        n = n.substring(1);
                    }
                    if (n.endsWith("}")) {
                        n = n.substring(0, n.length() - 1);
                    }
                    String name = JOptionPane.showInputDialog(null, "Nome dell gruppo", n);
                    if (name != null) {
                        name = "{" + name + "}";
                        CapturesGroupTreeNode mNode = (CapturesGroupTreeNode) node;
                        CapturesGroupTreeNode newNode = new CapturesGroupTreeNode(name, mNode);
                        int count = mNode.getChildCount();
                        for (int i = count - 1; i >= 0; i--) {
                            CaptureTreeNode cnode = (CaptureTreeNode) mNode.getChildAt(i);
                            mNode.remove(i);
                            newNode.add(cnode);
                        }
                        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                        model.removeNodeFromParent(node);
                        parent.add(newNode);
                        model.reload();
                        GuiUtils.scrollToNode(tree, name);
                        processNode(newNode);
                    }
                } else if (event.getActionCommand().startsWith("Rinomina t")) {
                    String name = JOptionPane.showInputDialog(null, "Nome della tabella", node.toString());
                    if (name != null) {
                        name = name.toLowerCase();
                        TableTreeNode mNode = (TableTreeNode) node;
                        if (name.equals(mNode.getNodeName())) {
                            return;
                        }
                        Set<String> names = getDictionaryAndTablesNames((DefaultMutableTreeNode) model.getRoot());
                        if (names.contains(name)) {
                            GuiUtils.showErrorDialog("Il nome della tabella non deve esistere nel dizionario e deve essere univoco", "Errore");
                            return;
                        }
                        TableTreeNode newNode = new TableTreeNode(name, mNode);
                        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
                        renameTable(root, mNode.getNodeName(), name);
                        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                        model.removeNodeFromParent(node);
                        parent.add(newNode);
                        model.reload();
                        GuiUtils.scrollToNode(tree, name);
                        processNode(newNode);
                    }
                } else if (event.getActionCommand().startsWith("Rinomina F")) {
                    String name = JOptionPane.showInputDialog(null, "Nome della formula", node.toString());
                    if (name != null) {
                        FormulaTreeNode mNode = (FormulaTreeNode) node;
                        FormulaTreeNode newNode = new FormulaTreeNode(name, mNode);
                        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                        model.removeNodeFromParent(node);
                        parent.add(newNode);
                        model.reload();
                        GuiUtils.scrollToNode(tree, name);
                        processNode(newNode);
                    }
                } else if (event.getActionCommand().startsWith("Comprimi")) {
                    int childCount = node.getChildCount();
                    Set<CaptureTreeNode> toRemove = new HashSet<>();
                    Map<String, CaptureTreeNode> toUnify = new HashMap<>();
                    for (int i = 0; i < childCount; i++) {
                        CaptureTreeNode child = (CaptureTreeNode) node.getChildAt(i);
                        if (child.getCaptureType().equals("boolean")) {
                            String format = child.getCaptureFormat();
                            if (format.length() > 0) {
                                //Siamo nel caso corretto
                                String captureName = child.getNodeName();
                                CaptureTreeNode master = toUnify.get(captureName);
                                if (master == null) {
                                    List<String[]> patterns = child.getPatterns();
                                    patterns.stream().forEach((pattern) -> {
                                        pattern[3] = format;
                                    });
                                    child.setCaptureFormat("");
                                    child.setCaptureType("text");
                                    toUnify.put(captureName, child);
                                } else {
                                    List<String[]> patterns = child.getPatterns();
                                    for (String[] pattern : patterns) {
                                        master.addPattern(Integer.parseInt(pattern[2]), pattern[1], format);
                                    }
                                    toRemove.add(child);
                                }
                            }
                        }
                    }
                    toRemove.stream().forEach((child) -> {
                        model.removeNodeFromParent(child);
                    });
                    model.reload();
                }
            } catch (Exception e) {
                LogGui.printException(e);
            }
        }
    };

    /**
     * Ritorna il nodo corrente
     *
     * @return nodo attualmente selezionato nell'editor
     */
    public ModelTreeNode getCurrentNode() {
        return currentNode;
    }

    /**
     * Imposta il nodo corrente e gestice la visualizzazione
     *
     * @param currentNode nodo corrente
     */
    public void setCurrentNode(ModelTreeNode currentNode) {
        if (currentNode != null) {
            this.currentPath = new TreePath(currentNode.getPath());
        } else {
            this.currentPath = null;
        }
        this.currentNode = currentNode;
    }

    /**
     * Ritorna il percorso corrente
     *
     * @return percorso
     */
    public TreePath getCurrentPath() {
        return currentPath;
    }

    /**
     * Verifica se un pattern è un nome file consentito. Viene applicato al nome
     * del dataprovider poiché il dataprovider viene memorizzato in una cartella
     * con il suo nome
     *
     * @param text testo da verificare
     * @return true se il testo è un nome valido di file
     */
    public static boolean isValidFileName(String text) {
        Pattern pattern = Pattern.compile(
                "# Match a valid Windows filename (unspecified file system).          \n"
                + "^                                # Anchor to start of string.        \n"
                + "(?!                              # Assert filename is not: CON, PRN, \n"
                + "  (?:                            # AUX, NUL, COM1, COM2, COM3, COM4, \n"
                + "    CON|PRN|AUX|NUL|             # COM5, COM6, COM7, COM8, COM9,     \n"
                + "    COM[1-9]|LPT[1-9]            # LPT1, LPT2, LPT3, LPT4, LPT5,     \n"
                + "  )                              # LPT6, LPT7, LPT8, and LPT9...     \n"
                + "  (?:\\.[^.]*)?                  # followed by optional extension    \n"
                + "  $                              # and end of string                 \n"
                + ")                                # End negative lookahead assertion. \n"
                + "[^<>:\"/\\\\|?*\\x00-\\x1F]*     # Zero or more valid filename chars.\n"
                + "[^<>:\"/\\\\|?*\\x00-\\x1F\\ .]  # Last char is not a space or dot.  \n"
                + "$                                # Anchor to end of string.            ",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.COMMENTS);
        Matcher matcher = pattern.matcher(text);
        boolean isMatch = matcher.matches();
        return isMatch;
    }
}
