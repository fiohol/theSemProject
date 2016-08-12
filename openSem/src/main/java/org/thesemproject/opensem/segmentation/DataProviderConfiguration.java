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

import org.thesemproject.opensem.classification.IndexManager;
import org.thesemproject.opensem.enrichment.CSVFileParser;
import org.thesemproject.opensem.gui.LogGui;
import org.thesemproject.opensem.utils.DateUtils;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

/**
 * Rappresenta la configurazione di un dataprovider. Un dataprovider è un
 * contenitore di dati che può essere usato per creare tabelle da cui fare le
 * catture oppure per arricchire di valori una cattura.
 *
 * I dati del csv verranno indicizzati in un indice di lucene in modo da rendere
 * più agevole il matching
 */
public class DataProviderConfiguration implements Serializable{

    /**
     * Tipi di sorgente gestibili da dataprovider CSV UTF-8 oppure CSV ISO
     * (windows)
     */
    public static final String[] SOURCE_TYPE = {"File CSV (UTF-8)", "File CSV (ISO-8859-1)"}; // "Database"};

    /**
     * Parametro per identificare il separatore di linea del csv
     */
    public static final String CSV_LINE_SEPARATOR = "csvLineSeparator";

    /**
     * Parametro per identificare il carattere di escaping del csv
     */
    public static final String CSV_ESCAPE = "csvEscape";

    /**
     * Parametro per identificare se nella lettura deve essere saltata la prima
     * riga
     */
    public static final String SKIP_FIRST = "skipFirst";

    /**
     * Parametro per identificare il delimitatore
     */
    public static final String CSV_DELIMITER = "csvDelimiter";

    /**
     * Parametro per identificare il carattere di quoting
     */
    public static final String CSV_QUOTE = "csvQuote";

    /**
     * Parametro per identificare il nome del file
     */
    public static final String FILE_NAME = "fileName";

    private final String name;
    private final String storageLocation;
    private String type;
    private final Map<String, String> fields;
    private final BiMap<String, String> fieldsPositions;
    private final BiMap<String, String> fieldsTable;
    private final Map<String, String> configurationValues;
    private IndexReader reader;

    /**
     * Crea un data provider
     *
     * @param name nome del dataprovider
     * @param type tipo del dataprovider
     * @param storageLocation directory dove verrà creato l'indice di lucene
     */
    public DataProviderConfiguration(String name, String type, String storageLocation) {
        this.name = name;
        this.type = type;
        fields = new HashMap<>();
        fieldsPositions = HashBiMap.create();
        fieldsTable = HashBiMap.create();
        configurationValues = new HashMap<>();
        this.storageLocation = storageLocation;
        reader = null;
    }

    /**
     * Crea un data provider
     *
     * @param name nome del dataprovider
     * @param type tipo del dataprovider
     * @param fields elenco dei fields. Mappa {nomefield, tipofield}
     * @param fieldsPositions elenco delle posizioni dei field. Mappa
     * {nomefield, posizione field nella testata}
     * @param fieldsTable mappa tra i fields e le tabelle {nomefield, tabella}
     * @param configurationValues parametri di configurazione (chiave, valore)
     * @param storageLocation directory per l'indice di lucene
     */
    public DataProviderConfiguration(String name, String type, Map<String, String> fields, Map<String, String> fieldsPositions, Map<String, String> fieldsTable, Map<String, String> configurationValues, String storageLocation) {
        this.name = name;
        this.type = type;
        this.fields = new HashMap<>(fields);
        this.fieldsPositions = HashBiMap.create(fieldsPositions);
        this.fieldsTable = HashBiMap.create(fieldsTable);
        this.configurationValues = new HashMap<>(configurationValues);
        this.storageLocation = storageLocation;
        reader = null;
    }

    /**
     * Ritorna il nome del dataprovider
     *
     * @return nome del dataprovider
     */
    public String getName() {
        return name;
    }

    /**
     * Ritorna il tipo del dataprovider
     *
     * @return tipo del dataprovider
     */
    public String getType() {
        return type;
    }

    /**
     * Ritorna la mappa dei fields del dataprovider {nomefield, tipofield}
     *
     * @return mappa dei fields
     */
    public Map<String, String> getFields() {
        return fields;
    }

    /**
     * Ritorna i parametri di configurazione
     *
     * @return mappa {parametro, valore}
     */
    public Map<String, String> getConfigurationValues() {
        return configurationValues;
    }

    /**
     * Imposta la configurazione dei fields
     *
     * @param fields fields come mappa {nome,tipo}
     * @param fieldsPositions posizioni dei fields come mappa {nome, posizione}
     * @param fieldsTable relazione con le tabelle come mappa {nome, tabella}
     */
    public void setFields(Map<String, String> fields, Map<String, String> fieldsPositions, Map<String, String> fieldsTable) {
        this.fields.clear();
        this.fields.putAll(fields);
        this.fieldsPositions.putAll(fieldsPositions);
        this.fieldsTable.putAll(fieldsTable);
    }

    /**
     * Imposta i parametri di configurazione
     *
     * @param configurationValues mappa {nomeParametro, valore}
     */
    public void setConfiguration(Map<String, String> configurationValues) {
        this.configurationValues.clear();
        this.configurationValues.putAll(configurationValues);
    }

    /**
     * Aggiunge un field
     *
     * @param name nome del field
     * @param type tipo del field
     * @param position posizione del field nella testata
     * @param tableName nome della tabella in relazione con il field (può essere
     * nullo se il field non mappa nessuna tabella)
     */
    public void addField(String name, String type, String position, String tableName) {
        this.fields.put(name, type);
        this.fieldsPositions.put(name, position);
        this.fieldsTable.remove(name);
        if (tableName != null) {
            if (tableName.trim().length() > 0) {
                this.fieldsTable.put(name, tableName);
            }
        }
    }

    /**
     * Rimuove un field
     *
     * @param fieldName nome del field da rimuovere
     */
    public void removeField(String fieldName) {
        this.fields.remove(fieldName);
    }

    /**
     * Ritorna le posizioni di ogni field
     *
     * @return mappa {nomeField, posizione}
     */
    public Map<String, String> getFieldsPosition() {
        return fieldsPositions;
    }

    /**
     * Ritorna la relazione field -&gt; Tabella
     *
     * @return mappa {nomeField, nomeTabella}
     */
    public Map<String, String> getFieldsTable() {
        return fieldsTable;
    }

    /**
     * Aggiorna un parametro di configurazione
     *
     * @param key nome del parametro
     * @param value valore del parametro
     */
    public void updateConfiguration(String key, String value) {
        this.configurationValues.put(key, value);
    }

    /**
     * Ritorna l'elenco di field come lista di array di stringhe. Questo metodo
     * è utile per la visualizzazione di JTable nella GUI
     *
     * @return lista dei fields
     */
    public List<String[]> getFieldsRows() {
        List<String[]> ret = new ArrayList<>();
        AtomicInteger count = new AtomicInteger(1);
        fields.keySet().stream().map((field) -> {
            String[] line = new String[5];
            line[0] = String.valueOf(count.getAndIncrement());
            line[1] = field;
            line[2] = fields.get(field);
            line[3] = fieldsPositions.get(field);
            line[4] = fieldsTable.get(field);
            return line;
        }).forEach((line) -> {
            ret.add(line);
        });
        return ret;
    }

    /**
     * Imposta il tipo del field
     *
     * @param string tipo del field
     */
    public void setType(String string) {
        this.type = string;
    }

    /**
     * Rimuove tutti i fields
     */
    public void removeAllFields() {
        fields.clear();
        fieldsPositions.clear();
        fieldsTable.clear();
    }

    /**
     * In funzione dei parametri, indicizza (cancellando l'eventuale indice
     * esistente) i dati contenuti nel file csv in un indice di lucene,
     * memorizzando tutti i dati come non tokenizzati e non parsati. La
     * posizione di memorizzazione è una sottocartella con il nome del data
     * provider all'interno della cartella dataproviders della cartella di
     * struttura
     */
    public void burnToStorage() {
        closeIndex();
        File luceneFolder = getLuceneFolder();
        String encoding = getEncoding();
        Path iDir = Paths.get(luceneFolder.getAbsolutePath());
        try {
            IndexWriter indexWriter = IndexManager.getIndexWriter(iDir, true, IndexWriterConfig.OpenMode.CREATE, new WhitespaceAnalyzer());
            CSVParser parser = CSVFileParser.getParser(configurationValues.get(FILE_NAME), configurationValues.get(CSV_DELIMITER), configurationValues.get(CSV_QUOTE), configurationValues.get(CSV_ESCAPE), configurationValues.get(CSV_LINE_SEPARATOR), encoding);
            BiMap<String, String> invFields = fieldsPositions.inverse();
            int count = 0;
            for (CSVRecord csvRecord : parser) {
                count++;
                if ("true".equalsIgnoreCase(configurationValues.get(SKIP_FIRST)) && count == 1) {
                    continue;
                }
                int pos = 1;
                Document doc = new Document();
                for (String field : csvRecord) {
                    String fieldName = invFields.get(String.valueOf(pos));
                    String tmp = fields.get(fieldName);
                    if (tmp == null) continue;
                    if (tmp.equals("date")) {
                        field = DateUtils.parseString(field);
                        if (field == null) {
                            field = "";
                        }
                    }
                    doc.add(new StringField(fieldName, field, Field.Store.YES));
                    doc.add(new StringField(fieldName+"_lower", field.toLowerCase(), Field.Store.YES));
                    pos++;
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
        openIndex();
    }

    /**
     * Ritorna il tipo di enconding
     *
     * @return encoding del file
     */
    public String getEncoding() {
        return type.contains("UTF-8") ? "UTF-8" : "ISO-8859-1";
    }

    /**
     * Costruisce la posizione dove viene memorizzato l'indice di lucene
     *
     * @return percorso dove deve essere memorizzato l'indice
     */
    public File getLuceneFolder() {
        String luceneFolder = storageLocation + "/dataproviders/" + name;
        File lf = new File(luceneFolder);
        if (!lf.exists()) {
            lf.mkdirs();
        }
        return lf;
    }

    /**
     * Chiude l'indice di lucene (che viene aperto in lettura all'apertura del
     * data provider)
     */
    public void closeIndex() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException iOException) {
                LogGui.printException(iOException);
            }
        }
    }

    /**
     * Apre l'indice di lucene
     */
    public void openIndex() {
        closeIndex(); //Per sicurezza
        try {
            reader = DirectoryReader.open(getFolderDir());
        } catch (IOException ex) {
            LogGui.printException(ex);
        }
    }

    private Directory getFolderDir() throws IOException {
        RAMDirectory ret;
        try (FSDirectory dir = FSDirectory.open(Paths.get(getLuceneFolder().getAbsolutePath()))) {
            ret = new RAMDirectory(dir, null);

        }
        return ret;
    }

    /**
     * Cerca nell'indice di lucene
     *
     * @param query query di ricerca
     * @return primo Documento che ha matchato la ricerca
     * @throws IOException Eccezione di input/output
     */
    public Document search(Query query) throws IOException {
//        LogGui.info("Query: " + query.toString());
        IndexSearcher indexSearcher = new IndexSearcher(reader);
        ScoreDoc[] sDocs = indexSearcher.search(query, 1).scoreDocs;
        if (sDocs.length > 0) {
            int docId = sDocs[0].doc;
            return indexSearcher.doc(docId);
        }
        return null;
    }

    /**
     * Ritorna una collezione di stringhe come valori per popolare la tabella.
     * Viene usato sia dalla parte gui per far vedere i valori, sia in
     * inizializzazione del Segmenter per popolare fisicamente la tabella
     *
     * @param tableName nome della tabella
     * @return lista di valori
     */
    public Collection<? extends String> getValuesForTable(String tableName) {
        BiMap<String, String> invFields = fieldsTable.inverse();
        String field = invFields.get(tableName);
        List<String> ret = new ArrayList<>();
        if (field != null) {
            if (reader == null) {
                openIndex();
            }
            try {
                final LeafReader ar = SlowCompositeReaderWrapper.wrap(reader);
                final int maxdoc = reader.maxDoc();
                for (int i = 0; i < maxdoc; i++) {
                    Document doc = ar.document(i);
                    String val = doc.get(field);
                    if (val != null) {
                        if (val.trim().length() > 0) {
                            ret.add(val.trim().toLowerCase());
                        }
                    }
                }
            } catch (Exception e) {
                LogGui.printException(e);
            }
        }
        return ret;
    }

    /**
     * Ritorna il nome della tabella associata al field
     *
     * @param fieldName nome del field
     * @return nome della tabella (null se nessuna tabella è associata al field)
     */
    public String getTable(String fieldName) {
        return fieldsTable.get(fieldName);
    }

    /**
     * Rimuove una tabella (ovvero rimuove la relazione field-tabella)
     *
     * @param table nome della tabella da rimuovere
     */
    public void removeTable(String table) {
        for (String field : fields.keySet()) {
            String t = fieldsTable.get(field);
            if (t != null) {
                if (t.equals(table)) {
                    fieldsTable.remove(field);
                }
            }
        }
    }

    /**
     * Rinomina la tabella associata al field
     *
     * @param table tabella
     * @param newName nuovo nome
     */
    public void renameTable(String table, String newName) {
        for (String field : fields.keySet()) {
            String t = fieldsTable.get(field);
            if (t != null) {
                if (t.equals(table)) {
                    fieldsTable.put(field, newName);
                }
            }
        }
    }

}
