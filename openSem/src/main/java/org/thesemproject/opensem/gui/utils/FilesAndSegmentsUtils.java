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

import java.awt.Color;
import org.thesemproject.opensem.utils.ParallelProcessor;
import org.thesemproject.opensem.classification.ClassificationPath;
import org.thesemproject.opensem.classification.MyAnalyzer;
import org.thesemproject.opensem.classification.Tokenizer;
import org.thesemproject.opensem.gui.SemDocument;
import org.thesemproject.opensem.gui.LogGui;
import org.thesemproject.opensem.gui.SemGui;
import org.thesemproject.opensem.gui.process.ReadExcelToTable;
import org.thesemproject.opensem.gui.process.ReadFolderToTable;
import org.thesemproject.opensem.segmentation.SegmentConfiguration;
import org.thesemproject.opensem.gui.process.SegmentationExcelWriter;
import org.thesemproject.opensem.segmentation.SegmentationResults;
import org.thesemproject.opensem.segmentation.SegmentationUtils;
import org.thesemproject.opensem.tagcloud.TagCloudResults;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import org.mcavallo.opencloud.Cloud;
import org.mcavallo.opencloud.Tag;
import org.thesemproject.opensem.tagcloud.TagClass;

/**
 *
 * Gestisce gli eventi legate alla tabella dei files e dei segmenti
 */
public class FilesAndSegmentsUtils {

    /**
     * Gestisce la cancellazione da tabella
     *
     * @param semGui frame
     */
    public static void filesTableDelete(SemGui semGui) {
        DefaultTableModel model = (DefaultTableModel) semGui.getFilesTable().getModel();
        DefaultTableModel segModel = (DefaultTableModel) semGui.getSegmentsTable().getModel();
        int[] rows = semGui.getFilesTable().getSelectedRows();
        Set<String> segToDelete = new HashSet<>();
        for (int i = 0; i < rows.length; i++) {
            int pos = semGui.getFilesTable().convertRowIndexToModel(rows[i] - i);
            Integer id = (Integer) model.getValueAt(pos, 0);
            SemDocument dto = semGui.getTableData().get(id);
            List<Object[]> sr = dto.getSegmentRows();
            for (Object[] s : sr) {
                segToDelete.add((String) s[0]);
            }
            semGui.getTableData().remove(id);
            model.removeRow(pos);
        }
        int rc = segModel.getRowCount();
        for (int i = rc - 1; i >= 0; i--) {
            if (segToDelete.contains(segModel.getValueAt(i, 0))) {
                segModel.removeRow(i);
            }
        }
        semGui.updateStats();
    }

    /**
     * Gestisce l'evidenziazione del segmento
     *
     * @param semGui frame
     */
    public static void segmentsTableHilightSegment(SemGui semGui) {
        DefaultTableModel segModel = (DefaultTableModel) semGui.getSegmentsTable().getModel();
        int[] rows = semGui.getSegmentsTable().getSelectedRows();
        for (int i = 0; i < rows.length; i++) {
            //int pos = semGui.getSegmentsTable().convertRowIndexToModel(rows[i]);
            semGui.getSegmentsTable().setValueAt("X", rows[i], 6);
            String seg = (String) semGui.getSegmentsTable().getValueAt(rows[i], 0);
            int id = Integer.parseInt(seg.substring(0, seg.indexOf(".")));
            SemDocument dto = semGui.getTableData().get(id);
            if (dto != null) {
                dto.udpateSegmentsRows(seg);
            }
        }
    }

    /**
     * Gestisce gli eventi sulla tabella fiels
     *
     * @param evt eventi
     * @param semGui frame
     */
    public static void filesTableEventsManagement(MouseEvent evt, SemGui semGui) {
        int currentFilesPosition = semGui.getFilesTable().getSelectedRow();
        int id = (Integer) semGui.getFilesTable().getValueAt(currentFilesPosition, 0);
        String text = semGui.getFilesTable().getValueAt(currentFilesPosition, 8).toString();
        if (text != null) {
            semGui.getFileText().setText(text.replace("\n\n", "\n"));
            semGui.getFileText().setCaretPosition(0);
            semGui.getFileText1().setText(text.replace("\n\n", "\n"));
            semGui.getFileText1().setCaretPosition(0);
        }
        Object cx = semGui.getFilesTable().getValueAt(currentFilesPosition, 9);
        if (cx == null) {
            cx = "";
        }
        String formatted = cx.toString();
        if (formatted != null) {
            semGui.getFilesPanelHtmlFormatted().setText(formatted);
            semGui.getFilesPanelHtmlFormatted().setCaretPosition(0);
            semGui.getFilesPanelHtmlFormatted1().setText(formatted);
            semGui.getFilesPanelHtmlFormatted1().setCaretPosition(0);
        }
        SemDocument dto = semGui.getTableData().get(id);
        if (dto == null) {
            return;
        }
        if (dto != null && dto.getIdentifiedSegments() != null) {
            Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments = dto.getIdentifiedSegments();
            String language = dto.getLanguage();
            try {
                semGui.getFilesPanelSegmentTree().setModel(new DefaultTreeModel(SegmentationUtils.getJTree(new DefaultMutableTreeNode("Segmentazione"), identifiedSegments, language)));
            } catch (Exception e) {
                LogGui.printException(e);
            }
            DefaultTableModel dm = (DefaultTableModel) semGui.getFilesPanleCapturesTable().getModel();
            int rowCount = dm.getRowCount();
            for (int i = rowCount - 1; i >= 0; i--) {
                dm.removeRow(i);
            }
            dto.getCapturesRows().stream().forEach((Object[] row) -> {
                dm.addRow(row);
            });
            String html;
            try {
                html = SegmentationUtils.getHtml(dto.getIdentifiedSegments(), language);
            } catch (Exception ex) {
                html = "";
            }
            semGui.getFilesPanelHtml().setContentType("text/html");
            semGui.getFilesPanelHtml().setText(html);
            semGui.getFilesPanelHtml().setCaretPosition(0);
            semGui.getFilesPanelHtml1().setContentType("text/html");
            semGui.getFilesPanelHtml1().setText(html);
            semGui.getFilesPanelHtml1().setCaretPosition(0);
        } else {
            semGui.getFilesPanelSegmentTree().setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Segmentazione non effettuata")));
            semGui.getFilesPanelHtml().setContentType("text/html");
            semGui.getFilesPanelHtml().setText("");
            semGui.getFilesPanelHtml1().setContentType("text/html");
            semGui.getFilesPanelHtml1().setText("");
            DefaultTableModel dm = (DefaultTableModel) semGui.getFilesPanleCapturesTable().getModel();
            int rowCount = dm.getRowCount();
            for (int i = rowCount - 1; i >= 0; i--) {
                dm.removeRow(i);
            }
        }
        if (evt != null) {
            if (evt.getClickCount() == 2 && !evt.isConsumed()) {
                evt.consume();
                semGui.doDocumentSegmentation(text, id, currentFilesPosition);
            }
        }
    }

    /**
     * Gestisce l'evento sulla segment table
     *
     * @param evt event
     * @param semGui frame
     * @throws NumberFormatException eccezione sui valori numerici
     */
    public static void segmentsTableMouseEventManagement(MouseEvent evt, SemGui semGui) throws NumberFormatException {
        int currentFilesPosition = semGui.getSegmentsTable().getSelectedRow();
        String sid = (String) semGui.getSegmentsTable().getValueAt(currentFilesPosition, 0);
        int id = Integer.parseInt(sid.substring(0, sid.indexOf(".")));
        String text = semGui.getSegmentsTable().getValueAt(currentFilesPosition, 4).toString();
        if (text != null) {
            semGui.getSegmentText().setText(text.replace("\n\n", "\n"));
            semGui.getSegmentText().setCaretPosition(0);
            semGui.getTesto().setText(text);
            semGui.getTesto().setCaretPosition(0);
            semGui.getSegmentTokens().setText(semGui.getME().tokenize(text, semGui.getSegmentsTable().getValueAt(currentFilesPosition, 3).toString()));
            semGui.getSegmentTokens().setCaretPosition(0);
        } else {
            semGui.getTesto().setText("");
            semGui.getSegmentText().setText("");
            semGui.getSegmentTokens().setText("");
            semGui.getImagesPanel().removeAll();
        }
        DefaultMutableTreeNode clResults = new DefaultMutableTreeNode("Classificazione");
        SemDocument dto = semGui.getTableData().get(id);
        if (dto.getIdentifiedSegments() != null) {
            List<ClassificationPath> bayes = dto.getClassPath(sid);
            if (evt != null) {
                if (evt.getClickCount() == 2 && !evt.isConsumed()) {
                    evt.consume();
                    if (semGui.isNeedUpdate()) {
                        semGui.initializeModel();
                        semGui.setNeedUpdate(false);
                    }
                    bayes = semGui.getME().bayesClassify(text, Double.parseDouble(semGui.getSoglia().getText()), semGui.getSegmentsTable().getValueAt(currentFilesPosition, 3).toString());
                    dto.setClassPath(sid, bayes);
                }
            }
            bayes.stream().forEach((ClassificationPath cp) -> {
                DefaultMutableTreeNode treeNode1 = new DefaultMutableTreeNode(cp.getTechnology());
                DefaultMutableTreeNode currentNode = treeNode1;
                for (int i = 0; i < 4; i++) {
                    String node = cp.getPath()[i];
                    if (node != null) {
                        String label = node + "(" + ClassificationPath.df.format(cp.getScore()[i]) + ")";
                        DefaultMutableTreeNode treeNode2 = new DefaultMutableTreeNode(label);
                        currentNode.add(treeNode2);
                        currentNode = treeNode2;
                    }
                }
                clResults.add(treeNode1);
            });
        }
        semGui.getSegmentClassificationResult().setModel(new DefaultTreeModel(clResults));
        GuiUtils.expandAll(semGui.getSegmentClassificationResult());
    }

    /**
     * filtra la tabella dei segmenti sui segmenti classificati sul primo
     * livello
     *
     * @param semGui frame
     * @param level livello
     */
    public static void segmentsTableFilterOnFirstLevel(SemGui semGui, int level) {
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) semGui.getSegmentsTable().getRowSorter();
        sorter.setRowFilter(new RowFilter() {
            @Override
            public boolean include(RowFilter.Entry entry) {
                String idSeg = (String) entry.getValue(0);
                Integer id = Integer.parseInt(idSeg.substring(0, idSeg.indexOf(".")));
                SemDocument dto = semGui.getTableData().get(id);
                if (dto != null) {
                    List<ClassificationPath> cpl = dto.getClassPath(idSeg);
                    if (cpl.size() > 0) {
                        if (cpl.get(0).getScore()[level] == 0) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
                return false;
            }
        });
        semGui.getSegmentsTable().setRowSorter(sorter);
        semGui.getStatusSegments().setText("Totale filtrati elementi: " + semGui.getSegmentsTable().getRowCount());
    }

    /**
     * Gestisce l'evento per popolare la tabella files con il contenuto di una
     * cartella
     *
     * @param sourceDir cartella sorgente
     * @param filter file da caricare
     * @param semGui frame
     */
    public static void filesTableReadFolder(String sourceDir, File[] filter, SemGui semGui) {
        if (semGui.getRtt() == null) {
            Thread t = new Thread(() -> {
                semGui.getInterrompi().setEnabled(true);
                int processors = semGui.getProcessori2().getSelectedIndex() + 1;
                GuiUtils.prepareTables(semGui);
                semGui.setRtt(new ReadFolderToTable(processors));
                if (semGui.getTableData() == null) {
                    semGui.setTableData(new ConcurrentHashMap<>());
                }
                semGui.updateLastSelectFolder(sourceDir);
                semGui.getFilesTab().requestFocus();
                semGui.getRtt().process(sourceDir, filter, semGui.getDP(), semGui.getFilesTable(), semGui.getFilesInfoLabel(), semGui.getTableData(), semGui.getPercorsoOCR().getText());
                semGui.getFilesTab().setTitleAt(0, "Storage (" + semGui.getFilesTable().getRowCount() + ")");
                semGui.getFilesTab().setTitleAt(1, "Segmenti (" + semGui.getSegmentsTable().getRowCount() + ")");
                semGui.setRtt(null);
                semGui.getInterrompi().setEnabled(false);
            });
            t.start();
        }
    }

    /**
     * Gestisce il filtro delle righe (segmenti) classificati con uno score
     * sottosoglia
     *
     * @param semGui frame
     * @param level livello
     * @throws NumberFormatException eccezione sui valori numerici
     */
    public static void segmentsTableUnderTreshold(SemGui semGui, int level) throws NumberFormatException {
        double sg = Double.parseDouble(semGui.getSoglia().getText());
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) semGui.getSegmentsTable().getRowSorter();
        sorter.setRowFilter(new RowFilter() {
            @Override
            public boolean include(RowFilter.Entry entry) {
                String idSeg = (String) entry.getValue(0);
                Integer id = Integer.parseInt(idSeg.substring(0, idSeg.indexOf(".")));
                SemDocument dto = semGui.getTableData().get(id);
                if (dto != null) {
                    List<ClassificationPath> cpl = dto.getClassPath(idSeg);
                    if (cpl.size() > 0) {
                        if (cpl.get(0).getScore()[level - 1] < sg) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
                return false;
            }
        });
        semGui.getSegmentsTable().setRowSorter(sorter);
        semGui.getStatusSegments().setText("Totale filtrati elementi: " + semGui.getSegmentsTable().getRowCount());
    }

    /**
     * Gestisce l'esportaizone excel della filestable (e dei segmenti)
     *
     * @param evt evento
     * @param semGui frame
     */
    public static void doExportToExcel(ActionEvent evt, SemGui semGui) {
        semGui.getSelectExportExcel().setVisible(false);
        String command = evt.getActionCommand();
        if (command.equals(JFileChooser.APPROVE_SELECTION)) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    File file = semGui.getExpotExcelFileChooser().getSelectedFile();
                    semGui.updateLastSelectFolder(file.getAbsolutePath());
                    String path = file.getAbsolutePath();
                    if (!path.endsWith(".xlsx")) {
                        path = path + ".xlsx";
                    }
                    semGui.getFilesInfoLabel().setText("Esportazione in corso...");
                    try {
                        FileOutputStream fos = new FileOutputStream(path);
                        SegmentationExcelWriter sew = new SegmentationExcelWriter(semGui.getSE());
                        int id = 1;
                        for (SemDocument dto : semGui.getTableData().values()) {
                            sew.addDocument(id++, dto.getFileName(), dto.getLanguage(), (String) dto.getRow()[8], dto.getIdentifiedSegments());
                            if (id % 7 == 0) {
                                semGui.getFilesInfoLabel().setText("Esportazione " + id + "/" + semGui.getTableData().size());
                            }
                        }
                        sew.write(fos);
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

    /**
     * Gestisce l'importazione del .SER
     *
     * @param evt evento
     * @param semGui frame
     */
    public static void doImportSER(ActionEvent evt, SemGui semGui) {
        semGui.getSelectOpenStorage().setVisible(false);
        String command = evt.getActionCommand();
        if (command.equals(JFileChooser.APPROVE_SELECTION)) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    semGui.getFilesInfoLabel().setText("Apertura in corso...");
                    resetSegmentationsActionManagement(null, semGui);
                    GuiUtils.prepareTables(semGui);
                    FileInputStream finp;
                    try {
                        String path = semGui.getOpenFileChooser().getSelectedFile().getAbsolutePath();
                        semGui.updateLastSelectFolder(path);
                        finp = new FileInputStream(new File(path));
                    } catch (Exception e) {
                        LogGui.printException(e);
                        return;
                    }
                    try {
                        ObjectInputStream oos = new ObjectInputStream(finp);
                        semGui.setTableData((Map<Integer, SemDocument>) oos.readObject());
                        DefaultTableModel model = (DefaultTableModel) semGui.getFilesTable().getModel();
                        DefaultTableModel segModel = (DefaultTableModel) semGui.getSegmentsTable().getModel();
                        semGui.getTableData().keySet().stream().forEach((Integer id) -> {
                            SemDocument dto = semGui.getTableData().get(id);
                            Object[] r = dto.getRow();
                            if (r != null) {
                                Map<String, Integer> stats = dto.getStats();
                                if (stats != null) {
                                    r[3] = stats.get("Segments");
                                    r[4] = stats.get("ClassSegments");
                                    r[5] = stats.get("Captures");
                                    r[6] = stats.get("Sentencies");
                                    r[7] = stats.get("Classifications");
                                }
                                model.addRow(r);
                            }
                            List<Object[]> rSegs = dto.getSegmentRows();
                            rSegs.stream().forEach((Object[] rSeg) -> {
                                segModel.addRow(rSeg);
                            });
                            if (id % 3 == 0) {
                                semGui.updateStats();
                            }
                        });
                        semGui.updateStats();
                    } catch (Exception e) {
                        LogGui.printException(e);
                        return;
                    }
                    try {
                        finp.close();
                    } catch (Exception e) {
                        LogGui.printException(e);
                    }
                    semGui.captureCoverageUpdate();
                    semGui.getFilesInfoLabel().setText("Dati caricati correttamente");
                }
            });
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * Aggiorna i KPI nella tabella dei files
     *
     * @param row riga
     * @param column colonna
     * @param kpi KPI
     * @param semGui frame
     */
    public static void updateFilesTable(int row, int column, Number kpi, SemGui semGui) {
        row = semGui.getFilesTable().convertRowIndexToModel(row);
        semGui.getFilesTable().getModel().setValueAt(kpi != null ? kpi : 0, row, column);
    }

    /**
     * Esegue il tagcouding dei contenuti della files
     *
     * @param semGui frame
     */
    public static void doFilesTableTagCloud(SemGui semGui) {
        if (semGui.isIsClassify()) {
            semGui.getStopTagCloud().setValue(true);
            semGui.getInterrompi().setEnabled(false);
        } else {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!semGui.isIsClassify()) {
                        final TagCloudResults ret = getTagCloudResults(semGui, true);
                        semGui.openCloudFrame(ret, 200);
                        semGui.getFilesTab().setTitleAt(0, "Storage");
                        LogGui.info("Terminated...");
                        semGui.getFilesInfoLabel().setText("Fine");
                        semGui.setIsClassify(false);
                        semGui.getInterrompi().setEnabled(false);
                    }
                }
            });
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * Estrae le frequenze dei termini a partire da un tagcloud
     *
     * @since 1.3.3
     * @param semGui frame
     */
    public static void doExtractFrequencies(SemGui semGui) {
        GuiUtils.clearTable(semGui.getFreqTable());
        semGui.getFreqLabel().setText("Calcolo frequenze in corso...");
        if (semGui.isIsClassify()) {
            semGui.getStopTagCloud().setValue(true);
            semGui.getInterrompi().setEnabled(false);
        } else {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!semGui.isIsClassify()) {
                        final TagCloudResults result = getTagCloudResults(semGui, false);
                        DefaultTableModel model = (DefaultTableModel) semGui.getFreqTable().getModel();

                        Cloud cloud = result.getCloud(10000);  //10000 termini credo siano sufficienti
                        Map<Integer, SemDocument> map = semGui.getTableData();
                        semGui.getWordFrequencies().setVisible(true);
                        for (Tag tag : cloud.tags()) {
                            TagClass tc = result.getTagClass(tag);
                            Set<String> docIds = tc.getDocumentsId();
                            String language = "it";
                            try {
                                if (docIds.size() > 0) {
                                    for (String id : docIds) {
                                        Integer iid = Integer.parseInt(id);
                                        SemDocument d = map.get(iid);
                                        if (d != null) {
                                            language = d.getLanguage();
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception e) {

                            }
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
                        semGui.getFilesTab().setTitleAt(0, "Storage");
                        semGui.getFreqLabel().setText("Frequenze calcolate. " + model.getRowCount() + " termini");
                        LogGui.info("Terminated...");
                        semGui.getFilesInfoLabel().setText("Fine");
                        semGui.setIsClassify(false);
                        semGui.getInterrompi().setEnabled(false);
                    }
                }
            });
            t.setDaemon(true);
            t.start();
        }
    }

    private static TagCloudResults getTagCloudResults(SemGui semGui, boolean fullText) {
        semGui.getStopTagCloud().setValue(false);
        semGui.getInterrompi().setEnabled(true);
        semGui.setIsClassify(true);
        semGui.resetFilesFilters();
        ChangedUtils.prepareChanged(semGui);
        semGui.getFilesTab().setTitleAt(0, "Storage (" + semGui.getFilesTable().getRowCount() + ") - Tag cloud in corso");
        int processors = semGui.getProcessori2().getSelectedIndex() + 1;
        ParallelProcessor tagClouding = new ParallelProcessor(processors, 6000); //100 ore
        AtomicInteger count = new AtomicInteger(0);
        LogGui.info("Start processing");
        final int size = semGui.getFilesTable().getRowCount();
        final TagCloudResults ret = new TagCloudResults();
        for (int j = 0; j < processors; j++) {
            tagClouding.add(() -> {
                //Legge il file... e agginge in coda
                while (true) {
                    if (semGui.getStopTagCloud().getValue()) {
                        break;
                    }
                    int row = count.getAndIncrement();
                    if (row >= size) {
                        break;
                    }
                    int pos = semGui.getFilesTable().convertRowIndexToModel(row);
                    Integer id = (Integer) semGui.getFilesTable().getValueAt(pos, 0);
                    SemDocument dto = semGui.getTableData().get(id);
                    String text = String.valueOf(dto.getRow()[8]);
                    if (!fullText) {
                        StringBuilder sb = new StringBuilder();
                        for (Object[] rowSeg : dto.getSegmentRows()) {
                            sb.append(rowSeg[4]).append("\n");

                        }
                        text = sb.toString();
                    }
                    if (text == null) {
                        text = "";
                    }
                    if (row % 3 == 0) {
                        semGui.getFilesTab().setTitleAt(0, "Storage (" + semGui.getFilesTable().getRowCount() + ") - " + row + "/" + size);
                    }
                    try {
                        String language = dto.getLanguage();
                        MyAnalyzer analyzer = semGui.getME().getAnalyzer(language);
                        Tokenizer.getTagClasses(ret, text, dto.getId(), analyzer);
                    } catch (Exception e) {
                        LogGui.printException(e);
                    }
                } //Quello che legge
            });
        }
        tagClouding.waitTermination();
        return ret;
    }

    private static Object lockSync = new Object();

    static void doAtomicSegment(String text, int id, int currentFilesPosition, boolean classify, SemGui semGui) {
        try {
            Map<SegmentConfiguration, List<SegmentationResults>> identifiedSegments;
            SemDocument dto = semGui.getTableData().get(id);
            SemDocument old = dto.clone();
            String language = dto.getLanguage();
            DefaultTableModel model = (DefaultTableModel) semGui.getSegmentsTable().getModel();
            if (classify) {
                identifiedSegments = semGui.getSE().getSegments(text, semGui.getME(), Double.parseDouble(semGui.getSoglia().getText()), language);
            } else {
                identifiedSegments = semGui.getSE().getSegments(text, null, Double.parseDouble(semGui.getSoglia().getText()), language);
            }
            dto.setIdentifiedSegments(identifiedSegments);
            Map<String, Integer> stats = dto.getStats();
            FilesAndSegmentsUtils.updateFilesTable(currentFilesPosition, 3, stats.get("Segments"), semGui);
            FilesAndSegmentsUtils.updateFilesTable(currentFilesPosition, 4, stats.get("ClassSegments"), semGui);
            FilesAndSegmentsUtils.updateFilesTable(currentFilesPosition, 5, stats.get("Captures"), semGui);
            FilesAndSegmentsUtils.updateFilesTable(currentFilesPosition, 6, stats.get("Sentencies"), semGui);
            FilesAndSegmentsUtils.updateFilesTable(currentFilesPosition, 7, stats.get("Classifications"), semGui);

            if (!semGui.getEvaluations().isEmpty()) {
                double rank = semGui.getEvaluations().evaluate(identifiedSegments);
                //dto.getRow()[10] = rank;
                FilesAndSegmentsUtils.updateFilesTable(currentFilesPosition, 10, rank, semGui);
            }
            ChangedUtils.updateChangedTable(dto, old, semGui);
            List<Object[]> rows = dto.getSegmentRows();
            synchronized (lockSync) {
                rows.stream().forEach((Object[] row) -> {
                    model.addRow(row);
                });
            }
        } catch (Exception e) {
            LogGui.printException(e);
        }
    }

    /**
     * Gestisce la segmentazione della tabella files
     *
     * @param classify true se deve anche classificare
     * @param semGui frame
     */
    public static void doFilesTableSegment(boolean classify, SemGui semGui) {
        if (semGui.isIsClassify()) {
            semGui.getStopSegmentAndClassify().setValue(true);
            semGui.getInterrompi().setEnabled(false);
        } else {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!semGui.isIsClassify()) {
                        semGui.getStopSegmentAndClassify().setValue(false);
                        semGui.getInterrompi().setEnabled(true);
                        semGui.setIsClassify(true);
                        semGui.resetFilesFilters();
                        ChangedUtils.prepareChanged(semGui);
                        GuiUtils.clearTable(semGui.getSegmentsTable());
                        semGui.getFilesTab().setTitleAt(0, "Storage (" + semGui.getFilesTable().getRowCount() + ") - Inizializzazione");
                        semGui.getFilesTab().setTitleAt(1, "Segmenti (" + semGui.getSegmentsTable().getRowCount() + ")");
                        semGui.getFilesTab().setTitleAt(2, "Cambiamenti (" + semGui.getChangedTable().getRowCount() + ")");
                        if (semGui.isNeedUpdate()) {
                            semGui.initializeModel();
                            semGui.setNeedUpdate(false);
                        }
                        semGui.getFilesTab().setTitleAt(0, "Storage (" + semGui.getFilesTable().getRowCount() + ") - Classificazione in corso");
                        int processors = semGui.getProcessori2().getSelectedIndex() + 1;
                        ParallelProcessor segmentAndClassify = new ParallelProcessor(processors, 6000); //100 ore
                        AtomicInteger count = new AtomicInteger(0);
                        LogGui.info("Start processing");
                        final int size = semGui.getFilesTable().getRowCount();
                        for (int j = 0; j < processors; j++) {
                            segmentAndClassify.add(() -> {
                                //Legge il file... e agginge in coda
                                while (true) {
                                    if (semGui.getStopSegmentAndClassify().getValue()) {
                                        break;
                                    }
                                    int row = count.getAndIncrement();
                                    if (row >= size) {
                                        break;
                                    }
                                    int pos = semGui.getFilesTable().convertRowIndexToModel(row);
                                    Integer id = (Integer) semGui.getFilesTable().getValueAt(pos, 0);
                                    SemDocument dto = semGui.getTableData().get(id);
                                    String text = String.valueOf(dto.getRow()[8]);
                                    if (text == null) {
                                        text = "";
                                    }
                                    if (row % 3 == 0) {
                                        semGui.getFilesTab().setTitleAt(0, "Storage (" + semGui.getFilesTable().getRowCount() + ") - " + row + "/" + size);
                                        semGui.getFilesTab().setTitleAt(1, "Segmenti (" + semGui.getSegmentsTable().getRowCount() + ")");
                                        semGui.getFilesTab().setTitleAt(2, "Cambiamenti (" + semGui.getChangedTable().getRowCount() + ")");
                                    }
                                    FilesAndSegmentsUtils.doAtomicSegment(text, id, pos, true, semGui);
                                } //Quello che legge
                            });
                        }
                        segmentAndClassify.waitTermination();
                        ChangedUtils.updateChangedTree(semGui);
                        LogGui.info("Terminated...");
                        GuiUtils.runGarbageCollection();
                        semGui.getFilesInfoLabel().setText("Fine");
                        semGui.updateStats();
                        semGui.setIsClassify(false);
                        semGui.captureCoverageUpdate();
                        semGui.getInterrompi().setEnabled(false);
                    }
                }
            });
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * Gestisce il popolamento della filesTable attraverso excel
     *
     * @param evt evento
     * @param semGui frame
     */
    public static void doFilesTableImportExcel(ActionEvent evt, SemGui semGui) {
        semGui.getSelectExcelFileSer().setVisible(false);
        String command = evt.getActionCommand();
        if (command.equals(JFileChooser.APPROVE_SELECTION)) {
            GuiUtils.prepareTables(semGui);
            final String fileName = semGui.getExcelCorpusChooser().getSelectedFile().getAbsolutePath();
            semGui.updateLastSelectFolder(fileName);
            Thread t = new Thread(() -> {
                ReadExcelToTable rtt = new ReadExcelToTable();
                if (semGui.getTableData() == null) {
                    semGui.setTableData(new ConcurrentHashMap<>());
                }
                rtt.process(fileName, semGui.getDP(), semGui.getFilesTable(), semGui.getFilesInfoLabel(), semGui.getTableData());
            });
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * Gestisce il reset (ripristino) dell'interfaccia
     *
     * @param evt evento
     * @param semGui frame
     * @throws HeadlessException eccezione
     */
    public static void resetSegmentationsActionManagement(ActionEvent evt, SemGui semGui) throws HeadlessException {
        if (semGui.isIsClassify()) {
            return;
        }
        int dialogResult;
        if (evt == null) {
            dialogResult = JOptionPane.YES_OPTION;
        } else {
            int dialogButton = JOptionPane.YES_NO_OPTION;
            dialogResult = JOptionPane.showConfirmDialog(null, "Confermi la pulizia completa del lavoro?", "Warning", dialogButton);
        }
        if (dialogResult == JOptionPane.YES_OPTION) {
            GuiUtils.clearTable(semGui.getFilesTable());
            GuiUtils.clearTable(semGui.getSegmentsTable());
            GuiUtils.clearTable(semGui.getFilesPanleCapturesTable());
            GuiUtils.clearTable(semGui.getChangedTable());
            GuiUtils.clearTable(semGui.getCoverageTable());
            GuiUtils.clearTable(semGui.getCaptureValues());
            GuiUtils.clearTable(semGui.getCoverageDocumentsTable());
            DefaultMutableTreeNode treeNode1 = new DefaultMutableTreeNode("Nessun elemento");
            semGui.getSegmentClassificationResult().setModel(new DefaultTreeModel(treeNode1));
            semGui.getFilesPanelSegmentTree().setModel(new DefaultTreeModel(treeNode1));
            semGui.getChangedFilterTree().setModel(new DefaultTreeModel(treeNode1));
            semGui.getFileText().setText("");
            semGui.getFilesPanelHtmlFormatted().setText("");
            semGui.getFilesPanelHtml().setContentType("text/html");
            semGui.getFilesPanelHtml().setText("");
            semGui.getSegmentText().setText("");
            semGui.getImagesPanel().removeAll();
            semGui.getSegmentTokens().setText("");
            if (semGui.getTableData() != null) {
                semGui.getTableData().clear();
            }
            semGui.updateStats();
        }
        return;
    }

    /**
     * Rimuove i duplicati nella tabella dei files basandosi sul testo
     * tokenizzato
     *
     * @since 1.2
     *
     * @param semGui frame
     */
    public static void removeDuplicates(SemGui semGui) {
        if (semGui.isIsClassify()) {
            semGui.getStopTagCloud().setValue(true);
            semGui.getInterrompi().setEnabled(false);
        } else {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!semGui.isIsClassify()) {
                        semGui.getStopTagCloud().setValue(false);
                        semGui.getInterrompi().setEnabled(true);
                        semGui.setIsClassify(true);
                        semGui.resetFilesFilters();
                        ChangedUtils.prepareChanged(semGui);
                        JTable filesTable = semGui.getFilesTable();
                        JTable segTable = semGui.getSegmentsTable();
                        DefaultTableModel model = (DefaultTableModel) filesTable.getModel();
                        DefaultTableModel segModel = (DefaultTableModel) segTable.getModel();
                        int count = model.getRowCount();
                        Set<String> texts = new HashSet<>();
                        Set<String> segToDelete = new HashSet<>();
                        Set<String> fileToDelete = new HashSet<>();
                        for (int i = count - 1; i >= 0; i--) {
                            int pos = filesTable.convertRowIndexToModel(i);
                            Integer id = (Integer) filesTable.getValueAt(pos, 0);
                            SemDocument dto = semGui.getTableData().get(id);
                            if (dto == null) {
                                continue;
                            }
                            String text = String.valueOf(dto.getRow()[8]);
                            if (text == null) {
                                text = "";
                            }
                            text = semGui.getME().tokenize(text, String.valueOf(dto.getRow()[2]), -1);
                            if (!texts.contains(text)) {
                                texts.add(text);
                            } else {
                                List<Object[]> sr = dto.getSegmentRows();
                                for (Object[] s : sr) {
                                    segToDelete.add((String) s[0]);
                                }
                                semGui.getTableData().remove(id);
                                model.removeRow(pos);
                            }
                        }
                        int rc = segModel.getRowCount();
                        for (int i = rc - 1; i >= 0; i--) {
                            if (segToDelete.contains(segModel.getValueAt(i, 0))) {
                                segModel.removeRow(i);
                            }
                        }
                        semGui.updateStats();
                        LogGui.info("Terminated...");
                        semGui.getFilesInfoLabel().setText("Fine");
                        semGui.setIsClassify(false);
                        semGui.getInterrompi().setEnabled(false);
                    }
                }
            });
            t.setDaemon(true);
            t.start();
        }
    }
}
