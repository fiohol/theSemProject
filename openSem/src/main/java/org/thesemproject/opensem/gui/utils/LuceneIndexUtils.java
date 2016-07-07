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

import org.thesemproject.opensem.classification.IndexManager;
import org.thesemproject.opensem.gui.LogGui;
import org.thesemproject.opensem.gui.SemGui;
import org.thesemproject.opensem.gui.TableCellListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.TreePath;

/**
 *
 * Gestione grafica della manutezione dell'indice di lucene
 */
public class LuceneIndexUtils {

    /**
     * Carica il contenuto dell'indice in una tabella a seconda della lingua
     * corrente
     *
     * @param semGui frame
     */
    public static void populateIndex(SemGui semGui) {
        GuiUtils.clearTable(semGui.getDocumentsTable());
        String language = String.valueOf(semGui.getLinguaAnalizzatoreIstruzione().getSelectedItem());
        List<String[]> docs = semGui.getME().getDocuments(language);
        DefaultTableModel model = (DefaultTableModel) semGui.getDocumentsTable().getModel();
        docs.stream().forEach((String[] row) -> {
            model.addRow(row);
        });
        semGui.getManageDocumentsStatus().setText("Lingua corrente: " + language + " - Totale documenti: " + docs.size());
    }

    /**
     * Gestisce i cambiamenti sul documento indicizzato
     *
     * @param tcl listner della cella
     * @param semGui frame
     */
    public static void changeIndexDocument(TableCellListener tcl, SemGui semGui) {
        try {
            DefaultTableModel model = (DefaultTableModel) semGui.getDocumentsTable().getModel();
            int[] rows = semGui.getDocumentsTable().getSelectedRows();
            String language = String.valueOf(semGui.getLinguaAnalizzatoreIstruzione().getSelectedItem());
            for (int i = 0; i < rows.length; i++) {
                int pos = semGui.getDocumentsTable().convertRowIndexToModel(rows[i] - i);
                String record = (String) model.getValueAt(pos, 0);
                String newStr = (String) tcl.getNewValue();
                semGui.getME().updateDocumentDescription(record, newStr, language);
            }
            semGui.setNeedUpdate(true);
        } catch (Exception e) {
            LogGui.printException(e);
        }
    }

    /**
     * Gestisce l'albero di classificazione
     *
     * @param evt evento
     * @param semGui frame
     */
    public static void manageClassificationTree(MouseEvent evt, SemGui semGui) {
        GuiUtils.treeActionPerformed(semGui.getManageClassificationTree(), null, semGui.getToken().getText(), evt, true, semGui);
        if (SwingUtilities.isLeftMouseButton(evt)) {
            GuiUtils.filterTable(semGui.getDocumentsTable(), null, 1);
            TreePath selPath = semGui.getManageClassificationTree().getPathForLocation(evt.getX(), evt.getY());
            if (selPath != null) {
                semGui.getManageClassificationTree().setSelectionPath(selPath);
                Object[] path = selPath.getPath();
                switch (path.length) {
                    case 1:
                        GuiUtils.filterTable(semGui.getDocumentsTable(), null, 1);
                        break;
                    case 2:
                        semGui.getSerachDocumentBody().setText("Level1:" + path[1].toString());
                        break;
                    case 3:
                        semGui.getSerachDocumentBody().setText("Level2:" + path[2].toString());
                        break;
                    case 4:
                        semGui.getSerachDocumentBody().setText("Level3:" + path[3].toString());
                        break;
                    case 5:
                        semGui.getSerachDocumentBody().setText("Level4:" + path[4].toString());
                        break;
                    case 6:
                        semGui.getSerachDocumentBody().setText("Level5:" + path[5].toString());
                        break;
                    case 7:
                        semGui.getSerachDocumentBody().setText("Level6:" + path[6].toString());
                        break;
                    default:
                        break;
                }
            }
            semGui.serachDocumentBodyKeyReleased();
        }
        semGui.getCategorieSegmentsPanel().setModel(semGui.getManageClassificationTree().getModel());
        semGui.getClassificationTree1().setModel(semGui.getManageClassificationTree().getModel());
        semGui.getCategorieSegmentsPanel().setModel(semGui.getManageClassificationTree().getModel());
    }

    /**
     * gestisce la ricerca nel testo del documento
     *
     * @param semGui frame
     */
    public static void searchDocumentBody(SemGui semGui) {
        int[] idxs = {1, 2, 3, 4, 5};
        String text = semGui.getSerachDocumentBody().getText();
        if (text.toLowerCase().startsWith("level1:") && text.length() > 7) {
            GuiUtils.filterTable(semGui.getDocumentsTable(), text.substring(7).trim(), 2);
        } else if (text.toLowerCase().startsWith("level2:") && text.length() > 7) {
            GuiUtils.filterTable(semGui.getDocumentsTable(), text.substring(7).trim(), 3);
        } else if (text.toLowerCase().startsWith("level3:") && text.length() > 7) {
            GuiUtils.filterTable(semGui.getDocumentsTable(), text.substring(7).trim(), 4);
        } else if (text.toLowerCase().startsWith("level4:") && text.length() > 7) {
            GuiUtils.filterTable(semGui.getDocumentsTable(), text.substring(7).trim(), 5);
        } else if (text.toLowerCase().startsWith("level5:") && text.length() > 7) {
            GuiUtils.filterTable(semGui.getDocumentsTable(), text.substring(7).trim(), 6);
        } else if (text.toLowerCase().startsWith("level6:") && text.length() > 7) {
            GuiUtils.filterTable(semGui.getDocumentsTable(), text.substring(7).trim(), 7);
        }else if (text.toLowerCase().startsWith("testo:") && text.length() > 6) {
            GuiUtils.filterTable(semGui.getDocumentsTable(), text.substring(6).trim(), 1);
        } else {
            GuiUtils.filterTable(semGui.getDocumentsTable(), text, idxs);
        }
    }

    /**
     * gestisce la cancellazione di un documento
     *
     * @param semGui frame
     */
    public static void deleteDocument(SemGui semGui) {
        if (GuiUtils.showConfirmDialog("Confermi l'eliminazione delle righe selezionate?", "Conferma")) {
            DefaultTableModel model = (DefaultTableModel) semGui.getDocumentsTable().getModel();
            int[] rows = semGui.getDocumentsTable().getSelectedRows();
            String language = String.valueOf(semGui.getLinguaAnalizzatoreIstruzione().getSelectedItem());
            List<String> toRemove = new ArrayList();
            for (int i = 0; i < rows.length; i++) {
                int pos = semGui.getDocumentsTable().convertRowIndexToModel(rows[i] - i);
                String record = (String) model.getValueAt(pos, 0);
                toRemove.add(record);
                model.removeRow(pos);
            }
            semGui.getME().removeDocuments(toRemove, language);
            semGui.getManageDocumentsStatus().setText("Lingua corrente: " + language + " - Totale documenti: " + model.getRowCount());
            semGui.setNeedUpdate(true);
        }
    }

    /**
     * Gestisce la creazione di un nuovo indice
     *
     * @param semGui frame
     */
    public static void buildIndex(SemGui semGui) {
        semGui.getLogIstruzione().setEnabled(true);
        semGui.getLogIstruzione().setEditable(false);
        LogGui.setjTextArea(semGui.getLogIstruzione());
        LogGui.info("Inizio attivit\u00e0 di istruzione");
        semGui.getStartBuildIndex().setEnabled(false);
        Thread t = new Thread(() -> {
            try {
                IndexManager.buildIndex(semGui.getPercorsoIndice1().getText(), new File(semGui.getFileExcel().getText()), new File(semGui.getStopWords2().getText()), String.valueOf(semGui.getLinguaAnalizzatoreIstruzione().getSelectedItem()), semGui.getUsaCategorie().isSelected());
            } catch (Exception e) {
                LogGui.printException(e);
            }
            LogGui.info("Fine attivit\u00e0 di istruzione");
            LogGui.setjTextArea(semGui.getLogInizializzazione());
            semGui.getStartBuildIndex().setEnabled(true);
        });
        t.setDaemon(true);
        t.start();
    }

}
