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

import org.thesemproject.opensem.gui.LogGui;
import org.thesemproject.opensem.gui.SemGui;
import org.thesemproject.opensem.gui.modelEditor.TableTreeNode;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * Gestisce gli eventi grafici sulla tabella
 */
public class TablesUtils {

    /**
     * Imposta il pannello del model editor della tabella
     *
     * @param tableTreeNode nodo
     * @param semGui frame
     */
    public static void populateTablePanel(TableTreeNode tableTreeNode, SemGui semGui) {
        GuiUtils.clearTable(semGui.getTable());
        DefaultTableModel model = (DefaultTableModel) semGui.getTable().getModel();
        semGui.getFromDataProvider().setText("Tabella da dataprovider");
        if (tableTreeNode.isPopulatedFromDp()) {
            if (tableTreeNode.isDpLinked()) {
                semGui.getFromDataProvider().setText("Tabella da dataprovider " + tableTreeNode.getDpName());
            }
            tableTreeNode.getTableContent().stream().forEach((String rowValue) -> {
                String[] r = new String[1];
                r[0] = rowValue;
                model.addRow(r);
            });
            semGui.getTable().setEnabled(false);
            semGui.getTableExport().setEnabled(false);
            semGui.getTableImport().setEnabled(false);
            semGui.getTableAddRecord().setEnabled(false);
            semGui.getTableDeleteRecord().setEnabled(false);
            semGui.getRemoveTableFilter().setEnabled(false);
            semGui.getSearchTable().setEnabled(false);
            semGui.getFromDataProvider().setSelected(true);
        } else {
            tableTreeNode.getTableContent().stream().forEach((String rowValue) -> {
                String[] row = new String[1];
                row[0] = rowValue;
                model.addRow(row);
            });
            semGui.getTable().setEnabled(true);
            semGui.getFromDataProvider().setSelected(false);
            semGui.getTableExport().setEnabled(true);
            semGui.getTableImport().setEnabled(true);
            semGui.getTableAddRecord().setEnabled(true);
            semGui.getTableDeleteRecord().setEnabled(true);
            semGui.getRemoveTableFilter().setEnabled(true);
            semGui.getSearchTable().setEnabled(true);
        }
    }

    /**
     * Aggiunge un record di tabella
     *
     * @param semGui frame
     * @throws HeadlessException
     */
    public static void addTableRecord(SemGui semGui) throws HeadlessException {
        String name = JOptionPane.showInputDialog(null, "Inserire il valore del record");
        if (name == null) {
            name = "";
        }
        if (name.length() > 0) {
            TableTreeNode node = (TableTreeNode) semGui.getModelEditor().getCurrentNode();
            if (node != null) {
                node.addRecord(name);
                TablesUtils.populateTablePanel(node, semGui);
            }
        }
    }

    /**
     * Gestisce la cancellazione di un record
     *
     * @param semGui frame
     */
    public static void deleteTableRecord(SemGui semGui) {
        if (GuiUtils.showConfirmDialog("Confermi l'eliminazione delle righe selezionate?", "Conferma")) {
            DefaultTableModel model = (DefaultTableModel) semGui.getTable().getModel();
            int[] rows = semGui.getTable().getSelectedRows();
            TableTreeNode node = (TableTreeNode) semGui.getModelEditor().getCurrentNode();
            if (node != null) {
                for (int i = 0; i < rows.length; i++) {
                    int pos = semGui.getTable().convertRowIndexToModel(rows[i] - i);
                    String record = (String) model.getValueAt(pos, 0);
                    node.deleteRecord(record);
                    model.removeRow(pos);
                }
            }
        }
    }

    /**
     * Gestisce l'importazione di una tabella
     *
     * @param evt evento
     * @param semGui frame
     */
    public static void importTable(ActionEvent evt, SemGui semGui) {
        semGui.getSelectImportTable().setVisible(false);
        String command = evt.getActionCommand();
        if (command.equals(JFileChooser.APPROVE_SELECTION)) {
            File file = semGui.getImportTableFileChooser().getSelectedFile();
            semGui.updateLastSelectFolder(file.getAbsolutePath());
            TableTreeNode node = (TableTreeNode) semGui.getModelEditor().getCurrentNode();
            if (node != null) {
                node.resetRecods();
                try {
                    GuiUtils.readFileLines(file.getAbsolutePath(), new GuiUtils.LineFilter() {
                        @Override
                        public void applyTo(String line) {
                            node.addRecord(line.toLowerCase());
                        }
                    });
                } catch (Exception e) {
                    LogGui.printException(e);
                }
                TablesUtils.populateTablePanel(node, semGui);
            }
        }
    }

    /**
     * Gestisce l'esportazione di una tabella su CSV
     *
     * @param evt evento
     * @param semGui frame
     */
    public static void exportTable(ActionEvent evt, SemGui semGui) {
        semGui.getSelectExportTable().setVisible(false);
        String command = evt.getActionCommand();
        if (command.equals(JFileChooser.APPROVE_SELECTION)) {
            File file = semGui.getExpotTableFileChooser().getSelectedFile();
            semGui.updateLastSelectFolder(file.getAbsolutePath());
            TableTreeNode node = (TableTreeNode) semGui.getModelEditor().getCurrentNode();
            if (node != null) {
                try {
                    List<String> lines = new ArrayList<>();
                    for (String row : node.getTableContent()) {
                        lines.add(row);
                    }
                    GuiUtils.writeCSV(file.getAbsolutePath(), lines);
                } catch (Exception e) {
                    LogGui.printException(e);
                }
            }
        }
    }
}
