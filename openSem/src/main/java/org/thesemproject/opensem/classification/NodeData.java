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
package org.thesemproject.opensem.classification;

import org.thesemproject.opensem.gui.LogGui;

import com.beust.jcommander.internal.Lists;
import java.util.Collections;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.classification.SimpleNaiveBayesClassifier;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.classification.KNearestNeighborClassifier;
import org.jdom2.Document;
import org.jdom2.*;
import org.thesemproject.opensem.utils.interning.InternPool;

/**
 * Questo oggetto rappresenta un nodo gerarchico di classificazione con i
 * relativi figli e i classificatori bayesiano e knn
 */
public class NodeData {

    /**
     * Nome della categoria
     */
    public String nodeName; //Nome della catgoria

    /**
     * Livello della categoria
     */
    public String level;
    private final Map<String, SimpleNaiveBayesClassifier> classifiers; //classificatore sui suoi figli
    private final Map<String, KNearestNeighborClassifier> knns; //classificatore sui suoi figli

    private final int k;
    private NodeData parent;
    private final Map<String, NodeData> children;
    private final Map<String, String> reverseMap;
    private final InternPool intern;
    private final Map<String, String> labels;

    /**
     * Crea un nodo radice (ROOT)
     *
     * @param k soglia KNN
     * @param intern internizzatore di stringhe
     */
    public NodeData(int k, InternPool intern) {
        this.nodeName = "root";
        this.k = 1;
        this.classifiers = new HashMap<>();
        this.knns = new HashMap<>();
        this.level = null;
        this.parent = null;
        this.children = new HashMap<>();
        this.reverseMap = new HashMap<>();
        this.labels = new HashMap<>();
        this.intern = intern;
    }

    /**
     * Crea un nodo dell'albero
     *
     * @param nodeName nome del nodo
     * @param parent nodo padre
     * @param k fattore K per il classificatore KNN
     * @param intern internizzatore stringhe
     * @throws Exception
     */
    public NodeData(String nodeName, NodeData parent, int k, InternPool intern) throws Exception {
        this.nodeName = nodeName;
        this.classifiers = new HashMap<>();
        this.knns = new HashMap<>();
        this.k = k;
        this.children = new HashMap<>();
        this.reverseMap = new HashMap<>();
        this.intern = intern;
        this.labels = new HashMap<>();
        if (parent != null) {
            if (!parent.children.containsKey(nodeName)) {
                if (parent.parent == null) {
                    this.level = IndexManager.LEVEL_1;
                } else if (parent.parent.parent == null) {
                    this.level = IndexManager.LEVEL_2;
                } else if (parent.parent.parent.parent == null) {
                    this.level = IndexManager.LEVEL_3;
                } else {
                    this.level = IndexManager.LEVEL_4;
                }
                this.parent = parent;
                parent.children.put((String) intern.intern(nodeName), this);
                parent.reverseMap.put(nodeName.hashCode() + "", (String) intern.intern(nodeName));
            } else {
                throw new Exception("This node already exists");
            }
        } else { //Caso root non ho padri
            this.level = null;
        }
    }

    /**
     * Imposta una label per il nodo in una particolare lingua
     *
     * @param language lingua
     * @param label etichetta
     */
    public void setLabel(String language, String label) {
        labels.put(language, label);
    }

    /**
     * Si occupa del training del classificatore di nodo
     *
     * @param ar reader lucene
     * @param analyzer analizzatore sintattico
     * @param language lingua
     */
    public void train(LeafReader ar, Analyzer analyzer, String language) {
        try {
            SimpleNaiveBayesClassifier classifier = classifiers.get(language);
            if (classifier == null) {
                classifier = new SimpleNaiveBayesClassifier();
            }
            KNearestNeighborClassifier knn = knns.get(language);
            if (knn == null) {
                knn = new KNearestNeighborClassifier(k);
            }
            LogGui.info("Istrisco il nodo: " + nodeName);
            if (level == null) { //root
                //Dobbiamo istruire il nodo con tutti i documenti usando il field level1
                classifier.train(ar, IndexManager.BODY, IndexManager.LEVEL_1, analyzer);
                knn.train(ar, IndexManager.BODY, IndexManager.LEVEL_1, analyzer);
            } else if (IndexManager.LEVEL_1.equalsIgnoreCase(level)) {
                //Ci troviamo su una categoria figlia di root
                //Dobbiamo istruire il suo classificatore con tutti i documenti che hanno in Level_1 il nome della categoria
                //con i soli campi di Level_2
                TermQuery query = new TermQuery(new Term(IndexManager.LEVEL_1, nodeName.hashCode() + ""));
                //IndexSearcher indexSearcher = new IndexSearcher(ar);
                //TopDocs td = indexSearcher.search(query,Integer.MAX_VALUE);
                classifier.train(ar, IndexManager.BODY, IndexManager.LEVEL_2, analyzer, query);
                knn.train(ar, IndexManager.BODY, IndexManager.LEVEL_2, analyzer, query);
            } else if (IndexManager.LEVEL_2.equalsIgnoreCase(level)) {
                //Ci troviamo su una categoria figlia di un livello 1
                //Dobbiamo istruire il suo classificatore con tutti i documenti che hanno in Level_2 il nome della categoria
                //con i soli campi di Level_3
                TermQuery query = new TermQuery(new Term(IndexManager.LEVEL_2, nodeName.hashCode() + ""));
                classifier.train(ar, IndexManager.BODY, IndexManager.LEVEL_3, analyzer, query);
                knn.train(ar, IndexManager.BODY, IndexManager.LEVEL_3, analyzer, query);
            } else if (IndexManager.LEVEL_3.equalsIgnoreCase(level)) {
                //Ci troviamo su una categoria figlia di un livello 2
                //Dobbiamo istruire il suo classificatore con tutti i documenti che hanno in Level_3 il nome della categoria
                //con i soli campi di Level_4
                TermQuery query = new TermQuery(new Term(IndexManager.LEVEL_3, nodeName.hashCode() + ""));
                classifier.train(ar, IndexManager.BODY, IndexManager.LEVEL_4, analyzer, query);
                knn.train(ar, IndexManager.BODY, IndexManager.LEVEL_4, analyzer, query);
            }
            classifiers.put(language, classifier);
            knns.put(language, knn);
        } catch (Exception e) {
            LogGui.printException(e);
        }
    }

    /**
     * Ritorna il figlio con un determinato nuome
     *
     * @param childrenName nome del filglio
     * @return figlio oppure null se non trovato.
     */
    public NodeData getNode(String childrenName) {
        return children.get(childrenName);
    }

    /**
     * Verifica se un nodo è una foglia
     *
     * @return true se non ha figli
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * Ritorna la lista dei nomi dei figli del nodo corrente
     *
     * @return Lista dei nomi dei figli
     */
    public List<String> getChildrenNames() {
        List<String> ret = Lists.newArrayList(children.keySet());
        Collections.sort(ret);
        return ret;
    }

    /**
     * Ritorna il nome di un nodo dato l'ID
     *
     * @param nid ID del nodo
     * @return nome del nodo
     */
    public String getNameFromId(String nid) {
        return reverseMap.get(nid);
    }

    /**
     * Ritorna una label associat al nodo
     *
     * @param language lingua in cui si vuole la label
     * @return label del nodo
     */
    public String getLabel(String language) {
        if (language == null) {
            return nodeName;
        }
        String name = labels.get(language);
        if (name == null) {
            return nodeName;
        }
        return name;
    }

    /**
     * Ritorna il classificatore bayesiano in una specifica lingua
     *
     * @param language lingua del classificatore
     * @return classificatore bayesiano
     */
    public SimpleNaiveBayesClassifier getClassifier(String language) {
        return classifiers.get(language);
    }

    /**
     * Ritorna il classificatore KNN per una specifica lingua
     *
     * @param language lingua del classificatore
     * @return classificatore KNN
     */
    public KNearestNeighborClassifier getKnn(String language) {
        return knns.get(language);

    }

    private Element getXml() {
        Element element = new Element("Node");
        element.setAttribute("nodeName", nodeName);
        element.setAttribute("k", String.valueOf(k));
        Element labelsElement = new Element("labels");
        labels.keySet().stream().forEach((key) -> {
            labelsElement.setAttribute(key, labels.get(key));
        });
        element.addContent(labelsElement);
        Element childrenElement = new Element("childrens");
        children.values().stream().forEach((child) -> {
            childrenElement.addContent(child.getXml());
        });
        element.addContent(childrenElement);
        return element;
    }

    /**
     * Ritorna la rappresentazione XML della struttura di classificazione
     *
     * @param root Nodo radice
     * @return Document XML che rappresenta la struttura di classificazione
     */
    public static Document getDocument(NodeData root) {
        if (root == null) {
            return null;
        }
        if ("root".equals(root.nodeName)) {
            Document document = new Document();
            Element classTree = new Element("ClassificationTree");
            classTree.addContent(root.getXml());
            document.addContent(classTree);
            return document;
        }
        return null;
    }

    /**
     * Ritorna la struttura di classificazione in formato csv (una colonna per
     * ogni livello)
     *
     * @param root Nodo radice
     * @return struttura csv della struttura di classificazione
     */
    public static String getCSVDocument(NodeData root) {
        if (root == null) {
            return null;
        }
        if ("root".equals(root.nodeName)) {
            StringBuffer ret = new StringBuffer();
            ret.append(root.getCSV(ret));
            return ret.toString();
        }
        return null;
    }

    /**
     * Ritorna la rappresentazione di una struttura di classificazione in
     * formato NodeData a partire da un file xml
     *
     * @param document file xml
     * @param intern internizzatore
     * @return root della struttura
     */
    public static NodeData getNodeData(Document document, InternPool intern) {
        NodeData root = null;
        Element classTree = document.getRootElement();
        if ("ClassificationTree".equals(classTree.getName())) {
            List<Element> children = classTree.getChildren();
            for (Element element : children) {
                if ("Node".equals(element.getName())) {
                    String nodeName = element.getAttributeValue("nodeName");
                    if ("root".equals(nodeName)) {
                        int k = Integer.parseInt(element.getAttributeValue("k"));
                        root = new NodeData(k, intern);
                        Element labelsElement = element.getChild("labels");
                        if (labelsElement != null) {
                            List<Attribute> attributes = labelsElement.getAttributes();
                            for (Attribute attribute : attributes) {
                                root.setLabel(attribute.getName(), attribute.getValue());
                            }
                        }
                        Element childrenElement = element.getChild("childrens");
                        if (childrenElement != null) {
                            for (Element childNodeElement : childrenElement.getChildren()) {
                                processChildrenElement(childNodeElement, root, intern);
                            }
                        }
                    }
                }
            }
        }
        return root;
    }

    private static void processChildrenElement(Element element, NodeData parent, InternPool intern) {
        try {
            String nodeName = element.getAttributeValue("nodeName");
            int k = Integer.parseInt(element.getAttributeValue("k"));
            NodeData node = new NodeData(nodeName, parent, k, intern);
            Element labelsElement = element.getChild("labels");
            if (labelsElement != null) {
                List<Attribute> attributes = labelsElement.getAttributes();
                attributes.stream().forEach((attribute) -> {
                    node.setLabel(attribute.getName(), attribute.getValue());
                });
            }
            Element childrenElement = element.getChild("childrens");
            if (childrenElement != null) {
                childrenElement.getChildren().stream().forEach((childNodeElement) -> {
                    processChildrenElement(childNodeElement, node, intern);
                });
            }
        } catch (Exception e) {
            LogGui.printException(e);
        }
    }

    /**
     *
     * @param path
     * @param level
     */
    protected void removeChild(Object[] path, int level) {
        if (level < path.length - 1) {
            NodeData child = children.get(path[level + 1].toString());
            if (child != null) {
                if (path.length == level + 2) {
                    children.remove(child.nodeName);
                    reverseMap.remove(child.nodeName.hashCode() + "");
                } else {
                    child.removeChild(path, level + 1);
                }
            }
        }
    }

    /**
     *
     * @param path
     * @param level
     */
    protected void addChild(Object[] path, int level) {
        if (level < path.length - 1) {
            NodeData child = children.get(path[level + 1].toString());
            if (path.length == level + 2) {
                try {
                    NodeData nd = new NodeData(path[path.length - 1].toString(), this, k, intern);
                } catch (Exception ex) {
                    LogGui.printException(ex);
                }
            } else if (child != null) {
                child.addChild(path, level + 1);
            }
        }
    }

    private StringBuffer getCSV(StringBuffer prefix) {
        StringBuffer ret = new StringBuffer();
        if (!"root".equals(nodeName)) {
            if (prefix.length() == 0) {
                prefix.append(nodeName);
            } else {
                prefix.append("\t").append(nodeName);
            }
        }
        for (String name : children.keySet()) {
            StringBuffer sb = new StringBuffer();
            sb.append(prefix);
            ret.append(children.get(name).getCSV(sb));
        }
        if (children.isEmpty()) {
            ret.append(prefix).append("\r\n");
        }
        return ret;
    }

    /**
     * Ritorna una struttura di classificazione a partire da una lista di righe
     * dove la struttura è contenuta con i nomi dei nodi separati da tabulatore
     * E' utilizzata per ricostruire la struttura a partire da un file csv
     * ottenuto esportando la struttura dal gui editor
     *
     * @param rows righe del file csv
     * @param k fattore K per il KNN
     * @param intern internizzatore
     * @return root della struttura
     */
    public static NodeData getNodeData(List<String> rows, int k, InternPool intern) {
        NodeData root = new NodeData(k, intern);
        Set<String> categories = new HashSet<>();
        for (String row : rows) {
            String[] doc = row.split("\t");
            if (doc.length > 0) {
                String level1 = (String) intern.intern(doc[0]);
                if (level1 != null) {
                    if (!categories.contains(level1)) { //Nuova categoria di livello 1
                        addNode(root, categories, level1, k, intern);
                    }
                    if (doc.length > 1) {
                        String level2 = (String) intern.intern(doc[1]);
                        if (!categories.contains(level2)) { //Nuova categoria di livello 2
                            NodeData parent = root.getNode(level1);
                            addNode(parent, categories, level2, k, intern);
                        }
                        if (doc.length > 2) {
                            String level3 = (String) intern.intern(doc[2]);
                            if (!categories.contains(level3)) { //Nuova categoria di livello 3
                                NodeData p1 = root.getNode(level1);
                                if (p1 != null) {
                                    NodeData p2 = p1.getNode(level2);
                                    addNode(p2, categories, level3, k, intern);
                                }
                            }
                            if (doc.length > 3) {
                                String level4 = (String) intern.intern(doc[3]);
                                if (!categories.contains(level4)) { //Nuova categoria di livello 4
                                    NodeData p1 = root.getNode(level1);
                                    if (p1 != null) {
                                        NodeData p2 = p1.getNode(level2);
                                        if (p2 != null) {
                                            NodeData p3 = p2.getNode(level3);
                                            addNode(p3, categories, level4, k, intern);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return root;
    }

    private static void addNode(NodeData parent, Set<String> cats, String name, int k, InternPool intern) {
        if (parent != null) {
            if (name.trim().length() > 0) {
                NodeData node = parent.getNode((String) intern.intern(name));
                if (node == null) {
                    try {
                        node = new NodeData((String) intern.intern(name), parent, k, intern);
                    } catch (Exception exception) {
                        LogGui.printException(exception);
                    }
                }
            }
            cats.add((String) intern.intern(name));
        }
    }

}
