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

import org.thesemproject.opensem.gui.utils.GuiUtils;
import org.thesemproject.opensem.gui.LogGui;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

/**
 * Classe per la gestione di tutti gli accessi (in scrittura e non solo)
 * all'indice di lucene L'indice di lucene è utilizzato per classificare e per
 * indicizzare i contenuti dell'istruzione
 *
 * Il sistema non gestice un solo indice ma gestisce n indici, uno per lingua,
 * memorizzati come sottocartelle nella cartella di struttura. Per ogni indice è
 * definito un set di stop words specifico (memorizzata nella cartella stopwords
 * denotro nella cartella di struttura) e una specifica sequenza di analizzatori
 * sintattici dipendenti dalla lingua
 */
public class IndexManager {

    /**
     * Identifica il nome del field che deve contenere il testo del docuemnto da
     * indicizzare
     */
    public static final String BODY = "body";

    /**
     * Identifica il nome del field che deve contenere lo stato del docuemnto
     * indicizzato
     */
    public static final String STATUS = "status";

    /**
     * Stato documento attivo
     */
    public static final String ACTIVE = "1000";

    /**
     * Stato documento non attivo
     */
    public static final String CANCELLED = "2000";

    /**
     * Identifica il nome del field che deve contenere l'ID univoco (UUID) del
     * documento indicizzato Lucene non assegna un id univoco ad ogni documento
     * indicizzato. Per questo l'id univoco deve essere gestito extrasistema
     */
    public static final String UUID = "uuid";

    /**
     * Identifica il nome del field che deve contenere l'ID della categoria di
     * primo livello dove è classificato il documento
     */
    public static final String LEVEL_1 = "level1";

    /**
     * Identifica il nome del field che deve contenere l'ID della categoria di
     * secondo livello dove è classificato il documento
     */
    public static final String LEVEL_2 = "level2";

    /**
     * Identifica il nome del field che deve contenere l'ID della categoria di
     * terzo livello dove è classificato il documento
     */
    public static final String LEVEL_3 = "level3";

    /**
     * Identifica il nome del field che deve contenere l'ID della categoria di
     * quarto livello dove è classificato il documento
     */
    public static final String LEVEL_4 = "level4";

    /**
     * Nome del field che contiene il nome della categoria di primo livello su
     * cui è classificato il documento
     */
    public static final String LEVEL1_NAME = "level1Name";

    /**
     * Nome del field che contiene il nome della categoria di secondo livello su
     * cui è classificato il documento
     */
    public static final String LEVEL2_NAME = "level2Name";

    /**
     * Nome del field che contiene il nome della categoria di terzo livello su
     * cui è classificato il documento
     */
    public static final String LEVEL3_NAME = "level3Name";

    /**
     * Nome del field che contiene il nome della categoria di quarto livello su
     * cui è classificato il documento
     */
    public static final String LEVEL4_NAME = "level4Name";

    /**
     * Ritorna un tipo field, stored, not tokenized e indexed
     *
     * @return field type utilizzabili per chiavi o elementi che non si vogliono
     * tokenizzare
     */
    public static FieldType getNotTokenizedFieldType() {
        FieldType keywordFieldType = new FieldType();
        keywordFieldType.setStored(true);
        keywordFieldType.setTokenized(false);
        keywordFieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        return keywordFieldType;
    }

    /**
     * Aggiunge un documento all'indice
     *
     * @param structurePath path dove è memorizzato il sistema di indici
     * @param text testo da indicizzare (da usare come training set per la
     * classificazione)
     * @param path percorso di classificazione (massimo 4 livelli non
     * necessariamente popolato)
     * @param language lingua del testo
     * @param factor fattore di istruzione (quanti documenti uguali devono
     * essere inseriti nell'indice per istruirlo)
     * @param tokenize a true se si vuole che il testo venga tokenizzato
     * @throws IOException Eccezione di lettura indice
     * @throws FileNotFoundException file non trovato
     * @throws Exception
     */
    public static void addToIndex(String structurePath, String text, Object[] path, String language, int factor, boolean tokenize) throws IOException, FileNotFoundException, Exception {
        File fPath = new File(structurePath);
        if (fPath.exists() && fPath.isDirectory()) {
            String indexDir = getIndexFolder(fPath, language);
            String fileStop = getStopWordPath(fPath, language);
            File fIndex = new File(indexDir);
            if (!fIndex.exists()) {
                fIndex.mkdirs();
            }
            File fFileStop = new File(fileStop);
            if (!fFileStop.exists()) {
                fFileStop.createNewFile();
            }
            Path iDir = Paths.get(fIndex.getAbsolutePath());
            addToIndex(iDir, text, path, fFileStop, language, factor, tokenize);
        }
    }

    private static void addToIndex(Path indexDir, String text, Object[] path, File fStop, String language, int factor, boolean tokenize) throws IOException, FileNotFoundException, Exception {
        try {
            IndexWriter indexWriter = getIndexWriter(indexDir, fStop, language);
            if (factor <= 0) {
                factor = 1;
            }
            String body = tokenize ? Tokenizer.tokenize(text.toLowerCase().trim(), indexWriter.getAnalyzer()) : text;
            FieldType ft = getNotTokenizedFieldType();
            for (int count = 0; count < factor; count++) {
                Document doc = new Document();
                doc.add(new TextField(BODY, body, Field.Store.YES));
                doc.add(new StringField(STATUS, ACTIVE, Field.Store.YES));
                doc.add(new Field(UUID, java.util.UUID.randomUUID().toString(), ft));
                for (int i = 0; i < path.length; i++) {
                    if (i == 1) {
                        doc.add(new StringField(LEVEL_1, path[i].toString().hashCode() + "", Field.Store.YES));
                        doc.add(new StringField(LEVEL1_NAME, path[i].toString(), Field.Store.YES));
                    }
                    if (i == 2) {
                        doc.add(new StringField(LEVEL_2, path[i].toString().hashCode() + "", Field.Store.YES));
                        doc.add(new StringField(LEVEL2_NAME, path[i].toString(), Field.Store.YES));
                    }
                    if (i == 3) {
                        doc.add(new StringField(LEVEL_3, path[i].toString().hashCode() + "", Field.Store.YES));
                        doc.add(new StringField(LEVEL3_NAME, path[i].toString(), Field.Store.YES));
                    }
                    if (i == 4) {
                        doc.add(new StringField(LEVEL_4, path[i].toString().hashCode() + "", Field.Store.YES));
                        doc.add(new StringField(LEVEL4_NAME, path[i].toString(), Field.Store.YES));
                    }
                }
                indexWriter.addDocument(doc);
            }
            indexWriter.commit();
            indexWriter.flush();
            LogGui.info("Close index...");
            indexWriter.close();
        } catch (Exception e) {
            LogGui.printException(e);
        }
        LogGui.info("Index written");
    }

    /**
     * Ritorna l'indexwriter corretto per accedere all'indice ottimizzando
     * l'indice in apertura. L'indexwriter viene aperto con il MyAnalizer
     * corretto per la lingua passata
     *
     * @param indexDir percorso dell'indice
     * @param fStop file di stop words
     * @param language lingua dell'indice
     * @return IndexWriter
     * @throws Exception
     */
    public static IndexWriter getIndexWriter(Path indexDir, File fStop, String language) throws Exception {
        return getIndexWriter(indexDir, fStop, language, true);
    }

    /**
     * Ritorna l'indexwriter corretto per accedere all'indice L'indexwriter
     * viene aperto con il MyAnalizer corretto per la lingua passata
     *
     * @param indexDir percorso dell'indice
     * @param fStop file di stop words
     * @param language lingua
     * @param optimize true se l'apertura deve ottimizzare il file
     * @return IndexWriter
     * @throws Exception
     */
    public static IndexWriter getIndexWriter(Path indexDir, File fStop, String language, boolean optimize) throws Exception {
        return getIndexWriter(indexDir, fStop, language, optimize, IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
    }

    /**
     * Ritorna l'indexwriter corretto per accedere all'indice. L'indexwriter
     * viene aperto con il MyAnalizer corretto per la lingua passata
     *
     * @param indexDir percorso dell'indice
     * @param fStop file di stop words
     * @param language lingua
     * @param optimize true se all'apertura deve ottimizzare l'indice
     * @param openMode specifica se l'indice deve essere aperto in append o in
     * create
     * @return IndexWriter
     * @throws Exception
     */
    public static IndexWriter getIndexWriter(Path indexDir, File fStop, String language, boolean optimize, IndexWriterConfig.OpenMode openMode) throws Exception {
        MyAnalyzer analyzer = getAnalyzer(fStop, language);
        return getIndexWriter(indexDir, optimize, openMode, analyzer);
    }

    /**
     * Ritorna l'indexwriter corretto per accedere all'indice.
     *
     * @param indexDir cartella indice
     * @param optimize true se si vuole ottimizzare
     * @param openMode indice aperto in append o in create
     * @param analyzer analyzer da utilizzare
     * @return IndexWriter
     * @throws Exception
     */
    public static IndexWriter getIndexWriter(Path indexDir, boolean optimize, IndexWriterConfig.OpenMode openMode, Analyzer analyzer) throws Exception {
        Directory fsDir = FSDirectory.open(indexDir);
        IndexWriterConfig iwConf = new IndexWriterConfig(analyzer);
        iwConf.setOpenMode(openMode);
        if (optimize) {
            iwConf.setIndexDeletionPolicy(new KeepLastIndexDeletionPolicy());
            iwConf.setUseCompoundFile(true);
        }
        return new IndexWriter(fsDir, iwConf);
    }

    /**
     * Costruisce un indice di istruzione del classificatore a partire da un
     * excel strutturato Ci si aspetta di trovare in colonna A il livello 0, in
     * B il livello 1, in C il livello 2, il D il livello 3 in E il testo da
     * usare come elemento di istruzione
     *
     * @param structurePath cartella della struttura
     * @param trainingExcel file excel di istruzione
     * @param fStop file delle stopwords
     * @param language lingua
     * @param useCategoryName a true se si vuole istruire le categorie anche con
     * il loro nome
     * @throws Exception
     * @throws FileNotFoundException
     */
    public static void buildIndex(String structurePath, File trainingExcel, File fStop, String language, boolean useCategoryName) throws Exception, FileNotFoundException {
        File fPath = new File(structurePath);
        if (!fPath.exists()) {
            fPath.mkdirs();
        }
        if (fPath.exists() && fPath.isDirectory()) {
            String indexDir = getIndexFolder(fPath, language);
            String fileStop = getStopWordPath(fPath, language);
            File fIndex = new File(indexDir);
            if (!fIndex.exists()) {
                fIndex.mkdirs();
            }
            Set<String> stopWords = new HashSet<>();
            File fFileStop = new File(fileStop);
            if (!fFileStop.exists()) {
                fFileStop.createNewFile();
            } else {
                readStopWords(fFileStop, stopWords);

            }
            readStopWords(fStop, stopWords);
            if (stopWords.size() > 0) {
                fFileStop.delete();
                storeStopWords(fPath, language, new ArrayList(stopWords));
            }
            Path iDir = Paths.get(fIndex.getAbsolutePath());
            buildIndex(iDir, trainingExcel, fFileStop, language, useCategoryName);
        }
    }

    private static void readStopWords(File fFileStop, Object stopWords) throws FileNotFoundException, UnsupportedEncodingException, IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fFileStop), "UTF-8"));
        String stop;
        while ((stop = br.readLine()) != null) {
            int spazio = stop.indexOf(" ");
            if (spazio > -1) {
                stop = stop.substring(0, spazio).trim();
            }
            if (stop.length() > 0) {
                if (stopWords instanceof Set) {
                    ((Set) stopWords).add(stop);
                } else if (stopWords instanceof CharArraySet) {
                    ((CharArraySet) stopWords).add(stop);
                }
            }
        }
        br.close();
    }

    /**
     * Memorizza una lista di stop words di una lingua e ritorna l'analizzatore
     * sintattico aggiornato
     *
     * @param structurePath percorso della struttura
     * @param language lingua
     * @param stopWords lista di stopwords
     * @return analizzatore sintattico aggiornato
     */
    public static MyAnalyzer storeStopWords(File structurePath, String language, List<String> stopWords) {
        try {
            String fileName = getStopWordPath(structurePath, language);
            File f = GuiUtils.writeCSV(fileName, stopWords);
            return IndexManager.getAnalyzer(f, language);
        } catch (Exception ex) {
            LogGui.printException(ex);
        }
        return null;
    }

    private static void buildIndex(Path indexDir, File trainingExcel, File fStop, String language, boolean useCategoryName) throws Exception, FileNotFoundException {
        IndexWriter indexWriter = getIndexWriter(indexDir, fStop, language);
        FileInputStream fis;
        try {
            fis = new FileInputStream(trainingExcel);
            Workbook workbook = new XSSFWorkbook(fis);
            int numberOfSheets = workbook.getNumberOfSheets();
            FieldType ft = getNotTokenizedFieldType();
            for (int i = 0; i < numberOfSheets; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                int count = 1;
                for (Row row : sheet) {
                    Document d = new Document();
                    d.add(new Field(UUID, java.util.UUID.randomUUID().toString(), ft));
                    d.add(new StringField(STATUS, ACTIVE, Field.Store.YES));
                    Cell level1 = row.getCell(0);
                    if (level1 != null) {
                        d.add(new StringField(LEVEL_1, level1.getStringCellValue().hashCode() + "", Field.Store.YES));
                        d.add(new StringField(LEVEL1_NAME, level1.getStringCellValue(), Field.Store.YES));
                        Cell level2 = row.getCell(1);
                        if (level2 != null) {
                            d.add(new StringField(LEVEL_2, level2.getStringCellValue().hashCode() + "", Field.Store.YES));
                            d.add(new StringField(LEVEL2_NAME, level2.getStringCellValue(), Field.Store.YES));
                        }
                        Cell level3 = row.getCell(2);
                        if (level3 != null) {
                            d.add(new StringField(LEVEL_3, level3.getStringCellValue().hashCode() + "", Field.Store.YES));
                            d.add(new StringField(LEVEL3_NAME, level3.getStringCellValue(), Field.Store.YES));
                        }
                        Cell level4 = row.getCell(3);
                        if (level4 != null) {
                            d.add(new StringField(LEVEL_4, level4.getStringCellValue().hashCode() + "", Field.Store.YES));
                            d.add(new StringField(LEVEL4_NAME, level4.getStringCellValue(), Field.Store.YES));
                        }
                        Cell textCell = row.getCell(4);
                        if (textCell != null) {
                            if (textCell.getCellType() == Cell.CELL_TYPE_STRING) {
                                String text = textCell.getStringCellValue().toLowerCase();
                                d.add(new TextField(BODY, Tokenizer.tokenize(text, indexWriter.getAnalyzer()), Field.Store.YES));
                                indexWriter.addDocument(d);
                                if (useCategoryName) {
                                    checkForCat(d, indexWriter, (MyAnalyzer) indexWriter.getAnalyzer(), ft);
                                }
                            }
                        }
                    }
                    if (count++ % 100 == 0) {
                        LogGui.info("Commit... " + count);
                        indexWriter.commit();
                    }
                }
            }
            fis.close();

        } catch (Exception e) {
            LogGui.printException(e);
        }
        indexWriter.commit();
        indexWriter.flush();
        LogGui.info("Close index...");
        indexWriter.close();
        LogGui.info("Index written");
    }

    /**
     * Ritorna l'analizzatore sintattico corretto per la lingua passata
     *
     * @param fStop file delle stopwords
     * @param language lingua
     * @return analizzatore sintattico
     * @throws IOException
     */
    public static MyAnalyzer getAnalyzer(File fStop, String language) throws IOException {
        CharArraySet stopwords = new CharArraySet(1, true);
        stopwords.addAll(MyAnalyzer.getDefaultStopSet(language));
        if (fStop.exists()) {
            readStopWords(fStop, stopwords);
        }
        if (stopwords.size() > 0) {
            return new MyAnalyzer(language, stopwords);
        } else {
            return new MyAnalyzer(language);
        }
    }

    private static void checkForCat(Document d, IndexWriter indexWriter, MyAnalyzer analyzer, FieldType ft) throws Exception {
        String l1 = d.get(LEVEL_1);
        String l1n = d.get(LEVEL1_NAME);
        String l2 = d.get(LEVEL_2);
        String l2n = d.get(LEVEL2_NAME);
        String l3 = d.get(LEVEL_3);
        String l3n = d.get(LEVEL3_NAME);
        String l4 = d.get(LEVEL_4);
        String l4n = d.get(LEVEL4_NAME);
        Document dCat = new Document();
        dCat.add(new StringField(STATUS, ACTIVE, Field.Store.YES));
        dCat.add(new Field(UUID, java.util.UUID.randomUUID().toString(), ft));
        dCat.add(new StringField(LEVEL_1, l1, Field.Store.YES));
        dCat.add(new StringField(LEVEL1_NAME, l1n, Field.Store.YES));
        dCat.add(new TextField(BODY, l1n, Field.Store.YES));
        indexWriter.addDocument(dCat);
        if (l2 != null) {
            dCat = new Document();
            dCat.add(new StringField(STATUS, ACTIVE, Field.Store.YES));
            dCat.add(new Field(UUID, java.util.UUID.randomUUID().toString(), ft));
            dCat.add(new StringField(LEVEL_1, l1, Field.Store.YES));
            dCat.add(new StringField(LEVEL1_NAME, l1n, Field.Store.YES));
            dCat.add(new StringField(LEVEL_2, l2, Field.Store.YES));
            dCat.add(new StringField(LEVEL2_NAME, l2n, Field.Store.YES));
            dCat.add(new TextField(BODY, l2n, Field.Store.YES));
            indexWriter.addDocument(dCat);
        }
        if (l3 != null) {
            dCat = new Document();
            dCat.add(new StringField(STATUS, ACTIVE, Field.Store.YES));
            dCat.add(new Field(UUID, java.util.UUID.randomUUID().toString(), ft));
            dCat.add(new StringField(LEVEL_1, l1, Field.Store.YES));
            dCat.add(new StringField(LEVEL_2, l2, Field.Store.YES));
            dCat.add(new StringField(LEVEL_3, l3, Field.Store.YES));
            dCat.add(new TextField(BODY, l3n, Field.Store.YES));
            dCat.add(new StringField(LEVEL1_NAME, l1n, Field.Store.YES));
            dCat.add(new StringField(LEVEL2_NAME, l2n, Field.Store.YES));
            dCat.add(new StringField(LEVEL3_NAME, l3n, Field.Store.YES));
            indexWriter.addDocument(dCat);
        }

        if (l4 != null) {
            dCat = new Document();
            dCat.add(new StringField(STATUS, ACTIVE, Field.Store.YES));
            dCat.add(new Field(UUID, java.util.UUID.randomUUID().toString(), ft));
            dCat.add(new StringField(LEVEL_1, l1, Field.Store.YES));
            dCat.add(new StringField(LEVEL_2, l2, Field.Store.YES));
            dCat.add(new StringField(LEVEL_3, l3, Field.Store.YES));
            dCat.add(new StringField(LEVEL_4, l4, Field.Store.YES));
            dCat.add(new TextField(BODY, l4n, Field.Store.YES));
            dCat.add(new StringField(LEVEL1_NAME, l1n, Field.Store.YES));
            dCat.add(new StringField(LEVEL2_NAME, l2n, Field.Store.YES));
            dCat.add(new StringField(LEVEL3_NAME, l3n, Field.Store.YES));
            dCat.add(new StringField(LEVEL4_NAME, l4n, Field.Store.YES));
            indexWriter.addDocument(dCat);
        }
    }

    /**
     * Elimina l'istruzione di un sottoramo (o di una foglia)
     *
     * @param structurePath percorso dove sono memorizzati gli indici
     * @param path percorso di classificazione da deistruire
     * @throws Exception
     */
    public static void removeFromIndex(String structurePath, Object[] path) throws Exception {
        File fStructurePath = new File(structurePath); //Verifica che esistano le structure path
        if (fStructurePath.exists() && fStructurePath.isDirectory()) {
            //Verifica se la struttura è ok.
            //TODO: Qui legge il file della struttura (in futuro)
            boolean ret = true;

            for (String language : MyAnalyzer.languages) {
                String indexFolder = getIndexFolder(fStructurePath, language);
                String stopWords = getStopWordPath(fStructurePath, language);
                File fIndex = new File(indexFolder);
                File fStop = new File(stopWords);
                if (!fStop.exists()) {
                    fStop.createNewFile();
                }
                if (fIndex.exists()) {
                    removeFromIndex(Paths.get(fIndex.getAbsolutePath()), path, fStop, language);
                }
            }
        }

    }

    private static void removeFromIndex(Path indexDir, Object[] path, File fStop, String language) throws Exception {
        TermQuery query = null;
        for (int i = 0; i < path.length; i++) {
            if (i == 1) {
                query = new TermQuery(new Term(LEVEL_1, path[i].toString().hashCode() + ""));
            }
            if (i == 2) {
                query = new TermQuery(new Term(LEVEL_2, path[i].toString().hashCode() + ""));
            }
            if (i == 3) {
                query = new TermQuery(new Term(LEVEL_3, path[i].toString().hashCode() + ""));
            }
            if (i == 4) {
                query = new TermQuery(new Term(LEVEL_4, path[i].toString().hashCode() + ""));
            }
        }
        try {

            IndexWriter iw = getIndexWriter(indexDir, fStop, language);
            iw.deleteDocuments(query);
            iw.commit();
            iw.forceMergeDeletes(true);
            iw.flush();
            iw.close();
        } catch (Exception e) {
            LogGui.printException(e);
        }
        LogGui.info("Index written");

    }

    /**
     * Reindicizza il contenuto dell'indice di istruzione imponendo l'UUID come
     * field non tokenizzato
     *
     * @param reindexDoc Lista dei documenti di lucene da reindicizzare
     * @param indexDir path dell'indice
     * @param fStop file di stopwords
     * @param language lingua
     * @throws Exception
     */
    public static void reindex(List<Document> reindexDoc, Path indexDir, File fStop, String language) throws Exception {
        IndexWriter indexWriter = getIndexWriter(indexDir, fStop, language, false, IndexWriterConfig.OpenMode.CREATE);
        FieldType ft = getNotTokenizedFieldType();
        try {
            int count = 0;
            for (Document d : reindexDoc) {
                d.removeField(UUID);
                d.add(new Field(UUID, java.util.UUID.randomUUID().toString(), ft));
                indexWriter.addDocument(d);
                if (count++ % 100 == 0) {
                    LogGui.info("Commit... " + count);
                    indexWriter.commit();
                }
            }
            indexWriter.commit();
            indexWriter.flush();
            LogGui.info("Close index...");
            indexWriter.close();
            LogGui.info("Index written");
        } catch (Exception e) {
            LogGui.printException(e);
        }
    }

    /**
     * Rimuove un documento dall'indice
     *
     * @param indexDir directory dell'indice
     * @param fStop stopwords
     * @param record riga da cancellare
     * @param language lingua del documento
     */
    public static void removeDocument(Path indexDir, File fStop, String record, String language) {
        TermQuery query = new TermQuery(new Term(UUID, record));
        try {

            IndexWriter iw = getIndexWriter(indexDir, fStop, language);
            iw.deleteDocuments(query);
            iw.commit();
            //iw.forceMergeDeletes(true);
            //iw.flush();
            iw.close();
        } catch (Exception e) {
            LogGui.printException(e);
        }
        LogGui.info("Index written");
    }

    /**
     * Modifica la descrizione di un documento
     *
     * @param indexDir percorso indice
     * @param fStop file di stopwords
     * @param language lingua
     * @param d Documento modificato
     */
    public static void updateDocumentDescription(Path indexDir, File fStop, String language, Document d) {
        TermQuery query = new TermQuery(new Term(UUID, d.get(UUID)));
        try {
            IndexWriter iw = getIndexWriter(indexDir, fStop, language);
            iw.deleteDocuments(query);
            iw.addDocument(d);
            iw.commit();
            iw.forceMergeDeletes(true);
            iw.flush();
            iw.close();
        } catch (Exception e) {
            LogGui.printException(e);
        }
        LogGui.info("Index written");

    }

    static void removeDocuments(Path indexDir, File fStop, List<String> toRemove, String language) {
        TermQuery[] queries = new TermQuery[toRemove.size()];
        int i = 0;
        for (String record : toRemove) {
            TermQuery query = new TermQuery(new Term(UUID, record));
            queries[i++] = query;
        }
        try {
            IndexWriter iw = getIndexWriter(indexDir, fStop, language);
            iw.deleteDocuments(queries);
            iw.commit();
            iw.forceMergeDeletes(true);
            iw.flush();
            iw.close();
        } catch (Exception e) {
            LogGui.printException(e);
        }
        LogGui.info("Index written");
    }

    /**
     * Ritorna il percorso del file di stopword
     *
     * @param structurePath percorso della struttura
     * @param language lingua
     * @return percorso completo del file di stopword
     */
    public static String getStopWordPath(File structurePath, String language) {
        if (structurePath == null) {
            return null;
        }
        File path = new File(structurePath.getAbsolutePath() + "/stopwords");
        if (!path.exists()) {
            path.mkdirs();
        }
        return path.getAbsolutePath() + "/stop_" + language + ".txt";
    }

    /**
     * Ritorna il percorso dell'indice di lucene per una certa lingua
     *
     * @param structurePath percorso della struttura
     * @param language lingua
     * @return percorso dell'indice
     */
    public static String getIndexFolder(File structurePath, String language) {
        if (structurePath == null) {
            return null;
        }
        return structurePath.getAbsolutePath() + "/" + language;
    }

    /**
     * Ritorna il percorso del file che contiene la struttura di classificazione
     *
     * @param structurePath percorso della struttura
     * @return file della struttura di classificazione
     */
    public static String getStructurePath(File structurePath) {
        if (structurePath == null) {
            return null;
        }
        return structurePath.getAbsolutePath() + "/structure.xml";
    }

}
