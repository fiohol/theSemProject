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

import org.thesemproject.opensem.segmentation.DataProviderConfiguration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jdom2.Element;

/**
 * Gestisce la rappresentazione grafica come nodo di un data provider
 */
public class DataProviderTreeNode extends ModelTreeNode {

    private DataProviderConfiguration dpConfiguration;

    /**
     * Istanzia il nodo
     *
     * @param nodeName nome del dataprovider
     * @param storageLocation posizione nello storage
     */
    public DataProviderTreeNode(String nodeName, String storageLocation) {
        super(nodeName, TYPE_DATA_PROVIDER_DEFINITION);
        dpConfiguration = new DataProviderConfiguration(nodeName, DataProviderConfiguration.SOURCE_TYPE[0], storageLocation);
        initDefaultCSV();

    }

    /**
     * Istanzia il nodo
     *
     * @param nodeName nome del dataprovider
     * @param configuration configurazione del dataprovider
     */
    public DataProviderTreeNode(String nodeName, DataProviderConfiguration configuration) {
        super(nodeName, TYPE_DATA_PROVIDER_DEFINITION);
        this.dpConfiguration = configuration;
        if (DataProviderConfiguration.SOURCE_TYPE[0].equals(dpConfiguration.getType())) {
            initDefaultCSV();
        }
    }

    private void initDefaultCSV() {
        setDelimiter(getDelimiter());
        setQuote(getQuote());
        setEscape(getEscape());
        setLineSeparator(getLineSeparator());
    }

    /**
     * Aggiunge la configurazione di un field
     *
     * @param fieldName nome del field
     * @param fieldType tipo del field
     * @param position posizione del field nella testata
     * @param tableName nome tabella (per le relazioni con il fileld)
     */
    public void addField(String fieldName, String fieldType, String position, String tableName) {
        dpConfiguration.addField(fieldName, fieldType, position, tableName);
    }

    /**
     * rimuove un field
     *
     * @param fieldName nome del field
     */
    public void removeField(String fieldName) {
        dpConfiguration.removeField(fieldName);
    }

    /**
     * Imposta i parametri
     *
     * @param parameters parametri
     */
    public void setParameters(Map<String, String> parameters) {
        dpConfiguration.setConfiguration(parameters);
    }

    /**
     * Imposta la configurazione
     *
     * @param configuration configurazione
     */
    public void setConfiguration(DataProviderConfiguration configuration) {
        this.dpConfiguration = configuration;
    }

    /**
     * Rappresentazione XML del nodo
     *
     * @return Elemento per il JDOM
     */
    public Element getXmlElement() {
        Element dataProvider = new Element("dp");
        dataProvider.setAttribute("n", getNodeName());
        if (dpConfiguration != null) {
            dataProvider.setAttribute("t", dpConfiguration.getType());
            Element fields = new Element("fs");
            Map<String, String> fMap = dpConfiguration.getFields();
            Map<String, String> fPos = dpConfiguration.getFieldsPosition();
            Map<String, String> fTab = dpConfiguration.getFieldsTable();
            fMap.keySet().stream().map((fieldName) -> {
                Element field = new Element("f");
                field.setAttribute("n", fieldName);
                field.setAttribute("t", fMap.get(fieldName));
                field.setAttribute("p", fPos.get(fieldName));
                String table = fTab.get(fieldName);
                if (table != null) {
                    field.setAttribute("tbl", table);
                }
                return field;
            }).forEach((field) -> {
                fields.addContent(field);
            });
            dataProvider.addContent(fields);

            Element cfgParam = new Element("c");
            Map<String, String> cfMap = dpConfiguration.getConfigurationValues();
            cfMap.keySet().stream().filter((paramName) -> (!"fileName".equals(paramName))).map((paramName) -> {
                Element cf = new Element("cp");
                cf.setAttribute("n", paramName);
                cf.setAttribute("v", cfMap.get(paramName));
                return cf;
            }).forEach((cf) -> {
                cfgParam.addContent(cf);
            });
            dataProvider.addContent(cfgParam);
        }
        return dataProvider;
    }

    /**
     * Ritorna il tipo del file del dataprovider
     *
     * @return tipo del dataprovider
     */
    public String getType() {
        return dpConfiguration.getType();
    }

    /**
     * Ritorna il nome del file
     *
     * @return nome del file
     */
    public String getFileName() {
        return getConfigurationValue(DataProviderConfiguration.FILE_NAME, "");
    }

    /**
     * Ritorna il delimitatore
     *
     * @return delimitatore tra colonne
     */
    public String getDelimiter() {
        return getConfigurationValue(DataProviderConfiguration.CSV_DELIMITER, ";");
    }

    /**
     * Ritorna il carattere di escaping
     *
     * @return carattere di escaping
     */
    public String getEscape() {
        return getConfigurationValue(DataProviderConfiguration.CSV_ESCAPE, "");
    }

    /**
     * Ritorna il carattere di quoting delle stringhe
     *
     * @return carattere di quoting delle stringhe
     */
    public String getQuote() {
        return getConfigurationValue(DataProviderConfiguration.CSV_QUOTE, "\"");
    }

    /**
     * Ritorna il separatore di linee
     *
     * @return separatore di linee
     */
    public String getLineSeparator() {
        return getConfigurationValue(DataProviderConfiguration.CSV_LINE_SEPARATOR, "\\n");
    }

    /**
     * Imposta il nome del file
     *
     * @param fileName nome del file
     */
    public void setFileName(String fileName) {
        this.dpConfiguration.updateConfiguration(DataProviderConfiguration.FILE_NAME, fileName);
    }

    private String getConfigurationValue(String key, String defaultValue) {
        String ret = this.dpConfiguration.getConfigurationValues().get(key);
        if (ret == null) {
            ret = defaultValue;
        }
        return ret;
    }

    /**
     * Imposta il delimitatore
     *
     * @param value delimitatore
     */
    public void setDelimiter(String value) {
        this.dpConfiguration.updateConfiguration(DataProviderConfiguration.CSV_DELIMITER, value);
    }

    /**
     * Imposta il carattere di escaping
     *
     * @param value carattere di escaping
     */
    public void setEscape(String value) {
        this.dpConfiguration.updateConfiguration(DataProviderConfiguration.CSV_ESCAPE, value);
    }

    /**
     * Imposta il carattere di quoting
     *
     * @param value carattere di quoting
     */
    public void setQuote(String value) {
        this.dpConfiguration.updateConfiguration(DataProviderConfiguration.CSV_QUOTE, value);
    }

    /**
     * Imposta il separatore di linea
     *
     * @param value separatore dei linea
     */
    public void setLineSeparator(String value) {
        this.dpConfiguration.updateConfiguration(DataProviderConfiguration.CSV_LINE_SEPARATOR, value);
    }

    /**
     * Ritorna le righe che rappresentano i fields per il dispaly della
     * configurazione nella GUI
     *
     * @return lista delle righe della tabella
     */
    public List<String[]> getFieldsRows() {
        return this.dpConfiguration.getFieldsRows();
    }

    /**
     * Verifica se esiste il field
     *
     * @param fieldName field da controllare
     * @return true se il field è già conosciuto dal data provider
     */
    public boolean containsField(String fieldName) {
        return dpConfiguration.getFields().get(fieldName) != null;
    }

    /**
     * Imposta il tipo del dataprovider
     *
     * @param type tipo del dataprovider
     */
    public void setType(String type) {
        dpConfiguration.setType(type);
    }

    /**
     * Rimuove tutti i fields
     */
    public void removeAllFields() {
        dpConfiguration.removeAllFields();
    }

    /**
     * Ritorna i fields
     *
     * @return mappa {nomeField, tipoField}
     */
    public Map<String, String> getFields() {
        return dpConfiguration.getFields();
    }

    /**
     * Imposta di saltare la prima riga nell'importazione
     *
     * @param selected true se deve saltare la prima riga
     */
    public void skipFirstRow(boolean selected) {
        this.dpConfiguration.updateConfiguration(DataProviderConfiguration.SKIP_FIRST, String.valueOf(selected));
    }

    /**
     * Scrive il dataprovider nello storage
     */
    public void burnToStorage() {
        this.dpConfiguration.burnToStorage();
    }

    /**
     * Ritorna il tipo di encoding del file
     *
     * @return encoding del file
     */
    public String getEncoding() {
        return this.dpConfiguration.getEncoding();
    }

    /**
     * Ritorna il nome della tabella associato al field
     *
     * @param fieldName field
     * @return nome della tabella
     */
    public String getTable(String fieldName) {
        return this.dpConfiguration.getTable(fieldName);
    }

    /**
     * Ritorna l'elenco delle tabelle associate al dataprovider
     *
     * @return elenco delle tabelle
     */
    public Set<String> getTables() {
        return new HashSet(this.dpConfiguration.getFieldsTable().values());
    }

    /**
     * Rimuove una tabella dalla relazione
     *
     * @param table nome della tabella
     */
    public void removeTable(String table) {
        dpConfiguration.removeTable(table);
    }

    /**
     * Rinomina la tabella
     *
     * @param table tabella da rinominare
     * @param newName nuovo nome
     */
    public void renameTable(String table, String newName) {
        dpConfiguration.renameTable(table, newName);
    }
}
