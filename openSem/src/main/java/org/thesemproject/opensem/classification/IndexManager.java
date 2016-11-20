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
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.thesemproject.opensem.utils.Log;
import org.thesemproject.opensem.utils.StringUtils;

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
     * Testo originale...
     */
    public static final String TEXT = "text";

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
     * Identifica il nome del field che deve contenere l'ID della categoria di
     * quinto livello dove è classificato il documento
     *
     * @since 1.1
     */
    public static final String LEVEL_5 = "level5";

    /**
     * Identifica il nome del field che deve contenere l'ID della categoria di
     * sesto livello dove è classificato il documento
     *
     * @since 1.1
     */
    public static final String LEVEL_6 = "level6";

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
     * Nome del field che contiene il nome della categoria di quinto livello su
     * cui è classificato il documento
     */
    public static final String LEVEL5_NAME = "level5Name";

    /**
     * Nome del field che contiene il nome della categoria di sesto livello su
     * cui è classificato il documento
     */
    public static final String LEVEL6_NAME = "level6Name";

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
     * @param path percorso di classificazione (massimo 6 livelli non
     * necessariamente popolato). Il livello 0 è la root e viene ignorato. I
     * livelli vanno quindi al relativo indice
     * @param language lingua del testo
     * @param factor fattore di istruzione (quanti documenti uguali devono
     * essere inseriti nell'indice per istruirlo)
     * @param tokenize a true se si vuole che il testo venga tokenizzato
     * @throws IOException Eccezione di input/output Eccezione di lettura indice
     * @throws FileNotFoundException file non trovato
     * @throws Exception Eccezione eccezione
     */
    public synchronized static void addToIndex(String structurePath, String text, Object[] path, String language, int factor, boolean tokenize) throws IOException, FileNotFoundException, Exception {
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
                doc.add(new TextField(TEXT, text, Field.Store.YES));
                doc.add(new StringField(STATUS, ACTIVE, Field.Store.YES));
                doc.add(new Field(UUID, java.util.UUID.randomUUID().toString(), ft));
                for (int i = 0; i < path.length; i++) { //IL path 0 del threepath è la root quindi non ci inderessa
                    if (path[i] == null) {
                        continue;
                    }
                    if (i == 1) {
                        doc.add(new StringField(LEVEL_1, NodeData.getNodeCodeForFilter(path[i].toString()) + "", Field.Store.YES));
                        doc.add(new StringField(LEVEL1_NAME, path[i].toString(), Field.Store.YES));
                    }
                    if (i == 2) {
                        doc.add(new StringField(LEVEL_2, NodeData.getNodeCodeForFilter(path[i].toString()) + "", Field.Store.YES));
                        doc.add(new StringField(NodeData.getNodeCodeForFilter(path[1].toString()) + "", NodeData.getNodeCodeForFilter(path[2].toString()) + "", Field.Store.YES));
                        doc.add(new StringField(LEVEL2_NAME, path[i].toString(), Field.Store.YES));
                    }
                    if (i == 3) {
                        doc.add(new StringField(LEVEL_3, NodeData.getNodeCodeForFilter(path[i].toString()) + "", Field.Store.YES));
                        doc.add(new StringField(NodeData.getNodeCodeForFilter(path[2].toString()) + "", NodeData.getNodeCodeForFilter(path[3].toString()) + "", Field.Store.YES));
                        doc.add(new StringField(LEVEL3_NAME, path[i].toString(), Field.Store.YES));
                    }
                    if (i == 4) {
                        doc.add(new StringField(LEVEL_4, NodeData.getNodeCodeForFilter(path[i].toString()) + "", Field.Store.YES));
                        doc.add(new StringField(NodeData.getNodeCodeForFilter(path[3].toString()) + "", NodeData.getNodeCodeForFilter(path[4].toString()) + "", Field.Store.YES));
                        doc.add(new StringField(LEVEL4_NAME, path[i].toString(), Field.Store.YES));
                    }
                    if (i == 5) {
                        doc.add(new StringField(LEVEL_5, NodeData.getNodeCodeForFilter(path[i].toString()) + "", Field.Store.YES));
                        doc.add(new StringField(NodeData.getNodeCodeForFilter(path[4].toString()) + "", NodeData.getNodeCodeForFilter(path[5].toString()) + "", Field.Store.YES));
                        doc.add(new StringField(LEVEL5_NAME, path[i].toString(), Field.Store.YES));
                    }
                    if (i == 6) {

                        doc.add(new StringField(LEVEL_6, NodeData.getNodeCodeForFilter(path[i].toString()) + "", Field.Store.YES));
                        doc.add(new StringField(NodeData.getNodeCodeForFilter(path[5].toString()) + "", NodeData.getNodeCodeForFilter(path[6].toString()) + "", Field.Store.YES));
                        doc.add(new StringField(LEVEL6_NAME, path[i].toString(), Field.Store.YES));
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
     * @throws Exception Eccezione eccezione
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
     * @throws Exception Eccezione eccezione
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
     * @throws Exception Eccezione eccezione
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
     * @throws Exception Eccezione eccezione
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
     * @throws Exception Eccezione eccezione
     * @throws FileNotFoundException file non trovato
     */
    public static void buildIndex(String structurePath, File trainingExcel, File fStop, String language, boolean useCategoryName) throws Exception, FileNotFoundException {
        LogGui.info("Build path...");
        File fPath = new File(structurePath);
        if (!fPath.exists()) {
            fPath.mkdirs();
        }
        if (fPath.exists() && fPath.isDirectory()) {
            String indexDir = getIndexFolder(fPath, language);
            LogGui.info("Index folder: " + indexDir);
            String fileStop = getStopWordPath(fPath, language);
            LogGui.info("File stop: " + fileStop);
            File fIndex = new File(indexDir);
            if (!fIndex.exists()) {
                fIndex.mkdirs();
            }
            LogGui.info("Start read stopwords...");
            Set<String> stopWords = new HashSet<>();
            File fFileStop = new File(fileStop);
            if (!fFileStop.exists()) {
                LogGui.info("Stopwords not exists...");
                fFileStop.createNewFile();
            } else {
                LogGui.info("Read old stopwords...");
                try {
                    readStopWords(fFileStop, stopWords);
                } catch (Exception e) {
                    Log.printStackTrace(e);
                }

            }
            LogGui.info("Read new stopwords...");
            try {
                readStopWords(fStop, stopWords);
            } catch (Exception e) {
                Log.printStackTrace(e);
            }
            if (stopWords.size() > 0) {
                fFileStop.delete();
                storeStopWords(fPath, language, new ArrayList(stopWords));
            }
            Path iDir = Paths.get(fIndex.getAbsolutePath());
            LogGui.info("Call build index");
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
            LogGui.info("Open Excel...");
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
                        if (level1.getCellType() != HSSFCell.CELL_TYPE_STRING) {
                            LogGui.info("Riga " + level1 + " contiene un valore numerico anziché stringa. Scartata");
                            continue;
                        }
                        level1.setCellValue(StringUtils.firstUpper(level1.getStringCellValue()));
                        d.add(new StringField(LEVEL_1, NodeData.getNodeCodeForFilter(level1.getStringCellValue()) + "", Field.Store.YES));
                        d.add(new StringField(LEVEL1_NAME, level1.getStringCellValue(), Field.Store.YES));
                        Cell level2 = row.getCell(1);
                        if (level2.getCellType() != HSSFCell.CELL_TYPE_STRING) {
                            LogGui.info("Riga " + level2 + " contiene un valore numerico anziché stringa. Scartata");
                            continue;
                        }
                        if (level2 != null && level2.getStringCellValue().trim().length() > 0) {
                            level2.setCellValue(StringUtils.firstUpper(level2.getStringCellValue()));
                            d.add(new StringField(LEVEL_2, NodeData.getNodeCodeForFilter(level2.getStringCellValue()) + "", Field.Store.YES));
                            d.add(new StringField(NodeData.getNodeCodeForFilter(level1.getStringCellValue()) + "", NodeData.getNodeCodeForFilter(level2.getStringCellValue()) + "", Field.Store.YES));
                            d.add(new StringField(LEVEL2_NAME, level2.getStringCellValue(), Field.Store.YES));

                            Cell level3 = row.getCell(2);
                            if (level3 != null && level3.getStringCellValue().trim().length() > 0) {
                                level3.setCellValue(StringUtils.firstUpper(level3.getStringCellValue()));
                                d.add(new StringField(LEVEL_3, NodeData.getNodeCodeForFilter(level3.getStringCellValue()) + "", Field.Store.YES));
                                d.add(new StringField(NodeData.getNodeCodeForFilter(level2.getStringCellValue()) + "", NodeData.getNodeCodeForFilter(level3.getStringCellValue()) + "", Field.Store.YES));
                                d.add(new StringField(LEVEL3_NAME, level3.getStringCellValue(), Field.Store.YES));

                                Cell level4 = row.getCell(3);
                                if (level4 != null && level4.getStringCellValue().trim().length() > 0) {
                                    level4.setCellValue(StringUtils.firstUpper(level4.getStringCellValue()));
                                    d.add(new StringField(LEVEL_4, NodeData.getNodeCodeForFilter(level4.getStringCellValue()) + "", Field.Store.YES));
                                    d.add(new StringField(NodeData.getNodeCodeForFilter(level3.getStringCellValue()) + "", NodeData.getNodeCodeForFilter(level4.getStringCellValue()) + "", Field.Store.YES));
                                    d.add(new StringField(LEVEL4_NAME, level4.getStringCellValue(), Field.Store.YES));

                                    //New versione 1.1 i livelli aumentano 
                                    Cell level5 = row.getCell(4);
                                    if (level5 != null && level5.getStringCellValue().trim().length() > 0) {
                                        level5.setCellValue(StringUtils.firstUpper(level5.getStringCellValue()));
                                        d.add(new StringField(LEVEL_5, NodeData.getNodeCodeForFilter(level5.getStringCellValue()) + "", Field.Store.YES));
                                        d.add(new StringField(NodeData.getNodeCodeForFilter(level4.getStringCellValue()) + "", NodeData.getNodeCodeForFilter(level5.getStringCellValue()) + "", Field.Store.YES));
                                        d.add(new StringField(LEVEL5_NAME, level5.getStringCellValue(), Field.Store.YES));
                                        Cell level6 = row.getCell(5);
                                        if (level6 != null && level6.getStringCellValue().trim().length() > 0) {
                                            level6.setCellValue(StringUtils.firstUpper(level6.getStringCellValue()));
                                            d.add(new StringField(LEVEL_6, NodeData.getNodeCodeForFilter(level6.getStringCellValue()) + "", Field.Store.YES));
                                            d.add(new StringField(NodeData.getNodeCodeForFilter(level5.getStringCellValue()) + "", NodeData.getNodeCodeForFilter(level6.getStringCellValue()) + "", Field.Store.YES));
                                            d.add(new StringField(LEVEL6_NAME, level6.getStringCellValue(), Field.Store.YES));
                                        }
                                    }
                                }
                            }
                        }

                        // New versione 1.1 il testo adesso è nella 7ma colonna
                        Cell textCell = row.getCell(6);
                        if (textCell != null) {
                            if (textCell.getCellType() == Cell.CELL_TYPE_STRING) {
                                String text = textCell.getStringCellValue().toLowerCase();
                                d.add(new TextField(BODY, Tokenizer.tokenize(text, indexWriter.getAnalyzer()), Field.Store.YES));
                                d.add(new StringField(TEXT, text, Field.Store.YES));
                                if (text.length() > 10 && text.length() < 600) {
                                    indexWriter.addDocument(d);
                                    if (useCategoryName) {
                                        checkForCat(d, indexWriter, (MyAnalyzer) indexWriter.getAnalyzer(), ft);
                                    }
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
     * @throws IOException Eccezione di input/output
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

    private static Document getDocument(String text, Document d, IndexWriter indexWriter) {
        try {
            String body = Tokenizer.tokenize(text, indexWriter.getAnalyzer());

            if (body != null && body.length() > 0) {
                d.add(new TextField(BODY, body, Field.Store.YES));
                d.add(new StringField(TEXT, text, Field.Store.YES));
                return d;
            }
            return null;
        } catch (Exception e) {
            return null;
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
        // Versione 1.1
        String l5 = d.get(LEVEL_5);
        String l5n = d.get(LEVEL5_NAME);
        String l6 = d.get(LEVEL_6);
        String l6n = d.get(LEVEL6_NAME);
        Document dCat = new Document();
        dCat.add(new StringField(STATUS, ACTIVE, Field.Store.YES));
        dCat.add(new Field(UUID, java.util.UUID.randomUUID().toString(), ft));
        dCat.add(new StringField(LEVEL_1, l1, Field.Store.YES));
        dCat.add(new StringField(LEVEL1_NAME, l1n, Field.Store.YES));
        if (l2 == null) {
            dCat = getDocument(l1n, dCat, indexWriter);
            if (dCat != null) {
                indexWriter.addDocument(dCat);
            }
        } else if (l3 == null) {
            dCat.add(new StringField(LEVEL_1, l1, Field.Store.YES));
            dCat.add(new StringField(LEVEL1_NAME, l1n, Field.Store.YES));
            dCat.add(new StringField(LEVEL_2, l2, Field.Store.YES));
            dCat.add(new StringField(l1, l2, Field.Store.YES));
            dCat.add(new StringField(LEVEL2_NAME, l2n, Field.Store.YES));
            dCat = getDocument(l2n, dCat, indexWriter);
            if (dCat != null) {
                indexWriter.addDocument(dCat);
            }
        } else {
            if (l4 == null) {
                dCat.add(new StringField(LEVEL_1, l1, Field.Store.YES));
                dCat.add(new StringField(LEVEL_2, l2, Field.Store.YES));
                dCat.add(new StringField(LEVEL_3, l3, Field.Store.YES));
                dCat.add(new StringField(l1, l2, Field.Store.YES));
                dCat.add(new StringField(l2, l3, Field.Store.YES));
                dCat.add(new StringField(LEVEL1_NAME, l1n, Field.Store.YES));
                dCat.add(new StringField(LEVEL2_NAME, l2n, Field.Store.YES));
                dCat.add(new StringField(LEVEL3_NAME, l3n, Field.Store.YES));
                dCat = getDocument(l3n, dCat, indexWriter);
                if (dCat != null) {
                    indexWriter.addDocument(dCat);
                }
            } else if (l5 == null) {
                dCat.add(new StringField(LEVEL_1, l1, Field.Store.YES));
                dCat.add(new StringField(LEVEL_2, l2, Field.Store.YES));
                dCat.add(new StringField(LEVEL_3, l3, Field.Store.YES));
                dCat.add(new StringField(LEVEL_4, l4, Field.Store.YES));
                dCat.add(new StringField(l1, l2, Field.Store.YES));
                dCat.add(new StringField(l2, l3, Field.Store.YES));
                dCat.add(new StringField(l3, l4, Field.Store.YES));
                dCat.add(new StringField(LEVEL1_NAME, l1n, Field.Store.YES));
                dCat.add(new StringField(LEVEL2_NAME, l2n, Field.Store.YES));
                dCat.add(new StringField(LEVEL3_NAME, l3n, Field.Store.YES));
                dCat.add(new StringField(LEVEL4_NAME, l4n, Field.Store.YES));
                dCat = getDocument(l4n, dCat, indexWriter);
                if (dCat != null) {
                    indexWriter.addDocument(dCat);
                }
            } else {
                if (l6 == null) {
                    dCat.add(new StringField(LEVEL_1, l1, Field.Store.YES));
                    dCat.add(new StringField(LEVEL_2, l2, Field.Store.YES));
                    dCat.add(new StringField(LEVEL_3, l3, Field.Store.YES));
                    dCat.add(new StringField(LEVEL_4, l4, Field.Store.YES));
                    dCat.add(new StringField(LEVEL_5, l5, Field.Store.YES));
                    dCat.add(new StringField(l1, l2, Field.Store.YES));
                    dCat.add(new StringField(l2, l3, Field.Store.YES));
                    dCat.add(new StringField(l3, l4, Field.Store.YES));
                    dCat.add(new StringField(l4, l5, Field.Store.YES));
                    dCat.add(new StringField(LEVEL1_NAME, l1n, Field.Store.YES));
                    dCat.add(new StringField(LEVEL2_NAME, l2n, Field.Store.YES));
                    dCat.add(new StringField(LEVEL3_NAME, l3n, Field.Store.YES));
                    dCat.add(new StringField(LEVEL4_NAME, l4n, Field.Store.YES));
                    dCat.add(new StringField(LEVEL5_NAME, l5n, Field.Store.YES));
                    dCat = getDocument(l5n, dCat, indexWriter);
                    if (dCat != null) {
                        indexWriter.addDocument(dCat);
                    }
                } else {
                    dCat.add(new StringField(LEVEL_1, l1, Field.Store.YES));
                    dCat.add(new StringField(LEVEL_2, l2, Field.Store.YES));
                    dCat.add(new StringField(LEVEL_3, l3, Field.Store.YES));
                    dCat.add(new StringField(LEVEL_4, l4, Field.Store.YES));
                    dCat.add(new StringField(LEVEL_5, l5, Field.Store.YES));
                    dCat.add(new StringField(LEVEL_6, l6, Field.Store.YES));
                    dCat.add(new StringField(l1, l2, Field.Store.YES));
                    dCat.add(new StringField(l2, l3, Field.Store.YES));
                    dCat.add(new StringField(l3, l4, Field.Store.YES));
                    dCat.add(new StringField(l4, l5, Field.Store.YES));
                    dCat.add(new StringField(l5, l6, Field.Store.YES));
                    dCat.add(new StringField(LEVEL1_NAME, l1n, Field.Store.YES));
                    dCat.add(new StringField(LEVEL2_NAME, l2n, Field.Store.YES));
                    dCat.add(new StringField(LEVEL3_NAME, l3n, Field.Store.YES));
                    dCat.add(new StringField(LEVEL4_NAME, l4n, Field.Store.YES));
                    dCat.add(new StringField(LEVEL5_NAME, l5n, Field.Store.YES));
                    dCat.add(new StringField(LEVEL6_NAME, l6n, Field.Store.YES));
                    dCat = getDocument(l6n, dCat, indexWriter);
                    if (dCat != null) {
                        indexWriter.addDocument(dCat);
                    }
                }
            }
        }
    }

    /**
     * Elimina l'istruzione di un sottoramo (o di una foglia)
     *
     * @param structurePath percorso dove sono memorizzati gli indici
     * @param path percorso di classificazione da deistruire
     * @throws Exception Eccezione eccezione
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
                query = new TermQuery(new Term(LEVEL_1, NodeData.getNodeCodeForFilter(path[i].toString()) + ""));
            }
            if (i == 2) {
                query = new TermQuery(new Term(LEVEL_2, NodeData.getNodeCodeForFilter(path[i].toString()) + ""));
            }
            if (i == 3) {
                query = new TermQuery(new Term(LEVEL_3, NodeData.getNodeCodeForFilter(path[i].toString()) + ""));
            }
            if (i == 4) {
                query = new TermQuery(new Term(LEVEL_4, NodeData.getNodeCodeForFilter(path[i].toString()) + ""));
            }
            if (i == 5) {
                query = new TermQuery(new Term(LEVEL_5, NodeData.getNodeCodeForFilter(path[i].toString()) + ""));
            }
            if (i == 6) {
                query = new TermQuery(new Term(LEVEL_6, NodeData.getNodeCodeForFilter(path[i].toString()) + ""));
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
     * @deprecated
     *
     * @param reindexDoc Lista dei documenti di lucene da reindicizzare
     * @param indexDir path dell'indice
     * @param fStop file di stopwords
     * @param language lingua
     * @throws Exception Eccezione eccezione
     */
    public static void reindexOld(List<Document> reindexDoc, Path indexDir, File fStop, String language) throws Exception {
        LogGui.info("Garbaging prima della reindicizzazione...");
        System.gc();
        LogGui.printMemorySummary();
        String pathOrigin = indexDir.toFile().getAbsolutePath();
        LogGui.info("Backup...");
        String pathBackup = pathOrigin + ".bck." + System.currentTimeMillis();
        File origin = new File(pathOrigin);
        File backup = new File(pathBackup);
        origin.renameTo(backup);
        File newFile = new File(pathOrigin);
        LogGui.info("Creo cartella...");
        newFile.mkdirs();
        LogGui.info("Apro l'indice...");
        IndexWriter indexWriter = getIndexWriter(indexDir, fStop, language, false, IndexWriterConfig.OpenMode.CREATE);
        FieldType ft = getNotTokenizedFieldType();
        Analyzer analyzer = indexWriter.getAnalyzer();
        try {
            int count = 0;
            for (Document d : reindexDoc) {
                reindexDoc(d, ft, analyzer, indexWriter);
                if (count++ % 100 == 0) {
                    LogGui.info("Reindex Commit... " + count);
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
     * Reindicizza il contenuto dell'indice di istruzione imponendo l'UUID come
     * field non tokenizzato
     *
     * @param d document da reindicizzare
     * @param ft field type
     * @param analyzer analizzatore
     * @param indexWriter indexwriter
     * @throws Exception Eccezione eccezione
     */
    public static void reindexDoc(Document d, FieldType ft, Analyzer analyzer, IndexWriter indexWriter) throws Exception {
        d.removeField(UUID);
        String text = d.get(TEXT);
        d.removeField(BODY);
        // d.removeField(TEXT);
        d.add(new Field(UUID, java.util.UUID.randomUUID().toString(), ft));
        d.add(new TextField(BODY, Tokenizer.tokenize(text, analyzer), Field.Store.YES));
        //d.add(new StringField(TEXT, text, Field.Store.YES));
        String l1 = d.get(LEVEL1_NAME);
        if (l1 != null) {
            d.removeField(LEVEL_1);
            d.add(new StringField(LEVEL_1, NodeData.getNodeCodeForFilter(l1) + "", Field.Store.YES));
        }
        String l2 = d.get(LEVEL2_NAME);
        if (l2 != null) {
            d.removeField(LEVEL_2);
            d.add(new StringField(LEVEL_2, NodeData.getNodeCodeForFilter(l2) + "", Field.Store.YES));
            d.add(new StringField(NodeData.getNodeCodeForFilter(l1) + "", NodeData.getNodeCodeForFilter(l2) + "", Field.Store.YES));
        }
        String l3 = d.get(LEVEL3_NAME);
        if (l3 != null) {
            d.removeField(LEVEL_3);
            d.add(new StringField(LEVEL_3, NodeData.getNodeCodeForFilter(l3) + "", Field.Store.YES));
            d.add(new StringField(NodeData.getNodeCodeForFilter(l2) + "", NodeData.getNodeCodeForFilter(l3) + "", Field.Store.YES));
        }
        String l4 = d.get(LEVEL4_NAME);
        if (l4 != null) {
            d.removeField(LEVEL_4);
            d.add(new StringField(LEVEL_4, NodeData.getNodeCodeForFilter(l4) + "", Field.Store.YES));
            d.add(new StringField(NodeData.getNodeCodeForFilter(l3) + "", NodeData.getNodeCodeForFilter(l4) + "", Field.Store.YES));
        }
        String l5 = d.get(LEVEL5_NAME);
        if (l5 != null) {
            d.removeField(LEVEL_5);
            d.add(new StringField(LEVEL_5, NodeData.getNodeCodeForFilter(l5) + "", Field.Store.YES));
            d.add(new StringField(NodeData.getNodeCodeForFilter(l4) + "", NodeData.getNodeCodeForFilter(l5) + "", Field.Store.YES));
        }
        String l6 = d.get(LEVEL6_NAME);
        if (l6 != null) {
            d.removeField(LEVEL_6);
            d.add(new StringField(LEVEL_6, NodeData.getNodeCodeForFilter(l6) + "", Field.Store.YES));
            d.add(new StringField(NodeData.getNodeCodeForFilter(l5) + "", NodeData.getNodeCodeForFilter(l6) + "", Field.Store.YES));
        }
        indexWriter.addDocument(d);
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
