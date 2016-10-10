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
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.table.DefaultTableModel;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.mcavallo.opencloud.Cloud;
import org.mcavallo.opencloud.Tag;
import org.thesemproject.opensem.classification.MyAnalyzer;
import org.thesemproject.opensem.classification.Tokenizer;
import org.thesemproject.opensem.tagcloud.TagClass;
import org.thesemproject.opensem.tagcloud.TagCloudResults;
import org.thesemproject.opensem.utils.ParallelProcessor;

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
     * Estrae le frequenze dei termini a partire da un tagcloud
     *
     * @since 1.5
     * @param semGui frame
     *
     */
    public static void doExtractFrequencies(SemGui semGui) {
        GuiUtils.clearTable(semGui.getFreqTable());
        semGui.getFreqLabel().setText("Calcolo frequenze in corso...");
        if (semGui.isIsClassify()) {
            semGui.getStopTagCloud().setValue(true);
            semGui.getInterrompi().setEnabled(false);
        } else {
            Thread t = new Thread(() -> {
                if (!semGui.isIsClassify()) {
                    String language = String.valueOf(semGui.getLinguaAnalizzatoreIstruzione().getSelectedItem());
                    final TagCloudResults result = getTagCloudResults(semGui);
                    DefaultTableModel model = (DefaultTableModel) semGui.getFreqTable().getModel();
                    Cloud cloud = result.getCloud(10000);  //10000 termini credo siano sufficienti
                    semGui.getWordFrequencies().setVisible(true);
                    for (Tag tag : cloud.tags()) {
                        TagClass tc = result.getTagClass(tag);
                        String words = tc.getWordsString();
                        String[] wArray = words.split(" ");
                        for (String w : wArray) {
                            Object[] row = new Object[5];
                            row[0] = tag.getName();
                            row[1] = w;
                            row[2] = tag.getWeight();
                            row[3] = tag.getNormScore();
                            row[4] = language;
                            model.addRow(row);
                        }
                    }
                    semGui.getFilesTab().setTitleAt(7, "Gestione Indice");
                    semGui.getFreqLabel().setText("Frequenze calcolate. " + model.getRowCount() + " termini");
                    LogGui.info("Terminated...");
                    semGui.getFilesInfoLabel().setText("Fine");
                    semGui.setIsClassify(false);
                    semGui.getInterrompi().setEnabled(false);
                }
            });
            t.setDaemon(true);
            t.start();
        }
    }

    private static TagCloudResults getTagCloudResults(SemGui semGui) {
        String language = String.valueOf(semGui.getLinguaAnalizzatoreIstruzione().getSelectedItem());
        semGui.getStopTagCloud().setValue(false);
        semGui.getInterrompi().setEnabled(true);
        semGui.setIsClassify(true);
        semGui.resetFilesFilters();
        ChangedUtils.prepareChanged(semGui);
        semGui.getFilesTab().setTitleAt(7, "Gestione Indice (" + semGui.getFilesTable().getRowCount() + ") - Tag cloud in corso");
        int processors = semGui.getProcessori2().getSelectedIndex() + 1;
        ParallelProcessor tagClouding = new ParallelProcessor(processors, 6000); //100 ore
        AtomicInteger count = new AtomicInteger(0);
        semGui.getME().resetAnalyzers(); //Resetta gli analyzers
        LogGui.info("Start processing");
        final int size = semGui.getDocumentsTable().getSelectedRows().length;
        final TagCloudResults ret = new TagCloudResults();
        for (int j = 0; j < processors; j++) {
            tagClouding.add(() -> {
                while (true) {
                    if (semGui.getStopTagCloud().getValue()) {
                        break;
                    }
                    int row = count.getAndIncrement();
                    if (row >= size) {
                        break;
                    }
                    int pos = semGui.getDocumentsTable().convertRowIndexToModel(row);
                    String text = String.valueOf(semGui.getDocumentsTable().getValueAt(pos, 2));
                    if (text == null) {
                        text = "";
                    }
                    if (row % 3 == 0) {
                        semGui.getFilesTab().setTitleAt(7, "Gestione Indice (" + semGui.getDocumentsTable().getRowCount() + ") - " + row + "/" + size);
                    }
                    try {
                        MyAnalyzer analyzer = semGui.getME().getAnalyzer(language);
                        Tokenizer.getTagClasses(ret, text, "", analyzer);
                    } catch (Exception e) {
                        LogGui.printException(e);
                    }
                } //Quello che legge
            });
        }
        tagClouding.waitTermination();
        return ret;
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
        GuiUtils.treeActionPerformed(semGui.getManageClassificationTree(), semGui.getDocText(), semGui.getDocTokens().getText(), evt, false, semGui, semGui.getDocumentsTable(), 2);
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
        int[] idxs = {1, 2, 3, 4, 5, 6};
        String text = semGui.getSerachDocumentBody().getText();
        if (text.toLowerCase().startsWith("level1:") && text.length() > 7) {
            GuiUtils.filterTable(semGui.getDocumentsTable(), text.substring(7).trim(), 3);
        } else if (text.toLowerCase().startsWith("level2:") && text.length() > 7) {
            GuiUtils.filterTable(semGui.getDocumentsTable(), text.substring(7).trim(), 4);
        } else if (text.toLowerCase().startsWith("level3:") && text.length() > 7) {
            GuiUtils.filterTable(semGui.getDocumentsTable(), text.substring(7).trim(), 5);
        } else if (text.toLowerCase().startsWith("level4:") && text.length() > 7) {
            GuiUtils.filterTable(semGui.getDocumentsTable(), text.substring(7).trim(), 6);
        } else if (text.toLowerCase().startsWith("level5:") && text.length() > 7) {
            GuiUtils.filterTable(semGui.getDocumentsTable(), text.substring(7).trim(), 7);
        } else if (text.toLowerCase().startsWith("level6:") && text.length() > 7) {
            GuiUtils.filterTable(semGui.getDocumentsTable(), text.substring(7).trim(), 8);
        } else if (text.toLowerCase().startsWith("testo:") && text.length() > 6) {
            GuiUtils.filterTable(semGui.getDocumentsTable(), text.substring(6).trim(), 1);
        } else if (text.toLowerCase().startsWith("origine:") && text.length() > 8) {
            GuiUtils.filterTable(semGui.getDocumentsTable(), text.substring(8).trim(), 2);
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
        int[] rows = semGui.getDocumentsTable().getSelectedRows();
        if (rows.length > 0) {
            if (GuiUtils.showConfirmDialog("Confermi l'eliminazione delle righe selezionate?", "Conferma")) {
                DefaultTableModel model = (DefaultTableModel) semGui.getDocumentsTable().getModel();
                String language = String.valueOf(semGui.getLinguaAnalizzatoreIstruzione().getSelectedItem());
                List<String> toRemove = new ArrayList();
                for (int i = 0; i < rows.length; i++) {
                    int pos = semGui.getDocumentsTable().convertRowIndexToModel(rows[i] - i);
                    String record = (String) model.getValueAt(pos, 0);
                    String text = (String) model.getValueAt(pos, 2);
                    LogGui.info("Remove " + record + " " + text);
                    toRemove.add(record);
                    model.removeRow(pos);
                }
                semGui.getME().removeDocuments(toRemove, language);
                semGui.getManageDocumentsStatus().setText("Lingua corrente: " + language + " - Totale documenti: " + model.getRowCount());
                semGui.serachDocumentBodyKeyReleased();
                semGui.setNeedUpdate(true);
            }
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

    /**
     * Esporta il contenuto di un indice su Excel
     *
     * @since 1.2
     * @param fileToExport file su cui esportare
     * @param semGui frame
     */
    public static void exportExcelFile(String fileToExport, SemGui semGui) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                String path = fileToExport;
                if (!path.endsWith(".xlsx")) {
                    path = path + ".xlsx";
                }
                try {

                    FileOutputStream fos = new FileOutputStream(path);
                    SXSSFWorkbook wb = new SXSSFWorkbook();
                    SXSSFSheet sheetResults = wb.createSheet("Index");
                    SXSSFRow headerResults = sheetResults.createRow(0);
                    headerResults.createCell(0).setCellValue("Text");
                    headerResults.createCell(1).setCellValue("Tokens");
                    headerResults.createCell(2).setCellValue("Level1");
                    headerResults.createCell(3).setCellValue("Level2");
                    headerResults.createCell(4).setCellValue("Level3");
                    headerResults.createCell(5).setCellValue("Level4");
                    headerResults.createCell(6).setCellValue("Level5");
                    headerResults.createCell(7).setCellValue("Level6");
                    semGui.getME().getDocumentsExcel((String) semGui.getLinguaAnalizzatoreIstruzione().getSelectedItem(), sheetResults);
                    wb.write(fos);
                    fos.close();
                } catch (Exception e) {
                    LogGui.printException(e);
                }
                semGui.getFilesInfoLabel().setText("Esportazione terminata");
            }
        });
        t.start();
    }

}
