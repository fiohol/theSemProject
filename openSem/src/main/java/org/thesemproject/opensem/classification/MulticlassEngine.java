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
import java.util.StringTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.classification.KNearestNeighborClassifier;
import org.apache.lucene.classification.SimpleNaiveBayesClassifier;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Bits;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import static org.thesemproject.opensem.classification.IndexManager.getNotTokenizedFieldType;
import static org.thesemproject.opensem.classification.IndexManager.reindexDoc;
import static org.thesemproject.opensem.classification.IndexManager.getIndexWriter;
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
            boolean ret = true;

            int level = 1;
            if (root != null) {
                level = root.getStartLevel();
            }
            this.structurePath = fStructurePath;

            String structueFileName = getStructurePath();
            File fStructure = new File(structueFileName);
            LogGui.info("Apro fil file di struttura: " + fStructure.getAbsolutePath());
            boolean fileExists = fStructure.exists();

            if (fileExists) {
                try {
                    org.jdom2.Document document = GuiUtils.readXml(fStructure.getAbsolutePath());
                    root = NodeData.getNodeData(document, intern);
                } catch (Exception e) {
                    LogGui.printException(e);
                }
            } else {
                root = new NodeData(level, k, intern); //Root
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
                            ret = ret && init(indexFolder, stopWords, language, level, k, reindex);
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

    private boolean init(String index, String stop, String language, int startLevel, int k, boolean needReindex) {
        try {

            // List<Document> reindexDoc = new ArrayList<>();
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
            LogGui.info("Init language: " + language);
            LogGui.info("Documents: " + maxdoc);
            LogGui.info("Start training NaiveBayes and KNN");
            LogGui.info("Read documents from idx to build the tree...");
            LogGui.info("Train root...");
            root.train(ar, analyzer, language);
            LogGui.info("Read all example dataset");
            HashSet categories = new HashSet<>();
            Bits liveDocs = MultiFields.getLiveDocs(reader);
            LogGui.info("Apro l'indice...");
            IndexWriter indexWriter = null;
            File origin = new File(index);
            String pathOrigin = origin.getAbsolutePath();
            String pathNew = pathOrigin + ".new." + System.currentTimeMillis();
            String pathBackup = pathOrigin + ".bck." + System.currentTimeMillis();
            if (needReindex) {
                LogGui.info("Creo il nuovo indice in: " + pathNew + " leggendo dall'indice in : " + pathOrigin);
                File fNew = new File(pathNew);
                fNew.mkdirs();
                indexWriter = getIndexWriter(Paths.get(pathNew), new File(stop), language, false, IndexWriterConfig.OpenMode.CREATE);
            }
            FieldType ft = getNotTokenizedFieldType();
            for (int i = 0; i < maxdoc; i++) {
                if (liveDocs != null && !liveDocs.get(i)) {
                    continue;
                }
                Document doc = ar.document(i);
                if (doc.get(IndexManager.UUID) == null) {
                    doc.add(new StringField(UUID, java.util.UUID.randomUUID().toString(), Field.Store.YES));
                }
                if (needReindex) {
                    reindexDoc(doc, ft, analyzer, indexWriter);
                    if (i % 100 == 0) {
                        LogGui.info("Reindex Commit... " + i);
                        indexWriter.commit();
                    }
                } else {
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
                                    String level5 = (String) intern.intern(doc.get(IndexManager.LEVEL5_NAME));
                                    if (level5 != null) {
                                        if (!categories.contains(level5)) { //Nuova categoria di livello 5
                                            NodeData p1 = root.getNode(level1);
                                            if (p1 != null) {
                                                NodeData p2 = p1.getNode(level2);
                                                if (p2 != null) {
                                                    NodeData p3 = p2.getNode(level3);
                                                    if (p3 != null) {
                                                        NodeData p4 = p3.getNode(level4);
                                                        addNode(ar, analyzer, p4, categories, level5, k, language);
                                                    }
                                                }
                                            }
                                        }
                                        String level6 = (String) intern.intern(doc.get(IndexManager.LEVEL6_NAME));
                                        if (level6 != null) {
                                            if (!categories.contains(level6)) { //Nuova categoria di livello 6
                                                NodeData p1 = root.getNode(level1);
                                                if (p1 != null) {
                                                    NodeData p2 = p1.getNode(level2);
                                                    if (p2 != null) {
                                                        NodeData p3 = p2.getNode(level3);
                                                        if (p3 != null) {
                                                            NodeData p4 = p3.getNode(level4);
                                                            if (p4 != null) {
                                                                NodeData p5 = p4.getNode(level5);
                                                                addNode(ar, analyzer, p5, categories, level6, k, language);
                                                            }
                                                        }
                                                    }
                                                }
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
            }

            if (needReindex) {
                indexWriter.commit();
                indexWriter.flush();
                LogGui.info("Close index...");
                indexWriter.close();
                closeReader(reader);
                LogGui.info("Index written");
                LogGui.info("Rename index...");
                File backup = new File(pathBackup);
                File originName = new File(origin.getAbsolutePath());
                origin.renameTo(backup);
                File newFile = new File(pathNew);
                newFile.renameTo(originName);
                LogGui.info("Re-Init...");
                return init(index, stop, language, startLevel, k, false);
            } else {
                LogGui.info("End training");
                this.cats.addAll(categories);
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
            File fIndex = new File(index);
            if (fIndex.exists()) {
                IndexReader reader = DirectoryReader.open(getFolderDir(index));
                final LeafReader ar = SlowCompositeReaderWrapper.wrap(reader);
                Bits liveDocs = MultiFields.getLiveDocs(reader);
                final int maxdoc = reader.maxDoc();
                for (int i = 0; i < maxdoc; i++) {
                    if (liveDocs != null && !liveDocs.get(i)) {
                        continue;
                    }
                    Document doc = ar.document(i);
                    String[] row = new String[10];
                    row[9] = "";
                    row[0] = doc.get(IndexManager.UUID);
                    row[1] = doc.get(IndexManager.BODY);
                    row[2] = doc.get(IndexManager.TEXT);
                    String level1 = (String) intern.intern(doc.get(IndexManager.LEVEL1_NAME));
                    row[3] = level1;
                    if (level1 != null) {
                        String level2 = (String) intern.intern(doc.get(IndexManager.LEVEL2_NAME));
                        if (level2 != null) {
                            row[4] = level2;
                            String level3 = (String) intern.intern(doc.get(IndexManager.LEVEL3_NAME));
                            if (level3 != null) {
                                row[5] = level3;
                                String level4 = (String) intern.intern(doc.get(IndexManager.LEVEL4_NAME));
                                if (level4 != null) {
                                    row[6] = level4;
                                    String level5 = (String) intern.intern(doc.get(IndexManager.LEVEL5_NAME));
                                    if (level5 != null) {
                                        row[7] = level5;
                                        String level6 = (String) intern.intern(doc.get(IndexManager.LEVEL6_NAME));
                                        if (level6 != null) {
                                            row[8] = level6;
                                        }
                                    }
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
            }
        } catch (Exception e) {
            LogGui.printException(e);
        }
        return rows;
    }

    /**
     * Popola un excel con il contenuto dell'indice
     *
     * @since 1.2
     * @param language lingua indice
     * @param sheetResults excel da popolare
     * @param c1 valori della colonna kpi1
     * @param c2 valori della colonna kpi2
     */
    public void getDocumentsExcel(String language, SXSSFSheet sheetResults, HashMap<String, String> c1, HashMap<String, String> c2) {
        try {
            int rownum = 1;
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
                SXSSFRow row = sheetResults.createRow(rownum++);
                String text = doc.get(IndexManager.TEXT);
                if (text == null) {
                    text = "";
                }
                row.createCell(6).setCellValue(text);
                row.createCell(7).setCellValue(doc.get(IndexManager.BODY));
                String id = doc.get(IndexManager.UUID);
                String c1v = c1.get(id);
                String c2v = c2.get(id);
                if (c1v != null) {
                    row.createCell(8).setCellValue(c1v);
                }
                if (c2v != null) {
                    row.createCell(9).setCellValue(c2v);
                }
                String level1 = (String) intern.intern(doc.get(IndexManager.LEVEL1_NAME));
                row.createCell(0).setCellValue(level1);
                if (level1 != null) {
                    String level2 = (String) intern.intern(doc.get(IndexManager.LEVEL2_NAME));
                    if (level2 != null) {
                        row.createCell(1).setCellValue(level2);
                        String level3 = (String) intern.intern(doc.get(IndexManager.LEVEL3_NAME));
                        if (level3 != null) {
                            row.createCell(2).setCellValue(level3);
                            String level4 = (String) intern.intern(doc.get(IndexManager.LEVEL4_NAME));
                            if (level4 != null) {
                                row.createCell(3).setCellValue(level4);
                                String level5 = (String) intern.intern(doc.get(IndexManager.LEVEL5_NAME));
                                if (level5 != null) {
                                    row.createCell(4).setCellValue(level5);
                                    String level6 = (String) intern.intern(doc.get(IndexManager.LEVEL6_NAME));
                                    if (level6 != null) {
                                        row.createCell(5).setCellValue(level6);
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
            reader.close();
        } catch (Exception e) {
            LogGui.printException(e);
        }
    }

    /**
     * Tokenizza un testo utilizzando l'analizzatore sintattico e le stop words
     * di lingua
     *
     * @param text testo da tokenizzare
     * @param language lingua del testo da toeknizzare
     * @return testo tokenizzato
     */
    public String tokenize(String text, String language) {
        return tokenize(text, language, -1);
    }

    /**
     * Tokenizza un testo utilizzando l'analizzatore sintattico e le stop words
     * di lingua
     *
     * Il servizio è fatto attraverso Tokenizer.tokenize(text, analyzer, tokens)
     *
     * @since 1.2
     *
     * @param text testo da tokenizzare
     * @param language lingua del testo da toeknizzare
     * @param tokens numero di token da preservare
     * @return testo tokenizzato
     */
    public String tokenize(String text, String language, int tokens) {
        if (isInit) {
            String ret;
            try {
                MyAnalyzer analyzer = getAnalyzer(language);
                if (tokens != -1) {
                    ret = Tokenizer.tokenize(text, analyzer, tokens);
                } else {
                    ret = Tokenizer.tokenize(text, analyzer);
                }
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
     * @throws IOException Eccezione di input/output
     */
    public MyAnalyzer getAnalyzer(String language) throws IOException {
        return getAnalyzer(language, false);
    }

    /**
     * Ripulisce gli analizzatori sintattici
     *
     * @since 1.4.1
     *
     */
    public void resetAnalyzers() {
        analyzers.clear();
    }

    /**
     * Ritorna l'analizzatore sintattico per una lingua
     *
     * @since 1.4.1
     *
     * @param language lingua dell'analizzatore
     * @param forceRefresh true se si vuole forzare il refresh
     * @return Analizzatore Sintattico
     * @throws IOException Eccezione di input/output
     */
    public MyAnalyzer getAnalyzer(String language, boolean forceRefresh) throws IOException {
        MyAnalyzer analyzer = analyzers.get(language);
        if (analyzer == null || forceRefresh) {
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
     * @param language lingua del testo
     * @return lista dei percorsi di classificazione (un documento può essere
     * classificato su più categorie)
     */
    public List<ClassificationPath> bayesClassify(String text, String language) {
        if (!isInit) {
            return null;
        }
        try {
            return classifyOnRoot(tokenize(text, language), root, false, language);
        } catch (Exception e) {
            LogGui.printException(e);
        }
        return null;
    }

    /**
     * Classifica un testo con il classificatore KNN di lucene
     *
     * @param text testo da analizzre
     * @param language lingua del testo
     * @return percorso di classificazione
     */
    public ClassificationPath knnClassify(String text, String language) {
        if (!isInit) {
            return null;
        }
        try {
            List<ClassificationPath> path = classifyOnRoot(tokenize(text, language), root, true, language);
            if (path.size() > 0) {
                return path.get(0);
            } else {
                return null;
            }
        } catch (Exception e) {
            LogGui.printException(e);
        }
        return null;
    }

    private List<ClassificationPath> classifyOnSubNode(String text, NodeData nd, int level, ClassificationPath classPath, String language) throws IOException {
        List<ClassificationPath> ret = new ArrayList<>();
        if (!isInit) {
            return null;
        }
        if (level < 1) {
            ret.add(classPath);
            return ret;
        }
        if (classPath == null) {
            return null;
        }
        List<ClassificationResult<BytesRef>> resultNdList = null;
        if (classPath.getTechnology().equals(ClassificationPath.BAYES)) {
            SimpleNaiveBayesClassifier snbc = nd.getClassifier(language);
            if (snbc != null) {
                resultNdList = snbc.getClasses(text);
                //  resultNdList = list.get(0);
                //Modifica 1.7.2 Controllo lo score dei fratelli e al limite faccio un branch di classificaizone

            }

        } else if (classPath.getTechnology().equals(ClassificationPath.KNN)) {
            KNearestNeighborClassifier knnc = nd.getKnn(language);
            if (knnc != null) {
                if (resultNdList == null) {
                    resultNdList = new ArrayList<>();
                }
                resultNdList.add(knnc.assignClass(text));
            }
        }
        if (resultNdList != null && resultNdList.size() > 0) {
            double score1 = resultNdList.get(0).getScore();
            ClonableClassificationPath ccp = new ClonableClassificationPath(classPath);
            ClassificationPath cp1 = ccp.clone();

            cp1.addResult(nd.getNameFromId(resultNdList.get(0).getAssignedClass().utf8ToString()), score1, level);
            NodeData child = nd.getNode(cp1.getNodeName(level));
            if (child != null) {
                if (child.hasChildren()) {
                    ret.addAll(classifyOnSubNode(text, child, level + 1, cp1, language));
                } else {
                    ret.add(cp1);
                }
            }
            if (resultNdList.size() > 1) {
                // double childrenSize = nd.getChildrenNames().size(); //Numero di figli
                double childrenSize = resultNdList.size();
                for (int j = 1; j < resultNdList.size(); j++) {
                    double score2 = resultNdList.get(j).getScore();
                    ClassificationPath cp2 = ccp.clone();
                    if (canClassifyOnSubtree(score1, score2, childrenSize)) {
                        cp2.addResult(nd.getNameFromId(resultNdList.get(j).getAssignedClass().utf8ToString()), score2, level);
                        NodeData child2 = nd.getNode(cp2.getNodeName(level));
                        if (child2 != null) {
                            if (child2.hasChildren()) {
                                ret.addAll(classifyOnSubNode(text, child2, level + 1, cp2, language));
                            } else {
                                ret.add(cp2);
                            }
                        }
                    }
                }
            }
        } else {
            ret.add(classPath);
        }
        return ret;

    }

    private List<ClassificationPath> classifyOnRoot(String text, NodeData root, boolean knn, String language) throws IOException {
        try {
            List<ClassificationPath> results = new ArrayList<>();
            if (text.length() == 0) {
                return results;
            }
            int level = root.getStartLevel() - 1;
            if (!knn) {
                ClassificationPath bChoice1 = new ClassificationPath(ClassificationPath.BAYES);
                ClassificationPath bChoice2 = new ClassificationPath(ClassificationPath.BAYES);
                //Classifica bayes
                SimpleNaiveBayesClassifier snbc = root.getClassifier(language);
                if (snbc != null) {
                    List<ClassificationResult<BytesRef>> resultNdList;
                    try {
                        resultNdList = snbc.getClasses(text);
                    } catch (Exception e) {
                        return results;
                    }
                    bChoice1.addResult(root.getNameFromId(resultNdList.get(0).getAssignedClass().utf8ToString()), resultNdList.get(0).getScore(), level);
                    NodeData child1 = root.getNode(bChoice1.getNodeName(level));
                    if (child1 != null) {
                        if (level != 0) { //Sto classificando a root ma con un level != 0 (cioè parto da un livello più basso nell'albero
                            NodeData.findPath(bChoice1, child1, level);
                        }
                        if (child1.hasChildren()) {
                            results.addAll(classifyOnSubNode(text, child1, level + 1, bChoice1, language));
                        }
                    } else {
                        results.add(bChoice1);
                    }
                    if (resultNdList.size() > 1) {
                        double score2 = resultNdList.get(1).getScore();
                        double score1 = resultNdList.get(0).getScore();
                        double childrenSize = root.getChildrenNames().size(); //Numero di figli
                        if (canClassifyOnSubtree(score1, score2, childrenSize)) {
                            bChoice2.addResult(root.getNameFromId(resultNdList.get(1).getAssignedClass().utf8ToString()), score2, level);
                            NodeData child2 = root.getNode(bChoice2.getNodeName(level));
                            if (child2 != null) {
                                if (level != 0) { //Sto classificando a root ma con un level != 0 (cioè parto da un livello più basso nell'albero
                                    NodeData.findPath(bChoice2, child2, level);
                                }
                                if (child2.hasChildren()) {
                                    results.addAll(classifyOnSubNode(text, child2, level + 1, bChoice2, language));
                                }
                            } else {
                                results.add(bChoice2);
                            }
                        }
                    }
                }
            } else {
                ClassificationPath kChoice1 = new ClassificationPath(ClassificationPath.KNN);
                KNearestNeighborClassifier knnc = root.getKnn(language);
                if (knnc != null) {
                    ClassificationResult<BytesRef> res;
                    try {
                        res = knnc.assignClass(text);
                    } catch (Exception exception) {
                        return results;
                    }
                    kChoice1.addResult(root.getNameFromId(res.getAssignedClass().utf8ToString()), res.getScore(), level);
                    NodeData child1 = root.getNode(kChoice1.getNodeName(level));
                    if (child1 != null) {
                        if (level != 0) { //Sto classificando a root ma con un level != 0 (cioè parto da un livello più basso nell'albero
                            NodeData.findPath(kChoice1, child1, level);
                        }
                        if (child1.hasChildren()) {
                            results.addAll(classifyOnSubNode(text, child1, level + 1, kChoice1, language));
                        }
                    } else {
                        results.add(kChoice1);
                    }
                }
            }
            return results;
        } catch (Exception e) {
            LogGui.printException(e);
            return null;
        }
    }

    private boolean canClassifyOnSubtree(double score1, double score2, double childrenSize) {
        double realThreshold = 1 / childrenSize;
        return (((score2 >= realThreshold) && (score2*2.2 >= score1)) || (Math.abs(score2 - score1) < 0.1));
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
     * @throws Exception Eccezione eccezione
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

    private static final String[] puctuation = {"\t", "\r", "\n", ".", ":", ";", "\\", "/", "-", "_", ",", "[", "]", "(", ")", "{", "}", "@", "+", "*", "^", "?", "=", "&", "%", "$", "£", "\"", "!"};

    private static final String aRegex = "(à|á|â|ã|ä)";
    private static final String eRegex = "(è|é|ê|ë)";
    private static final String iRegex = "(ì|í|î|ï)";
    private static final String oRegex = "(ò|ó|ô|õ|ö)";
    private static final String uRegex = "(ù|ú|û|ü)";
    private static final String yRegex = "(ý|ÿ)";
    private static final String nRegex = "(ñ)";

    /**
     * Ritorna una regex dato un testo
     *
     * @param text testo
     * @param language lingua
     * @return regex
     */
    public String getPatterenFromText(String text, String language) {
        if (!isInit) {
            return "Sistema non inizializzato";
        }
        StringBuilder ret = new StringBuilder();
        text = text.toLowerCase();
        String toToken = text;
        for (String s : puctuation) {
            toToken = toToken.replace(s, " ");
        }
        String tokenized = tokenize(toToken, language, -1); //Tokenizza il testo

        String[] tokensSplit = tokenized.split(" ");
        int consumed = 0;
        StringTokenizer stText = new StringTokenizer(text, " "); //Spezza in parole il testo
        while (stText.hasMoreTokens()) {
            boolean found = false;
            String token = stText.nextToken();
            //Ho il primo token
            for (int i = consumed; i < tokensSplit.length; i++) {
                if (tokensSplit[i].length() == 0) {
                    continue;
                }
                if (token.equals(tokensSplit[i])) {
                    found = true;
                    if (ret.length() == 0) {
                        ret.append(token);
                    } else {
                        if (!ret.toString().endsWith("(.*)") && !ret.toString().endsWith("(\\s+)")) {
                            ret.append("(\\s+)");
                        }
                        ret.append(token);
                    }
                } else if (token.startsWith(tokensSplit[i])) {   //Ho trovato cosa è diventato
                    found = true;
                    if (ret.length() == 0) {
                        ret.append(tokensSplit[i]).append("(.*)");
                    } else {
                        if (!ret.toString().endsWith("(.*)") && !ret.toString().endsWith("(\\s+)")) {
                            ret.append("(\\s+)");
                        }

                        ret.append(tokensSplit[i]).append("(.*)");
                    }
                }
                if (found) {
                    consumed = i + 1;
                    break;
                }
            }
            if (!found) {
                if (ret.length() > 0) {
                    if (!ret.toString().endsWith("(.*)") && !ret.toString().endsWith("(\\s+)")) {
                        ret.append("(.*)");
                    }
                }
            }
        }
        String returnString = ret.toString();
        returnString = returnString.replaceAll(aRegex, "(a|" + aRegex + ")");
        returnString = returnString.replaceAll(eRegex, "(e|" + eRegex + ")");
        returnString = returnString.replaceAll(iRegex, "(i|" + iRegex + ")");
        returnString = returnString.replaceAll(oRegex, "(o|" + oRegex + ")");
        returnString = returnString.replaceAll(uRegex, "(u|" + uRegex + ")");
        returnString = returnString.replaceAll(yRegex, "(y|" + yRegex + ")");
        returnString = returnString.replaceAll(nRegex, "(n|" + nRegex + ")");
        return "\\b(" + returnString + ")\\b";
    }

}
