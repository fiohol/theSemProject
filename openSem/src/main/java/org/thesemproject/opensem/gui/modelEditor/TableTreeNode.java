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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jdom2.Element;

/**
 * Gestisce un nodo che rappresenta una tabella
 */
public class TableTreeNode extends ModelTreeNode {

    private final Set<String> tableContent;
    private String dpName;
    private boolean populatedFromDp;

    /**
     * Istanzia il nodo
     *
     * @param nodeName nome della tabella
     * @param populatedFromDp true se popolato da dataprovider
     * @param dpName nome del dataprovider
     */
    public TableTreeNode(String nodeName, boolean populatedFromDp, String dpName) {
        super(nodeName, TYPE_TABLE_DEFINITION);
        tableContent = new LinkedHashSet<>();
        this.populatedFromDp = populatedFromDp;
        this.dpName = dpName;
    }

    /**
     * Istanzia il nodo
     *
     * @param nodeName nome della tabella
     */
    public TableTreeNode(String nodeName) {
        super(nodeName, TYPE_TABLE_DEFINITION);
        tableContent = new LinkedHashSet<>();
        this.populatedFromDp = false;
        this.dpName = null;
    }

    /**
     * Istanzia il nodo
     *
     * @param name nome della tabella
     * @param mNode nodo da clonare
     */
    public TableTreeNode(String name, TableTreeNode mNode) {
        super(name, TYPE_TABLE_DEFINITION);
        tableContent = mNode.tableContent;
        populatedFromDp = mNode.populatedFromDp;
        dpName = mNode.dpName;
    }

    /**
     * Aggiunge un record alla tabella
     *
     * @param value valore della tabella
     */
    public void addRecord(String value) {
        tableContent.add((String) intern.intern(value));
    }

    /**
     * Cancella un record dalla tabella
     *
     * @param value record da cancellare
     */
    public void deleteRecord(String value) {
        tableContent.remove(value);
    }

    /**
     * Ritorna il contenuto della tabella
     *
     * @return contenuto della tabella per inserirla in una JTable
     */
    public List<String> getTableContent() {
        return new ArrayList<>(tableContent);
    }

    /**
     * Ritorna la rappresentazione XML del nodo
     *
     * @return rappresentazione XML
     */
    public Element getXmlElement() {
        Element table = new Element("t");
        table.setAttribute("n", getNodeName());
        table.setAttribute("dp", String.valueOf(populatedFromDp));
        if (dpName != null) {
            table.setAttribute("dpn", dpName);
        }
        if (!populatedFromDp) {
            for (String value : tableContent) {
                Element record = new Element("r");
                record.addContent(value);
                table.addContent(record);
            }
        }
        return table;
    }

    /**
     * PUlisce la tabella
     */
    public void resetRecods() {
        tableContent.clear();
    }

    /**
     * Imposta la tabella come popolata da dataprovider
     *
     * @param selected true se la tabella è popolata dal dataprovider
     */
    public void setPopulateFromDp(boolean selected) {
        this.populatedFromDp = selected;
        if (selected) {
            tableContent.clear();
        }
        this.dpName = null;
    }

    /**
     * Verifica se la tabella è popolata da dataprovider
     *
     * @return true se popolata da dataprovider
     */
    public boolean isPopulatedFromDp() {
        return populatedFromDp;
    }

    /**
     * Verifica se la tabella è linkata ad un dataprovider
     *
     * @return true se è linkata ad un dataprovider
     */
    public boolean isDpLinked() {
        return dpName != null;
    }

    /**
     * Ritorna il nome del dataprovider
     *
     * @return nome del dataprovider
     */
    public String getDpName() {
        return dpName;
    }

    /**
     * Imposta il nome del dataprovider
     *
     * @param name nome del dataprovider
     */
    public void setDpName(String name) {
        dpName = name;
    }

}
