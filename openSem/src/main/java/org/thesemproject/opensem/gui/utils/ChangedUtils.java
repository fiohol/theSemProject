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

import org.thesemproject.opensem.gui.SemDocument;
import org.thesemproject.opensem.gui.SemGui;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 *
 * Gestisce gli eventi sul pannello dei cambiati
 */
public class ChangedUtils {

    /**
     * Aggiorna la tabella dei cambiati
     *
     * @param dto documento
     * @param old vecchio documento
     * @param semGui frame
     */
    public synchronized static void updateChangedTable(SemDocument dto, SemDocument old, SemGui semGui) {
        List<Object[]> diff = dto.compareWith(old);
        DefaultTableModel changedModel = (DefaultTableModel) semGui.getChangedTable().getModel();
        diff.stream().forEach((Object[] row) -> {
            changedModel.addRow(row);
            synchronized (semGui.getClasses()) {
                String key = (String) row[5];
                Integer count = semGui.getClasses().get(key);
                if (count == null) {
                    count = 0;
                }
                count++;
                semGui.getClasses().put(key, count);
            }
        });
    }

    /**
     * Pulisce la tabella dei cambiati
     *
     * @param semGui frame
     */
    public static void prepareChanged(SemGui semGui) {
        GuiUtils.clearTable(semGui.getChangedTable());
        semGui.getClasses().clear();
    }

    /**
     * Gestisce l'evento mouse sulla tabella
     *
     * @param semGui frame
     */
    public static void changedTableMouseEventManagement(SemGui semGui) {
        int currentPosition = semGui.getChangedTable().getSelectedRow();
        String sid = (String) semGui.getChangedTable().getValueAt(currentPosition, 0);
        String element = (String) semGui.getChangedTable().getValueAt(currentPosition, 1);
        if (element.startsWith(sid + ".")) {
            //E' un segmento classificabile
            String classe = (String) semGui.getChangedTable().getValueAt(currentPosition, 5);
            if ("Classificazione".equals(classe)) {
                int pos = element.indexOf(".Bayes");
                if (pos != -1) {
                    String segId = element.substring(0, pos);
                    TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) semGui.getSegmentsTable().getRowSorter();
                    sorter.setRowFilter(RowFilter.regexFilter("^" + segId + "$", 0));
                    semGui.getSegmentsTable().setRowSorter(sorter);
                    semGui.getStatusSegments().setText("Totale filtrati elementi: " + semGui.getSegmentsTable().getRowCount());
                }
            }
        }
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) semGui.getFilesTable().getRowSorter();
        sorter.setRowFilter(RowFilter.regexFilter("^" + sid + "$", 0));
        semGui.getFilesTable().setRowSorter(sorter);
    }

    /**
     * Gestisce l'evento di filtraggio dei cambiati attraverso l'albero che
     * appare sulla sinistra
     *
     * @param evt evento
     * @param semGui frame
     */
    public static void changedFilterTree(MouseEvent evt, SemGui semGui) {
        int selRow = semGui.getChangedFilterTree().getRowForLocation(evt.getX(), evt.getY());
        TreePath selPath = semGui.getChangedFilterTree().getPathForLocation(evt.getX(), evt.getY());
        semGui.getChangedFilterTree().setSelectionPath(selPath);
        if (selRow != -1) {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) semGui.getChangedFilterTree().getLastSelectedPathComponent();
            if (node.toString().length() > 0) {
                if (!"Cambiamenti".equals(node.toString())) {
                    TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) semGui.getChangedTable().getRowSorter();
                    String filterString = node.toString();
                    int pos = filterString.indexOf("(");
                    if (pos != -1) {
                        filterString = filterString.substring(0, pos);
                    }
                    sorter.setRowFilter(RowFilter.regexFilter(filterString, 5));
                    semGui.getChangedTable().setRowSorter(sorter);
                } else {
                    TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) semGui.getChangedTable().getRowSorter();
                    sorter.setRowFilter(null);
                    semGui.getChangedTable().setRowSorter(sorter);
                }
            }
        }
    }

    /**
     * Aggiorna l'albero dei cambiati
     *
     * @param semGui frame
     */
    public static void updateChangedTree(SemGui semGui) {
        DefaultMutableTreeNode clResults = new DefaultMutableTreeNode("Cambiamenti");
        DefaultMutableTreeNode clResults1 = new DefaultMutableTreeNode("Classificazione");
        DefaultMutableTreeNode clResults2 = new DefaultMutableTreeNode("Catture");
        clResults.add(clResults1);
        clResults.add(clResults2);
        List<String> listaClassi = new ArrayList(semGui.getClasses().keySet());
        Collections.sort(listaClassi);
        listaClassi.stream().forEach((String classe) -> {
            DefaultMutableTreeNode treeNode1 = new DefaultMutableTreeNode(classe + "(" + semGui.getClasses().get(classe) + ")");
            if ("Classificazione".equals(classe)) {
                clResults1.add(treeNode1);
            } else {
                clResults2.add(treeNode1);
            }
        });
        semGui.getChangedFilterTree().setModel(new DefaultTreeModel(clResults));
        GuiUtils.expandAll(semGui.getChangedFilterTree());
        semGui.updateStats();
    }

}
