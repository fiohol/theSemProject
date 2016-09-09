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

import org.thesemproject.opensem.classification.ClassificationPath;
import org.thesemproject.opensem.gui.LogGui;
import org.thesemproject.opensem.gui.SemGui;
import org.thesemproject.opensem.gui.modelEditor.CaptureTreeNode;
import org.thesemproject.opensem.gui.modelEditor.ModelTreeNode;
import org.thesemproject.opensem.segmentation.SegmentEngine;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import org.jdom2.Document;

/**
 *
 * Utility per la gestione degli eventi grafici legati ai patterns
 */
public class PatternsUtils {

    /**
     * Testa se il pattern è valido
     *
     * @param value pattern
     * @param showPositive true se vogliamo che venga mostrato un dialog in caso
     * di successo (la procedura può essere usata sia on line che batch)
     * @param semGui frame
     * @return true se il test del pattern ha dato esito positivo
     */
    public static boolean testPattern(String value, boolean showPositive, SemGui semGui) {
        return testPattern(value, showPositive, null, semGui);
    }

    /**
     * Testa se un pattern è valido e se matcha la stringa passata
     *
     * @param value pattern
     * @param showPositive true se vogliamo che venga mostrato un dialog in caso
     * di successo (la procedura può essere usata sia on line che batch)
     * @param testMatch stringa da testare
     * @param semGui frame
     * @return true se il test del pattern ha dato esito positivo
     */
    public static boolean testPattern(String value, boolean showPositive, String testMatch, SemGui semGui) {
        return testPattern(value, showPositive, testMatch, -1, "", semGui);
    }

    /**
     * Testa se un pattern è valido se matcha la stringa passata e se è in grado
     * di catturare correttamente
     *
     * @param value valore del pattern
     * @param showPositive true se vogliamo che venga mostrato un dialog in caso
     * di successo (la procedura può essere usata sia on line che batch)
     * @param testMatch stringa da testare
     * @param pos posizione della cattura
     * @param fixed normalizzazione del valore catturato
     * @param semGui frame
     * @return true se il test del pattern ha dato esito positivo
     */
    public static boolean testPattern(String value, boolean showPositive, String testMatch, int pos, String fixed, SemGui semGui) {
        try {
            if (value.trim().length() == 0) {
                GuiUtils.showErrorDialog("Pattern vuoto!", "Esito negativo");
                return false;
            }
            SegmentEngine seTest = new SegmentEngine();
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) semGui.getModelTree().getModel().getRoot();
            Document doc = semGui.getModelEditor().getXml(root);
            seTest.init(doc, semGui.getPercorsoIndice().getText());
            Pattern p = seTest.getPattern(value);
            if (p == null) {
                GuiUtils.showErrorDialog("Pattern " + value + " errato", "Esito negativo");
                return false;
            }
            if (testMatch != null) {
                Matcher match = p.matcher(testMatch);
                if (match.find()) {
                    if (pos == -1) {
                        if (showPositive) {
                            GuiUtils.showDialog("Il pattern matcha il testo!", "Esito positivo");
                        }
                        return true;
                    } else {
                        String vx = match.group(pos);
                        if (fixed.length() > 0) {
                            vx = fixed;
                        }
                        if (showPositive) {
                            GuiUtils.showDialog("Risultato del match: " + vx, "Esito positivo");
                        }
                    }
                } else {
                    GuiUtils.showErrorDialog("Il pattern non matcha il testo!", "Esito negativo");
                    return false;
                }
            } else if (showPositive) {
                GuiUtils.showDialog("Pattern corretto!", "Esito positivo");
            }
            return true;
        } catch (Exception e) {
            GuiUtils.showErrorDialog("Pattern errato: " + e.getLocalizedMessage(), "Esito negativo");
        }
        return false;
    }

    /**
     * Gestisce gli eventi sui pattern di classificazione
     *
     * @param evt evento
     * @param semGui frame
     */
    public static void classifyPattern(ActionEvent evt, SemGui semGui) {
        semGui.getCatClass().setText("");
        semGui.getjTextField2().setText("");
        semGui.getClassificationTree1().setSelectionRow(0);
        ModelTreeNode node = semGui.getModelEditor().getCurrentNode();
        if (node != null) {
            if (semGui.getModelEditor().isCaptureChild(node)) {
                CaptureTreeNode ctn = (CaptureTreeNode) node;
                ClassificationPath cp = ctn.getClassificationPath();
                if (cp != null) {
                    String st = cp.toSmallClassString();
                    semGui.getCatClass().setText(st);
                    int pos = st.lastIndexOf(">");
                    if (pos != -1) {
                        semGui.getjTextField2().setText(st.substring(pos + 1));
                        semGui.classTree1Find();
                    }
                }
            }
        }
        semGui.getCaptureClassificationRelationship().setVisible(true);
    }

    /**
     * Gestisce l'aggiunta di una cattura da bloccare
     *
     * @since 1.4
     * 
     * @param evt evento
     * @param semGui gui
     */
    public static void addBlockCapture(ActionEvent evt, SemGui semGui) {
        ModelTreeNode node = semGui.getModelEditor().getCurrentNode();
        if (node != null) {
            if (semGui.getModelEditor().isCaptureChild(node)) {
                CaptureTreeNode ctn = (CaptureTreeNode) node;
                DefaultTableModel model = (DefaultTableModel) semGui.getBlockedTable().getModel();
                List<String> cx = semGui.getModelEditor().getSegmentsCaptures((DefaultMutableTreeNode) semGui.getModelTree().getModel().getRoot(), null);
                cx.removeAll(ctn.getBlockedCaptures()); //Rimuove quelle già usate...
                Object[] capturesList = cx.toArray();
                String capture = (String) GuiUtils.showChoiceDIalog("Selezionare la cattura da bloccare", "Selezionare cattura", capturesList);
                if (capture != null) {
                    Object[] row = {capture};
                    model.addRow(row);
                    ctn.addBlockedCapture(capture);
                }
            }
        }
    }

    /**
     * Gestice la rimozione di una cattura da bloccare
     *
     * @since 1.4
     * 
     * @param evt evento
     * @param semGui gui
     */
    public static void removeBlockedCapture(ActionEvent evt, SemGui semGui) {
        ModelTreeNode node = semGui.getModelEditor().getCurrentNode();
        if (node != null) {
            if (semGui.getModelEditor().isCaptureChild(node)) {
                CaptureTreeNode ctn = (CaptureTreeNode) node;
                int sel = semGui.getBlockedTable().getSelectedRow();
                if (sel != -1) {
                    String capture = (String) semGui.getBlockedTable().getValueAt(sel, 0);
                    if (capture != null) {
                        ctn.removeBlockedCapture(capture);
                        DefaultTableModel model = (DefaultTableModel) semGui.getBlockedTable().getModel();
                        model.removeRow(sel);
                    }
                }
            }
        }
    }

    /**
     * Esporta i pattern su file csv separato da tab
     *
     * @param evt evento
     * @param semGui frame
     */
    public static void exportPatterns(ActionEvent evt, SemGui semGui) {
        semGui.getSelectExportPatterns().setVisible(false);
        String command = evt.getActionCommand();
        if (command.equals(JFileChooser.APPROVE_SELECTION)) {
            File file = semGui.getExpotPatternsFileChooser().getSelectedFile();
            String path = file.getAbsolutePath();
            semGui.updateLastSelectFolder(path);
            CaptureTreeNode node = (CaptureTreeNode) semGui.getModelEditor().getCurrentNode();
            if (node != null) {
                try {
                    if (!path.endsWith(".csv")) {
                        path = path + ".csv";
                    }
                    List<String> lines = new ArrayList<>();
                    node.getPatterns().stream().map((String[] row) -> {
                        StringBuffer raf = new StringBuffer();
                        for (int i = 1; i < row.length; i++) {
                            raf.append(row[i]).append("\t");
                        }
                        return raf;
                    }).map((StringBuffer raf) -> {
                        raf.append("\r\n");
                        return raf;
                    }).forEach((StringBuffer raf) -> {
                        lines.add(raf.toString());
                    });
                    GuiUtils.writeCSV(file.getAbsolutePath(), lines);
                } catch (Exception e) {
                    LogGui.printException(e);
                }
            }
        }
    }

    /**
     * Gestisce l'importazione di pattern da file
     *
     * @param evt evento
     * @param semGui frame
     */
    public static void importPatterns(ActionEvent evt, SemGui semGui) {
        semGui.getSelectImportPatterns().setVisible(false);
        String command = evt.getActionCommand();
        if (command.equals(JFileChooser.APPROVE_SELECTION)) {
            File file = semGui.getImportPatternsFileChooser().getSelectedFile();
            semGui.updateLastSelectFolder(file.getAbsolutePath());
            CaptureTreeNode node = (CaptureTreeNode) semGui.getModelEditor().getCurrentNode();
            if (node != null) {
                node.resetPatterns();
                try {
                    GuiUtils.readFileLines(file.getAbsolutePath(), new GuiUtils.LineFilter() {
                        public void applyTo(String row) {
                            String[] fields = row.split("\t");
                            if (fields.length == 3) {
                                node.addPattern(Integer.parseInt(fields[1]), fields[0], fields[2]);
                            } else if (fields.length == 2) {
                                node.addPattern(Integer.parseInt(fields[1]), fields[0], "");
                            }
                        }
                    });
                } catch (Exception e) {
                    LogGui.printException(e);
                }
                CapturesUtils.populateCaptureSplit(node, semGui);
            }
        }
    }
}
