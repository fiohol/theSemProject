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

import static org.thesemproject.opensem.classification.IndexManager.BODY;
import static org.thesemproject.opensem.classification.IndexManager.UUID;
import org.thesemproject.opensem.gui.utils.GuiUtils;
import org.thesemproject.opensem.gui.LogGui;
import org.apache.lucene.classification.ClassificationResult;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.index.SlowCompositeReaderWrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.classification.KNearestNeighborClassifier;
import org.apache.lucene.classification.SimpleNaiveBayesClassifier;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Bits;
import org.thesemproject.opensem.utils.interning.InternPool;

/**
 * Motore di classificazione statistico basato su Lucene IL motore è stato
 * strutturato per ottimizzare i tempi di classificazione Dato che l'albero di
 * classificazione è gerarchico tutti i livelli sono istruiti Il sistema prima
 * prova a classificare sul primo livello, poi se il documento supera la soglia
 * il sistema prova a classificare sul secondo livello e quindi sul terzo...
 *
 * Questo approccio permette di passare, nel caso di molte categorie ad un
 * approccio con tempi logaritmici anziché quadratici. I tempi di
 * classificazione dipendono infatti dal numero dei documenti (n) ma anche dal
 * numero di categorie. Se si valutasse un documento solo sulle foglie i tempi
 * sarebbero n x m. valutando prima sul primo livello i tempi diventano n x
 * (numero di categorie primo livello) + n x (numero di categorie sottolivello
 * scelto)...
 *
 * Il sistema tiene in memoria un albero di nodi. Ogni nodo è istruito
 * attraverso una query di lucene
 */
public class MulticlassEngine {

    /**
     * Verifica se il motore è inizializzato
     *
     * @return true se il motore è inizializzato
     */
    public boolean isIsInit() {
        return isInit;
    }

    private NodeData root;
    private Set<String> cats;
    private final Map<String, MyAnalyzer> analyzers;
    private final Map<String, IndexReader> readers;
    private boolean isInit;
    private File structurePath;

    private final InternPool intern;

    /**
     * Costruisce il motore
     */
    public MulticlassEngine() {
        intern = new InternPool();
        analyzers = new HashMap<>();
        readers = new HashMap<>();
    }

    /**
     * Costruisce il motore
     *
     * @param intern intern per le stringhe
     */
    public MulticlassEngine(InternPool intern) {
        this.intern = intern;
        analyzers = new HashMap<>();
        readers = new HashMap<>();
    }

    /**
     * Ritorna il nodo radice. A partire dalla radice si può navigare tutto
     * l'albero di classificazione
     *
     * @return radice
     */
    public NodeData getRoot() {
        return root;
    }

    /**
     * Ritorna tutte le categorie su cui si può classificare. Il set è nullo se
     * il sistema non è inizializzato
     *
     * @return categorie
     */
    public Set<String> getCats() {
        return cats;
    }

    /**
     * Inizializza il motore con uno specifico indice
     *
     * @param structurePath posizione della struttura
     * @param k fattore K per il classificatore KNN
     * @return true se il sistema è inizializzato
     */
    public boolean init(String structurePath, int k) {
        return init(structurePath, k, false);
    }

    /**
     * Inizializza il sistema ricostruendo l'indice
     *
     * @param structurePath posizione della struttura
     * @param k fattore K per la classificazione KNN
     * @param reindex true se si vuole reindicizzare prima di inizializzare
     * @return true se il sistema è inizializzato
     */
    public boolean init(String structurePath, int k, boolean reindex) {
        File fStructurePath = new File(structurePath); //Verifica che esistano le structure path
        if (fStructurePath.exists() && fStructurePath.isDirectory()) {
            //Verifica se la struttura è ok.
            //TODO: Qui legge il file della struttura (in futuro)
            boolean ret = true;
            root = null;
            this.structurePath = fStructurePath;
            String structueFileName = getStructurePath();
            File fStructure = new File(structueFileName);
            boolean fileExists = fStructure.exists();
            if (fileExists) {
                try {
                    org.jdom2.Document document = GuiUtils.readXml(fStructure.getAbsolutePath());
                    root = NodeData.getNodeData(document, intern);
                } catch (Exception e) {
                    LogGui.printException(e);
                }
            }
            cats = new HashSet<>();
            for (String language : MyAnalyzer.languages) {
                String indexFolder = getIndexFolder(language);
                String stopWords = getStopWordPath(language);
                File fIndex = new File(indexFolder);
                File fStop = new File(stopWords);
                if (fIndex.exists()) {
                    if (fIndex.listFiles().length > 0) {
                        try {
                            ret = ret && init(indexFolder, stopWords, language, k, reindex);
                        } catch (Exception e) {
                            LogGui.printException(e);
                        }
                    }
                }
            }
            org.jdom2.Document document = NodeData.getDocument(root);
            GuiUtils.storeXml(document, structueFileName);
            isInit = true;
            return ret;
        } else {
            LogGui.info("Il percorso indicato non è una cartella.");
            return false;
        }
    }

    private boolean init(String index, String stop, String language, int k, boolean needReindex) {
        try {

            List<Document> reindexDoc = new ArrayList<>();
            IndexReader reader = readers.get(language);
            if (isInit) {
                closeReader(reader);
            }
            isInit = false;
            reader = DirectoryReader.open(getFolderDir(index));
            readers.put(language, reader);
            final LeafReader ar = SlowCompositeReaderWrapper.wrap(reader);
            MyAnalyzer analyzer = IndexManager.getAnalyzer(new File(stop), language);
            analyzers.put(language, analyzer);
            final int maxdoc = reader.maxDoc();
            if (root == null) {
                root = new NodeData(k, intern); //Root
            }
            LogGui.info("Init language: " + language);
            LogGui.info("Documents: " + maxdoc);
            LogGui.info("Start training NaiveBayes and KNN");
            LogGui.info("Read documents from idx to build the tree...");
            LogGui.info("Train root...");
            root.train(ar, analyzer, language);
            LogGui.info("Read all example dataset");
            HashSet categories = new HashSet<>();
            Bits liveDocs = MultiFields.getLiveDocs(reader);
            for (int i = 0; i < maxdoc; i++) {
                if (liveDocs != null && !liveDocs.get(i)) {
                    continue;
                }
                Document doc = ar.document(i);
                if (doc.get(IndexManager.UUID) == null) {
                    doc.add(new StringField(UUID, java.util.UUID.randomUUID().toString(), Field.Store.YES));

                }
                if (needReindex) {
                    reindexDoc.add(doc);

                }
                String level1 = (String) intern.intern(doc.get(IndexManager.LEVEL1_NAME));
                if (level1 != null) {
                    if (!categories.contains(level1)) { //Nuova categoria di livello 1
                        addNode(ar, analyzer, root, categories, level1, k, language);
                    }
                    String level2 = (String) intern.intern(doc.get(IndexManager.LEVEL2_NAME));
                    if (level2 != null) {
                        if (!categories.contains(level2)) { //Nuova categoria di livello 2
                            NodeData parent = root.getNode(level1);
                            addNode(ar, analyzer, parent, categories, level2, k, language);
                        }
                        String level3 = (String) intern.intern(doc.get(IndexManager.LEVEL3_NAME));
                        if (level3 != null) {
                            if (!categories.contains(level3)) { //Nuova categoria di livello 3
                                NodeData p1 = root.getNode(level1);
                                if (p1 != null) {
                                    NodeData p2 = p1.getNode(level2);
                                    addNode(ar, analyzer, p2, categories, level3, k, language);
                                }
                            }
                            String level4 = (String) intern.intern(doc.get(IndexManager.LEVEL4_NAME));
                            if (level4 != null) {
                                if (!categories.contains(level4)) { //Nuova categoria di livello 4
                                    NodeData p1 = root.getNode(level1);
                                    if (p1 != null) {
                                        NodeData p2 = p1.getNode(level2);
                                        if (p2 != null) {
                                            NodeData p3 = p2.getNode(level3);
                                            addNode(ar, analyzer, p3, categories, level4, k, language);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (i % 1000 == 0) {
                    LogGui.info("Read Progress... " + i);
                }
            }
            LogGui.info("End training");
            this.cats.addAll(categories);
            if (needReindex) {
                LogGui.info("Start reindex...");
                IndexManager.reindex(reindexDoc, Paths.get(index), new File(stop), language);
                LogGui.info("End reindex...");
            }
            return true;
        } catch (Exception e) {
            LogGui.printException(e);
        }
        return false;
    }

    /**
     * Ritorna l'elenco dei documenti sottoforma di stringhe contenuti
     * nell'indice di una particolare lingua. Viene usato dall'interfaccia
     * grafica del SEM GUI
     *
     * @param language lingua di cui si vogliono i documenti.
     * @return lista dei documenti sottoforma di stringhe per popolare la JTable
     * della GUI
     */
    public List<String[]> getDocuments(String language) {
        List<String[]> rows = new ArrayList<>();
        try {

            String index = getIndexFolder(language);
            IndexReader reader = DirectoryReader.open(getFolderDir(index));
            final LeafReader ar = SlowCompositeReaderWrapper.wrap(reader);
            Bits liveDocs = MultiFields.getLiveDocs(reader);
            final int maxdoc = reader.maxDoc();
            for (int i = 0; i < maxdoc; i++) {
                if (liveDocs != null && !liveDocs.get(i)) {
                    continue;
                }
                Document doc = ar.document(i);
                String[] row = new String[6];
                row[0] = doc.get(IndexManager.UUID);
                row[1] = doc.get(IndexManager.BODY);
                String level1 = (String) intern.intern(doc.get(IndexManager.LEVEL1_NAME));
                row[2] = level1;
                if (level1 != null) {
                    String level2 = (String) intern.intern(doc.get(IndexManager.LEVEL2_NAME));
                    if (level2 != null) {
                        row[3] = level2;
                        String level3 = (String) intern.intern(doc.get(IndexManager.LEVEL3_NAME));
                        if (level3 != null) {
                            row[4] = level3;
                            String level4 = (String) intern.intern(doc.get(IndexManager.LEVEL4_NAME));
                            if (level4 != null) {
                                row[5] = level4;
                            }
                        }
                    }
                }
                if (i % 1000 == 0) {
                    LogGui.info("Read Progress... " + i);
                }
                rows.add(row);
            }
            reader.close();
        } catch (Exception e) {
            LogGui.printException(e);
        }
        return rows;
    }

    /**
     * Tokenizza un testo utilizzando l'analizzatore sintattico e le stop words
     * di lingua
     *
     * Il servizio è fatto attraverso Tokenizer.tokenize(text, analyzer)
     *
     * @param text testo da tokenizzare
     * @param language lingua del testo da toeknizzare
     * @return testo tokenizzato
     */
    public String tokenize(String text, String language) {
        if (isInit) {
            String ret;
            try {
                MyAnalyzer analyzer = getAnalyzer(language);
                ret = Tokenizer.tokenize(text, analyzer);
            } catch (Exception e) {
                LogGui.printException(e);
                ret = "";
            }
            return ret;
        }
        return "";
    }

    /**
     * Ritorna l'analizzatore sintattico per una lingua
     *
     * @param language lingua dell'analizzatore
     * @return Analizzatore Sintattico
     * @throws IOException
     */
    public MyAnalyzer getAnalyzer(String language) throws IOException {
        MyAnalyzer analyzer = analyzers.get(language);
        if (analyzer == null) {
            String stopWords = getStopWordPath(language);
            analyzer = IndexManager.getAnalyzer(new File(stopWords), language);
            analyzers.put(language, analyzer);
        }
        return analyzer;
    }

    /**
     * Classifica un testo con il classificatore Bayesiano di lucene
     *
     * @param text testo da analizzare
     * @param th soglia (tra 0 e 1) di accettabilità della classificazione. Il
     * bayesiano per definizione classifica sempre su tutte le categorie. Ad
     * ogni classificazione assegna uno score di affidabilità espresso in %. La
     * somma degli score da sempre 1.
     * @param language lingua del testo
     * @return lista dei percorsi di classificazione (un documento può essere
     * classificato su più categorie)
     */
    public List<ClassificationPath> bayesClassify(String text, double th, String language) {
        if (!isInit) {
            return null;
        }
        try {
            return classifyOnNode(tokenize(text, language), root, th, false, language);
        } catch (Exception e) {
            LogGui.printException(e);
        }
        return null;
    }

    /**
     * Classifica un testo con il classificatore KNN di lucene
     *
     * @param text testo da analizzre
     * @param th soglia (tra 0 e 1) di accettabilità della classificazione
     * @param language lingua del testo
     * @return percorso di classificazione
     */
    public ClassificationPath knnClassify(String text, double th, String language) {
        if (!isInit) {
            return null;
        }
        try {
            return classifyOnNode(tokenize(text, language), root, th, true, language).get(0);
        } catch (Exception e) {
            LogGui.printException(e);
        }
        return null;
    }

    private ClassificationPath classifyOnSubNode(String text, NodeData nd, int level, ClassificationPath cp, double threshold, String language) throws IOException {
        if (!isInit) {
            return null;
        }
        if (level < 1) {
            return cp;
        }
        if (cp == null) {
            return null;
        }
        ClassificationResult<BytesRef> resultNdList = null;
        if (cp.getTechnology().equals(ClassificationPath.BAYES)) {
            SimpleNaiveBayesClassifier snbc = nd.getClassifier(language);
            if (snbc != null) {
                resultNdList = snbc.getClasses(text).get(0);
            }

        } else if (cp.getTechnology().equals(ClassificationPath.KNN)) {
            KNearestNeighborClassifier knnc = nd.getKnn(language);
            if (knnc != null) {
                resultNdList = knnc.assignClass(text);
            }
        }
        if (resultNdList != null) {
            double score = resultNdList.getScore();
            if (score >= threshold) {
                cp.addResult(nd.getNameFromId(resultNdList.getAssignedClass().utf8ToString()), score, level);
                NodeData child = nd.getNode(cp.getNodeName(level));
                if (child != null) {
                    if (child.hasChildren()) {
                        return classifyOnSubNode(text, child, level + 1, cp, threshold, language);
                    }
                }
            }
        }
        return cp;
    }

    private List<ClassificationPath> classifyOnNode(String text, NodeData nd, double threshold, boolean knn, String language) throws IOException {
        try {
            List<ClassificationPath> results = new ArrayList<>();
            int level = 0;
            if (!knn) {
                ClassificationPath bChoice1 = new ClassificationPath(ClassificationPath.BAYES);
                ClassificationPath bChoice2 = new ClassificationPath(ClassificationPath.BAYES);
                //Classifica bayes
                SimpleNaiveBayesClassifier snbc = nd.getClassifier(language);
                if (snbc != null) {
                    List<ClassificationResult<BytesRef>> resultNdList = snbc.getClasses(text);
                    bChoice1.addResult(nd.getNameFromId(resultNdList.get(0).getAssignedClass().utf8ToString()), resultNdList.get(0).getScore(), level);

                    NodeData child1 = nd.getNode(bChoice1.getNodeName(level));
                    if (child1 != null) {
                        if (child1.hasChildren()) {
                            bChoice1 = classifyOnSubNode(text, child1, level + 1, bChoice1, threshold, language);
                        }
                    }
                    results.add(bChoice1);
                    if (resultNdList.size() > 1) {
                        double score2 = resultNdList.get(1).getScore();
                        if (score2 >= threshold) {
                            bChoice2.addResult(nd.getNameFromId(resultNdList.get(1).getAssignedClass().utf8ToString()), score2, level);
                            NodeData child2 = nd.getNode(bChoice2.getNodeName(level));
                            if (child2 != null) {
                                if (child2.hasChildren()) {
                                    bChoice2 = classifyOnSubNode(text, child2, level + 1, bChoice2, threshold, language);
                                }
                            }
                            results.add(bChoice2);
                        }
                    }
                }
            } else {
                ClassificationPath kChoice1 = new ClassificationPath(ClassificationPath.KNN);
                KNearestNeighborClassifier knnc = nd.getKnn(language);
                if (knnc != null) {
                    ClassificationResult<BytesRef> res = knnc.assignClass(text);
                    kChoice1.addResult(nd.getNameFromId(res.getAssignedClass().utf8ToString()), res.getScore(), level);
                    NodeData child1 = nd.getNode(kChoice1.getNodeName(level));
                    if (child1 != null) {
                        if (child1.hasChildren()) {
                            kChoice1 = classifyOnSubNode(text, child1, level + 1, kChoice1, threshold, language);
                        }
                    }
                    results.add(kChoice1);
                }
            }
            return results;
        } catch (Exception e) {
            LogGui.printException(e);
            return null;
        }
    }

    private void addNode(LeafReader ar, MyAnalyzer analyzer, NodeData parent, Set<String> cats, String name, int k, String language) throws Exception {
        if (parent != null) {
            LogGui.info("Add node " + name + " to " + parent.nodeName);
            NodeData node = parent.getNode((String) intern.intern(name));
            if (node == null) {
                node = new NodeData((String) intern.intern(name), parent, k, intern);
            }
            LogGui.info("Istruzione " + name + " language: " + language);
            node.train(ar, analyzer, language);
            LogGui.info("Fine istruzione...");
            cats.add((String) intern.intern(name));
        }
    }

    private Directory getFolderDir(String indexDir) throws IOException {
        RAMDirectory ret;
        try (FSDirectory dir = FSDirectory.open(Paths.get(indexDir))) {
            ret = new RAMDirectory(dir, null);

        }
        return ret;
    }

    private void closeReader(IndexReader reader) {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (Exception e) {
            LogGui.printException(e);
        }
    }

    /**
     * Chiude i reader su tutti gli indici. Il sistema per classificare deve
     * avere gli indici aperti Esiste un reader per ogni lingua visto che esiste
     * un indice per ogni lingua
     */
    public void closeAllReaders() {
        readers.values().stream().forEach((reader) -> {
            closeReader(reader);
        });
    }

    /**
     * Ritorna la lista delle stop words di una lingua
     *
     * @param language lingua
     * @return lista delle stop words
     */
    public List<String> getStopWords(String language) {
        MyAnalyzer analyzer = analyzers.get(language);
        List<String> ret = new ArrayList<>();
        String stopWords = getStopWordPath(language);
        if (analyzer == null) {
            try {
                analyzer = IndexManager.getAnalyzer(new File(stopWords), language);
            } catch (Exception e) {
                LogGui.printException(e);
            }
            analyzers.put(language, analyzer);
        }
        if (analyzer != null) {
            CharArraySet cas = analyzer.getStopwordSet();
            cas.stream().map((c) -> (char[]) c).forEach((row) -> {
                ret.add(String.valueOf(row));
            });
        }
        Collections.sort(ret);
        return ret;
    }

    /**
     * Ritorna l'elenco delle stop words di default per una lingua. L'elenco è
     * costituito dalla lista delle stop words di una lingua prendendole
     * direttamente dall'anlizzatore che le riprende da lucene
     *
     * @param language linga
     * @return Lista delle stop words di default per una lingua
     */
    public List<String> getDefaultStopWords(String language) {
        MyAnalyzer analyzer = analyzers.get(language);
        List<String> ret = new ArrayList<>();
        String stopWords = getStopWordPath(language);
        if (analyzer == null) {
            try {
                analyzer = IndexManager.getAnalyzer(new File(stopWords), language);
            } catch (Exception e) {
                LogGui.printException(e);
            }
            analyzers.put(language, analyzer);
        }
        if (analyzer != null) {
            CharArraySet cas = MyAnalyzer.getDefaultStopSet(language);
            cas.stream().map((c) -> (char[]) c).forEach((row) -> {
                ret.add(String.valueOf(row));
            });
        }
        Collections.sort(ret);
        return ret;
    }

    /**
     * Memorizza le stop word per una specifica lingua
     *
     * @param language lingua
     * @param stopWords elenco delle stop words. Ogni elemento della lista è una
     * stop word
     */
    public void storeStopWords(String language, List<String> stopWords) {
        analyzers.put(language, IndexManager.storeStopWords(structurePath, language, stopWords));

    }

    /**
     * Rimuove un documento dall'indice
     *
     * @param uuid id del documento
     * @param language lingua del documento
     */
    public void removeDocument(String uuid, String language) {
        String indexFolder = getIndexFolder(language);
        String stopWords = getStopWordPath(language);
        closeAllReaders();
        IndexManager.removeDocument(Paths.get(indexFolder), new File(stopWords), uuid, language);
    }

    /**
     * Aggiorna la descrizione di un documento
     *
     * @param uuid id univoco del docuemnto
     * @param description descrizione modificata
     * @param language lingua
     * @throws Exception
     */
    public void updateDocumentDescription(String uuid, String description, String language) throws Exception {
        String indexFolder = getIndexFolder(language);
        String stopWords = getStopWordPath(language);
        IndexReader reader = readers.get(language);
        IndexSearcher indexSearcher = new IndexSearcher(reader);
        MyAnalyzer analyzer = analyzers.get(language);
        TermQuery query = new TermQuery(new Term(UUID, uuid));
        ScoreDoc[] sDocs = indexSearcher.search(query, 1).scoreDocs;
        if (sDocs.length > 0) {
            Document doc = indexSearcher.doc(sDocs[0].doc);
            doc.add(new TextField(BODY, Tokenizer.tokenize(description, analyzer), Field.Store.YES));
            IndexManager.updateDocumentDescription(Paths.get(indexFolder), new File(stopWords), language, doc);
        }
    }

    /**
     * Rimuove una lista di documenti dall'indice
     *
     * @param uuids lista degli id univoci dei documenti
     * @param language lingua
     */
    public void removeDocuments(List<String> uuids, String language) {
        String indexFolder = getIndexFolder(language);
        String stopWords = getStopWordPath(language);
        IndexManager.removeDocuments(Paths.get(indexFolder), new File(stopWords), uuids, language);
    }

    private String getStopWordPath(String language) {
        return IndexManager.getStopWordPath(structurePath, language);
    }

    private String getIndexFolder(String language) {
        return IndexManager.getIndexFolder(structurePath, language);
    }

    private String getStructurePath() {
        return IndexManager.getStructurePath(structurePath);
    }

    /**
     * Rimuove un nodo dalla struttura di classificazione e tutti i documeti
     * associati al path
     *
     * @param path percorso del nodo da rimuovere
     */
    public void removeNode(Object[] path) {
        root.removeChild(path, 0);
        GuiUtils.storeXml(NodeData.getDocument(root), getStructurePath());
    }

    /**
     * Aggiunge un nuovo nodo alla struttura di classificazione
     *
     * @param path percorso da aggiungere
     */
    public void addNewNode(Object[] path) {
        root.addChild(path, 0);
        GuiUtils.storeXml(NodeData.getDocument(root), getStructurePath());

    }

    /**
     * Salva la struttura di classificazione su un file xml
     *
     * @param document Documento dove salvare la struttura
     */
    public void storeXml(org.jdom2.Document document) {
        GuiUtils.storeXml(document, getStructurePath());
    }

}