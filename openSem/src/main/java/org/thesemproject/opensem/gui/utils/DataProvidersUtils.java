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
package org.thesemproject.opensem.gui.utils;

import org.thesemproject.opensem.enrichment.CSVFileParser;
import org.thesemproject.opensem.gui.SemGui;
import org.thesemproject.opensem.gui.modelEditor.DataProviderTreeNode;
import org.thesemproject.opensem.gui.modelEditor.DataProviderTreeRelationshipNode;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * Gestisce gli eventi su dataprovider e dataprovider relationship
 */
public class DataProvidersUtils {

    /**
     * Ripulisce tutti i fields
     *
     * @param semGui frame
     */
    public static void clearDpFieldsDetails(SemGui semGui) {
        semGui.getDpFieldId().setText("");
        semGui.getDpFieldName().setText("");
        semGui.getDpFieldType().setSelectedItem("");
        semGui.getDpFieldPosition().setValue(new Integer(0));
        semGui.getConfirmDpField().setEnabled(false);
        semGui.getDpFieldName().setEnabled(false);
        semGui.getDpFieldType().setEnabled(false);
        semGui.getDpFieldPosition().setEnabled(false);
        semGui.getDpFieldTableRelationship().setEnabled(false);
    }

    /**
     * Gestisce le action sulla tabella
     *
     * @param semGui frame
     */
    public static void dpFieldsTableAction(SemGui semGui) {
        int position = semGui.getDpFieldsTable().getSelectedRow();
        String id = (String) semGui.getDpFieldsTable().getValueAt(position, 0);
        String field = (String) semGui.getDpFieldsTable().getValueAt(position, 1);
        String type = (String) semGui.getDpFieldsTable().getValueAt(position, 2);
        String pos = (String) semGui.getDpFieldsTable().getValueAt(position, 3);
        if (pos == null) {
            pos = "1";
        }
        semGui.getDpFieldName().setText(field);
        semGui.getDpFieldType().setSelectedItem(type);
        semGui.getDpFieldPosition().setValue(new Integer(pos));
        semGui.getDpFieldId().setText(id);
        DataProvidersUtils.enableDpFieldsDetails(semGui);
        semGui.getDpFieldName().setEditable(false);
        String currTable = (String) semGui.getDpFieldsTable().getValueAt(position, 4);
        semGui.getDpFieldTableRelationship().setModel(new DefaultComboBoxModel(getOpenTables(currTable, semGui)));
        semGui.getDpFieldTableRelationship().setSelectedItem(currTable);
    }

    /**
     * abilita i dettagli dei fields
     *
     * @param semGui frame
     */
    public static void enableDpFieldsDetails(SemGui semGui) {
        semGui.getConfirmDpField().setEnabled(true);
        semGui.getDpFieldName().setEnabled(true);
        semGui.getDpFieldName().setEditable(true);
        semGui.getDpFieldType().setEnabled(true);
        semGui.getDpFieldPosition().setEnabled(true);
        semGui.getDpFieldTableRelationship().setModel(new DefaultComboBoxModel(getOpenTables("", semGui)));
        semGui.getDpFieldTableRelationship().setEnabled(true);
    }

    /**
     * Gestisce la conferma di creazione
     *
     * @param evt evento
     * @param semGui frame
     */
    public static void confirmDpFieldActionPerformed(ActionEvent evt, SemGui semGui) {
        String id = semGui.getDpFieldId().getText();
        DataProviderTreeNode node = (DataProviderTreeNode) semGui.getModelEditor().getCurrentNode();
        if (node != null) {
            String fieldName = semGui.getDpFieldName().getText();
            if (fieldName.trim().length() == 0) {
                GuiUtils.showErrorDialog("Il nome del field non pu\u00f2 essere vuoto", "Errore nel nome del field");
            } else {
                String oldTableName = node.getTable(fieldName);
                String newTableName = (String) semGui.getDpFieldTableRelationship().getSelectedItem();
                if (id.length() > 0) {
                    semGui.getModelEditor().modifyTableRelationship((DefaultMutableTreeNode) semGui.getModelTree().getModel().getRoot(), oldTableName, newTableName, node.getNodeName()); //Aggiunge la relazione con la tabella
                    node.addField(fieldName, (String) semGui.getDpFieldType().getSelectedItem(), String.valueOf(semGui.getDpFieldPosition().getValue()), String.valueOf(semGui.getDpFieldTableRelationship().getSelectedItem()));
                } else if (node.containsField(fieldName)) {
                    GuiUtils.showErrorDialog("Il field esiste", "Errore nel nome del field");
                } else {
                    semGui.getModelEditor().modifyTableRelationship((DefaultMutableTreeNode) semGui.getModelTree().getModel().getRoot(), oldTableName, newTableName, node.getNodeName()); //Aggiunge la relazione con la tabella
                    node.addField(fieldName, (String) semGui.getDpFieldType().getSelectedItem(), String.valueOf(semGui.getDpFieldPosition().getValue()), String.valueOf(semGui.getDpFieldTableRelationship().getSelectedItem()));
                }
                populateDataProviderSplit(node, semGui);
            }
        }
    }

    /**
     * Predispone la scheda nella gui con tutti i dati del dataprovider
     *
     * @param dataProviderTreeNode nodo da mostrare
     * @param semGui frame
     */
    public static void populateDataProviderSplit(DataProviderTreeNode dataProviderTreeNode, SemGui semGui) {
        DataProvidersUtils.clearDpFieldsDetails(semGui);
        semGui.getDpName().setText(dataProviderTreeNode.getNodeName());
        semGui.getDpType().setSelectedItem(dataProviderTreeNode.getType());
        semGui.getDpFileName().setText(dataProviderTreeNode.getFileName());
        semGui.getDpDelimitatore().setText(dataProviderTreeNode.getDelimiter());
        semGui.getDpEscape().setText(dataProviderTreeNode.getEscape());
        semGui.getDpQuote().setText(dataProviderTreeNode.getQuote());
        semGui.getDpLineSep().setText(dataProviderTreeNode.getLineSeparator());
        DefaultTableModel model = (DefaultTableModel) semGui.getDpFieldsTable().getModel();
        GuiUtils.clearTable(semGui.getDpFieldsTable());
        List<String[]> rows = dataProviderTreeNode.getFieldsRows();
        rows.stream().forEach((String[] row) -> {
            model.addRow(row);
        });
    }

    /**
     * Predispone la scheda nella GUI con tutti i dati della relazione
     *
     * @param dprn nodo del dataprovider
     * @param semGui frame
     */
    public static void populateDataProviderRelationship(DataProviderTreeRelationshipNode dprn, SemGui semGui) {
        semGui.getDprName().setText(dprn.getNodeName());
        semGui.getDprSegment().setSelectedItem(dprn.getSegmentName());
        semGui.getDprPriority().setSelected(dprn.hasPriority());
        GuiUtils.clearTable(semGui.getDprTable());
        DefaultTableModel model = (DefaultTableModel) semGui.getDprTable().getModel();
        dprn.getMappingRows((DataProviderTreeNode) dprn.getParent()).stream().forEach((Object[] row) -> {
            model.addRow(row);
        });
        disableDprPanel(semGui);
    }

    /**
     * Gestisce la cancellazione del dataprovider
     *
     * @param semGui frame
     */
    public static void deleteDataProvider(SemGui semGui) {
        int position = semGui.getDpFieldsTable().getSelectedRow();
        String id = (String) semGui.getDpFieldsTable().getValueAt(position, 1);
        if (id.length() > 0) {
            if (GuiUtils.showConfirmDialog("Sei sicuro di voler cancellare il field selezionato?", "Confermi cancellazione?")) {
                DataProviderTreeNode node = (DataProviderTreeNode) semGui.getModelEditor().getCurrentNode();
                if (node != null) {
                    String table = (String) semGui.getDpFieldsTable().getValueAt(position, 4);
                    if (table != null) {
                        if (table.length() > 0) {
                            semGui.getModelEditor().modifyTableRelationship((DefaultMutableTreeNode) semGui.getModelTree().getModel().getRoot(), table, null, null);
                        }
                    }
                    node.removeField(id);
                    DataProvidersUtils.populateDataProviderSplit(node, semGui);
                }
            }
        }
    }

    /**
     * Gestisce l'importazione dei fields da csv
     *
     * @param semGui frame
     */
    public static void importDpFields(SemGui semGui) {
        DataProviderTreeNode node = (DataProviderTreeNode) semGui.getModelEditor().getCurrentNode();
        if (node != null) {
            String nomeFile = semGui.getDpFileName().getText();
            String delimiter = semGui.getDpDelimitatore().getText();
            String quoting = semGui.getDpQuote().getText();
            String escape = semGui.getDpEscape().getText();
            String lineSep = semGui.getDpLineSep().getText();
            String type = (String) semGui.getDpType().getSelectedItem();
            String encoding = node.getEncoding();
            List<String[]> fields = CSVFileParser.getFiledsType(nomeFile, delimiter, quoting, escape, lineSep, encoding);
            GuiUtils.clearTable(semGui.getDpFieldsTable());
            node.removeAllFields();
            for (String[] field : fields) {
                node.addField(field[1], field[2], field[3], null);
            }
            DataProvidersUtils.populateDataProviderSplit(node, semGui);
        }
    }

    /**
     * Gestisce la selezione del file csv da caricare
     *
     * @param evt evento
     * @param semGui frame
     */
    public static void csvDataProvider(ActionEvent evt, SemGui semGui) {
        String command = evt.getActionCommand();
        if (command.equals(JFileChooser.APPROVE_SELECTION)) {
            semGui.updateLastSelectFolder(semGui.getCsvdpchooser().getSelectedFile().getAbsolutePath());
            semGui.getDpFileName().setText(semGui.getCsvdpchooser().getSelectedFile().getAbsolutePath());
            DataProviderTreeNode node = (DataProviderTreeNode) semGui.getModelEditor().getCurrentNode();
            if (node != null) {
                node.setFileName(semGui.getDpFileName().getText());
            }
        }
        semGui.getSelectCSVDataProvider().setVisible(false);
    }

    /**
     * Gestisce il salvataggio del dataprovider
     *
     * @param semGui frame
     */
    public static void dprSave(SemGui semGui) {
        disableDprPanel(semGui);
        String fn = semGui.getDprFieldName().getText();
        String capture = (String) semGui.getDprCapture().getSelectedItem();
        boolean isKey = semGui.getDprKey().isSelected();
        if (isKey && fn.isEmpty()) {
            GuiUtils.showErrorDialog("Un campo chiave deve essere sempre mappato su una cattura", "Errore");
            return;
        }
        boolean isEnrich = semGui.getDprEnrich().isSelected();
        DataProviderTreeRelationshipNode node = (DataProviderTreeRelationshipNode) semGui.getModelEditor().getCurrentNode();
        if (node != null) {
            node.setMapping(fn, capture, isKey, isEnrich);
            DataProvidersUtils.populateDataProviderRelationship(node, semGui);
        }
    }

    /**
     * Disabilita il pannello della relazione
     *
     * @param semGui frame
     */
    public static void disableDprPanel(SemGui semGui) {
        semGui.getDprCapture().setEnabled(false);
        semGui.getDprFieldName().setEnabled(false);
        semGui.getDprKey().setEnabled(false);
        semGui.getDprEnrich().setEnabled(false);
        semGui.getDprSave().setEnabled(false);
    }

    /**
     * gestisce l'evento sulla tabella di relazione
     *
     * @param semGui frame
     */
    public static void dprTableMouseClick(SemGui semGui) {
        int position = semGui.getDprTable().getSelectedRow();
        String field = (String) semGui.getDprTable().getValueAt(position, 0);
        String capture = (String) semGui.getDprTable().getValueAt(position, 1);
        boolean key = (Boolean) semGui.getDprTable().getValueAt(position, 2);
        boolean toImport = (Boolean) semGui.getDprTable().getValueAt(position, 3);
        semGui.getDprFieldName().setText(field);
        semGui.getDprKey().setSelected(key);
        semGui.getDprEnrich().setSelected(toImport);
        semGui.getDprCapture().removeAllItems();
        semGui.getDprCapture().addItem("");
        String segment = (String) semGui.getDprSegment().getSelectedItem();
        List<String> captures = semGui.getModelEditor().getSegmentsCaptures((DefaultMutableTreeNode) semGui.getModelTree().getModel().getRoot(), segment);
        for (String scapture : captures) {
            semGui.getDprCapture().addItem(scapture);
        }
        semGui.getDprCapture().setSelectedItem(capture);
        semGui.getDprCapture().setEnabled(true);
        semGui.getDprFieldName().setEnabled(true);
        semGui.getDprKey().setEnabled(true);
        semGui.getDprEnrich().setEnabled(true);
        semGui.getDprSave().setEnabled(true);
    }

    /**
     * Gestisce la conferma di creazione del dataprovider
     *
     * @param semGui frame
     */
    public static void confirmDpFiled(SemGui semGui) {
        String id = semGui.getDpFieldId().getText();
        DataProviderTreeNode node = (DataProviderTreeNode) semGui.getModelEditor().getCurrentNode();
        if (node != null) {
            String fieldName = semGui.getDpFieldName().getText();
            if (fieldName.trim().length() == 0) {
                GuiUtils.showErrorDialog("Il nome del field non pu\u00f2 essere vuoto", "Errore nel nome del field");
            } else {
                String oldTableName = node.getTable(fieldName);
                String newTableName = (String) semGui.getDpFieldTableRelationship().getSelectedItem();
                if (id.length() > 0) {
                    semGui.getModelEditor().modifyTableRelationship((DefaultMutableTreeNode) semGui.getModelTree().getModel().getRoot(), oldTableName, newTableName, node.getNodeName()); //Aggiunge la relazione con la tabella
                    node.addField(fieldName, (String) semGui.getDpFieldType().getSelectedItem(), String.valueOf(semGui.getDpFieldPosition().getValue()), String.valueOf(semGui.getDpFieldTableRelationship().getSelectedItem()));
                } else if (node.containsField(fieldName)) {
                    GuiUtils.showErrorDialog("Il field esiste", "Errore nel nome del field");
                } else {
                    semGui.getModelEditor().modifyTableRelationship((DefaultMutableTreeNode) semGui.getModelTree().getModel().getRoot(), oldTableName, newTableName, node.getNodeName()); //Aggiunge la relazione con la tabella
                    node.addField(fieldName, (String) semGui.getDpFieldType().getSelectedItem(), String.valueOf(semGui.getDpFieldPosition().getValue()), String.valueOf(semGui.getDpFieldTableRelationship().getSelectedItem()));
                }
                DataProvidersUtils.populateDataProviderSplit(node, semGui);
            }
        }
    }

    /**
     * Ritorna l'elenco delle tabelle non ancora assegnate a dataprovider
     *
     * @param currTable tabella corrente
     * @param semGui frame
     * @return elenco delle tabelle non ancora assegnate a dataprovider
     */
    public static String[] getOpenTables(String currTable, SemGui semGui) {
        if (currTable == null) {
            currTable = "";
        }
        currTable = currTable.trim();
        if (semGui.getModelEditor() != null) {
            List<String> dpTables = semGui.getModelEditor().getFreeDpTablesNames((DefaultMutableTreeNode) semGui.getModelTree().getModel().getRoot());
            if (dpTables != null) {
                if (!dpTables.contains(currTable)) {
                    dpTables.add(currTable);
                }
                String[] elements = new String[dpTables.size()];
                for (int i = 0; i < dpTables.size(); i++) {
                    elements[i] = dpTables.get(i);
                }
                return elements;
            }
        }
        String[] ret = new String[1];
        ret[0] = "";
        return ret;
    }
}
